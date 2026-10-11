import { forwardRef, useEffect, useRef, useState, type ButtonHTMLAttributes, type HTMLAttributes, type ReactNode } from "react";

export type CommunitySection = "overview" | "events" | "coupons";
export type BusinessSection = "overview" | "verify" | "usage" | "profile";

type IconName = CommunitySection | "verify" | "history" | "profile" | "menu" | "close" | "logout" | "arrow" | "calendar"
  | "dining" | "dorm" | "heart" | "megaphone" | "message" | "bell" | "store" | "users" | "bag" | "spark" | "panel";

const iconPaths: Record<IconName, ReactNode> = {
  overview: <><path d="M4 13h6V4H4v9Zm0 7h6v-3H4v3Zm10 0h6v-9h-6v9Zm0-16v3h6V4h-6Z" /></>,
  events: <><path d="M7 3v3M17 3v3M4 9h16M5 5h14a1 1 0 0 1 1 1v14H4V6a1 1 0 0 1 1-1Z" /></>,
  coupons: <><path d="M5 5h14v4a3 3 0 0 0 0 6v4H5v-4a3 3 0 0 0 0-6V5Z" /><path d="M12 8v8" /></>,
  verify: <><path d="M5 5h14v4a3 3 0 0 0 0 6v4H5v-4a3 3 0 0 0 0-6V5Z" /><path d="m9 12 2 2 4-4" /></>,
  history: <><path d="M4 12a8 8 0 1 0 2.3-5.7L4 8.6" /><path d="M4 4v4.6h4.6M12 8v4l3 2" /></>,
  profile: <><path d="M12 13a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" /><path d="M5 21a7 7 0 0 1 14 0" /></>,
  menu: <><path d="M4 7h16M4 12h16M4 17h16" /></>,
  close: <><path d="m6 6 12 12M18 6 6 18" /></>,
  logout: <><path d="M10 5H5v14h5M14 8l4 4-4 4M18 12H9" /></>,
  arrow: <><path d="m9 18 6-6-6-6" /></>,
  calendar: <><path d="M7 3v3M17 3v3M4 9h16M5 5h14a1 1 0 0 1 1 1v14H4V6a1 1 0 0 1 1-1Z" /></>,
  dining: <><path d="M7 3v7a2 2 0 0 0 4 0V3M9 10v11M17 21V3c-2 1.5-3 4-3 7h3" /></>,
  dorm: <><path d="M4 21V9l8-5 8 5v12M9 21v-6h6v6" /></>,
  heart: <><path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10Z" /></>,
  megaphone: <><path d="M4 10v4h3l8 5V5L7 10H4Z" /><path d="M18 9a4 4 0 0 1 0 6" /></>,
  message: <><path d="M5 5h14v10H9l-4 4V5Z" /></>,
  bell: <><path d="M6 16V11a6 6 0 0 1 12 0v5l2 2H4l2-2Z" /><path d="M10 20a2 2 0 0 0 4 0" /></>,
  store: <><path d="M4 9 6 4h12l2 5M4 9h16v11H4V9Z" /><path d="M9 20v-6h6v6" /></>,
  users: <><path d="M9 12a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM3 20a6 6 0 0 1 12 0" /><path d="M16 5.5a3.5 3.5 0 0 1 0 6.5M18 14a6 6 0 0 1 3 6" /></>,
  bag: <><path d="M5 8h14l-1 12H6L5 8Z" /><path d="M9 8a3 3 0 0 1 6 0" /></>,
  spark: <><path d="M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5 18 18M18 6l-2.5 2.5M8.5 15.5 6 18" /></>,
  panel: <><path d="M4 5h16v14H4zM9 5v14" /></>,
};

