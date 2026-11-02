import AppKit
let args=CommandLine.arguments
let root=URL(fileURLWithPath:args[1])
func board(_ names:[String], _ titles:[String], _ output:String, columns:Int, width:CGFloat) {
 let gap:CGFloat=24, margin:CGFloat=28, header:CGFloat=100, footer:CGFloat=54
 let first=NSBitmapImageRep(data:try! Data(contentsOf:root.appendingPathComponent(names[0]+".png")))!
 let height=width*CGFloat(first.pixelsHigh)/CGFloat(first.pixelsWide)
 let rows=Int(ceil(Double(names.count)/Double(columns)))
 let size=NSSize(width:margin*2+CGFloat(columns)*width+CGFloat(columns-1)*gap,height:header+CGFloat(rows)*(height+44)+footer)
 let canvas=NSImage(size:size);canvas.lockFocus()
 NSColor(calibratedRed:0.93,green:0.94,blue:0.91,alpha:1).setFill();NSBezierPath(rect:NSRect(origin:.zero,size:size)).fill()
 func text(_ value:String,_ x:CGFloat,_ y:CGFloat,_ font:NSFont) {
  (value as NSString).draw(at:NSPoint(x:x,y:y),withAttributes:[.font:font,.foregroundColor:NSColor(calibratedRed:0.15,green:0.18,blue:0.14,alpha:1)])
 }
 text("Préviens-moi · proposition UI",margin,size.height-42,NSFont.systemFont(ofSize:22,weight:.semibold))
 for (index,name) in names.enumerated() {
  let column=index%columns,row=index/columns
  let x=margin+CGFloat(column)*(width+gap),top=size.height-header-CGFloat(row)*(height+44)
  text(titles[index],x,top+14,NSFont.systemFont(ofSize:16,weight:.medium))
  let image=NSImage(contentsOf:root.appendingPathComponent(name+".png"))!
  image.draw(in:NSRect(x:x,y:top-height,width:width,height:height),from:.zero,operation:.copy,fraction:1)
 }
 text("Captures Android natives · données d’exemple · à valider",margin,18,NSFont.systemFont(ofSize:13))
 canvas.unlockFocus()
 let png=NSBitmapImageRep(data:canvas.tiffRepresentation!)!.representation(using:.png,properties:[:])!
 try! png.write(to:root.appendingPathComponent(output+".png"))
}
board(["home","active"],["Mes suivis","Détail · actif"],"presentation",columns:2,width:486)
board(["paused","preparing","loading","active-dark"],["En pause","En préparation","Chargement","Mode sombre"],"etats",columns:4,width:270)
