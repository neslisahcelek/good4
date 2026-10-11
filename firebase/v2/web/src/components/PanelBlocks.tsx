import { useId, type ReactNode } from "react";
import { Illustration, type IllustrationName } from "./Illustrations";

/** "i" next to a title; the text is read by screen readers and shown on hover/focus. */
export function InfoHint({ text }: { text: string }) {
  const id = useId();
  return (
    <span className="info-hint">
      <button type="button" className="info-hint__button" aria-describedby={id} aria-label="Bilgi">i</button>
      <span role="tooltip" id={id} className="info-hint__bubble">{text}</span>
    </span>
  );
}

/** Cream band that opens a page: eyebrow, large title, short text and up to two actions. */
export function HeroBand({ eyebrow, title, children, actions }: {
  eyebrow?: string;
  title: string;
  children?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <section className="hero-band">
      {eyebrow ? <p className="hero-band__eyebrow">{eyebrow}</p> : null}
      <h2>{title}</h2>
      {children ? <div className="hero-band__text">{children}</div> : null}
      {actions ? <div className="hero-band__actions">{actions}</div> : null}
    </section>
  );
}

/** Illustration on the left, title, text and one pill action on the right. */
export function ActionCard({ illustration, title, count, children, actionLabel, onAction, compact = false }: {
  illustration: IllustrationName;
  title: string;
  count?: number;
  children: ReactNode;
  actionLabel: string;
  onAction: () => void;
  compact?: boolean;
}) {
  return (
    <article className={`action-card ${compact ? "action-card--compact" : ""}`.trim()}>
      <Illustration name={illustration} className="action-card__art" />
      <div className="action-card__body">
        <h3>
          {title}
          {typeof count === "number" && count > 0 ? <span className="action-card__count">{count}</span> : null}
        </h3>
        <p>{children}</p>
        <button type="button" className="pill-button" onClick={onAction}>{actionLabel}</button>
      </div>
    </article>
  );
}

/** Titled list of label/value rows, like a plan-usage card. */
export function SummaryCard({ title, hint, rows, footer }: {
  title: string;
  hint?: string;
  rows: Array<{ label: string; value: ReactNode; detail?: string }>;
  footer?: ReactNode;
}) {
  return (
    <section className="summary-card">
      <h3>{title}{hint ? <InfoHint text={hint} /> : null}</h3>
      <dl>
        {rows.map((row) => (
          <div key={row.label}>
            <dt>{row.label}</dt>
            <dd><strong>{row.value}</strong>{row.detail ? <span>{row.detail}</span> : null}</dd>
          </div>
        ))}
      </dl>
      {footer ? <div className="summary-card__footer">{footer}</div> : null}
    </section>
  );
}

/** Empty state with an illustration, bold title and short explanation. */
export function IllustratedEmpty({ illustration = "box", title, children }: {
  illustration?: IllustrationName;
  title: string;
  children?: ReactNode;
}) {
  return (
    <div className="illustrated-empty">
      <Illustration name={illustration} />
      <h3>{title}</h3>
      {children ? <p>{children}</p> : null}
    </div>
  );
}