export function CommunityIcon({ name }: { name: IconName }) {
  const filled = name === "overview";
  return (
    <svg
      aria-hidden="true"
      className="community-icon"
      viewBox="0 0 24 24"
      fill={filled ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {iconPaths[name]}
    </svg>
  );
}

type CommunityAppShellProps = {
  communityName: string;
  university: string;
  logoUrl: string;
  userEmail: string;
  activeSection: CommunitySection;
  counts: { events: number; coupons: number };
  onNavigate: (section: CommunitySection) => void;
  onLogout: () => void;
  children: ReactNode;
};

type AppShellItem = {
  id: string;
  icon: IconName;
  label: string;
  count?: number;
};

type AppShellProps = {
  brandLabel: string;
  productLabel: string;
  identityName: string;
  identityMeta: string;
  logoUrl: string;
  userEmail: string;
  activeItem: string;
  groups: Array<{ label?: string; items: AppShellItem[] }>;
  onNavigate: (itemId: string) => void;
  onLogout: () => void;
  children: ReactNode;
};

const SIDEBAR_COLLAPSED_KEY = "good4.panel.sidebarCollapsed";

function AppShell({
  brandLabel,
  productLabel,
  identityName,
  identityMeta,
  logoUrl,
  userEmail,
  activeItem,
  groups,
  onNavigate,
  onLogout,
  children,
}: AppShellProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  // Desktop-only preference; a blocked or empty storage simply keeps the full menu.
  const [collapsed, setCollapsed] = useState(() => {
    try { return window.localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === "1"; } catch { return false; }
  });

  function toggleCollapsed() {
    setCollapsed((current) => {
      try { window.localStorage.setItem(SIDEBAR_COLLAPSED_KEY, current ? "0" : "1"); } catch { /* keep in memory only */ }
      return !current;
    });
  }

  function navigate(itemId: string) {
    onNavigate(itemId);
    setMenuOpen(false);
  }

  return (
    <div className={`community-app-shell ${collapsed ? "is-collapsed" : ""}`.trim()}>
      <button
        className={`community-drawer-backdrop ${menuOpen ? "is-visible" : ""}`}
        aria-label="Menüyü kapat"
        tabIndex={menuOpen ? 0 : -1}
        onClick={() => setMenuOpen(false)}
      />

      <aside className={`community-sidebar ${menuOpen ? "is-open" : ""}`} aria-label={`${productLabel} menüsü`}>
        <div className="community-sidebar__brand">
          <img src="/good4-logo.png" alt="" />
          <span>{brandLabel}</span>
          <button className="community-sidebar__collapse" type="button" aria-label={collapsed ? "Menüyü genişlet" : "Menüyü daralt"}
            aria-pressed={collapsed} onClick={toggleCollapsed}>
            <CommunityIcon name="panel" />
          </button>
          <button className="community-sidebar__close" type="button" aria-label="Menüyü kapat" onClick={() => setMenuOpen(false)}>
            <CommunityIcon name="close" />
          </button>
        </div>

        <div className="community-sidebar__identity">
          {logoUrl ? (
            <img src={logoUrl} alt="" />
          ) : (
            <span aria-hidden="true">{identityName.slice(0, 1).toLocaleUpperCase("tr-TR")}</span>
          )}
          <div>
            <strong>{identityName}</strong>
            <small>{identityMeta}</small>
          </div>
        </div>

        <nav className="community-nav">
          {groups.map((group, groupIndex) => (
            <div className="community-nav__group" key={`${group.label ?? "main"}-${groupIndex}`}>
              {group.label ? <p>{group.label}</p> : null}
              {group.items.map((item) => (
                <NavButton
                  key={item.id}
                  active={activeItem === item.id}
                  icon={item.icon}
                  label={item.label}
                  count={item.count}
                  onClick={() => navigate(item.id)}
                />
              ))}
            </div>
          ))}
        </nav>

        <div className="community-sidebar__footer">
          <span title={userEmail}>{userEmail}</span>
          <button type="button" onClick={onLogout}>
            <CommunityIcon name="logout" />
            Çıkış
          </button>
        </div>
      </aside>

      <div className="community-workspace">
        <header className="community-topbar">
          <button
            className="community-menu-button"
            type="button"
            aria-label="Menüyü aç"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen(true)}
          >
            <CommunityIcon name="menu" />
          </button>
          <div className="community-topbar__title">
            <strong>{identityName}</strong>
            <span>{productLabel}</span>
          </div>
          <div className="community-topbar__account">
            <span className="community-topbar__avatar" aria-hidden="true">{identityName.slice(0, 1).toLocaleUpperCase("tr-TR")}</span>
            <div>
              <strong>{identityName}</strong>
              <span title={userEmail}>{userEmail}</span>
            </div>
            <button type="button" className="community-topbar__logout" onClick={onLogout} aria-label="Çıkış">
              <CommunityIcon name="logout" />
            </button>
          </div>
        </header>
        <main className="community-main">{children}</main>
      </div>
    </div>
  );
}

