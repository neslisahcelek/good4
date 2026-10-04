import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import sharp from "sharp";
import { Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb } from "./firebase.js";
import {
  acceptMarketTermsService,
  blockMarketUserService,
  createMarketListingService,
  eraseMarketData,
  getMarketFeedService,
  getMarketListingService,
  getMarketMessagesService,
  listMarketConversationsService,
  listMarketModerationQueueService,
  listMyMarketListingsService,
  MARKET_LIMITS,
  MARKET_TERMS_VERSION,
  marketDayKey,
  markMarketConversationReadService,
  type MarketDeps,
  reportMarketContentService,
  resolveMarketReportService,
  respondMarketOfferService,
  reviewMarketListingService,
  sendMarketMessageService,
  updateMarketListingStatusService,
} from "./market.js";
import { checkMarketText, containsPhoneNumber } from "./marketModeration.js";
import {
  cleanupMarketDataService, MARKET_LISTING_LIFETIME, renewMarketListingService, listMarketBlockedService, listMarketFavoritesService, publicName, searchTokensFor,
  setMarketFavoriteService, unblockMarketUserService, updateMarketListingPriceService,
} from "./market.js";

const seller = "seller1";
const buyer = "buyer1";
const admin = "admin1";
const NOW = Date.UTC(2026, 9, 3, 9, 0, 0);

let saved: string[];
let deleted: string[];
let notified: { uid: string; title: string; data?: Record<string, string> }[];
let deps: MarketDeps;

