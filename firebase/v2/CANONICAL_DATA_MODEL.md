# Good4 V2 canonical data model

This document is the shared V2 contract for production (`good4tr-v2`) and test (`good4tr-test`). Persisted date/time fields are Firestore `Timestamp` values. IDs that relate records are stored explicitly; display names are snapshots, never authorization inputs.

## Identity and organizations

| Domain / path | Document ID | Required fields | Optional fields / enums | Relations |
|---|---|---|---|---|
| `users/{uid}` | Firebase Auth UID | `email:string`, `displayName:string`, `role:string`, `status:string`, `university:string`, `createdAt:Timestamp`, `updatedAt:Timestamp` | `legalAcknowledgements.kvkkNotice`: `{version:string, acknowledgedAt:Timestamp}`; `legalAcknowledgements.userAgreement`: `{version:string, acceptedAt:Timestamp}`; `legalAcknowledgements.privacyPolicy`: `{version:string, presentedAt:Timestamp}`; `status`: `active`, `disabled`; `role`: `student`, `good4Admin`, `businessOwner`, `businessStaff`, `communityManager`, `communityStaff` | Auth UID |
| `organizations/{organizationId}` | Server-generated | `name:string`, `type:string`, `status:string`, `createdAt:Timestamp`, `createdBy:uid`, `updatedAt:Timestamp` | `type`: `community`, `business`; `status`: `active`, `disabled`; `university`, `description`, `logoUrl`, `coverUrl`, `followerCount:number`; temporary `legacyTestBusinessId` / `legacyTestCommunityId` links | `createdBy -> users` |
| `organizations/{organizationId}/members/{uid}` | User UID | `userId:uid`, `role:string`, `status:string`, `assignedBy:uid`, `assignedAt:Timestamp`, `updatedAt:Timestamp` | Community roles: `manager`, `staff`; business roles: `owner`, `staff`; status `active`, `disabled` | parent organization, user |
| `organizations/{organizationId}/followers/{uid}` | User UID | `userId:uid`, `followedAt:Timestamp` | none | parent organization, user |

Communities and businesses are organization types, not separate canonical top-level collections. UI-specific views may call them “community” or “business”, but ownership and membership always resolve through `organizations`.

Following is managed by the authenticated `setCommunityFollowing` callable. Its transaction checks the active actor, permits new follows only for active community organizations, writes a server timestamp, and updates `followerCount` only when the follow state changes. Users may leave an organization that was subsequently disabled or removed. Organizations without a counter initialize it from their existing followers on the first change. Direct client follower writes remain denied. `getFollowingCommunityIds` checks the active actor and runs one `followers` collection group query for their UID, returning only IDs from canonical `organizations/{id}/followers/{uid}` paths. No client collection group read permission is added; the client intersects these IDs with its already loaded active, unblocked communities. `followers.userId` needs the collection group index in `firestore.indexes.json`.

## Events, registration, and attendance

| Domain / path | Document ID | Required fields | Optional fields / enums | Relations |
|---|---|---|---|---|
| `events/{eventId}` | Server-generated | `organizationId:string`, `title:string`, `description:string`, `startsAt:Timestamp`, `endsAt:Timestamp`, `timezone:string`, `location:string`, `capacity:number`, `registrationCount:number`, `attendanceCount:number`, `status:string`, `createdAt:Timestamp`, `createdBy:uid`, `updatedAt:Timestamp` | `imageUrl:string`, `categoryId:string`; status `draft`, `published`, `cancelled`, `completed`; capacity `0` means unlimited | `organizationId -> organizations` |
| `events/{eventId}/registrations/{registrationId}` | Server-generated UUID | `eventId:string`, `organizationId:string`, `userId:uid`, `displayName:string`, `status:string`, `registeredAt:Timestamp`, `updatedAt:Timestamp` | status currently `registered` | event, organization, user |
| `events/{eventId}/checkins/{registrationId}` | Same ID as registration | `eventId:string`, `organizationId:string`, `registrationId:string`, `userId:uid`, `checkedInBy:uid`, `checkedInAt:Timestamp`, `method:string` | method `qr`, `manual` | registration, event, user, gatekeeper |

