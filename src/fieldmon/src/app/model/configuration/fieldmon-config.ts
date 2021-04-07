import {D3Node} from "../../graph/d3-widget/force-simulation";

export interface FieldmonConfig {
  backgroundImage?: string;
  fixedNodes?: D3Node[];
  minRssi?: number;
  maxLinkAgeSeconds?: number;
}