beforeEach(async () => {
  await Promise.all([
    "users", "marketListings", "marketConversations", "marketUserState", "marketReports",
    "marketViolations", "mail", "auditLogs", "app_config", "campusEmailVerifications", "marketPhotoDeletions",
  ].map((collection) => db.recursiveDelete(db.collection(collection))));
  saved = [];
  deleted = [];
  notified = [];
  deps = {
    photos: {
      async save(objectName, bytes) {
        saved.push(objectName);
        const metadata = await sharp(bytes).metadata();
        assert.equal(metadata.exif, undefined, "photos must be stored without EXIF");
        assert.ok((metadata.width ?? 0) <= 1280 && (metadata.height ?? 0) <= 1280);
        return `https://example.test/${objectName}`;
      },
      async deletePrefix(prefix) {
        deleted.push(prefix);
      },
    },
    notify: async (uid, payload) => {
      notified.push({ uid, title: payload.title, data: payload.data });
    },
    now: () => NOW,
  };
  await Promise.all([
    student(seller, "Ayşe Yılmaz", "ayse@ogr.akdeniz.edu.tr"),
    student(buyer, "Mehmet Kaya", "mehmet@ogr.akdeniz.edu.tr"),
    db.doc(`users/${admin}`).set({ role: "good4Admin", status: "active", email: "admin@good4.test" }),
  ]);
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

async function student(uid: string, displayName: string, eduEmail: string | null, terms = true) {
  await db.doc(`users/${uid}`).set({
    role: "student", status: "active", displayName,
    ...(eduEmail ? { eduEmail, eduVerified: true } : {}),
  });
  if (terms) await db.doc(`marketUserState/${uid}`).set({ termsVersion: MARKET_TERMS_VERSION });
}

let photoCache: string | null = null;
async function photo(): Promise<string> {
  photoCache ??= (await sharp({ create: { width: 2400, height: 1800, channels: 3, background: "#3a7" } })
    .withMetadata({ exif: { IFD0: { Copyright: "camera" } } })
    .jpeg()
    .toBuffer()).toString("base64");
  return photoCache;
}

async function listingInput(overrides: Record<string, unknown> = {}) {
  return {
    category: "clothing",
    condition: "good",
    title: "Kışlık mont",
    description: "Bir sezon giyildi, temiz ve sağlam.",
    price: 1000,
    photos: [await photo()],
    ...overrides,
  };
}

async function publishedListing(): Promise<string> {
  const { listingId } = await createMarketListingService(db, seller, await listingInput(), deps);
  await reviewMarketListingService(db, admin, { listingId, decision: "approve" }, deps);
  return listingId;
}

async function rejectsWith(promise: Promise<unknown>, message: string) {
  await assert.rejects(promise, (error: Error) => {
    assert.equal(error.message, message);
    return true;
  });
}

test("the text filter blocks banned items and common spellings but leaves everyday words alone", () => {
  for (const text of [
    "Kutusu açılmamış sigara", "s.i.g.a.r.a var", "S1G4RA", "sigaaaara", "biralar soğuk",
    "rakıyı getiririm", "elektronik sigara", "Xanax", "esrar", "kaçak karton", "Marlboro",
  ]) {
    assert.ok(checkMarketText(text).blockedTerm, `should block: ${text}`);
  }
  for (const text of [
    "biraz kullanıldı", "rakip ürünlerden ucuz", "mor fincan seti", "puffer mont", "likit fondöten",
    "reçeteli gözlük çerçevesi", "silikon tabancası", "kenevir çanta", "masası garajda duruyor",
    "otobüs durağında buluşalım", "karton kutusuyla", "150 TL", "AirPods Pro kulaklık",
  ]) {
    assert.equal(checkMarketText(text).blockedTerm, null, `should allow: ${text}`);
  }
  assert.deepEqual(checkMarketText("likit fondöten").flaggedTerms, ["likit"]);
  assert.ok(containsPhoneNumber("Ulaşın: 0532 123 45 67"));
  assert.ok(containsPhoneNumber("+90 (532) 123-45-67"));
  assert.ok(!containsPhoneNumber("Fiyat 1500 TL, 2 adet"));
});

test("selling needs the exact Akdeniz student domain and the accepted rules", async () => {
  await student("gmail-only", "Ali Veli", null, true);
  await rejectsWith(createMarketListingService(db, "gmail-only", await listingInput(), deps), "MARKET_EDU_REQUIRED");

  for (const email of ["ali@akdeniz.edu.tr", "ali@ogr.ege.edu.tr", "ali@sub.ogr.akdeniz.edu.tr"]) {
    await student("wrong-domain", "Ali Veli", email, true);
    await rejectsWith(createMarketListingService(db, "wrong-domain", await listingInput(), deps), "MARKET_EDU_REQUIRED");
    assert.equal((await getMarketFeedService(db, "wrong-domain", {}, deps)).me.eduVerified, false);
  }
  await student("no-terms", "Zeynep Ak", "zeynep@ogr.akdeniz.edu.tr", false);
  await rejectsWith(createMarketListingService(db, "no-terms", await listingInput(), deps), "MARKET_TERMS_REQUIRED");
  await acceptMarketTermsService(db, "no-terms", { version: MARKET_TERMS_VERSION }, deps);
  const created = await createMarketListingService(db, "no-terms", await listingInput(), deps);
  assert.equal(created.status, "pending");
});

test("a new listing is stored as pending with re-encoded photos and the admins are told", async () => {
  const { listingId } = await createMarketListingService(db, seller,
    await listingInput({ photos: [await photo(), await photo()] }), deps);
  const listing = await db.doc(`marketListings/${listingId}`).get();
  assert.equal(listing.get("status"), "pending");
  assert.equal(listing.get("sellerName"), "A.. Y..");
  assert.equal(listing.get("eduDomain"), "akdeniz.edu.tr");
  assert.equal(listing.get("universityName"), "Akdeniz Üniversitesi");
  assert.equal(listing.get("photos").length, 2);
  assert.equal(saved.length, 4, "a full photo and a thumbnail per upload");
  assert.ok(saved.every((name) => name.startsWith(`market-listings/${listingId}/`)));
  const mail = await db.collection("mail").get();
  assert.deepEqual(mail.docs[0]?.get("to"), ["admin@good4.test"]);
  assert.deepEqual(notified.map((entry) => entry.uid), [admin]);
});

test("pending listings are hidden until an admin approves or rejects them", async () => {
  const { listingId } = await createMarketListingService(db, seller, await listingInput(), deps);
  await rejectsWith(reviewMarketListingService(db, buyer, { listingId, decision: "approve" }, deps), "ROLE_NOT_ALLOWED");
  const queue = await listMarketModerationQueueService(db, admin);
  assert.deepEqual(queue.pendingListings.map((listing) => listing.id), [listingId]);

  await reviewMarketListingService(db, admin, { listingId, decision: "reject", reason: "Fotoğraf net değil" }, deps);
  const rejected = await db.doc(`marketListings/${listingId}`).get();
  assert.equal(rejected.get("status"), "rejected");
  assert.equal(rejected.get("rejectReason"), "Fotoğraf net değil");
  assert.deepEqual(rejected.get("photos"), []);
  assert.deepEqual(deleted, [`market-listings/${listingId}/`]);
  assert.equal(notified.at(-1)?.uid, seller);
});

test("banned items are rejected, counted, and three attempts suspend the account for a day", async () => {
  for (let attempt = 1; attempt <= 2; attempt += 1) {
    await rejectsWith(createMarketListingService(db, seller,
      await listingInput({ title: "Karton sigara" }), deps), "MARKET_CONTENT_BLOCKED");
  }
  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba, duruyor mu?" }, deps);
  await rejectsWith(sendMarketMessageService(db, seller, { conversationId, text: "bira da veririm" }, deps),
    "MARKET_CONTENT_BLOCKED_SUSPENDED");
  await rejectsWith(sendMarketMessageService(db, seller, { conversationId, text: "Duruyor" }, deps),
    "MARKET_SUSPENDED");
  assert.equal((await db.collection("marketViolations").get()).size, 3);
  assert.equal(saved.length, 2, "blocked listings never upload photos");
});

test("phone numbers are refused in public listings but allowed in messages", async () => {
  await rejectsWith(createMarketListingService(db, seller,
    await listingInput({ description: "Detay için 0532 123 45 67 arayın" }), deps), "MARKET_PHONE_IN_LISTING");
  const listingId = await publishedListing();
  await sendMarketMessageService(db, buyer,
    { conversationId: `${listingId}_${buyer}`, text: "Numaram 0532 123 45 67, yazabilirsin" }, deps);
});

test("a seller keeps at most fifteen active listings", async () => {
  await Promise.all(Array.from({ length: MARKET_LIMITS.maxActiveListings }, (_, index) => db.doc(`marketListings/old${index}`).set({
    sellerUid: seller, status: index % 2 ? "published" : "pending",
  })));
  await rejectsWith(createMarketListingService(db, seller, await listingInput(), deps), "MARKET_ACTIVE_LISTING_LIMIT");
  await db.doc("marketListings/old0").update({ status: "sold" });
  await createMarketListingService(db, seller, await listingInput(), deps);
});

test("buyer and seller message each other with unread counters and a price offer", async () => {
  const listingId = await publishedListing();
  let clock = NOW;
  deps.now = () => (clock += 1000);
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba, hâlâ satılık mı?" }, deps);
  await sendMarketMessageService(db, buyer, { conversationId, offerPercent: 15 }, deps);
  await rejectsWith(sendMarketMessageService(db, buyer, { conversationId, offerPercent: 20 }, deps),
    "MARKET_OFFER_PENDING");

  let conversation = await db.doc(`marketConversations/${conversationId}`).get();
  assert.deepEqual(conversation.get("participants"), [seller, buyer]);
  assert.equal(conversation.get("messageCount"), 2);
  assert.equal(conversation.get(`unread.${seller}`), 2);
  assert.equal(conversation.get("offer.price"), 850);
  assert.equal((await db.doc(`marketUserState/${seller}`).get()).get("unreadCount"), 2);
  assert.equal(notified.filter((entry) => entry.uid === seller && entry.data?.type === "market_message").length, 2);

  await rejectsWith(respondMarketOfferService(db, buyer, { conversationId, accept: true }, deps),
    "MARKET_OFFER_SELLER_ONLY");
  await respondMarketOfferService(db, seller, { conversationId, accept: true }, deps);
  assert.deepEqual(await markMarketConversationReadService(db, seller, { conversationId }), { unreadCount: 0 });

  conversation = await db.doc(`marketConversations/${conversationId}`).get();
  assert.equal(conversation.get("offer.status"), "accepted");
  assert.equal(conversation.get(`unread.${seller}`), 0);
  assert.equal(conversation.get(`unread.${buyer}`), 1);
  const messages = await db.collection(`marketConversations/${conversationId}/messages`).orderBy("createdAt").get();
  assert.deepEqual(messages.docs.map((message) => message.get("type")), ["text", "offer", "offerResponse"]);
});

test("conversations are limited to the listing's campus and participants, not to a lifetime message count", async () => {
  const listingId = await publishedListing();
  await student("othercampus", "Elif Su", "elif@ogr.ege.edu.tr");
  await rejectsWith(sendMarketMessageService(db, "othercampus",
    { conversationId: `${listingId}_othercampus`, text: "Merhaba" }, deps), "MARKET_EDU_REQUIRED");
  await rejectsWith(sendMarketMessageService(db, buyer,
    { conversationId: `${listingId}_${seller}`, text: "Merhaba" }, deps), "MARKET_NOT_PARTICIPANT");
  await rejectsWith(sendMarketMessageService(db, seller,
    { conversationId: `${listingId}_${seller}`, text: "Merhaba" }, deps), "MARKET_NOT_PARTICIPANT");

  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba" }, deps);
  await student("stranger2", "Can Er", "can@ogr.akdeniz.edu.tr");
  await rejectsWith(sendMarketMessageService(db, "stranger2", { conversationId, text: "Ben de" }, deps),
    "MARKET_NOT_PARTICIPANT");
  // The old 100-message lifetime cap is gone; the per-listing daily quota is the limit.
  await db.doc(`marketConversations/${conversationId}`).update({ messageCount: 150 });
  await sendMarketMessageService(db, seller, { conversationId, text: "Selam" }, deps);
  assert.equal((await db.doc(`marketConversations/${conversationId}`).get()).get("messageCount"), 151);
});

const today = marketDayKey(NOW);
const LIMIT = MARKET_LIMITS.maxMessagesPerListingPerDay;

test("each sender gets 200 messages per listing per day, reset the next day", async () => {
  assert.equal(LIMIT, 200);
  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba" }, deps);
  // A high all-listings total no longer blocks; only this listing's count does.
  await db.doc(`marketUserState/${buyer}`).update({ messagesToday: 5000, [`messagesByListing.${listingId}`]: LIMIT - 1 });
  await sendMarketMessageService(db, buyer, { conversationId, text: "Son mesajım" }, deps);
  await rejectsWith(sendMarketMessageService(db, buyer, { conversationId, text: "Bir tane daha" }, deps),
    "MARKET_DAILY_MESSAGE_LIMIT");
  await rejectsWith(sendMarketMessageService(db, buyer, { conversationId, offerPercent: 10 }, deps),
    "MARKET_DAILY_MESSAGE_LIMIT");
  // The other participant has a separate quota for the same listing.
  await sendMarketMessageService(db, seller, { conversationId, text: "Cevap yazabilirim" }, deps);

  deps.now = () => NOW + 24 * 60 * 60 * 1000;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Yeni gün" }, deps);
  const state = await db.doc(`marketUserState/${buyer}`).get();
  assert.equal(state.get("dayKey"), marketDayKey(NOW + 24 * 60 * 60 * 1000));
  assert.deepEqual(state.get("messagesByListing"), { [listingId]: 1 }, "yesterday's counts are dropped");
});

test("listings do not share quotas: five listings allow 1000 messages a day", async () => {
  const listingIds: string[] = [];
  for (let index = 0; index < 5; index += 1) listingIds.push(await publishedListing());
  for (const listingId of listingIds) {
    await sendMarketMessageService(db, buyer, { conversationId: `${listingId}_${buyer}`, text: "Merhaba" }, deps);
  }
  await db.doc(`marketUserState/${buyer}`).update(Object.fromEntries(
    listingIds.map((listingId) => [`messagesByListing.${listingId}`, LIMIT - 1])));

  // Exhausting the first listing leaves the other four untouched.
  await sendMarketMessageService(db, buyer, { conversationId: `${listingIds[0]}_${buyer}`, text: "200." }, deps);
  await rejectsWith(sendMarketMessageService(db, buyer,
    { conversationId: `${listingIds[0]}_${buyer}`, text: "201." }, deps), "MARKET_DAILY_MESSAGE_LIMIT");
  for (const listingId of listingIds.slice(1)) {
    await sendMarketMessageService(db, buyer, { conversationId: `${listingId}_${buyer}`, text: "200." }, deps);
  }
  const counts = (await db.doc(`marketUserState/${buyer}`).get()).get("messagesByListing") as Record<string, number>;
  assert.deepEqual(listingIds.map((listingId) => counts[listingId]), [LIMIT, LIMIT, LIMIT, LIMIT, LIMIT]);
  assert.equal(Object.values(counts).reduce((sum, count) => sum + count, 0), 5 * LIMIT);
  for (const listingId of listingIds) {
    await rejectsWith(sendMarketMessageService(db, buyer,
      { conversationId: `${listingId}_${buyer}`, text: "fazla" }, deps), "MARKET_DAILY_MESSAGE_LIMIT");
  }
});

test("concurrent sends cannot pass the per-listing daily limit", async () => {
  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba" }, deps);
  await db.doc(`marketUserState/${buyer}`).update({ [`messagesByListing.${listingId}`]: LIMIT - 3 });
  const before = (await db.collection(`marketConversations/${conversationId}/messages`).get()).size;

  const results = await Promise.allSettled(Array.from({ length: 8 }, (_, index) =>
    sendMarketMessageService(db, buyer, { conversationId, text: `Aynı anda ${index}` }, deps)));
  const fulfilled = results.filter((result) => result.status === "fulfilled").length;
  const rejected = results.filter((result): result is PromiseRejectedResult => result.status === "rejected");
  assert.equal(fulfilled, 3);
  assert.ok(rejected.every((result) => (result.reason as Error).message === "MARKET_DAILY_MESSAGE_LIMIT"),
    rejected.map((result) => (result.reason as Error).message).join(", "));
  const state = await db.doc(`marketUserState/${buyer}`).get();
  assert.equal((state.get("messagesByListing") as Record<string, number>)[listingId], LIMIT);
  assert.equal((await db.collection(`marketConversations/${conversationId}/messages`).get()).size, before + 3);
  assert.equal(state.get("dayKey"), today);
});

test("blocking stops a conversation in both directions", async () => {
  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba" }, deps);
  await blockMarketUserService(db, seller, { conversationId });
  await rejectsWith(sendMarketMessageService(db, buyer, { conversationId, text: "Cevap ver" }, deps), "MARKET_BLOCKED");
  assert.deepEqual((await db.doc(`marketUserState/${seller}`).get()).get("blockedUids"), [buyer]);
});

test("reports reach the admin, who can remove the listing or suspend the seller", async () => {
  const listingId = await publishedListing();
  const { reportId } = await reportMarketContentService(db, buyer,
    { targetType: "listing", targetId: listingId, reason: "prohibited" }, deps);
  await rejectsWith(reportMarketContentService(db, seller,
    { targetType: "listing", targetId: listingId, reason: "scam" }, deps), "MARKET_REPORT_SELF");

  const queue = await listMarketModerationQueueService(db, admin);
  assert.equal(queue.reports[0]?.id, reportId);
  assert.equal(queue.reports[0]?.listing?.id, listingId);

  await resolveMarketReportService(db, admin, { reportId, action: "suspendUser", suspendDays: 7 }, deps);
  const state = await db.doc(`marketUserState/${seller}`).get();
  assert.equal((state.get("suspendedUntil") as Timestamp).toMillis(), NOW + 7 * 24 * 60 * 60 * 1000);
  assert.equal((await db.doc(`marketReports/${reportId}`).get()).get("status"), "resolved");
});

test("sellers mark listings reserved, sold or remove them", async () => {
  const listingId = await publishedListing();
  await updateMarketListingStatusService(db, seller, { listingId, action: "markReserved" }, deps);
  await updateMarketListingStatusService(db, seller, { listingId, action: "markSold" }, deps);
  await rejectsWith(updateMarketListingStatusService(db, buyer, { listingId, action: "remove" }, deps),
    "MARKET_LISTING_NOT_FOUND");
  await updateMarketListingStatusService(db, seller, { listingId, action: "remove" }, deps);
  assert.equal((await db.doc(`marketListings/${listingId}`).get()).get("status"), "removed");
  assert.deepEqual(deleted, [`market-listings/${listingId}/`]);
});

test("account deletion removes listings, conversations and market state", async () => {
  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba" }, deps);
  await sendMarketMessageService(db, seller, { conversationId, text: "Selam" }, deps);
  assert.equal((await db.doc(`marketUserState/${buyer}`).get()).get("unreadCount"), 1);

  const prefixes: string[] = [];
  await eraseMarketData(db, seller, async (prefix) => { prefixes.push(prefix); });
  assert.equal((await db.doc(`marketListings/${listingId}`).get()).exists, false);
  assert.equal((await db.doc(`marketConversations/${conversationId}`).get()).exists, false);
  assert.equal((await db.collection(`marketConversations/${conversationId}/messages`).get()).size, 0);
  assert.equal((await db.doc(`marketUserState/${seller}`).get()).exists, false);
  assert.equal((await db.doc(`marketUserState/${buyer}`).get()).get("unreadCount"), 0);
  assert.deepEqual(prefixes, [`market-listings/${listingId}/`]);
});

test("the feed shows the student's own campus, hides blocked sellers and pages by publish time", async () => {
  const listingId = await publishedListing();
  await db.doc("marketListings/ege1").set({
    sellerUid: "egeSeller", status: "published", eduDomain: "ege.edu.tr", category: "books", title: "Ege kitabı",
    price: 50, photos: [], publishedAt: Timestamp.fromMillis(NOW + 1000), createdAt: Timestamp.fromMillis(NOW),
  });
  const feed = await getMarketFeedService(db, buyer, {}, deps);
  assert.deepEqual(feed.listings.map((listing) => listing.id), [listingId]);
  assert.equal(feed.me.eduVerified, true);
  assert.equal(feed.me.universityName, "Akdeniz Üniversitesi");
  assert.equal(feed.nextBefore, null);
  assert.deepEqual((await getMarketFeedService(db, buyer, { category: "books" }, deps)).listings, []);

  await student("browser", "Gezgin Öğrenci", null);
  const browsing = await getMarketFeedService(db, "browser", {}, deps);
  assert.deepEqual(browsing.listings.map((listing) => listing.id), ["ege1", listingId]);
  assert.equal(browsing.me.eduVerified, false);

  await db.doc(`marketUserState/${buyer}`).set({ blockedUids: [seller] }, { merge: true });
  assert.deepEqual((await getMarketFeedService(db, buyer, {}, deps)).listings, []);
});

test("listing details, my listings and the inbox show only what the caller may see", async () => {
  const { listingId: pendingId } = await createMarketListingService(db, seller, await listingInput(), deps);
  await rejectsWith(getMarketListingService(db, buyer, { listingId: pendingId }, deps), "MARKET_LISTING_NOT_FOUND");
  const mine = await getMarketListingService(db, seller, { listingId: pendingId }, deps);
  assert.equal(mine.listing.isMine, true);
  assert.equal(mine.listing.status, "pending");
  assert.deepEqual((await listMyMarketListingsService(db, seller)).listings.map((listing) => listing.id), [pendingId]);

  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  const detail = await getMarketListingService(db, buyer, { listingId }, deps);
  assert.equal(detail.listing.description, "Bir sezon giyildi, temiz ve sağlam.");
  assert.equal(detail.conversationId, null);
  assert.equal(detail.sameCampus, true);
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba" }, deps);
  assert.equal((await getMarketListingService(db, buyer, { listingId }, deps)).conversationId, conversationId);

  const inbox = await listMarketConversationsService(db, seller);
  assert.equal(inbox.unreadCount, 1);
  assert.equal(inbox.conversations[0]?.role, "seller");
  assert.equal(inbox.conversations[0]?.otherName, "M.. K..");
  assert.equal(inbox.conversations[0]?.unread, 1);
  await rejectsWith(listMarketConversationsService(db, "stranger"), "ACCOUNT_NOT_ACTIVE");
  await student("stranger", "Öğrenci", null);
  assert.deepEqual((await listMarketConversationsService(db, "stranger")).conversations, []);
});

test("polling messages returns only newer ones and clears the reader's unread count", async () => {
  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  let clock = NOW;
  deps.now = () => (clock += 1000);
  await sendMarketMessageService(db, buyer, { conversationId, text: "Bir" }, deps);
  await sendMarketMessageService(db, buyer, { conversationId, text: "İki" }, deps);

  const first = await getMarketMessagesService(db, seller, { conversationId });
  assert.deepEqual(first.messages.map((message) => [message.text, message.mine]), [["Bir", false], ["İki", false]]);
  assert.equal((await db.doc(`marketUserState/${seller}`).get()).get("unreadCount"), 0);

  await sendMarketMessageService(db, seller, { conversationId, text: "Üç" }, deps);
  const newer = await getMarketMessagesService(db, buyer, { conversationId, after: first.messages.at(-1)?.createdAt });
  assert.deepEqual(newer.messages.map((message) => [message.text, message.mine]), [["Üç", false]]);
  await student("stranger", "Öğrenci", null);
  await rejectsWith(getMarketMessagesService(db, "stranger", { conversationId }), "MARKET_NOT_PARTICIPANT");
});

test("students see each other only as initials, including names stored in the old format", async () => {
  assert.equal(publicName("Ayşe Yılmaz"), "A.. Y..");
  assert.equal(publicName("  ışıl  nur   öztürk "), "I.. Ö..", "first and last name, Turkish upper case");
  assert.equal(publicName("Can"), "C..");
  assert.equal(publicName(""), "Öğrenci");
  assert.equal(publicName(undefined), "Öğrenci");
  assert.equal(publicName("Öğrenci"), "Öğrenci");
  assert.equal(publicName("Ayşe Y."), "A.. Y..", "legacy stored names are masked on read");
  assert.equal(publicName("A.. Y.."), "A.. Y..", "idempotent");

  const listingId = await publishedListing();
  await db.doc(`marketListings/${listingId}`).update({ sellerName: "Ayşe Y." });
  const detail = await getMarketListingService(db, buyer, { listingId }, deps);
  assert.equal(detail.listing.sellerName, "A.. Y..");
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba" }, deps);
  const stored = await db.doc(`marketConversations/${conversationId}`).get();
  assert.equal(stored.get("buyerName"), "M.. K..");
  assert.equal(stored.get("sellerName"), "A.. Y..");
  await db.doc(`marketConversations/${conversationId}`).update({ buyerName: "Mehmet Kaya" });
  assert.equal((await listMarketConversationsService(db, seller)).conversations[0]?.otherName, "M.. K..");
  assert.ok(!JSON.stringify(await getMarketFeedService(db, buyer, {}, deps)).includes("Ayşe"));
});

test("sellers can change only the price, without a new review, and conversations follow", async () => {
  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba" }, deps);
  await rejectsWith(updateMarketListingPriceService(db, buyer, { listingId, price: 1 }, deps), "MARKET_LISTING_NOT_FOUND");
  await rejectsWith(updateMarketListingPriceService(db, seller, { listingId, price: -5 }, deps), "MARKET_PRICE_INVALID");
  await rejectsWith(updateMarketListingPriceService(db, seller, { listingId, price: 12.5 }, deps), "MARKET_PRICE_INVALID");
  assert.deepEqual(await updateMarketListingPriceService(db, seller, { listingId, price: 700 }, deps), { price: 700 });
  const listing = await db.doc(`marketListings/${listingId}`).get();
  assert.equal(listing.get("price"), 700);
  assert.equal(listing.get("status"), "published", "no new review");
  assert.equal(listing.get("title"), "Kışlık mont");
  assert.equal((await db.doc(`marketConversations/${conversationId}`).get()).get("listingPrice"), 700);
  await updateMarketListingStatusService(db, seller, { listingId, action: "markSold" }, deps);
  await rejectsWith(updateMarketListingPriceService(db, seller, { listingId, price: 600 }, deps), "MARKET_LISTING_STATUS_INVALID");
});

test("search finds listings by any word or word start across all pages, ignoring Turkish letters", async () => {
  assert.deepEqual(searchTokensFor("Kulaklık"), ["ku", "kul", "kula", "kulak", "kulakl", "kulakli", "kulaklik"]);
  const titles = ["Bluetooth kulaklık", "Kışlık mont", "Calculus kitabı", "Kulaklık standı"];
  const ids: string[] = [];
  for (const [index, title] of titles.entries()) {
    const id = `search${index}`;
    ids.push(id);
    await db.doc(`marketListings/${id}`).set({
      sellerUid: "searchSeller", status: "published", eduDomain: "akdeniz.edu.tr", category: "electronics", title,
      price: 100, photos: [], searchTokens: searchTokensFor(title), publishedAt: Timestamp.fromMillis(NOW - index * 1000),
    });
  }
  // 25 newer listings push the matches past the first page of an unfiltered feed.
  for (let index = 0; index < 25; index += 1) {
    await db.doc(`marketListings/filler${index}`).set({
      sellerUid: "searchSeller", status: "published", eduDomain: "akdeniz.edu.tr", category: "other", title: `Eşya ${index}`,
      price: 1, photos: [], searchTokens: searchTokensFor(`Eşya ${index}`), publishedAt: Timestamp.fromMillis(NOW + 10_000 + index),
    });
  }
  const ids1 = (await getMarketFeedService(db, buyer, { query: "kulaklik" }, deps)).listings.map((l) => l.id);
  assert.deepEqual(ids1, ["search0", "search3"]);
  assert.deepEqual((await getMarketFeedService(db, buyer, { query: "KULAK" }, deps)).listings.map((l) => l.id), ["search0", "search3"]);
  assert.deepEqual((await getMarketFeedService(db, buyer, { query: "kulaklık stand" }, deps)).listings.map((l) => l.id), ["search3"]);
  assert.deepEqual((await getMarketFeedService(db, buyer, { query: "kışlık" }, deps)).listings.map((l) => l.id), ["search1"]);
  assert.deepEqual((await getMarketFeedService(db, buyer, { query: "kulak", category: "other" }, deps)).listings, []);
  assert.deepEqual((await getMarketFeedService(db, buyer, { query: "masa" }, deps)).listings, []);
  await rejectsWith(getMarketFeedService(db, buyer, { query: "x".repeat(61) }, deps), "MARKET_QUERY_INVALID");
  const created = await createMarketListingService(db, seller, await listingInput({ title: "Kırmızı şemsiye" }), deps);
  assert.ok(((await db.doc(`marketListings/${created.listingId}`).get()).get("searchTokens") as string[]).includes("kirmizi"));
});

test("a student can lift their own block, but not the other side's", async () => {
  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba" }, deps);
  await blockMarketUserService(db, seller, { conversationId });
  assert.deepEqual((await listMarketBlockedService(db, seller)).blocked.map((entry) => [entry.conversationId, entry.otherName]),
    [[conversationId, "M.. K.."]]);
  assert.deepEqual((await listMarketBlockedService(db, buyer)).blocked, []);
  await rejectsWith(unblockMarketUserService(db, "stranger", { conversationId }), "ACCOUNT_NOT_ACTIVE");

  // The buyer has no block of their own to lift; the seller's block stays.
  assert.equal((await unblockMarketUserService(db, buyer, { conversationId })).status, "blocked");
  await rejectsWith(sendMarketMessageService(db, buyer, { conversationId, text: "Hâlâ engelli" }, deps), "MARKET_BLOCKED");

  assert.equal((await unblockMarketUserService(db, seller, { conversationId })).status, "open");
  await sendMarketMessageService(db, buyer, { conversationId, text: "Tekrar merhaba" }, deps);
  assert.deepEqual((await db.doc(`marketUserState/${seller}`).get()).get("blockedUids"), []);
  assert.deepEqual((await listMarketBlockedService(db, seller)).blocked, []);
  assert.equal((await getMarketFeedService(db, seller, {}, deps)).listings.length >= 0, true);
});

test("students save listings to favourites; hidden or removed ones drop out", async () => {
  const listingId = await publishedListing();
  const { listingId: pendingId } = await createMarketListingService(db, seller, await listingInput(), deps);
  await rejectsWith(setMarketFavoriteService(db, buyer, { listingId: pendingId, saved: true }), "MARKET_LISTING_NOT_FOUND");
  await rejectsWith(setMarketFavoriteService(db, buyer, { listingId, saved: "yes" }), "MARKET_FAVORITE_INVALID");
  assert.deepEqual(await setMarketFavoriteService(db, buyer, { listingId, saved: true }), { saved: true });
  assert.equal((await getMarketFeedService(db, buyer, {}, deps)).listings.find((l) => l.id === listingId)?.isFavorite, true);
  assert.equal((await getMarketListingService(db, buyer, { listingId }, deps)).listing.isFavorite, true);
  assert.equal((await getMarketFeedService(db, seller, {}, deps)).listings.find((l) => l.id === listingId)?.isFavorite, false);
  assert.deepEqual((await listMarketFavoritesService(db, buyer)).listings.map((l) => l.id), [listingId]);

  await updateMarketListingStatusService(db, seller, { listingId, action: "remove" }, deps);
  assert.deepEqual((await listMarketFavoritesService(db, buyer)).listings, []);
  assert.deepEqual((await db.doc(`marketUserState/${buyer}`).get()).get("favoriteIds"), [], "vanished listings are pruned");

  const other = await publishedListing();
  await setMarketFavoriteService(db, buyer, { listingId: other, saved: true });
  await setMarketFavoriteService(db, buyer, { listingId: other, saved: false });
  assert.deepEqual((await listMarketFavoritesService(db, buyer)).listings, []);
});

test("the daily cleanup enforces the KVKK retention periods and leaves recent data", async () => {
  const day = 24 * 60 * 60 * 1000;
  const at = (days: number) => Timestamp.fromMillis(NOW - days * day);
  await Promise.all([
    db.doc("marketListings/oldRemoved").set({ status: "removed", updatedAt: at(31), photos: [] }),
    db.doc("marketListings/newRemoved").set({ status: "rejected", updatedAt: at(5), photos: [] }),
    db.doc("marketListings/soldMonth").set({ status: "sold", soldAt: at(40), photos: [{ url: "u", thumbUrl: "t" }] }),
    db.doc("marketListings/soldHalfYear").set({ status: "sold", soldAt: at(181), photos: [] }),
    db.doc("marketListings/soldRecent").set({ status: "sold", soldAt: at(3), photos: [{ url: "u", thumbUrl: "t" }] }),
    db.doc("marketListings/live").set({ status: "published", updatedAt: at(400), photos: [] }),
    db.doc("marketConversations/oldChat").set({ participants: [seller, buyer], lastMessageAt: at(366), unread: { [buyer]: 2 } }),
    db.doc("marketConversations/oldChat/messages/m1").set({ text: "eski" }),
    db.doc("marketConversations/newChat").set({ participants: [seller, buyer], lastMessageAt: at(10), unread: {} }),
    db.doc(`marketUserState/${buyer}`).set({ termsVersion: MARKET_TERMS_VERSION, unreadCount: 3 }),
    db.doc("marketViolations/old").set({ createdAt: at(366) }),
    db.doc("marketViolations/new").set({ createdAt: at(1) }),
    db.doc("marketReports/oldResolved").set({ status: "resolved", resolvedAt: at(366) }),
    db.doc("marketReports/oldOpen").set({ status: "open", createdAt: at(400) }),
    db.doc("campusEmailVerifications/oldRequest").set({ expiresAt: at(31) }),
    db.doc("campusEmailVerifications/newRequest").set({ expiresAt: at(1) }),
  ]);
  const removedPhotos: string[] = [];
  const counts = await cleanupMarketDataService(db, async (prefix) => { removedPhotos.push(prefix); }, NOW);
  assert.deepEqual(counts, {
    expired: 0, reminders: 0,
    closedListings: 1, soldPhotos: 1, soldListings: 1, conversations: 1, violations: 1, reports: 1, verifications: 1,
  });
  const exists = async (path: string) => (await db.doc(path).get()).exists;
  assert.equal(await exists("marketListings/oldRemoved"), false);
  assert.equal(await exists("marketListings/newRemoved"), true);
  assert.equal(await exists("marketListings/soldHalfYear"), false);
  assert.deepEqual((await db.doc("marketListings/soldMonth").get()).get("photos"), []);
  assert.equal((await db.doc("marketListings/soldRecent").get()).get("photos").length, 1);
  assert.equal(await exists("marketListings/live"), true);
  assert.equal(await exists("marketConversations/oldChat"), false);
  assert.equal(await exists("marketConversations/oldChat/messages/m1"), false);
  assert.equal(await exists("marketConversations/newChat"), true);
  assert.equal((await db.doc(`marketUserState/${buyer}`).get()).get("unreadCount"), 1);
  assert.equal(await exists("marketViolations/new"), true);
  assert.equal(await exists("marketReports/oldOpen"), true, "open reports wait for a decision");
  assert.equal(await exists("campusEmailVerifications/newRequest"), true);
  assert.ok(removedPhotos.includes("market-listings/soldMonth/"));
});

test("approved listings live 30 days; sellers get a reminder, can renew three times, and expired ones are cleaned up", async () => {
  const day = 24 * 60 * 60 * 1000;
  assert.equal(MARKET_LISTING_LIFETIME.days, 30);
  const listingId = await publishedListing();
  let listing = await db.doc(`marketListings/${listingId}`).get();
  assert.equal((listing.get("expiresAt") as Timestamp).toMillis(), NOW + 30 * day);
  const mine = await getMarketListingService(db, seller, { listingId }, deps);
  assert.equal((mine.listing as { renewsLeft?: number }).renewsLeft, 3);
  assert.equal((await getMarketListingService(db, buyer, { listingId }, deps)).listing.hasOwnProperty("renewsLeft"), false);

  // Day 28: a single reminder, nothing expires yet.
  notified = [];
  let counts = await cleanupMarketDataService(db, async () => undefined, NOW + 28 * day, deps.notify);
  assert.equal(counts.reminders, 1);
  assert.equal(counts.expired, 0);
  assert.equal(notified.filter((entry) => entry.uid === seller).length, 1);
  counts = await cleanupMarketDataService(db, async () => undefined, NOW + 29 * day, deps.notify);
  assert.equal(counts.reminders, 0, "reminded once per lifetime");

  // Day 31: expired and hidden from the feed, but an existing chat still works.
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba" }, deps);
  counts = await cleanupMarketDataService(db, async () => undefined, NOW + 31 * day, deps.notify);
  assert.equal(counts.expired, 1);
  assert.equal((await db.doc(`marketListings/${listingId}`).get()).get("status"), "expired");
  assert.ok(!(await getMarketFeedService(db, buyer, {}, deps)).listings.some((l) => l.id === listingId));
  deps.now = () => NOW + 31 * day;
  await sendMarketMessageService(db, seller, { conversationId, text: "Hâlâ duruyor" }, deps);
  await student("lateBuyer", "Geç Kalan", "gec@ogr.akdeniz.edu.tr");
  await rejectsWith(sendMarketMessageService(db, "lateBuyer", { conversationId: `${listingId}_lateBuyer`, text: "Satılık mı?" }, deps),
    "MARKET_LISTING_UNAVAILABLE");

  // Renew: back in the feed without moving up, three renewals at most.
  await rejectsWith(renewMarketListingService(db, buyer, { listingId }, deps), "MARKET_LISTING_NOT_FOUND");
  const renewed = await renewMarketListingService(db, seller, { listingId }, deps);
  assert.equal(renewed.status, "published");
  assert.equal(renewed.renewsLeft, 2);
  listing = await db.doc(`marketListings/${listingId}`).get();
  assert.equal((listing.get("expiresAt") as Timestamp).toMillis(), NOW + 61 * day);
  assert.equal((listing.get("publishedAt") as Timestamp).toMillis(), NOW, "feed position is unchanged");
  assert.equal(listing.get("expiryReminderSentAt"), undefined);
  await renewMarketListingService(db, seller, { listingId }, deps);
  await renewMarketListingService(db, seller, { listingId }, deps);
  await rejectsWith(renewMarketListingService(db, seller, { listingId }, deps), "MARKET_RENEW_LIMIT");

  // Sellers can still take down an expired listing themselves.
  const other = await publishedListing();
  await db.doc(`marketListings/${other}`).update({ status: "expired" });
  await updateMarketListingStatusService(db, seller, { listingId: other, action: "remove" }, deps);
  assert.equal((await db.doc(`marketListings/${other}`).get()).get("status"), "removed");

  // An expired listing nobody renews is deleted 30 days later.
  await db.doc(`marketListings/${listingId}`).update({ status: "expired", updatedAt: Timestamp.fromMillis(NOW + 31 * day) });
  counts = await cleanupMarketDataService(db, async () => undefined, NOW + 62 * day, deps.notify);
  assert.equal(counts.closedListings, 2, "the expired listing and the one removed on day 31");
  assert.equal((await db.doc(`marketListings/${listingId}`).get()).exists, false);
});


test("inactive accounts cannot read or mutate private market state, but market suspensions remain read-only", async () => {
  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Merhaba" }, deps);
  await db.doc(`marketUserState/${buyer}`).set({ suspendedUntil: Timestamp.fromMillis(NOW + 60_000) }, { merge: true });
  assert.equal((await listMarketConversationsService(db, buyer)).conversations.length, 1);
  for (const status of ["disabled", "deleting"]) {
    await db.doc(`users/${buyer}`).update({ status });
    for (const operation of [
      () => getMarketMessagesService(db, buyer, { conversationId }),
      () => listMarketConversationsService(db, buyer),
      () => listMyMarketListingsService(db, buyer),
      () => listMarketFavoritesService(db, buyer),
      () => listMarketBlockedService(db, buyer),
      () => markMarketConversationReadService(db, buyer, { conversationId }),
    ]) await rejectsWith(operation(), "ACCOUNT_NOT_ACTIVE");
  }
});

test("failed photo deletions survive loss of a listing and are retried", async () => {
  const day = 86_400_000;
  await db.doc("marketListings/orphan").set({ sellerUid: seller, status: "expired", updatedAt: Timestamp.fromMillis(NOW - 40 * day) });
  let attempts = 0;
  await cleanupMarketDataService(db, async () => { attempts++; throw new Error("Storage offline"); }, NOW);
  assert.equal((await db.doc("marketListings/orphan").get()).exists, false);
  assert.equal((await db.doc("marketPhotoDeletions/orphan").get()).exists, true);
  await cleanupMarketDataService(db, async () => { attempts++; }, NOW + day);
  assert.equal(attempts, 2);
  assert.equal((await db.doc("marketPhotoDeletions/orphan").get()).exists, false);
});

test("seller and moderation removals queue photo failures atomically", async () => {
  const offline = { ...deps, photos: { ...deps.photos, deletePrefix: async () => { throw new Error("offline"); } } };
  const removed = await publishedListing();
  await updateMarketListingStatusService(db, seller, { listingId: removed, action: "remove" }, offline);
  const { listingId: rejected } = await createMarketListingService(db, seller, await listingInput(), deps);
  await reviewMarketListingService(db, admin, { listingId: rejected, decision: "reject", reason: "Uygun değil" }, offline);
  for (const id of [removed, rejected]) {
    assert.equal((await db.doc(`marketPhotoDeletions/${id}`).get()).get("ownerUid"), seller);
    assert.deepEqual((await db.doc(`marketListings/${id}`).get()).get("photos"), []);
  }
  await cleanupMarketDataService(db, async () => undefined, NOW);
  assert.equal((await db.collection("marketPhotoDeletions").get()).empty, true);
});

test("account deletion closes the write gate before scanning and resumes after a Storage failure", async () => {
  const listingId = await publishedListing();
  const input = await listingInput();
  let checked = false;
  await rejectsWith(eraseMarketData(db, seller, async () => {
    assert.equal((await db.doc(`users/${seller}`).get()).get("status"), "deleting");
    await rejectsWith(createMarketListingService(db, seller, input, deps), "ACCOUNT_NOT_ACTIVE");
    checked = true;
    throw new Error("offline");
  }), "MARKET_PHOTO_CLEANUP_PENDING");
  assert.equal(checked, true);
  assert.equal((await db.doc(`marketPhotoDeletions/${listingId}`).get()).exists, true);
  const retried: string[] = [];
  await eraseMarketData(db, seller, async (prefix) => { retried.push(prefix); });
  assert.deepEqual(retried, [`market-listings/${listingId}/`]);
  assert.equal((await db.collection("marketPhotoDeletions").get()).empty, true);
  assert.equal((await db.collection("marketListings").where("sellerUid", "==", seller).get()).empty, true);
});

test("retention photo and reminder scans progress beyond 200 matching records", async () => {
  const day = 86_400_000;
  const batch = db.batch();
  for (let index = 0; index < 201; index++) {
    batch.set(db.doc(`marketListings/sold${index.toString().padStart(4, "0")}`), {
      sellerUid: seller, status: "sold", soldAt: Timestamp.fromMillis(NOW - 31 * day), photos: [{ url: "u" }],
    });
    batch.set(db.doc(`marketListings/soon${index.toString().padStart(4, "0")}`), {
      sellerUid: seller, status: "published", expiresAt: Timestamp.fromMillis(NOW + day),
      ...(index < 200 ? { expiryReminderSentAt: Timestamp.fromMillis(NOW - day) } : {}),
    });
  }
  await batch.commit();
  const counts = await cleanupMarketDataService(db, async () => undefined, NOW, deps.notify);
  assert.equal(counts.soldPhotos, 201);
  assert.equal(counts.reminders, 1);
  const second = await cleanupMarketDataService(db, async () => undefined, NOW + 1000, deps.notify);
  assert.equal(second.soldPhotos, 0);
  assert.equal(second.reminders, 0);
});

test("offer responses respect either participant's block and an unavailable listing", async () => {
  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, offerPercent: 10 }, deps);
  for (const blocker of [seller, buyer]) {
    const other = blocker === seller ? buyer : seller;
    await db.doc(`marketUserState/${blocker}`).set({ blockedUids: [other] }, { merge: true });
    const notifications = notified.length;
    await rejectsWith(respondMarketOfferService(db, seller, { conversationId, accept: true }, deps), "MARKET_BLOCKED");
    assert.equal(notified.length, notifications);
    assert.equal((await db.collection(`marketConversations/${conversationId}/messages`).get()).size, 1);
    await db.doc(`marketUserState/${blocker}`).update({ blockedUids: [] });
  }
  await blockMarketUserService(db, buyer, { conversationId }, deps);
  await rejectsWith(respondMarketOfferService(db, seller, { conversationId, accept: false }, deps), "MARKET_BLOCKED");
  await unblockMarketUserService(db, buyer, { conversationId }, deps);
  await updateMarketListingStatusService(db, seller, { listingId, action: "markSold" }, deps);
  await rejectsWith(respondMarketOfferService(db, seller, { conversationId, accept: true }, deps), "MARKET_OFFER_UNAVAILABLE");
});