export function CommunityAppShell({
  communityName,
  university,
  logoUrl,
  userEmail,
  activeSection,
  counts,
  onNavigate,
  onLogout,
  children,
}: CommunityAppShellProps) {
  return (
    <AppShell
      brandLabel="Good4"
      productLabel="Topluluk Yönetici Paneli"
      identityName={communityName}
      identityMeta={university}
      logoUrl={logoUrl}
      userEmail={userEmail}
      activeItem={activeSection}
      groups={[
        { items: [{ id: "overview", icon: "overview", label: "Genel Bakış" }] },
        { label: "Yönetim", items: [
          { id: "events", icon: "events", label: "Etkinlikler", count: counts.events },
          { id: "coupons", icon: "coupons", label: "Kuponlar", count: counts.coupons },
        ] },
      ]}
      onNavigate={(itemId) => onNavigate(itemId as CommunitySection)}
      onLogout={onLogout}
    >
      {children}
    </AppShell>
  );
}

export function BusinessAppShell({
  businessName,
  userEmail,
  activeSection,
  usageCount,
  onNavigate,
  onLogout,
  children,
}: {
  businessName: string;
  userEmail: string;
  activeSection: BusinessSection;
  usageCount: number;
  onNavigate: (section: BusinessSection) => void;
  onLogout: () => void;
  children: ReactNode;
}) {
  return (
    <AppShell
      brandLabel="Good4 Business"
      productLabel="İşletme Paneli"
      identityName={businessName}
      identityMeta="Good4 işletmesi"
      logoUrl=""
      userEmail={userEmail}
      activeItem={activeSection}
      groups={[
        { items: [{ id: "overview", icon: "overview", label: "Genel Bakış" }] },
        { label: "İşlemler", items: [
          { id: "verify", icon: "verify", label: "Kupon Doğrula" },
          { id: "usage", icon: "history", label: "Kullanımlar", count: usageCount },
        ] },
        { label: "Hesap", items: [{ id: "profile", icon: "profile", label: "İşletme Profili" }] },
      ]}
      onNavigate={(itemId) => onNavigate(itemId as BusinessSection)}
      onLogout={onLogout}
    >
      {children}
    </AppShell>
  );
}

export type AdminSection = "home" | "notifications" | "coupons" | "menu" | "kyk" | "suspended" | "ads" | "calendar" | "feedback"
  | "businesses" | "communities" | "market" | "social" | "audit";

export function AdminAppShell({
  displayName,
  userEmail,
  activeSection,
  counts,
  socialLabel,
  onNavigate,
  onLogout,
  children,
}: {
  displayName: string;
  userEmail: string;
  activeSection: AdminSection;
  counts: { coupons?: number; feedback?: number; communities?: number; businesses?: number; market?: number; social?: number };
  socialLabel: string;
  onNavigate: (section: AdminSection) => void;
  onLogout: () => void;
  children: ReactNode;
}) {
  // Badges only for queues that need action; zero is shown as no badge.
  const badge = (value?: number) => (value ? value : undefined);
  return (
    <AppShell
      brandLabel="Good4"
      productLabel="Yönetim Paneli"
      identityName={displayName}
      identityMeta="Good4 yönetimi"
      logoUrl=""
      userEmail={userEmail}
      activeItem={activeSection}
      groups={[
        { items: [
          { id: "home", icon: "overview", label: "Genel bakış" },
          { id: "coupons", icon: "coupons", label: "Bekleyen kuponlar", count: badge(counts.coupons) },
          { id: "feedback", icon: "message", label: "Bildirimler ve geri bildirimler", count: badge(counts.feedback) },
          { id: "notifications", icon: "bell", label: "Telefon bildirimleri" },
        ] },
        { label: "Kampüs", items: [
          { id: "menu", icon: "dining", label: "Yemek Menüsü" },
          { id: "kyk", icon: "dorm", label: "KYK Menüsü" },
          { id: "suspended", icon: "heart", label: "Askıda Yemek" },
          { id: "calendar", icon: "calendar", label: "Akademik Takvim" },
          { id: "ads", icon: "megaphone", label: "Ana Sayfa Reklamı" },
        ] },
        { label: "Kurumlar", items: [
          { id: "businesses", icon: "store", label: "İşletmeler", count: badge(counts.businesses) },
          { id: "communities", icon: "users", label: "Topluluklar", count: badge(counts.communities) },
        ] },
        { label: "Öğrenciler", items: [
          { id: "market", icon: "bag", label: "Kampüs Dolabı", count: badge(counts.market) },
          { id: "social", icon: "spark", label: socialLabel, count: badge(counts.social) },
        ] },
        { label: "Sistem", items: [{ id: "audit", icon: "history", label: "İşlem geçmişi" }] },
      ]}
      onNavigate={(itemId) => onNavigate(itemId as AdminSection)}
      onLogout={onLogout}
    >
      {children}
    </AppShell>
  );
}

