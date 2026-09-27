import path from "node:path";
import sharp from "sharp";
import { createWorker, PSM } from "tesseract.js";
import type { OcrResult } from "./diningMenuImport.js";

// Turkish language data ships with the function so OCR never downloads anything at run time.
const LANG_PATH = path.join(path.dirname(require.resolve("@tesseract.js-data/tur/package.json")), "4.0.0_best_int");

/**
 * Reads the SKS menu image with Tesseract. The cards sit on coloured paper, so the image is
 * greyscaled and contrast-stretched first, and sparse-text mode lets each card be found on its own.
 */
export async function recognizeMenuImage(image: Buffer): Promise<OcrResult> {
  const source = sharp(image);
  const { width = 0 } = await source.metadata();
  const prepared = await source.greyscale().normalise().linear(1.6, -60).png().toBuffer();

  const worker = await createWorker("tur", 1, { langPath: LANG_PATH, cacheMethod: "none", gzip: true });
  try {
    await worker.setParameters({ tessedit_pageseg_mode: PSM.SPARSE_TEXT, preserve_interword_spaces: "1" });
    const { data } = await worker.recognize(prepared, {}, { blocks: true });
    const lines = (data.blocks ?? []).flatMap((block) =>
      block.paragraphs.flatMap((paragraph) =>
        paragraph.lines.map((line) => ({
          text: line.text.trim(),
          x0: line.bbox.x0,
          y0: line.bbox.y0,
          x1: line.bbox.x1,
          y1: line.bbox.y1,
        })),
      ),
    );
    return { width, lines };
  } finally {
    await worker.terminate();
  }
}
