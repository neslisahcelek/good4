import { FieldValue, Timestamp, type Firestore } from "firebase-admin/firestore";
import type { Bucket } from "@google-cloud/storage";

/** Bounded cleanup; inbox and audit history are never included. */
export async function cleanupTechnicalRecords(database: Firestore, bucket?: Bucket, now = Date.now()) {
  const cutoff = Timestamp.fromMillis(now - 30 * 86400000);
  let deleted = 0;
  const expiredJobs = await database.collection("notificationJobs").where("expiresAt", "<", Timestamp.fromMillis(now)).limit(20).get();
  for (const job of expiredJobs.docs) {
    await job.ref.update({ expiresAt: FieldValue.delete(),
      ...(!["complete", "cancelled", "expired"].includes(job.get("status")) ? { status: "expired", completedAt: Timestamp.fromMillis(now) } : {}) });
  }

  const jobs = await database.collection("notificationJobs").where("completedAt", "<", cutoff).limit(5).get();
  for (const job of jobs.docs) {
    if (!["complete", "cancelled", "expired"].includes(job.get("status"))) continue;
    const pages = await job.ref.collection("pages").limit(100).get();
    const deliveries = await job.ref.collection("deliveries").limit(25).get();
    const batch = database.batch();
    pages.docs.forEach((page) => { batch.delete(page.ref); deleted++; });
    for (const delivery of deliveries.docs) {
      const devices = await delivery.ref.collection("devices").limit(10).get();
      devices.docs.forEach((device) => { batch.delete(device.ref); deleted++; });
      if (devices.size < 10) { batch.delete(delivery.ref); deleted++; }
    }
    if (pages.empty && deliveries.empty) { batch.delete(job.ref); deleted++; }
    await batch.commit();
  }
  for (const name of ["notificationDispatches", "notificationQuotas", "notificationAnnouncementQuotas"]) {
    const query = name === "notificationDispatches" ? database.collection(name).where("createdAt", "<", cutoff) : database.collection(name).where("updatedAt", "<", cutoff);
    const records = await query.limit(100).get();
    const batch = database.batch();
    records.docs.forEach((record) => { batch.delete(record.ref); deleted++; });
    if (!records.empty) await batch.commit();
  }
  if (bucket) {
    const banners = await database.getAll(...[1, 2, 3, 4].map((slot) => database.doc(`app_config/${slot === 1 ? "home_banner" : `home_banner_${slot}`}`)));
    const active = new Set<string>();
    for (const banner of banners) {
      try { const url = new URL(String(banner.get("imageUrl") ?? ""));
        if (url.pathname.startsWith(`/v0/b/${bucket.name}/o/`)) active.add(decodeURIComponent(url.pathname.split("/o/")[1]!));
      } catch { /* Missing banner. */ }
    }
    const cursorRef = database.doc("system/banner_cleanup");
    const cursor = (await cursorRef.get()).get("pageToken");
    const [files, next] = await bucket.getFiles({ prefix: "home-banners/", maxResults: 100, autoPaginate: false, ...(cursor ? { pageToken: cursor } : {}) });
    for (const file of files) {
      const createdAt = Date.parse(String(file.metadata.timeCreated ?? ""));
      if (!active.has(file.name) && Number.isFinite(createdAt) && createdAt < now - 7 * 86400000) await file.delete({ ifGenerationMatch: Number(file.metadata.generation) });
    }
    await cursorRef.set({ pageToken: next?.pageToken ?? null });
  }
  return { deleted };
}
