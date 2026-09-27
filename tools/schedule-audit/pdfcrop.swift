import PDFKit
import AppKit
// args: pdf out page x0 y0 x1 y1 (fractions of page, origin top-left) scale
let a = CommandLine.arguments
let doc = PDFDocument(url: URL(fileURLWithPath: a[1]))!
let page = doc.page(at: Int(a[3])! - 1)!
let b = page.bounds(for: .mediaBox)
let scale = CGFloat(Double(a[8])!)
let x0 = CGFloat(Double(a[4])!), y0 = CGFloat(Double(a[5])!), x1 = CGFloat(Double(a[6])!), y1 = CGFloat(Double(a[7])!)
let w = Int(b.width * (x1 - x0) * scale), h = Int(b.height * (y1 - y0) * scale)
let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: w, pixelsHigh: h, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
let ctx = NSGraphicsContext(bitmapImageRep: rep)!
NSGraphicsContext.current = ctx
let cg = ctx.cgContext
cg.setFillColor(NSColor.white.cgColor); cg.fill(CGRect(x: 0, y: 0, width: w, height: h))
cg.scaleBy(x: scale, y: scale)
cg.translateBy(x: -b.width * x0, y: -b.height * (1 - y1))
page.draw(with: .mediaBox, to: cg)
try! rep.representation(using: .jpeg, properties: [.compressionFactor: 0.85])!.write(to: URL(fileURLWithPath: a[2]))
