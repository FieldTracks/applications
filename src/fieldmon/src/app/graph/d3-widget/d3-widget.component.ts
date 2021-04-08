import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from "rxjs";
import {MqttAdapterService} from "../../mqtt-adapter.service";
import {ForceGraph} from "./force-simulation";
import {zoom, ZoomBehavior, ZoomedElementBaseType, zoomIdentity, ZoomTransform} from "d3-zoom";
import {select} from "d3-selection";

@Component({
  selector: 'app-d3-widget',
  templateUrl: './d3-widget.component.html',
  styleUrls: ['./d3-widget.component.css']
})
export class D3WidgetComponent implements AfterViewInit, OnDestroy {

  private graphSubscription: Subscription
  private forceGraph: ForceGraph

  @ViewChild('canvas') canvas: ElementRef<HTMLCanvasElement>
  private nativeCanvas: HTMLCanvasElement;
  private context2d: CanvasRenderingContext2D;



  constructor(private mqttService: MqttAdapterService) { }

  ngOnDestroy(): void {
    if(this.graphSubscription) {
      this.graphSubscription.unsubscribe();
    }
  }

  ngAfterViewInit(): void {
    const that = this
    this.nativeCanvas = this.canvas.nativeElement
    this.context2d = this.nativeCanvas.getContext("2d")


    this.forceGraph = new ForceGraph(this.nativeCanvas, this.context2d)
    select(this.nativeCanvas).call(zoom()
      .scaleExtent([1/10,8])
      .on("zoom", function(event) {that.zoomed(event.transform)}))
    this.zoomed(zoomIdentity)
    this.graphSubscription = this.mqttService.aggregatedGraphSubject().subscribe( (graph) => {
      this.forceGraph.updateData(graph)
    })
  }

  private zoomed(transform: ZoomTransform) {
    console.log("Zooming", transform)
    this.context2d.save()
    this.context2d.fillStyle = '#000'
    this.context2d.clearRect(0,0,this.nativeCanvas.width, this.nativeCanvas.height)
    this.context2d.translate(transform.x, transform.y)
    this.context2d.scale(transform.k, transform.k)
    this.forceGraph.drawNodesAndLinks()
    this.context2d.restore()
  }
}
