import {AggregatedGraph} from "../../model/aggregated/aggregated-graph";
import {SimulationLinkDatum, SimulationNodeDatum} from "d3-force";

export interface D3Node extends SimulationNodeDatum{
  name: string;
  id: string;
  x?: number;
  y?: number;
  fx?: number;
  fy?: number;
  fixed?: boolean;
}
export interface D3Link extends SimulationLinkDatum<D3Node>{
  source: D3Node;
  target: D3Node;
  value: number;
}

export class ForceGraphModel {
  public links: D3Link[] = []
  private nodeMap = new Map<string,D3Node>()
  public nodes: D3Node[] = []


  updateData(data: AggregatedGraph) {
    const nonNeededNodes = new Set(this.nodes.map(n => n.id))
    this.nodes = data.nodes.map((dataNode) => {
      const idStr = dataNode.id
      nonNeededNodes.delete(idStr)
      let node = this.nodeMap.get(idStr)
      if (!node) {
        node = {
          name: dataNode.id,
          id: dataNode.id,
        }
        this.nodeMap.set(idStr, node)
      }
      return node

    })
    nonNeededNodes.forEach((idStr) => {
      this.nodeMap.delete(idStr)
    })
    this.links = data.links.map((dataLink) => {
      return {
        source: this.nodeMap.get(dataLink.source),
        target: this.nodeMap.get(dataLink.target),
        value: 0.5
      }
    })
  }
}



