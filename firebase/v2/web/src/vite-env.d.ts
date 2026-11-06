/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_USE_FIREBASE_EMULATORS?: string;
  readonly VITE_FIREBASE_PROJECT?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

interface Window {
  FIREBASE_APPCHECK_DEBUG_TOKEN?: boolean | string;
}
