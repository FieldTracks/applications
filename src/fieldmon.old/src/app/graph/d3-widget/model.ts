import {SimulationLinkDatum, SimulationNodeDatum} from "d3-force";
import {AggregatedGraphNew} from "../../AggregatedGraphNew";


export interface D3Node extends SimulationNodeDatum{
  name: string;
  id: string;
  x?: number;
  y?: number;
  fx?: number;
  fy?: number;
  fixed?: boolean
  stone?: boolean
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


  updateData(data: AggregatedGraphNew, initialPos: [number,number]) {
    const nonNeededNodes = new Set(this.nodes.map(n => n.id))
    this.nodes = data.nodes.map((dataNode) => {
      const idStr = dataNode.id
      nonNeededNodes.delete(idStr)
      let node = this.nodeMap.get(idStr)
      if (!node) {
        node = {
          name: dataNode.id,
          id: dataNode.id,
          stone: dataNode.localstone,
          x: initialPos[0],
          y: initialPos[1],
        }
        this.nodeMap.set(idStr, node)
      } else {
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
        value: 1
      }
    })
  }
}



