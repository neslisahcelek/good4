import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";
import { httpsCallable } from "firebase/functions";
import { functions } from "../firebase";
import { normalizeGoogleMapsUrl } from "../../../functions/src/businessDiscountMaps";
import { createAccountRequestScope } from "../accountRequestScope";
import {
  discountDates,
  discountNumbers,
  istanbulDateTime,
} from "../businessDiscountForm";
import {
  Button,
  Card,
  ConfirmationDialog,
  EmptyState,
  PageHeader,
} from "./CommunityUi";

type Discount = {
  id: string;
  businessId: string;
  businessName: string;
  title: string;
  conditions: string;
  mapsUrl?: string;
  discountValue: number;
  totalLimit: number | null;
  redemptionCount: number;
  startsAt: string;
  endsAt: string;
  status: string;
};
type Page = {
  discounts: Discount[];
  businesses: Array<{ id: string; name: string }>;
  nextCursor: string | null;
  businessesNextCursor: string | null;
};
const list = httpsCallable<Record<string, unknown>, Page>(
  functions,
  "listBusinessDiscounts",
);
const save = httpsCallable<Record<string, unknown>, unknown>(
  functions,
  "saveBusinessDiscount",
);
const end = httpsCallable<{ discountId: string }, unknown>(
  functions,
  "endBusinessDiscount",
);
const blank = {
  discountId: "",
  businessId: "",
  title: "",
  conditions: "",
  mapsUrl: "",
  percentage: "",
  quota: "",
  startsAt: "",
  endsAt: "",
};
const dateLabel = (value: string) =>
  new Intl.DateTimeFormat("tr-TR", {
    timeZone: "Europe/Istanbul",
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));

