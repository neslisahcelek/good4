import PDFKit
let doc = PDFDocument(url: URL(fileURLWithPath: CommandLine.arguments[1]))!
for i in 0..<doc.pageCount { print("=== PAGE \(i+1)"); print(doc.page(at: i)?.string ?? "") }