function NavButton({
  active,
  icon,
  label,
  count,
  onClick,
}: {
  active: boolean;
  icon: IconName;
  label: string;
  count?: number;
  onClick: () => void;
}) {
  return (
    <button type="button" className={active ? "is-active" : ""} aria-current={active ? "page" : undefined} title={label} onClick={onClick}>
      <CommunityIcon name={icon} />
      <span>{label}</span>
      {typeof count === "number" ? <small>{count}</small> : null}
    </button>
  );
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger";
  icon?: IconName;
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = "secondary", icon, className = "", children, ...props },
  ref,
) {
  return (
    <button ref={ref} className={`ui-button ui-button--${variant} ${className}`.trim()} {...props}>
      {icon ? <CommunityIcon name={icon} /> : null}
      {children}
    </button>
  );
});

export function Card({ className = "", children, ...props }: HTMLAttributes<HTMLElement>) {
  return <section className={`ui-card ${className}`.trim()} {...props}>{children}</section>;
}

export function Table({ label, children, className = "" }: { label: string; children: ReactNode; className?: string }) {
  return (
    <div className={`ui-table ${className}`.trim()} role="table" aria-label={label}>
      {children}
    </div>
  );
}

export function ConfirmationDialog({
  open,
  title,
  description,
  confirmLabel,
  busy = false,
  onConfirm,
  onClose,
}: {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  busy?: boolean;
  onConfirm: () => void;
  onClose: () => void;
}) {
  const cancelButton = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;
    cancelButton.current?.focus();
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !busy) onClose();
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [busy, onClose, open]);

  if (!open) return null;

  return (
    <div className="ui-modal" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget && !busy) onClose();
    }}>
      <div className="ui-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="confirmation-title" aria-describedby="confirmation-description">
        <h2 id="confirmation-title">{title}</h2>
        <p id="confirmation-description">{description}</p>
        <div className="ui-modal__actions">
          <Button ref={cancelButton} type="button" variant="secondary" disabled={busy} onClick={onClose}>Vazgeç</Button>
          <Button type="button" variant="danger" disabled={busy} onClick={onConfirm}>{busy ? "İşleniyor…" : confirmLabel}</Button>
        </div>
      </div>
    </div>
  );
}

export function PageHeader({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <header className="community-page-header">
      <div>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action ? <div className="community-page-header__action">{action}</div> : null}
    </header>
  );
}

export function StatCard({ label, value, detail }: { label: string; value: ReactNode; detail: string }) {
  return (
    <article className="community-stat-card">
      <p>{label}</p>
      <strong>{value}</strong>
      <span>{detail}</span>
    </article>
  );
}

export function StatusBadge({ status }: { status: "published" | "pending" | "cancelled" }) {
  const label = status === "published" ? "Yayında" : status === "pending" ? "Onay bekliyor" : "Yayından kaldırıldı";
  return <span className={`ui-badge ui-badge--${status}`}>{label}</span>;
}

export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return (
    <div className="community-empty-state">
      <span className="community-empty-state__icon"><CommunityIcon name="calendar" /></span>
      <h3>{title}</h3>
      <p>{description}</p>
      {action ? <div>{action}</div> : null}
    </div>
  );
}

export function CommunityDashboardSkeleton() {
  return (
    <div className="community-skeleton" aria-label="Topluluk bilgileri yükleniyor" aria-busy="true">
      <div className="skeleton-line skeleton-line--title" />
      <div className="skeleton-line skeleton-line--copy" />
      <div className="community-skeleton__stats">
        {[0, 1, 2, 3].map((item) => <div className="skeleton-card" key={item} />)}
      </div>
      <div className="skeleton-panel" />
    </div>
  );
}
