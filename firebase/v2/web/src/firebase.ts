import { initializeApp } from "firebase/app";
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

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const functions = getFunctions(app, "europe-west1");
let firestorePromise: Promise<Firestore> | null = null;

export function getFirestoreDb(): Promise<Firestore> {
  if (!firestorePromise) {
    firestorePromise = import("firebase/firestore/lite").then(({ connectFirestoreEmulator, getFirestore }) => {
      const database = getFirestore(app);
      if (import.meta.env.VITE_USE_FIREBASE_EMULATORS === "true") {
        connectFirestoreEmulator(database, "127.0.0.1", 8180);
      }
      return database;
    });
  }
  return firestorePromise;
}

void setPersistence(auth, browserLocalPersistence);

if (import.meta.env.VITE_USE_FIREBASE_EMULATORS === "true") {
  connectAuthEmulator(auth, "http://127.0.0.1:9199", { disableWarnings: true });
  connectFunctionsEmulator(functions, "127.0.0.1", 5105);
}
