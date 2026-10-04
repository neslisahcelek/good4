import { type Firestore, type Transaction, FieldValue, Timestamp } from "firebase-admin/firestore";

/** Written in the same transaction that hides/deletes a listing. Never discard a failed Storage job. */
export function queueMarketPhotoDeletion(
  database: Firestore, transaction: Transaction, listingId: string, ownerUid?: unknown,
): void {
  transaction.set(database.doc(`marketPhotoDeletions/${listingId}`), {
    prefix: `market-listings/${listingId}/`,
    ...(typeof ownerUid === "string" ? { ownerUid } : {}),
    attemptedAt: Timestamp.fromMillis(0),
  }, { merge: true });
}

export async function tryMarketPhotoDeletion(
  database: Firestore, listingId: string, deletePhotos: (prefix: string) => Promise<void>, now = Date.now(),
): Promise<boolean> {
  const jobRef = database.doc(`marketPhotoDeletions/${listingId}`);
  const job = await jobRef.get();
  if (!job.exists) return true;
  try {
    await deletePhotos(String(job.get("prefix")));
    await jobRef.delete();
    return true;
  } catch {
    // A missing job after another worker succeeded needs no retry. Updating,
    // rather than setting, prevents that worker's deletion being resurrected.
    await jobRef.update({ attemptedAt: Timestamp.fromMillis(now), attempts: FieldValue.increment(1) })
      .catch((error: { code?: number }) => { if (error.code !== 5) throw error; });
    return false;
  }
}

/** Old failed jobs move behind fresh work, so one failure cannot starve the queue. */
export async function retryMarketPhotoDeletions(
  database: Firestore, deletePhotos: (prefix: string) => Promise<void>, now: number,
): Promise<void> {
  const page = await database.collection("marketPhotoDeletions").orderBy("attemptedAt").limit(200).get();
  for (const job of page.docs) await tryMarketPhotoDeletion(database, job.id, deletePhotos, now);
}
