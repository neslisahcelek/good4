import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";

export default defineConfig({
  plugins: [react()],
  build: {
    rolldownOptions: {
      input: {
        main: fileURLToPath(new URL("./index.html", import.meta.url)),
        campus: fileURLToPath(new URL("./campus-email-verification.html", import.meta.url)),
      },
    },
  },
  server: {
    host: "127.0.0.1",
    port: 4175,
  },
  preview: {
    host: "127.0.0.1",
    port: 4175,
  },
});