`events/{eventId}` is the sole V2 event model. `communities/{id}/entries/{id}` remains legacy-only and receives no new V2 event copy. Registration capacity and QR-only check-in are enforced by trusted transactions. New V2 attendance requires a scanned registration ID; manual check-in and undo are rejected. Historical `manual` records remain readable. The V2 QR payload is `good4:event:v2/{eventId}/{registrationId}`; the backend re-loads the event, registration, membership, and existing check-in instead of trusting client claims.

### Event categories

Categories belong to individual events, not organizations. The callable validates any supplied `categoryId` against this fixed list; new web/mobile forms require a choice. For older clients, an omitted field is accepted and the existing value is preserved on edits. Old events are not backfilled. Missing or unknown categories are displayed as “Kategori belirtilmemiş”, included in “Tümü”, and selectable through the presentation-only `uncategorized` filter (never a stored category).

| `categoryId` | Display label |
|---|---|
| `academic-science` | Akademik ve Bilim |
| `career-entrepreneurship` | Kariyer ve Girişimcilik |
| `technology` | Teknoloji |
| `culture-arts` | Kültür ve Sanat |
| `sports-nature` | Spor ve Doğa |
| `social-entertainment` | Sosyal ve Eğlence |
| `volunteering` | Gönüllülük |
| `other` | Diğer |

The backend and web form share `functions/src/eventCategories.ts`; the mobile enum is `community/EventCategories.kt`. Event writes remain callable-only, with manager/membership/organization ownership checks. Student discovery applies category and followed-community filters before limiting the carousel to ten eligible events. Neither filter changes the community discovery search.

## Campaigns, claims, codes, and redemptions

These paths are the existing V2 target contract. Mobile migration to them is Phase 2.

| Domain / path | Document ID | Required fields | Optional fields / enums | Relations |
|---|---|---|---|---|
| `campaigns/{campaignId}` | Server-generated | `organizationId:string`, `title:string`, `description:string`, `startsAt:Timestamp`, `endsAt:Timestamp`, `status:string`, `redemptionCount:number`, `createdAt:Timestamp`, `createdBy:uid`, `updatedAt:Timestamp` | `totalLimit:number|null`; status `draft`, `published`, `cancelled`, `completed` | business organization |
| `campaignClaims/{campaignId}_{uid}` | Deterministic campaign + student | `campaignId:string`, `studentId:uid`, `code:string`, `status:string`, `issuedAt:Timestamp`, `expiresAt:Timestamp` | `redeemedAt:Timestamp`, `redeemedBy:uid`, `updatedAt:Timestamp`; status `issued`, `redeemed`, `expired` | campaign, student, code |
| `campaignCodes/{code}` | Normalized 6–12 character code | `code:string`, `campaignId:string`, `organizationId:string`, `studentId:uid`, `status:string`, `issuedAt:Timestamp`, `expiresAt:Timestamp` | `redeemedAt:Timestamp|null`, `redeemedBy:uid|null`, `updatedAt:Timestamp`; status `issued`, `redeemed`, `expired` | campaign, business organization, student |
| `redemptions/{code}` | Redeemed code | `code:string`, `campaignId:string`, `organizationId:string`, `studentId:uid`, `redeemedBy:uid`, `redeemedAt:Timestamp` | none | code, campaign, business, student |

The canonical naming is `campaignClaims`, `campaignCodes`, and `redemptions`; legacy `community_coupon_codes` and nested entry `claims` are not extended in V2.

## Media contract (design only)

V2 Storage remains deny-all in Phase 1. No public upload permission was added. Proposed object paths are:

- `organizations/{organizationId}/profile/{assetId}` for logo/cover media.
- `events/{eventId}/{assetId}` for event media.
- `campaigns/{campaignId}/{assetId}` for campaign media.

