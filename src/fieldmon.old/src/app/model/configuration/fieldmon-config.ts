import {D3Node} from "../../graph/d3-widget/model";

export interface FieldmonConfig {
  backgroundImage?: string;
  fixedNodes?: D3Node[];
  minRssi?: number;
  maxLinkAgeSeconds?: number;
}
