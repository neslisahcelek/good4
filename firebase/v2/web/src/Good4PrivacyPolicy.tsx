import { useEffect, type ReactNode } from "react";
import policyMarkdown from "./good4-gizlilik-politikasi-v1.3.md?raw";

function inlineMarkdown(value: string): ReactNode[] {
  return value.split(/(\*\*[^*]+\*\*)/g).filter(Boolean).map((part, index) =>
    part.startsWith("**") && part.endsWith("**")
      ? <strong key={index}>{part.slice(2, -2)}</strong>
      : <span key={index}>{part}</span>,
  );
}

function renderMarkdown(markdown: string): ReactNode[] {
  const lines = markdown.split(/\r?\n/);
  const blocks: ReactNode[] = [];
  let index = 0;

  while (index < lines.length) {
    const line = lines[index]?.trim() ?? "";
    if (!line) {
      index += 1;
      continue;
    }
    if (line.startsWith("# ") || line.startsWith("## ")) {
      blocks.push(<h2 key={index}>{line.replace(/^#+\s*/, "")}</h2>);
      index += 1;
      continue;
    }
    if (line.startsWith("* ")) {
      const items: string[] = [];
      while (index < lines.length && (lines[index]?.trim().startsWith("* ") ?? false)) {
        items.push((lines[index]?.trim() ?? "").slice(2));
        index += 1;
      }
      blocks.push(<ul key={`list-${index}`}>{items.map((item, itemIndex) => <li key={itemIndex}>{inlineMarkdown(item)}</li>)}</ul>);
      continue;
    }

    const paragraph = [line];
    index += 1;
    while (index < lines.length) {
      const next = lines[index]?.trim() ?? "";
      if (!next || next.startsWith("# ") || next.startsWith("## ") || next.startsWith("* ")) break;
      paragraph.push(next);
      index += 1;
    }
    blocks.push(<p key={`paragraph-${index}`}>{inlineMarkdown(paragraph.join(" "))}</p>);
  }

  return blocks;
}

export default function Good4PrivacyPolicy() {
  useEffect(() => {
    const previousTitle = document.title;
    document.title = "Good4 Gizlilik Politikası · Sürüm 1.3";
    return () => { document.title = previousTitle; };
  }, []);

  return (
    <div className="privacy-page">
      <header className="privacy-header">
        <a className="privacy-brand" href="https://good4tr.com" aria-label="Good4 ana sayfa">
          <img src="/good4-logo.png" alt="" />
          <span>Good4</span>
        </a>
        <nav className="legal-header-links" aria-label="Yasal sayfalar">
          <a href="/gizlilik">KVKK Aydınlatma Metni</a>
          <a href="/uyelik-sozlesmesi">Kullanıcı Sözleşmesi</a>
          <a className="privacy-home-link" href="https://good4tr.com">Ana sayfa</a>
        </nav>
      </header>
      <main className="privacy-content">
        <div className="privacy-intro">
          <p className="privacy-eyebrow">Good4 · Gizlilik</p>
          <h1>Gizlilik Politikası</h1>
          <p>Good4 mobil uygulaması ve bağlantılı hizmetlerdeki bilgi işleme uygulamaları.</p>
          <time dateTime="2026-09-25">Sürüm 1.3 · Son güncelleme: 25 Eylül 2026</time>
        </div>
        <article className="privacy-document">{renderMarkdown(policyMarkdown)}</article>
      </main>
      <footer className="privacy-footer">
        <span>© 2026 Good4</span>
        <a href="/gizlilik">KVKK Aydınlatma Metni</a>
        <a href="/hesabimi-sil">Hesap silme</a>
        <a href="mailto:cannklnc7@gmail.com">İletişim</a>
      </footer>
    </div>
  );
}
