import {AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Forcesimulation} from "./forcesimulation";
import {MqttAdapterService} from "../../mqtt-adapter.service";
import {Subscription} from "rxjs";
import {BackgroundImage} from "./background-image";
import {ConfigService} from "../../config.service";
import {WebdavService} from "../../webdav.service";

@Component({
  selector: 'app-d3-widget',
  templateUrl: './d3-widget.component.html',
  styleUrls: ['./d3-widget.component.css']
})
export class D3WidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('canvas') canvasRef: ElementRef<HTMLCanvasElement>
  @ViewChild('container') containerRf: ElementRef<HTMLDivElement>
  private force: Forcesimulation
  private ctx: CanvasRenderingContext2D
  private canvas: HTMLCanvasElement;
  private bgImage: BackgroundImage

  private activeSubscripts: Subscription[] = []


  constructor(private mqtt: MqttAdapterService, private configService: ConfigService, private webdavService: WebdavService) { }


  ngOnDestroy(): void {
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.canvas = this.canvasRef.nativeElement
    this.ctx = this.canvas.getContext("2d")
    this.bgImage = new BackgroundImage(this.ctx, this.canvas, () =>  {this.redraw()})
    this.force = new Forcesimulation(this.ctx, this.canvas, () =>  {this.redraw()})
    this.force.start()

    window.addEventListener('resize', () =>  {
      this.resizeCanvas()
      this.redraw();
    });
    this.resizeCanvas()
    this.activeSubscripts.push(this.mqtt.graphSubject().subscribe( (data) => {
      this.force.updateData(data)
    }))
    this.activeSubscripts.push(this.configService.currentConfiguration().subscribe( (config) => {
      if(config.backgroundImage) {
        this.webdavService.getAsObjectUrl(config.backgroundImage).subscribe( (image) => {
          if(this.bgImage.imageSrc() != image) {
            this.bgImage.updateImage(image)
          }
        });

      }
    }))

  }

  resizeCanvas() {
    this.canvas.width = window.innerWidth
    this.canvas.height = window.innerHeight - 75
  }

  redraw(): void {
    this.ctx.save();
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    this.force.applyZoom()
    if(this.bgImage) {
      this.bgImage.paint()
    }
    if(this.force) {
      this.force.paint()
    }
    this.ctx.restore();
  }
}