Before enabling uploads, a trusted service must create immutable metadata containing `ownerType`, `ownerId`, `organizationId`, `uploadedBy`, `contentType`, `size`, `createdAt`, and a random `assetId`. Rules or signed-upload endpoints must verify active organization membership, an allowlisted image content type, a strict size limit, and path ownership. Reads should follow the owning record's visibility; writes must never be globally authenticated/public.

## Backward compatibility

### Update notice copy

`app_config/update_notice` contains optional string fields `title` and `message` for the mobile update card. Authenticated clients may get this document; client listing and all writes remain denied. Operators edit it through Firebase Console or a trusted Admin SDK. Presentation accepts a nonblank title up to 120 characters and message up to 400 characters, otherwise uses localized resource defaults. Download/install copy and update eligibility stay in the app. See [setup instructions](../../docs/app-update-notice.md).

- The supported staging and production flavors both read canonical organizations/events and use V2 callable transactions for registration/check-in.
- `scripts/align-test-schema.mjs` previews the test-only transition by default. Applying it requires active test callables, backs up existing test records, normalizes user roles/dates, and adds canonical organizations/events. Legacy event, coupon and attendance history remains in place; incompatible image-only campaign documents are archived to `legacyCampaigns`.
- The legacy production project `good4tr` is outside this transition. No private production data or Auth accounts are copied into test.
- Legacy numeric/string dates may still be decoded by compatibility adapters, but every new V2 server write uses Firestore `Timestamp`.
- Temporary legacy links support only functionality not migrated in this phase (notably coupons). They are not a new canonical model.

## Kampüs Dolabı temizleme işleri — 4 Ekim 2026

`marketPhotoDeletions/{listingId}` yalnızca sunucuya açık, kalıcı Storage silme kuyruğudur. İlanın silinmesi/gizlenmesiyle aynı transaction içinde `prefix`, `ownerUid`, `attemptedAt` yazılır. Başarısız denemelerde `attempts` artar; iş başarılı Storage silmesinden sonra kaldırılır. Günlük temizleme görevi yeniden dener. Hesap silme, ilan artık bulunmasa bile `ownerUid` ile kalan işleri bulur; fotoğraf işi tamamlanmamışsa başarı dönmez.

`marketUserState.listingQuotaRevision` aktivasyonların ortak kota kilididir. Aktif (`pending/published/reserved`) ilan sınırı 15'tir; oluşturma, yenileme ve tekrar yayımlama aynı kotayı uygular. `marketListings.photosCleanupQueuedAt` kuyruk tarihidir, Storage başarı tarihi değildir. `marketConversations.cleanupToken`, paralel temizlik çalışmaları sırasında yeni konuşma neslinin yanlışlıkla silinmesini önler.

[İnceleme ve doğrulama kaydı](../../docs/code-review-2026-10-04.md).


## Daily meal ratings

`meal_ratings/{yyyy-MM-dd}_{meal}` stores `date`, `meal`, `good`, `okay`, `bad`, and `updatedAt:Timestamp`. Meal keys are `kyk_breakfast`, `cafeteria`, and `kyk_dinner`. The `votes/{uid}` subcollection stores `userId`, `rating` (`good`, `okay`, `bad`) and `updatedAt:Timestamp`; only that active account can read its vote. Signed-in accounts can read public counters; clients cannot list or write summaries/votes.

`rateMeal` accepts any active account, opens at 06:00/11:00/16:00 Istanbul time, and checks the published menu inside its vote transaction. New clients send the displayed `date`; a date that differs from the server day is rejected. For older clients `date` is optional. Updating a vote adjusts both counters atomically. Account deletion closes the account write gate, scans summary pages using a cursor and deletes each UID vote with its counter adjustment in a transaction.

Community covers are stored at `community-events/{organizationId}/{uuid}.{extension}`. The upload callable resolves the active manager's organization, validates type/size/signature, and writes via Admin SDK; direct client writes are denied and cover reads are public. Event saves accept owning-organization cover URLs, preserve unchanged legacy URLs, and clear the cover when an explicit empty `imageUrl` is sent. Event discovery uses `status == published` plus `endsAt >= now`, requiring the `status + endsAt` index.

