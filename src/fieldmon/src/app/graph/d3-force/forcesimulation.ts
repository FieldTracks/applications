import {
  forceCenter, forceCollide,
  forceLink,
  forceManyBody,
  forceSimulation, forceX, forceY, Simulation,
} from "d3-force";
import {zoom, zoomIdentity, ZoomTransform} from "d3-zoom";
import {D3DragEvent, drag} from "d3-drag";
import {select} from "d3-selection";
import {D3Link, D3Node, ForceGraphModel, ScanGraph} from "./model";
import {DrawUtils} from "./utils";

export class Forcesimulation {
  private zoomTransform: ZoomTransform = zoomIdentity
  private forceGraphModel = new ForceGraphModel()
  private simulation: Simulation<D3Node, D3Link>;

  constructor(private ctx: CanvasRenderingContext2D, private canvas: HTMLCanvasElement, private redraw: () => void) {
  }

  applyZoom() {
    this.ctx.translate(this.zoomTransform.x, this.zoomTransform.y)
    this.ctx.scale(this.zoomTransform.k, this.zoomTransform.k)
    const [x0,y0] = this.zoomTransform.apply([0,0])
    this.simulation
      .restart()
  }

  paint() {
    this.forceGraphModel.links.forEach((n) => DrawUtils.drawLink(n,this.ctx));
    this.forceGraphModel.nodes.forEach((n) => DrawUtils.drawNode(n,this.ctx));
  }

  start(): Simulation<D3Node, D3Link> {
    const that = this;

    this.simulation = forceSimulation<D3Node, D3Link>(this.forceGraphModel.nodes)
      .force('charge', forceManyBody().strength(-10).distanceMax(100))
      .force('collide', forceCollide(30))
      .on('tick', function () {
        that.redraw()
      })
      .alphaDecay(0.025);

    const zoomBehavior = zoom()
      .scaleExtent([1 / 8, 10])
      .on('zoom', function (event) {
        that.zoomTransform = event.transform
        that.redraw();
      });


    const dragBehavior = drag()
      .subject((event: D3DragEvent<any, any, any>) => {
        const [xpos, ypos] = this.zoomTransform.invert([event.x, event.y]);
        const node = this.simulation.find(xpos, ypos, 40);
        if (node) {
          [node.x, node.y] = [event.x, event.y]
        }
        return node;
      })
      .on('start', (event) => {
        if (!event.active) {
          this.simulation.alphaTarget(0.1).restart();
        }
        [event.subject.fx, event.subject.fy] = this.zoomTransform.invert([event.subject.x, event.subject.y])
      })
      .on('drag', (event) => {
        [event.subject.fx, event.subject.fy] = this.zoomTransform.invert([event.x, event.y])
      })
      .on('end', (event) => {
        if (!event.active) {
          this.simulation.alphaTarget(0);
        }
        if(!event.subject.stone) {
          [event.subject.fx, event.subject.fy] = [null, null]
        } else {
          event.subject.fixed = true
        }
      });

    select(that.canvas as Element)
      .call(dragBehavior)
      .call(zoomBehavior)
    return this.simulation
  }

  updateData(data: ScanGraph): void {
    const [x0, y0] = this.zoomTransform.invert([this.canvas.width / 2, this.canvas.height / 2])
    this.forceGraphModel.updateData(data,[x0,y0])
    this.simulation
      .nodes(this.forceGraphModel.nodes)
      .force('link', forceLink<D3Node, D3Link>(this.forceGraphModel.links).id((n) => n.id))
      .alpha(1)
      .restart()
  }


}
