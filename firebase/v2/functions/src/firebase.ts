import { getApps, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";

const projectId = process.env.GCLOUD_PROJECT ?? "demo-good4-v2";
const defaultApp = getApps()[0] ?? initializeApp({
  projectId,
  storageBucket: projectId === "good4tr-v2"
    ? "good4tr-v2.firebasestorage.app"
    : `${projectId}.appspot.com`,
});

export const db = getFirestore(defaultApp);
export const storageBucket = getStorage(defaultApp).bucket();

// Temporary, test-only bridge for the current Good4Test mobile build. Never
// point this at the legacy production project or make this configurable by a client.
const legacyTestApp = initializeApp({ projectId: "good4tr-test" }, "legacy-good4tr-test");
export const legacyTestDb = getFirestore(legacyTestApp);
