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

  private transform: ZoomTransform = zoomIdentity

  constructor(private mqttService: MqttAdapterService) { }

  // Called to trigger a repaint
  repaint() {
    this.context2d.save()
    this.context2d.fillStyle = '#000'
    this.context2d.clearRect(0,0,this.nativeCanvas.width, this.nativeCanvas.height)
    this.context2d.translate(this.transform.x, this.transform.y)
    this.context2d.scale(this.transform.k, this.transform.k)
    // For now, solely the force-graph needs to be painted
    this.forceGraph.paint(this.context2d)
    this.context2d.restore()
  }

  ngOnDestroy(): void {
    if(this.graphSubscription) {
      this.graphSubscription.unsubscribe();
    }
  }

  ngAfterViewInit(): void {
    const that = this
    this.nativeCanvas = this.canvas.nativeElement
    this.context2d = this.nativeCanvas.getContext("2d")


    this.forceGraph = new ForceGraph(this.nativeCanvas, () => this.repaint())
    select(this.nativeCanvas).call(zoom()
      .scaleExtent([1/10,8])
      .on("zoom", function(event) {
        that.transform = event.transform
        that.repaint()
      }))
    this.graphSubscription = this.mqttService.aggregatedGraphSubject().subscribe( (graph) => {
      this.forceGraph.updateData(graph)
    })
  }

}
