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
  private readonly ctx: CanvasRenderingContext2D;

  constructor(private canvas: HTMLCanvasElement, private context2d: CanvasRenderingContext2D) {
    const sim = this
    this.ctx = context2d
    this.force = forceSimulation<D3Node,D3Link>()
      .force('charge', forceManyBody())
      .force('center', forceCenter(this.canvas.width / 2, this.canvas.height / 2).strength(1))
      .on('tick', function () { if(sim.ctx) sim.redraw()})
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

  drawNodesAndLinks(): void {
    this.links.forEach(l=> {this.drawLink(l)})
    this.nodes.forEach(n => {this.drawNode(n)})
  }

  private redraw(): void {
    this.ctx.save()
    this.ctx.clearRect(0,0,this.canvas.width, this.canvas.height)
    this.drawNodesAndLinks()
    this.ctx.restore()
  }

  drawNode(node: D3Node): void {
    const fillColor = '#F00'
    this.ctx.beginPath()
    this.ctx.arc(node.x, node.y, 8,0, 2*Math.PI)
    this.ctx.fillStyle = fillColor
    this.ctx.fill()
  }

  drawLink(link: D3Link): void {
    const gradient = this.ctx.createLinearGradient(link.source.x, link.source.y, link.target.x, link.target.y)
    // TODO: define nice gradient for stones and contacts
    gradient.addColorStop(0.45, '#000')
    gradient.addColorStop(0.55, '#000')

    this.ctx.beginPath()
    this.ctx.moveTo(link.source.x, link.source.y)
    this.ctx.lineTo(link.target.x, link.target.y)
    this.ctx.strokeStyle = gradient
    this.ctx.globalAlpha = 0.2
    this.ctx.lineWidth = 1.5
    this.ctx.stroke()
    this.ctx.globalAlpha = 1

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