## Operatör tarafından pasife alınan Kampüs Dolabı ilanları

Firebase Console / güvenilir Admin SDK üzerinden `marketListings/{listingId}.status` alanını `inactive` ve `updatedAt` alanını güncel Timestamp yaparak ilan geçici olarak gizlenir. Bu durum için kullanıcı callable geçişi veya butonu yoktur. Feed yalnızca `published` ilanları getirir; `inactive` fotoğraf silme, süre dolumu ve kapalı ilan temizliğine girmez, aktif ilan kotasına dahil değildir. Satıcı İlanlarım/detay ekranında Pasif etiketini görür. Tekrar yayınlama operatör tarafından `published` ile yapılır; yayın öncesi 15 aktif ilan kotası, mevcut `expiresAt`, satıcının aktifliği/pazar yetkisi ve fotoğraflar kontrol edilmelidir. Pasifteyken süre dondurulmaz. Hesap silme pasif ilanları da mevcut silme akışında temizler.

## Sosyal etkinlikler — 6 Ekim 2026

Öğrenciden öğrenciye sosyal ve spor etkinlikleri; topluluk etkinliklerinden (`events`) tamamen ayrıdır. Bütün okuma ve yazmalar `functions/src/social.ts` callable'larından geçer; Firestore kuralları bu koleksiyonları istemciye kapalı tutar. Özellik `app_config/social_activities.enabled == true` olmadan kapalıdır (fail-closed).

| Domain / path | Document ID | Required fields | Optional fields / enums | Relations |
|---|---|---|---|---|
| `socialActivities/{activityId}` | Server-generated | `organizerUid:uid`, `organizerName:string` (maskeli "A.. Y.."), `eduDomain:string`, `universityName:string`, `kind:string`, `type:string`, `title:string`, `note:string`, `startsAt:Timestamp`, `capacity:number` (1–10), `acceptedCount:number`, `pendingCount:number`, `status:string`, `createdAt:Timestamp`, `updatedAt:Timestamp` | `kind`: `social`, `sport`; `type`: `SOCIAL_ACTIVITY_TYPES`; `level:string|null` (yalnız spor: `any`, `beginner`, `intermediate`, `advanced`); isteğe bağlı `game:string|null` (`board-games`: `okey`, `backgammon`, `chess`, `uno`, `taboo`, `cards`, `other`; `video-games`: `fifa`, `pes`); status `open`, `full`, `cancelled`, `ended`, `removed`; `moderationFlags:string[]`, `cancelledAt` | organizer |
| `socialActivities/{activityId}/joinRequests/{uid}` | Requester UID (etkinlik başına tek istek) | `activityId`, `organizerUid`, `requesterUid`, `requesterName` (maskeli), `note`, `status`, `activityStartsAt:Timestamp`, `createdAt`, `updatedAt` | status `pending`, `accepted`, `declined`, `withdrawn`, `left`, `removed`, `closed`; `respondedAt` | activity, requester |
| `socialConversations/{activityId}_{participantUid}` | Deterministic | `activityId`, `organizerUid`, `participantUid`, `participants:[uid,uid]`, `activityTitle`, `activityKind`, `activityType`, `activityStartsAt`, `organizerName`, `participantName`, `status`, `lastMessageText`, `lastMessageAt`, `lastSenderUid|null`, `unread:{uid:number}`, `messageCount`, `createdAt`, `updatedAt` | status `open`, `closed`, `blocked`, `deleting`; `blockedBy` | activity, organizer, participant |
| `socialConversations/{id}/messages/{messageId}` | Server-generated | `senderUid:uid|null`, `type`, `text`, `createdAt` | type `text`, `system` | conversation |
| `socialUserState/{uid}` | User UID | — | `termsVersion`, `termsAcceptedAt`, `dayKey`, `activitiesToday`, `requestsToday`, `messagesToday`, `photoChangesToday`, `unreadCount`, `nameMode` (`masked` varsayılan, `shown`), `photoUrl`, `photoThumbUrl`, `photoFolder` | user |
| `socialReports/{targetType}_{targetId}_{reporterUid}` | Deterministic | `reporterUid`, `targetType`, `targetId`, `activityId`, `reportedUid`, `reason`, `note`, `status`, `createdAt` | targetType `activity`, `conversation`; status `open`, `resolved`; `resolution`, `resolvedBy`, `resolvedAt` | activity, reporter, reported user |

