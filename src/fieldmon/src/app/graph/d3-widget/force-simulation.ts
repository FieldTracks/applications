import {
  forceCenter,
  forceLink,
  forceManyBody,
  forceSimulation,
  Simulation,
  SimulationLinkDatum,
  SimulationNodeDatum
} from "d3-force";


import {AggregatedGraph} from "../../model/aggregated/aggregated-graph";

export class ForceGraph {
  private links: D3Link[] = []
  private nodeMap = new Map<string,D3Node>()
  private nodes: D3Node[] = []

  private force: Simulation<D3Node,D3Link>

  constructor(private canvas: HTMLCanvasElement, private repaintCb: () => void) {
    this.force = forceSimulation<D3Node,D3Link>()
      .force('charge', forceManyBody())
      .force('center', forceCenter(this.canvas.width / 2, this.canvas.height / 2).strength(1))
      .on('tick', function () { repaintCb()})
  }

  updateData(data: AggregatedGraph) {
    console.log("Got data", data)
    this.force.stop()
    const nonNeededNodes = new Set(this.nodes.map( n => n.id))
    this.nodes = data.nodes.map( (dataNode) => {
      const idStr = dataNode.id
      nonNeededNodes.delete(idStr)
      let node = this.nodeMap.get(idStr)
      if(!node) {
        node = {
          name: dataNode.id,
          id: dataNode.id,
          group: 1
        }
        this.nodeMap.set(idStr, node)
      }
      return node

    })
    nonNeededNodes.forEach( (idStr) => {this.nodeMap.delete(idStr)})
    this.links = data.links.map( (dataLink) => {
      return {
        source: this.nodeMap.get(dataLink.source),
        target: this.nodeMap.get(dataLink.target),
        value: 1
      }
    })
    this.force = this.force
      .alpha(1)
      .nodes(this.nodes)
      .force('link', forceLink<D3Node,D3Link>(this.links).id( d => d.id))
      .restart()
  }

  private drawNode(node: D3Node, ctx: CanvasRenderingContext2D): void {
    const fillColor = '#F00'
    ctx.beginPath()
    ctx.arc(node.x, node.y, 8,0, 2*Math.PI)
    ctx.fillStyle = fillColor
    ctx.fill()
  }

  private drawLink(link: D3Link, ctx: CanvasRenderingContext2D): void {
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

  paint(context2d: CanvasRenderingContext2D) {
    this.links.forEach(l=> {this.drawLink(l,context2d)})
    this.nodes.forEach(n => {this.drawNode(n,context2d)})

  }
}
export interface D3Link extends SimulationLinkDatum<D3Node>{
  source: D3Node;
  target: D3Node;
  value: number;
}

export interface D3Node extends SimulationNodeDatum{
  name: string;
  id: string;
  group: 1;
  x?: number;
  y?: number;
  fx?: number;
  fy?: number;
  fixed?: boolean;
}

