// Publishes a transcribed monthly KYK menu to Firestore (kyk_menu_days/{date}).
//
// Usage (from firebase/v2):
//   npm --prefix functions run build
//   node scripts/publish-kyk-menu.mjs data/kyk-menu/2026-10.txt            # preview only
//   node scripts/publish-kyk-menu.mjs data/kyk-menu/2026-10.txt --publish  # write to good4tr-v2
//
// Writing uses Google Application Default Credentials of the person running it
// (`gcloud auth application-default login`); no key file is stored in the repo.
import { readFile } from "node:fs/promises";

const args = process.argv.slice(2);
const [file, flag] = args;
if (!file || file.startsWith("--") || args.length > 2 || (flag !== undefined && flag !== "--publish")) {
  console.error("Kullanım: node scripts/publish-kyk-menu.mjs <menü.txt> [--publish]");
  process.exit(1);
}
const publish = flag === "--publish";
if (publish && process.env.FIRESTORE_EMULATOR_HOST) {
  console.error("Canlı yayın için FIRESTORE_EMULATOR_HOST ortam değişkenini kaldırın.");
  process.exit(1);
}
if (publish) process.env.GCLOUD_PROJECT = "good4tr-v2";

const { parseKykMenuText } = await import("../functions/lib/kykMenuParser.js");
const { days, errors } = parseKykMenuText(await readFile(file, "utf8"));
if (errors.length > 0 || days.length === 0) {
  console.error(errors.length ? errors.join("\n") : "Metinde gün bulunamadı.");
  process.exit(1);
}
const { validateKykMenuDays, writeKykMenuDays } = await import("../functions/lib/kykMenu.js");
const validDays = validateKykMenuDays(days);
for (const day of validDays) {
  console.log(`\n${day.date}`);
  console.log("KAHVALTI");
  day.breakfast.forEach((item) => console.log(`- ${item}`));
  console.log("AKŞAM YEMEĞİ");
  day.dinner.forEach((item) => console.log(`- ${item}`));
}
if (!publish) {
  console.log(`\n${days.length} gün okundu. Yayınlamak için --publish ekleyin.`);
  process.exit(0);
}

const { db } = await import("../functions/lib/firebase.js");
const { savedDates } = await writeKykMenuDays(db, validDays, "script:kyk-menu-publisher");
console.log(`\ngood4tr-v2: ${savedDates.length} gün yayınlandı (${savedDates[0]} – ${savedDates.at(-1)}).`);
process.exit(0);