export function BusinessDiscountsAdmin({ uid }: { uid: string }) {
  const scope = useRef(createAccountRequestScope());
  const [page, setPage] = useState<Page>({
    discounts: [],
    businesses: [],
    nextCursor: null,
    businessesNextCursor: null,
  });
  const [form, setForm] = useState(blank);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [ending, setEnding] = useState<Discount | null>(null);
  const load = useCallback(
    async (
      kind: "refresh" | "discounts" | "businesses" = "refresh",
      cursor?: string,
    ) => {
      const current = scope.current.start(uid);
      if (!current) return;
      setBusy(true);
      setError("");
      try {
        const response = await list(
          kind === "businesses"
            ? { businessesOnly: true, businessesCursor: cursor }
            : kind === "discounts"
              ? { cursor, includeBusinesses: false }
              : {},
        );
        if (!current()) return;
        setPage((previous) =>
          kind === "refresh"
            ? response.data
            : kind === "businesses"
              ? {
                  ...previous,
                  businesses: [
                    ...previous.businesses,
                    ...response.data.businesses,
                  ],
                  businessesNextCursor: response.data.businessesNextCursor,
                }
              : {
                  ...previous,
                  discounts: [
                    ...previous.discounts,
                    ...response.data.discounts,
                  ],
                  nextCursor: response.data.nextCursor,
                },
        );
      } catch {
        if (current()) setError("İndirimler yüklenemedi. Tekrar dene.");
      } finally {
        if (current()) setBusy(false);
      }
    },
    [uid],
  );
  useEffect(() => {
    scope.current.setAccount(uid);
    void load();
    return () => scope.current.setAccount(null);
  }, [uid, load]);
  async function submit(event: FormEvent) {
    event.preventDefault();
    const current = scope.current.start(uid);
    if (!current) return;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await save({
        ...(form.discountId ? { discountId: form.discountId } : {}),
        businessId: form.businessId,
        title: form.title.trim(),
        conditions: form.conditions.trim(),
        mapsUrl: normalizeGoogleMapsUrl(form.mapsUrl),
        ...discountNumbers(form.percentage, form.quota),
        ...discountDates(form.startsAt, form.endsAt),
      });
      if (!current()) return;
      setForm(blank);
      setNotice("İndirim kaydedildi.");
      await load();
    } catch (failure) {
      if (current())
        setError(
          failure instanceof Error ? failure.message : "İndirim kaydedilemedi.",
        );
    } finally {
      if (current()) setBusy(false);
    }
  }
  async function endDiscount() {
    if (!ending) return;
    const current = scope.current.start(uid);
    if (!current) return;
    setBusy(true);
    setError("");
    try {
      await end({ discountId: ending.id });
      if (!current()) return;
      setEnding(null);
      setNotice("İndirim yayından kaldırıldı.");
      await load();
    } catch {
      if (current()) setError("İndirim kaldırılamadı. Tekrar dene.");
    } finally {
      if (current()) setBusy(false);
    }
  }
  return (
    <>
      <PageHeader
        title="İndirimler"
        description="Good4’un anlaşmalı işletme indirimlerini oluştur ve yönet."
      />
      {error && (
        <p className="inline-message inline-message--error" role="alert">
          {error}
        </p>
      )}
      {notice && (
        <p className="inline-message inline-message--success" role="status">
          {notice}
        </p>
      )}
      <Card>
        <h2>{form.discountId ? "İndirimi düzenle" : "İndirim ekle"}</h2>
        <form
          className="discount-admin-form"
          onSubmit={(event) => void submit(event)}
        >
          <label>
            İşletme
            <select
              required
              value={form.businessId}
              disabled={busy || Boolean(form.discountId)}
              onChange={(event) =>
                setForm({ ...form, businessId: event.target.value })
              }
            >
              <option value="">Onaylanmış işletme seç</option>
              {page.businesses.map((business) => (
                <option key={business.id} value={business.id}>
                  {business.name}
                </option>
              ))}
              {form.discountId &&
                !page.businesses.some(
                  (business) => business.id === form.businessId,
                ) && (
                  <option value={form.businessId}>
                    {
                      page.discounts.find(
                        (discount) => discount.id === form.discountId,
                      )?.businessName
                    }
                  </option>
                )}
            </select>
          </label>
          {page.businessesNextCursor && (
            <Button
              type="button"
              disabled={busy}
              onClick={() =>
                void load("businesses", page.businessesNextCursor!)
              }
            >
              Diğer işletmeleri yükle
            </Button>
          )}
          <label>
            Başlık
            <input
              required
              maxLength={100}
              value={form.title}
              onChange={(event) =>
                setForm({ ...form, title: event.target.value })
              }
            />
          </label>
          <label>
            Yüzde indirim
            <input
              required
              type="number"
              min="1"
              max="100"
              step="1"
              value={form.percentage}
              onChange={(event) =>
                setForm({ ...form, percentage: event.target.value })
              }
            />
          </label>
          <label>
            Kapsam ve koşullar
            <textarea
              required
              maxLength={2000}
              rows={4}
              value={form.conditions}
              onChange={(event) =>
                setForm({ ...form, conditions: event.target.value })
              }
            />
          </label>
          <label>
            Google Maps konum bağlantısı (isteğe bağlı)
            <input
              type="url"
              maxLength={2048}
              placeholder="https://maps.app.goo.gl/…"
              value={form.mapsUrl}
              onChange={(event) =>
                setForm({ ...form, mapsUrl: event.target.value })
              }
            />
            <small>
              Google Maps’te mekanın paylaşım bağlantısını kopyalayıp buraya
              yapıştır. Öğrenci indirim detayından konumu açabilir.
            </small>
          </label>
          <label>
            Başlangıç
            <input
              required
              type="datetime-local"
              value={form.startsAt}
              onChange={(event) =>
                setForm({ ...form, startsAt: event.target.value })
              }
            />
          </label>
          <label>
            Son kullanım tarihi
            <input
              required
              type="datetime-local"
              value={form.endsAt}
              onChange={(event) =>
                setForm({ ...form, endsAt: event.target.value })
              }
            />
          </label>
          <label>
            Toplam kullanım limiti
            <input
              type="number"
              min="1"
              max="1000000"
              step="1"
              placeholder="Sınırsız için boş bırak"
              value={form.quota}
              onChange={(event) =>
                setForm({ ...form, quota: event.target.value })
              }
            />
          </label>
          <p>
            Her öğrenci aynı indirimi günde bir kez kullanabilir. Düzenleme,
            önceki kullanılmamış kodları geçersiz kılar.
          </p>
          <div className="review-actions">
            <Button type="submit" disabled={busy}>
              {busy ? "Kaydediliyor…" : "Kaydet"}
            </Button>
            {form.discountId && (
              <Button
                type="button"
                disabled={busy}
                onClick={() => setForm(blank)}
              >
                Vazgeç
              </Button>
            )}
          </div>
        </form>
      </Card>
      <section className="admin-section">
        <div className="section-title-row">
          <h2>Kayıtlı indirimler</h2>
          <Button disabled={busy} onClick={() => void load()}>
            Yenile
          </Button>
        </div>
        {!busy && page.discounts.length === 0 && (
          <EmptyState
            title="Henüz indirim yok"
            description="Onaylı bir işletme seçerek ilk indirimi ekleyebilirsin."
          />
        )}
        <div className="coupon-review-list">
          {page.discounts.map((discount) => (
            <article
              key={discount.id}
              className="admin-card coupon-review-card"
            >
              <div className="coupon-review-main">
                <h3>
                  {discount.title} · %{discount.discountValue}
                </h3>
                <p>
                  <strong>{discount.businessName}</strong>
                </p>
                {discount.mapsUrl && (
                  <p>
                    <a
                      href={discount.mapsUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      Google Maps’te konumu aç
                    </a>
                  </p>
                )}
                <p className="coupon-description">{discount.conditions}</p>
                <p>
                  {dateLabel(discount.startsAt)} – {dateLabel(discount.endsAt)}
                </p>
                <p>
                  {discount.status === "ended"
                    ? "Yayından kaldırıldı"
                    : Date.now() >= Date.parse(discount.endsAt)
                      ? "Süresi doldu"
                      : Date.now() < Date.parse(discount.startsAt)
                        ? "Yakında başlayacak"
                        : "Yayında"}{" "}
                  · {discount.redemptionCount} kullanım ·{" "}
                  {discount.totalLimit === null
                    ? "Sınırsız"
                    : `Toplam limit: ${discount.totalLimit}`}
                </p>
              </div>
              <div className="review-actions">
                <Button
                  disabled={busy}
                  onClick={() => {
                    setForm({
                      discountId: discount.id,
                      businessId: discount.businessId,
                      title: discount.title,
                      conditions: discount.conditions,
                      mapsUrl: discount.mapsUrl ?? "",
                      percentage: String(discount.discountValue),
                      quota:
                        discount.totalLimit === null
                          ? ""
                          : String(discount.totalLimit),
                      startsAt: istanbulDateTime(discount.startsAt),
                      endsAt: istanbulDateTime(discount.endsAt),
                    });
                    setError("");
                  }}
                >
                  Düzenle
                </Button>
                {discount.status === "published" && (
                  <Button disabled={busy} onClick={() => setEnding(discount)}>
                    Yayından kaldır
                  </Button>
                )}
              </div>
            </article>
          ))}
        </div>
        {page.nextCursor && (
          <Button
            disabled={busy}
            onClick={() => void load("discounts", page.nextCursor!)}
          >
            Diğer indirimleri yükle
          </Button>
        )}
      </section>
      {ending && (
        <ConfirmationDialog
          title="İndirimi yayından kaldır"
          description={`${ending.title} artık kullanılamayacak.`}
          confirmLabel="Yayından kaldır"
          onConfirm={() => void endDiscount()}
          open
          onClose={() => setEnding(null)}
          busy={busy}
        />
      )}
    </>
  );
}
