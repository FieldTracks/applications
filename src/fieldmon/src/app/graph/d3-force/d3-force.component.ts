import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {Forcesimulation} from "./forcesimulation";

@Component({
  selector: 'app-d3-force',
  templateUrl: './d3-force.component.html',
  styleUrls: ['./d3-force.component.sass']
})
export class D3ForceComponent implements OnInit {

  constructor() { }

  @ViewChild('canvas') canvasRef: ElementRef<HTMLCanvasElement>
  @ViewChild('container') containerRf: ElementRef<HTMLDivElement>
  private force: Forcesimulation
  private ctx: CanvasRenderingContext2D
  private canvas: HTMLCanvasElement;
  //private bgImage: BackgroundImage


  ngOnInit(): void {
  }

}
