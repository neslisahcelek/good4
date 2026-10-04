import { useEffect, useRef, useState } from "react";

type Photo = { url: string; thumbUrl: string };

export function ListingPhotoViewer({ photos, title, initialIndex, onClose }: {
  photos: Photo[];
  title: string;
  initialIndex: number;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [index, setIndex] = useState(initialIndex);
  const [imageFailed, setImageFailed] = useState(false);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const previousOverflow = document.body.style.overflow;
    dialog.showModal();
    document.body.style.overflow = "hidden";
    return () => {
      dialog.close();
      document.body.style.overflow = previousOverflow;
      previousFocus?.focus({ preventScroll: true });
    };
  }, []);

  function moveTo(nextIndex: number) {
    if (nextIndex < 0 || nextIndex >= photos.length) return;
    setImageFailed(false);
    setIndex(nextIndex);
  }

  return (
    <dialog
      ref={dialogRef}
      className="market-photo-viewer"
      aria-label={`${title} — ilan fotoğrafları`}
      onCancel={onClose}
      onClose={() => { if (!dialogRef.current?.open) onClose(); }}
      onClick={(event) => { if (event.target === event.currentTarget) onClose(); }}
      onKeyDown={(event) => {
        if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
          event.preventDefault();
          moveTo(index + (event.key === "ArrowRight" ? 1 : -1));
        }
      }}
    >
      <div className="market-photo-viewer__content">
        <header className="market-photo-viewer__header">
          <span>{title}</span>
          <button type="button" onClick={onClose} aria-label="Fotoğrafı kapat">Kapat <span aria-hidden="true">×</span></button>
        </header>
        <div className="market-photo-viewer__image">
          {imageFailed ? (
            <p role="alert">Fotoğraf yüklenemedi.</p>
          ) : (
            <img
              key={photos[index].url}
              src={photos[index].url}
              alt={`${title}, fotoğraf ${index + 1}`}
              onError={() => setImageFailed(true)}
              draggable={false}
            />
          )}
        </div>
        {photos.length > 1 && (
          <footer className="market-photo-viewer__navigation">
            <button type="button" onClick={() => moveTo(index - 1)} disabled={index === 0} aria-label="Önceki fotoğraf">← Önceki</button>
            <span aria-live="polite">{index + 1} / {photos.length}</span>
            <button type="button" onClick={() => moveTo(index + 1)} disabled={index === photos.length - 1} aria-label="Sonraki fotoğraf">Sonraki →</button>
          </footer>
        )}
      </div>
    </dialog>
  );
}
