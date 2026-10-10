import { defineSecret } from "firebase-functions/params";
import { HttpsError } from "firebase-functions/v2/https";

export const brevoApiKey = defineSecret("BREVO_API_KEY");
export type VerificationEmailSender = (email: string, code: string) => Promise<void>;

/** No response bodies or transport errors escape: they can contain recipient data. */
export function brevoVerificationSender(
  key: () => string,
  transport: typeof fetch = fetch,
): VerificationEmailSender {
  return async (email, code) => {
    try {
      const apiKey = key();
      if (!apiKey) throw new Error();
      const response = await transport("https://api.brevo.com/v3/smtp/email", {
        method: "POST",
        headers: { "api-key": apiKey, "Content-Type": "application/json", Accept: "application/json" },
        signal: AbortSignal.timeout(10_000),
        body: JSON.stringify({
          sender: { name: "Good4", email: "noreply@good4tr.com" },
          to: [{ email }],
          subject: `Good4 okul e-postası doğrulama kodun: ${code}`,
          textContent: `Good4 okul e-postası doğrulama kodun: ${code}\n\nBu kod 10 dakika geçerli. Bu isteği sen yapmadıysan e-postayı yok sayabilirsin.`,
          htmlContent: `<p>Good4 okul e-postası doğrulama kodun:</p><p style="font-size:28px;font-weight:600;letter-spacing:6px">${code}</p><p>Bu kod 10 dakika geçerli. Bu isteği sen yapmadıysan e-postayı yok sayabilirsin.</p>`,
        }),
      });
      if (!response.ok) throw new Error();
    } catch {
      throw new HttpsError("unavailable", "EDU_EMAIL_SEND_FAILED");
    }
  };
}

export function isDemoMailEmulator(): boolean {
  return process.env.FUNCTIONS_EMULATOR === "true" && process.env.GCLOUD_PROJECT === "demo-good4-v2";
}

export const sendVerificationEmail: VerificationEmailSender = async (email, code) => {
  // Only the explicitly named demo Functions emulator can simulate delivery.
  // Never contact Brevo or read a production secret during local UI verification.
  if (isDemoMailEmulator()) return;
  await brevoVerificationSender(() => brevoApiKey.value())(email, code);
};
