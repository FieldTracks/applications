import {
  forceCenter,
  forceLink,
  forceManyBody,
  forceSimulation,
  Simulation,
} from "d3-force";


import {DrawUtils} from "./utils";
import {D3Link, D3Node, ForceGraphModel} from "./model";
import {AggregatedGraphNew} from "../../AggregatedGraphNew";

// Controls the D3 force simulation
// This class adapts all d3-centric functionality excepts painting
//
export class ForceGraph {

  // Reference to the simulation running at the moment
  private force: Simulation<D3Node,D3Link>

  // Model class for the simulation
  private model = new ForceGraphModel()

  constructor(private width: number, private height: number, private repaintCb: () => void) {
    this.force = forceSimulation<D3Node,D3Link>()
      .force('charge', forceManyBody().distanceMax(100))
      .on('tick', function () { repaintCb()})
      .force('center', forceCenter(width / 2, height / 2).strength(1))
  }

  updateSize(width: number, height: number): void {
    console.log("New Size is:", width, height)
    this.force.force('center', forceCenter(width / 2, height / 2).strength(1))
      .restart()
  }

  // Update Data: To be called for updating the data in the simulation
  updateData(data: AggregatedGraphNew) {
    console.log("Got data", data)
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
}

