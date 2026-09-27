import { readFile } from "node:fs/promises";
import { homedir } from "node:os";
import { resolve } from "node:path";

const projectId = "good4tr-v2";
const sourcePath = resolve(process.cwd(), "data/academic-calendar-2026-2027.json");
const authPath = resolve(homedir(), ".config/configstore/firebase-tools.json");
const events = JSON.parse(await readFile(sourcePath, "utf8"));
const auth = JSON.parse(await readFile(authPath, "utf8"));
const accessToken = auth?.tokens?.access_token;
if (!accessToken) throw new Error("Firebase CLI oturumu bulunamadı.");

function value(input) {
  if (input === null || input === undefined) return { nullValue: null };
  if (typeof input === "boolean") return { booleanValue: input };
  if (typeof input === "number") return Number.isInteger(input) ? { integerValue: String(input) } : { doubleValue: input };
  return { stringValue: String(input) };
}

const collectionUrl = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/academic_calendar_events`;
const headers = { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" };

async function listExistingDocuments() {
  const documents = [];
  let pageToken = "";
  do {
    const url = new URL(collectionUrl);
    url.searchParams.set("pageSize", "1000");
    if (pageToken) url.searchParams.set("pageToken", pageToken);
    const response = await fetch(url, { headers });
    if (response.status === 404) return [];
    if (!response.ok) throw new Error(`Mevcut takvim okunamadı: ${response.status} ${await response.text()}`);
    const page = await response.json();
    documents.push(...(page.documents ?? []));
    pageToken = page.nextPageToken ?? "";
  } while (pageToken);
  return documents;
}

async function importOne(item) {
  const { id, ...fields } = item;
  const url = `${collectionUrl}/${encodeURIComponent(id)}`;
  const response = await fetch(url, {
    method: "PATCH",
    headers,
    body: JSON.stringify({ fields: Object.fromEntries(Object.entries({
      ...fields,
      description: fields.description ?? "",
      academicYear: "2026-2027",
      active: true,
      sourceDataset: "akdeniz-2026-2027-pdf",
    }).map(([key, fieldValue]) => [key, value(fieldValue)])) }),
  });
  if (!response.ok) throw new Error(`${id}: ${response.status} ${await response.text()}`);
}

const existingDocuments = await listExistingDocuments();
for (let index = 0; index < events.length; index += 20) {
  await Promise.all(events.slice(index, index + 20).map(importOne));
}

const currentIds = new Set(events.map((item) => item.id));
const staleDocuments = existingDocuments.filter((document) => {
  const id = document.name.split("/").at(-1);
  const fields = document.fields ?? {};
  const academicYear = fields.academicYear?.stringValue;
  const sourceDataset = fields.sourceDataset?.stringValue;
  const editedFromPanel = Boolean(fields.updatedBy?.stringValue);
  return !currentIds.has(id) && (sourceDataset === "akdeniz-2026-2027-pdf" || (academicYear === "2026-2027" && !editedFromPanel));
});
for (let index = 0; index < staleDocuments.length; index += 20) {
  await Promise.all(staleDocuments.slice(index, index + 20).map(async (document) => {
    const response = await fetch(`https://firestore.googleapis.com/v1/${document.name}`, { method: "DELETE", headers });
    if (!response.ok) throw new Error(`Eski kayıt silinemedi: ${response.status} ${await response.text()}`);
  }));
}

console.log(`${events.length} akademik takvim kaydı yüklendi, ${staleDocuments.length} eski kayıt temizlendi.`);
