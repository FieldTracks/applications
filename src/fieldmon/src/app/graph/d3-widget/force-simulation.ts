import {
  forceCenter,
  forceLink,
  forceManyBody,
  forceSimulation,
  Simulation,
} from "d3-force";


import {DrawUtils} from "./utils";
import {D3Link, D3Node, DragAndDropHandlers, ForceGraphModel, GraphWidgetCallbacks} from "./model";
import {AggregatedGraphNew} from "../../AggregatedGraphNew";
import {drag} from "d3-drag";
import {normalizeFileReplacements} from "@angular-devkit/build-angular/src/utils";

// Controls the D3 force simulation
// This class adapts all d3-centric functionality excepts painting
//
export class ForceGraph {

  // Reference to the simulation running at the moment
  private force: Simulation<D3Node,D3Link>

  // Model class for the simulation
  private model = new ForceGraphModel()

  constructor(private w: GraphWidgetCallbacks)  {
    this.force = forceSimulation<D3Node,D3Link>()
      .force('charge', forceManyBody().distanceMax(100))
      .on('tick', function () { w.onRepaint()})
      .force('center', forceCenter(w.width() / 2, w.height() / 2))//.strength(1))
      .alphaDecay(0.1)
  }

  updateSize(): void {
    this.force.force('center', forceCenter(this.w.width() / 2, this.w.height() / 2).strength(1))
      .restart()
  }

  // Update Data: To be called for updating the data in the simulation
  updateData(data: AggregatedGraphNew) {
    this.force.stop()
    this.model.updateData(data)

    this.force = this.force
      .alpha(1)
      .nodes(this.model.nodes)
      .force('link', forceLink<D3Node,D3Link>(this.model.links).id( d => d.id))
      .restart()
  }
  paint(context2d: CanvasRenderingContext2D) {
    this.model.links.forEach(l=> {DrawUtils.drawLink(l,context2d)})
    this.model.nodes.forEach(n => {DrawUtils.drawNode(n,context2d)})
  }
    dragAndDropHandlers(): DragAndDropHandlers {
    const that = this
    return {
      dragstarted: function (event) {
        event.subject.fixed = true
        if (!event.active) {
          that.force.alphaTarget(0.5).restart();
        }
      },
      dragged: function (event) {
        [event.subject.fx, event.subject.fy] = that.w.transform().invert([event.x, event.y])
      },
      dragended: function (event) {
        console.log("Node placed:", event.subject)
        if (!event.active) {
          that.force.alphaTarget(0);
        }
        event.subject.fixed = true
        //event.subject.fx = null;
        //event.subject.fy = null;
      },
      dragsubject: function (event) {
        const point = that.w.transform().invert([event.x, event.y]);
        const node = that.force.find(point[0], point[1], 40);
        return node
      }

    }
  }
}

