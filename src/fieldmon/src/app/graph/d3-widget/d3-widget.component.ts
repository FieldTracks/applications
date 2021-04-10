import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from "rxjs";
import {MqttAdapterService} from "../../mqtt-adapter.service";
import {ForceGraph} from "./force-simulation";
import {zoom, zoomIdentity, ZoomTransform} from "d3-zoom";
import {select} from "d3-selection";
import {WebdavService} from "../../webdav.service";
import {BackgroundImage} from "./background-image";
import {ConfigService} from "../../config.service";
import {GraphWidgetCallbacks} from "./model";

@Component({
  selector: 'app-d3-widget',
  templateUrl: './d3-widget.component.html',
  styleUrls: ['./d3-widget.component.css']
})
export class D3WidgetComponent implements AfterViewInit, OnDestroy {

  private activeSubscriptions: Subscription[] = []
  private forceGraph: ForceGraph

  @ViewChild('canvas') canvas: ElementRef<HTMLCanvasElement>
  private nativeCanvas: HTMLCanvasElement;
  private context2d: CanvasRenderingContext2D;

  private transform: ZoomTransform = zoomIdentity
  private bgImage: BackgroundImage

  constructor(private mqttService: MqttAdapterService,
              private webdavService: WebdavService,
              private configService: ConfigService) { }

  // Called to trigger a repaint
  repaint() {
    this.context2d.save()
    this.context2d.fillStyle = '#000'
    this.context2d.clearRect(0,0,this.nativeCanvas.width, this.nativeCanvas.height)
    this.context2d.translate(this.transform.x, this.transform.y)
    this.context2d.scale(this.transform.k, this.transform.k)
    if(this.bgImage) {
      this.bgImage.paint(this.context2d)
    }
    if(this.forceGraph) {
      this.forceGraph.paint(this.context2d)
    }
    this.context2d.restore()
  }

  ngOnDestroy(): void {
   this.activeSubscriptions.forEach( (s) => {s.unsubscribe()} )
  }

  ngAfterViewInit(): void {
    const that = this
    this.nativeCanvas = this.canvas.nativeElement
    this.context2d = this.nativeCanvas.getContext("2d")
    this.bgImage = new BackgroundImage( this.toWidgetCallbacks())
    this.initZoom()
    window.addEventListener('resize', () => {
      that.updateSize()
      this.forceGraph.updateSize()
    })
    this.forceGraph = new ForceGraph(this.toWidgetCallbacks())
    this.activeSubscriptions.push(this.mqttService.graphSubject().subscribe( (graph) => {
      this.forceGraph.updateData(graph)
    }))
    this.activeSubscriptions.push(this.configService.currentConfiguration().subscribe( (config) => {
      if(config.backgroundImage) {
        this.loadBackgroundImage(config.backgroundImage)
      }
    }))
  }

  private toWidgetCallbacks(): GraphWidgetCallbacks {
    const that = this
    return {
      repaint: () =>  {return that.repaint()},
      width:  () => {return that.widgetWidth()},
      height: () => {return that.widgetHeight()},
      transform: () => { return that.transform}
    }
  }

  private widgetWidth(): number {
    return window.innerWidth
  }

  private widgetHeight() : number {
    return window.innerHeight - 75
  }

  private initZoom(): void {
    const that = this
    select(this.nativeCanvas).call(zoom()
      .scaleExtent([1/10,8])
      .on("zoom", function(event) {
        that.transform = event.transform
        that.repaint()
      }))
  }

  private updateSize():  void {
    this.nativeCanvas.width = this.widgetWidth()
    this.nativeCanvas.height = this.widgetHeight()
    this.repaint()
  }

  private loadBackgroundImage(url: string) {
    this.webdavService.getAsObjectUrl(url).subscribe( (image) => {
      if(this.bgImage.imageSrc() != image) {
        console.log("Updating background image...")
        this.bgImage.updateImage(image)
      }
    });
  }
}
