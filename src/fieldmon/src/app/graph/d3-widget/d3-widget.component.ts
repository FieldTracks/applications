import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from "rxjs";
import {MqttAdapterService} from "../../mqtt-adapter.service";
import {ForceGraph} from "./force-simulation";
import {D3ZoomEvent, zoom, ZoomBehavior, ZoomedElementBaseType, zoomIdentity, ZoomTransform} from "d3-zoom";
import {select} from "d3-selection";
import {WebdavService} from "../../webdav.service";
import {BackgroundImage} from "./background-image";
import {ConfigService} from "../../config.service";
import {GraphWidgetCallbacks} from "./model";
import {drag} from "d3-drag";

@Component({
  selector: 'app-d3-widget',
  templateUrl: './d3-widget.component.html',
  styleUrls: ['./d3-widget.component.css']
})
export class D3WidgetComponent implements AfterViewInit, OnDestroy {

  private activeSubscriptions: Subscription[] = []
  private forceGraph: ForceGraph

  //@ViewChild('canvas') canvas: ElementRef<HTMLCanvasElement>
  @ViewChild('container') containerDiv: ElementRef<HTMLDivElement>

  private nativeCanvas: HTMLCanvasElement;
  private context2d: CanvasRenderingContext2D;

  private transform: ZoomTransform = zoomIdentity
  private bgImage: BackgroundImage
  private zoomBehavior: ZoomBehavior<ZoomedElementBaseType, unknown>;


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
    const canvasElem = select(this.containerDiv.nativeElement)
      .append('canvas')
      .attr('width', this.widgetWidth() + 'px')
      .attr('height', this.widgetHeight() + 'px');

    this.nativeCanvas = canvasElem.node()

    this.context2d = this.nativeCanvas.getContext("2d")
    this.bgImage = new BackgroundImage( this.toWidgetCallbacks())
    //this.updateSize()
    window.addEventListener('resize', () => {
      canvasElem
        .attr('width', window.innerWidth + 'px')
        .attr('height', window.innerHeight - 75 + 'px');
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
    this.initActions()
  }

  private toWidgetCallbacks(): GraphWidgetCallbacks {
    const that = this
    return {
      onRepaint: () =>  {return that.repaint()},
      width:  () => {return that.widgetWidth()},
      height: () => {return that.widgetHeight()},
      transform: () => { return that.transform},
      onNodeSelected: (node) => {}
    }
  }

  private widgetWidth(): number {
    return window.innerWidth
  }

  private widgetHeight() : number {
    return window.innerHeight - 75
  }

  private initActions(): void {
    const that = this
    const handlers = that.forceGraph.dragAndDropHandlers()
    this.zoomBehavior = zoom()
      .scaleExtent([1/10,8])
      .on("zoom", function(event: D3ZoomEvent<any, any>) {
        console.log("old / new transform / event", that.transform, event.transform,event)
        that.transform = event.transform
        that.repaint()
      })
    select(this.nativeCanvas)
      .call(drag()
          //.container(this.nativeCanvas)
          .subject(handlers.dragsubject)
          .on('start', handlers.dragstarted)
          .on('drag', handlers.dragged)
          .on('end', handlers.dragstarted)
        )
    .call(this.zoomBehavior)
  }

  // Disabled, hoping that it helps to go back to the old fieldmon proceedings
  // private updateSize():  void {
  //   const w = this.widgetWidth()
  //   const h = this.widgetHeight()
  //   this.nativeCanvas.width = w
  //   this.nativeCanvas.height = h
  //   // C.f. https://stackoverflow.com/questions/39735367/d3-zoom-behavior-when-window-is-resized
  //   // https://stackoverflow.com/questions/16265123/resize-svg-when-window-is-resized-in-d3-js
  //   // Due to some oddity, the zoom has no clue about windows or canvases being resized
  //   // This is also addressed in https://github.com/d3/d3-zoom/issues/136
  //   // In summary, one could considered the api to be tousled, but working as specified
  //   // as a result, our resize handler is required to update the size in used for the zoom behaviour
  //   if(this.zoomBehavior) {
  //     this.zoomBehavior = this.zoomBehavior
  //       //.extent([[0,0],[w,h]])
  //       //.translateExtent([[0,0],[w,h]])
  //     select(this.nativeCanvas).call(this.zoomBehavior)
  //   }
  //
  //   this.repaint()
  // }

  private loadBackgroundImage(url: string) {
    this.webdavService.getAsObjectUrl(url).subscribe( (image) => {
      if(this.bgImage.imageSrc() != image) {
        this.bgImage.updateImage(image)
      }
    });
  }
}