test("a renewal after the expiry query cannot be overwritten by a stale cleanup snapshot", async () => {
  for (const id of ["raceA", "raceB"]) await db.doc(`marketListings/${id}`).set({
    sellerUid: seller, status: "published", expiresAt: Timestamp.fromMillis(NOW - 1), renewCount: 0,
  });
  let renewed = "";
  const counts = await cleanupMarketDataService(db, async () => undefined, NOW, async (_uid, payload) => {
    if (!renewed) {
      renewed = payload.data?.listingId === "raceA" ? "raceB" : "raceA";
      await renewMarketListingService(db, seller, { listingId: renewed }, deps);
    }
  });
  assert.equal(counts.expired, 1);
  const current = await db.doc(`marketListings/${renewed}`).get();
  assert.equal(current.get("status"), "published");
  assert.ok(current.get("expiresAt").toMillis() > NOW);
});

test("renewal and reactivation share the 15-active-listing quota under concurrency", async () => {
  const batch = db.batch();
  for (let index = 0; index < MARKET_LIMITS.maxActiveListings - 1; index++) batch.set(db.doc(`marketListings/live${index}`), {
    sellerUid: seller, status: "published", photos: [{ url: "u" }],
  });
  for (const id of ["expiredA", "expiredB"]) batch.set(db.doc(`marketListings/${id}`), { sellerUid: seller, status: "expired" });
  batch.set(db.doc("marketListings/sold"), { sellerUid: seller, status: "sold", photos: [{ url: "u" }] });
  await batch.commit();
  const results = await Promise.allSettled([
    renewMarketListingService(db, seller, { listingId: "expiredA" }, deps),
    renewMarketListingService(db, seller, { listingId: "expiredB" }, deps),
    updateMarketListingStatusService(db, seller, { listingId: "sold", action: "markAvailable" }, deps),
  ]);
  assert.equal(results.filter((result) => result.status === "fulfilled").length, 1);
  for (const result of results) if (result.status === "rejected") assert.equal(result.reason.message, "MARKET_ACTIVE_LISTING_LIMIT");
  assert.equal((await db.collection("marketListings").where("sellerUid", "==", seller)
    .where("status", "in", ["pending", "published", "reserved"]).get()).size, 15);
});

