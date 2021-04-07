import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from "rxjs";
import {MqttAdapterService} from "../../mqtt-adapter.service";
import {ForceGraph} from "./force-simulation";

@Component({
  selector: 'app-d3-widget',
  templateUrl: './d3-widget.component.html',
  styleUrls: ['./d3-widget.component.css']
})
export class D3WidgetComponent implements AfterViewInit, OnDestroy {

  private graphSubscription: Subscription
  private forceGraph: ForceGraph

  @ViewChild('canvas') canvas: ElementRef<HTMLCanvasElement>

  constructor(private mqttService: MqttAdapterService) { }

  ngOnDestroy(): void {
    if(this.graphSubscription) {
      this.graphSubscription.unsubscribe();
    }
  }

  ngAfterViewInit(): void {
    const canvas = this.canvas.nativeElement
    this.forceGraph = new ForceGraph(canvas)

    this.graphSubscription = this.mqttService.aggregatedGraphSubject().subscribe( (graph) => {
      //this.mqttData = graph
      this.forceGraph.updateData(graph)
    })

  }

}
