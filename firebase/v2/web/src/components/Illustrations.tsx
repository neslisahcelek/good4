import type { ReactNode } from "react";

/**
 * Small line illustrations for action cards and empty states: mint and cream
 * shapes with a thin charcoal outline and a Good4-green accent. Decorative only.
 */
export type IllustrationName = "coupon" | "message" | "users" | "bag" | "dining" | "megaphone" | "bell" | "spark" | "box";

const INK = "#1b1b1b";
const MINT = "#d7fec8";
const CREAM = "#faf5e3";
const GREEN = "#008556";
const LEAF = "#006a43";

// No fill here: shapes set their own, and the <svg> default is "none" for plain strokes.
const line = { stroke: INK, strokeWidth: 1.5, strokeLinecap: "round" as const, strokeLinejoin: "round" as const };

const art: Record<IllustrationName, ReactNode> = {
  coupon: (
    <>
      <circle cx="78" cy="38" r="26" fill={MINT} />
      <path d="M20 34h64v12a8 8 0 0 0 0 16v12H20V62a8 8 0 0 0 0-16V34Z" fill={CREAM} {...line} />
      <path d="M58 36v36" {...line} strokeDasharray="3 4" />
      <path d="m30 54 6 6 12-12" {...line} stroke={GREEN} strokeWidth={2.5} />
      <path d="M96 18v8M92 22h8" {...line} />
    </>
  ),
  message: (
    <>
      <circle cx="40" cy="34" r="24" fill={MINT} />
      <path d="M22 26h46a6 6 0 0 1 6 6v22a6 6 0 0 1-6 6H40l-12 10V60h-6a6 6 0 0 1-6-6V32a6 6 0 0 1 6-6Z" fill={CREAM} {...line} />
      <path d="M58 44h34a6 6 0 0 1 6 6v18a6 6 0 0 1-6 6h-4v8l-10-8H58a6 6 0 0 1-6-6V50a6 6 0 0 1 6-6Z" fill={GREEN} {...line} />
      <circle cx="34" cy="43" r="2.5" fill={INK} /><circle cx="44" cy="43" r="2.5" fill={INK} /><circle cx="54" cy="43" r="2.5" fill={INK} />
    </>
  ),
  users: (
    <>
      <circle cx="62" cy="46" r="30" fill={MINT} />
      <circle cx="46" cy="40" r="11" fill={CREAM} {...line} />
      <path d="M26 80a20 20 0 0 1 40 0" fill={GREEN} {...line} />
      <circle cx="76" cy="36" r="9" fill={CREAM} {...line} />
      <path d="M62 70a16 16 0 0 1 30 4" fill={CREAM} {...line} />
      <path d="M100 18l2 6 6 2-6 2-2 6-2-6-6-2 6-2 2-6Z" fill={GREEN} />
    </>
  ),
  bag: (
    <>
      <circle cx="60" cy="48" r="30" fill={MINT} />
      <path d="M34 38h52l-4 42H38l-4-42Z" fill={CREAM} {...line} />
      <path d="M48 38a12 12 0 0 1 24 0" {...line} />
      <path d="M86 64c10-2 14-10 14-18-8 0-14 6-14 18Z" fill={GREEN} {...line} />
      <path d="M50 58h20" {...line} />
    </>
  ),
  dining: (
    <>
      <circle cx="60" cy="46" r="30" fill={MINT} />
      <circle cx="60" cy="50" r="24" fill={CREAM} {...line} />
      <circle cx="60" cy="50" r="14" fill="#fff" {...line} />
      <path d="M24 30v14a5 5 0 0 0 10 0V30M29 44v34" {...line} />
      <path d="M94 78V30c-6 4-8 12-8 20h8" fill={GREEN} {...line} />
      <path d="M52 50c4-6 12-6 16 0" {...line} stroke={LEAF} />
    </>
  ),
  megaphone: (
    <>
      <circle cx="58" cy="46" r="30" fill={MINT} />
      <path d="M26 42v14h12l34 18V24L38 42H26Z" fill={CREAM} {...line} />
      <path d="M38 56l6 20h10l-4-18" fill={GREEN} {...line} />
      <path d="M84 38a14 14 0 0 1 0 22M92 30a26 26 0 0 1 0 38" {...line} />
    </>
  ),
  bell: (
    <>
      <circle cx="60" cy="44" r="30" fill={MINT} />
      <path d="M40 66V48a20 20 0 0 1 40 0v18l6 6H34l6-6Z" fill={CREAM} {...line} />
      <path d="M52 78a8 8 0 0 0 16 0" fill={GREEN} {...line} />
      <circle cx="82" cy="30" r="7" fill={GREEN} {...line} />
    </>
  ),
  spark: (
    <>
      <circle cx="60" cy="46" r="30" fill={MINT} />
      <path d="M60 18l7 21 21 7-21 7-7 21-7-21-21-7 21-7 7-21Z" fill={CREAM} {...line} />
      <path d="M92 66l3 8 8 3-8 3-3 8-3-8-8-3 8-3 3-8Z" fill={GREEN} />
      <circle cx="28" cy="72" r="4" {...line} />
    </>
  ),
  box: (
    <>
      <ellipse cx="60" cy="84" rx="38" ry="5" fill={MINT} />
      <path d="M28 44h64v38H28V44Z" fill={MINT} {...line} />
      <path d="M28 44 18 30h44l-2 14M92 44l10-14H58" fill={CREAM} {...line} />
      <path d="M50 60h20" {...line} />
      <path d="M60 14v8M44 18l4 6M76 18l-4 6" {...line} />
      <path d="M96 20l2 5 5 2-5 2-2 5-2-5-5-2 5-2 2-5Z" fill={GREEN} />
    </>
  ),
};

export function Illustration({ name, className = "" }: { name: IllustrationName; className?: string }) {
  return (
    <svg className={`illustration ${className}`.trim()} viewBox="0 0 120 96" fill="none" aria-hidden="true" focusable="false">
      {art[name]}
    </svg>
  );
}