test("a partial upload settles before its failed cleanup is queued for retry", async () => {
  let thumbnailFinished = false;
  let deletionStartedAfterUpload = false;
  const failing: MarketDeps = {
    ...deps,
    photos: {
      async save(name) {
        if (!name.endsWith("_thumb.jpg")) throw new Error("upload failed");
        await new Promise((resolve) => setTimeout(resolve, 10));
        thumbnailFinished = true;
        return `https://example.test/${name}`;
      },
      async deletePrefix() {
        deletionStartedAfterUpload = thumbnailFinished;
        throw new Error("Storage offline");
      },
    },
  };
  await assert.rejects(createMarketListingService(db, seller, await listingInput(), failing), /upload failed/);
  assert.equal(deletionStartedAfterUpload, true);
  assert.equal((await db.collection("marketListings").get()).empty, true);
  const jobs = await db.collection("marketPhotoDeletions").get();
  assert.equal(jobs.size, 1);
  assert.equal(jobs.docs[0]?.get("ownerUid"), seller);
  await cleanupMarketDataService(db, async () => undefined, NOW);
  assert.equal((await db.collection("marketPhotoDeletions").get()).empty, true);
});

test("overlapping conversation cleanup cannot remove a freshly recreated conversation", async () => {
  const listingId = await publishedListing();
  const conversationId = `${listingId}_${buyer}`;
  await sendMarketMessageService(db, buyer, { conversationId, text: "Eski konuşma" }, deps);
  await db.doc(`marketConversations/${conversationId}`).update({ lastMessageAt: Timestamp.fromMillis(NOW - 366 * 86_400_000) });
  const messageBatch = db.batch();
  for (let index = 0; index < 250; index++) messageBatch.set(db.doc(`marketConversations/${conversationId}/messages/old${index}`), {
    text: "Eski", senderUid: buyer, createdAt: Timestamp.fromMillis(NOW - 366 * 86_400_000),
  });
  await messageBatch.commit();
  const cleanups = [cleanupMarketDataService(db, async () => undefined, NOW), cleanupMarketDataService(db, async () => undefined, NOW)];
  await Promise.race(cleanups);
  await sendMarketMessageService(db, buyer, { conversationId, text: "Yeni konuşma" }, { ...deps, now: () => NOW + 1000 });
  await Promise.all(cleanups);
  const current = await getMarketMessagesService(db, buyer, { conversationId });
  assert.equal(current.conversation.status, "open");
  assert.deepEqual(current.messages.map((message) => message.text), ["Yeni konuşma"]);
});
