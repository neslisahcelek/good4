import { forwardRef, useEffect, useRef, useState, type ButtonHTMLAttributes, type HTMLAttributes, type ReactNode } from "react";

export type CommunitySection = "overview" | "events" | "coupons";
export type BusinessSection = "overview" | "verify" | "usage" | "profile";

type IconName = CommunitySection | "verify" | "history" | "profile" | "menu" | "close" | "logout" | "arrow" | "calendar";

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

  function navigate(itemId: string) {
    onNavigate(itemId);
    setMenuOpen(false);
  }

  return (
    <div className="community-app-shell">
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
          <div>
            <strong>{identityName}</strong>
            <span>{productLabel}</span>
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
    <button type="button" className={active ? "is-active" : ""} aria-current={active ? "page" : undefined} onClick={onClick}>
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
