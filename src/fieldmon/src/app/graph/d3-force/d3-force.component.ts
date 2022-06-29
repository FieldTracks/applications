import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Forcesimulation} from "./forcesimulation";
import {MqttService} from "../../mqtt.service";
import {Subscription} from "rxjs";
import {ScanGraph} from "./model";

@Component({
  selector: 'app-d3-force',
  templateUrl: './d3-force.component.html',
  styleUrls: ['./d3-force.component.sass']
})
export class D3ForceComponent implements AfterViewInit, OnDestroy {



  constructor(private mqttService: MqttService) {

  }

  @ViewChild('canvas') canvasRef: ElementRef<HTMLCanvasElement>
  @ViewChild('container') containerRf: ElementRef<HTMLDivElement>
  private force: Forcesimulation
  private ctx: CanvasRenderingContext2D
  private canvas: HTMLCanvasElement;
  //private bgImage: BackgroundImage

  private aggregatedNamesSubscription: Subscription;

  ngAfterViewInit() {
    this.canvas = this.canvasRef.nativeElement
    this.ctx = this.canvas.getContext("2d")!!
    this.force = new Forcesimulation(this.ctx, this.canvas, () => { this.redraw()})
    this.force.start()
    this.resizeCanvas()

    this.aggregatedNamesSubscription = this.mqttService.mqttTopic<ScanGraph>("Aggregated/scan").subscribe(
      (next:ScanGraph) => {
        this.force.updateData(next)
        this.force.paint()
      }
    )
  }

  resizeCanvas() {
    this.canvas.width = window.innerWidth
    this.canvas.height = window.innerHeight - 75
  }

  redraw(): void {
    this.ctx.save();
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    this.force.applyZoom()
    // if(this.bgImage) {
    //   this.bgImage.paint()
    // }
    if(this.force) {
      this.force.paint()
    }
    this.ctx.restore();
  }

  ngOnDestroy(): void {
    this.aggregatedNamesSubscription?.unsubscribe()
  }

}
