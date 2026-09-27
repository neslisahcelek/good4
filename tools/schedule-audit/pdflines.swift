import PDFKit
// Prints every text line of a PDF with its bounds in image coordinates (origin top-left)
// at the given scale, matching pdfcrop.swift output: PAGE<TAB>x0<TAB>y0<TAB>x1<TAB>y1<TAB>text
let a = CommandLine.arguments
let doc = PDFDocument(url: URL(fileURLWithPath: a[1]))!
let scale = CGFloat(Double(a.count > 2 ? a[2] : "2")!)
for i in 0..<doc.pageCount {
  guard let page = doc.page(at: i), let all = page.selection(for: page.bounds(for: .mediaBox)) else { continue }
  let h = page.bounds(for: .mediaBox).height
  for line in all.selectionsByLine() {
    let b = line.bounds(for: page)
    let text = (line.string ?? "").replacingOccurrences(of: "\n", with: " ").trimmingCharacters(in: .whitespaces)
    if text.isEmpty { continue }
    print("\(i + 1)\t\(Int(b.minX * scale))\t\(Int((h - b.maxY) * scale))\t\(Int(b.maxX * scale))\t\(Int((h - b.minY) * scale))\t\(text)")
  }
}
