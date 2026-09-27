import PDFKit
import AppKit
let doc = PDFDocument(url: URL(fileURLWithPath: CommandLine.arguments[1]))!
let page = doc.page(at: 0)!
let img = page.thumbnail(of: CGSize(width: 1600, height: 1600), for: .mediaBox)
let rep = NSBitmapImageRep(data: img.tiffRepresentation!)!
try! rep.representation(using: .jpeg, properties: [.compressionFactor: 0.8])!.write(to: URL(fileURLWithPath: CommandLine.arguments[2]))
