
export class BackgroundImage {
  readonly background: HTMLImageElement = new Image();

  constructor(private ctx: CanvasRenderingContext2D, private canvas: HTMLCanvasElement, private repaint: () => void) {

    // Retry, when image cannot be loaded
    this.background.onerror = () => {
      setTimeout(() => {
        const url = this.background.src;
        this.background.src = undefined;
        this.background.src = url; // The url need to be reset, so the image can bes refetch
      }, 2000);
    }

    // Repaint canvas, when image was loaded
    this.background.onload = () => {
      this.repaint()
    }
  }

  imageSrc() : string {
    return this.background.src
  }

  updateImage(src: string): void {
    this.background.src = src
  }

  paint() {
    //const [gwidth, gheight] = [this.canvas.width, this.canvas.height]
    //const [imageWidth, imageHeight] = [this.background.width, this.background.height]
    this.ctx.drawImage(this.background, 0,0)

    // Alternative, falls Bild in die Mitte soll
    // const xoffset = (gwidth > imageWidth) ? (gwidth - imageWidth) / 2: 0
    // const yoffset = (gheight > imageHeight) ? (gheight - imageHeight) / 2: 0
    // //g.drawImage(this.background, xoffset,yoffset)
  }
}