- Etkinliklerin buluşma yeri alanı yoktur; yeri organizatör ile kabul edilen katılımcı sohbette belirler.
- Oluşturma kataloğu 12 sosyal ve 15 spor türünden oluşur. Masa oyunları sosyal listenin ilk sırasındadır; piknik ve gönüllülük yeni etkinlik türleri arasından kaldırıldı (6 Ekim 2026 kullanıcı isteği). Dijital oyunlarda FIFA/PES seçimi isteğe bağlıdır.
- Öğrenci adı okuma anında çözülür: `users.displayName` + `socialUserState.nameMode` (`masked` → "A.. Y..", `shown` → "Ayşe Y."). Etkinlik, istek ve sohbet belgelerindeki ad alanları her zaman maskelidir ve yalnızca yedektir; ayarı sonradan değiştirmek mevcut etkinlik ve sohbetlere de yansır.
- Profil fotoğrafı `social-profiles/{uid}/{id}/photo.jpg` ve `thumb.jpg` olarak sunucuda yeniden kodlanır (kare kırpma, EXIF silinir), günde en fazla 5 değişiklik. Fotoğraf yalnızca okul e-postasını doğrulamış, aynı üniversitenin öğrencilerine döner. Eski fotoğraf klasörü değişiklikte, tüm `social-profiles/{uid}/` hesap silmede silinir.
- Admin, açık bir sosyal şikayetin hedef kullanıcısının fotoğrafını `removeSocialReportedProfilePhoto` ile kaldırabilir: önce ilgili Storage klasörü silinir, ardından `photoUrl`, `photoThumbUrl`, `photoFolder` alanları kaldırılır ve `auditLogs` içine `social.profile.photoRemoved` yazılır. Şikayet açık kalır. Storage hatasında alanlar tekrar denemek için korunur; eşzamanlı yüklenen yeni fotoğraf korunur ve admin listeyi yenileyerek tekrar incelemelidir.
- Kapı Kampüs Dolabı ile aynıdır: `users.eduVerified` + `@ogr.akdeniz.edu.tr` (`hasVerifiedCampusEmail`). Okul adresini Kampüs Dolabı'nda doğrulayan öğrenci burada tekrar doğrulamaz.
- Engelleme listesi (`marketUserState.blockedUids`), 24 saatlik kapatma (`marketUserState.suspendedUntil`) ve yasaklı içerik ihlalleri (`marketViolations`, `context: socialActivity|socialRequest|socialMessage`) Kampüs Dolabı ile ortaktır; günlük sayaçlar `socialUserState`'tedir.
- Etkinlik oluşturma her seferinde `socialUserState` belgesine yazar; aynı kullanıcının eşzamanlı oluşturmaları bu belge üzerinde sıraya girer ve aktif etkinlik sınırı (5) aşılamaz.
- Kabul, kontenjan dolunca bekleyen bütün istekleri aynı transaction'da `closed` yapar. İstek sahibine `declined`, `closed` ve `removed` durumları tek bir `closed` ("Yer kalmadı") olarak döner.
- Sohbet, etkinlik başladıktan 7 gün sonra salt-okunurdur. `cleanupSocialActivities` (her gün 04:45) başlamış etkinlikleri `ended` yapar; etkinlik, istekleri ve sohbetleri başlangıçtan 30 gün sonra, çözülmüş şikayetler 365 gün sonra silinir. Hesap silme `eraseSocialData` ile bütün sosyal veriyi kaldırır ve kabul edilmiş yerleri boşaltır.
