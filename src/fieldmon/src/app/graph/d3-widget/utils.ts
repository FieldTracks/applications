import {D3Link, D3Node} from "./model";

export class DrawUtils {

  static drawNode(node: D3Node, ctx: CanvasRenderingContext2D): void {
    const fillColor = '#F00'
    ctx.beginPath()
    ctx.arc(node.x, node.y, 8,0, 2*Math.PI)
    ctx.fillStyle = fillColor
    ctx.fill()
  }

  static drawLink(link: D3Link, ctx: CanvasRenderingContext2D): void {
    const gradient = ctx.createLinearGradient(link.source.x, link.source.y, link.target.x, link.target.y)
    // TODO: define nice gradient for stones and contacts
    gradient.addColorStop(0.45, '#000')
    gradient.addColorStop(0.55, '#000')

    ctx.beginPath()
    ctx.moveTo(link.source.x, link.source.y)
    ctx.lineTo(link.target.x, link.target.y)
    ctx.strokeStyle = gradient
    ctx.globalAlpha = 0.2
    ctx.lineWidth = 1.5
    ctx.stroke()
    ctx.globalAlpha = 1

  }

}

