import { initializeApp } from "firebase/app";
import { initializeAppCheck, ReCaptchaEnterpriseProvider } from "firebase/app-check";
import {
  browserLocalPersistence,
  connectAuthEmulator,
  getAuth,
  setPersistence,
} from "firebase/auth";
import {
  connectFunctionsEmulator,
  getFunctions,
} from "firebase/functions";
import type { Firestore } from "firebase/firestore/lite";

const firebaseConfig = {
  apiKey: "AIzaSyA9OIJZLXKFyzO5C7GZ5RtrQDAGl44fya4",
  authDomain: "good4tr-v2.firebaseapp.com",
  projectId: "good4tr-v2",
  storageBucket: "good4tr-v2.firebasestorage.app",
  messagingSenderId: "654697131931",
  appId: "1:654697131931:web:ceff2acfa4ff4529603b3e",
};

const useFirebaseEmulators = import.meta.env.VITE_USE_FIREBASE_EMULATORS === "true";
const app = initializeApp(useFirebaseEmulators ? { apiKey: "demo-api-key", projectId: "demo-good4-v2", appId: "1:123456789:web:demo", authDomain: "localhost" } : firebaseConfig);

if (!useFirebaseEmulators) {
  // Configure App Check before Auth, Functions, or Firestore can issue requests.
  if (import.meta.env.DEV) {
    self.FIREBASE_APPCHECK_DEBUG_TOKEN = true;
  }
  initializeAppCheck(app, {
    provider: new ReCaptchaEnterpriseProvider("6LdG2r0tAAAAAIOzRvZPbhrWE4m-Q4CBQGO-b4zO"),
    isTokenAutoRefreshEnabled: true,
  });
}

export const auth = getAuth(app);
export const functions = getFunctions(app, "europe-west1");
let firestorePromise: Promise<Firestore> | null = null;

export function getFirestoreDb(): Promise<Firestore> {
  if (!firestorePromise) {
    firestorePromise = import("firebase/firestore/lite").then(({ connectFirestoreEmulator, getFirestore }) => {
      const database = getFirestore(app);
      if (useFirebaseEmulators) {
        connectFirestoreEmulator(database, "127.0.0.1", 8285);
      }
      return database;
    });
  }
  return firestorePromise;
}

void setPersistence(auth, browserLocalPersistence);

if (useFirebaseEmulators) {
  connectAuthEmulator(auth, "http://127.0.0.1:9199", { disableWarnings: true });
  connectFunctionsEmulator(functions, "127.0.0.1", 5105);
}
