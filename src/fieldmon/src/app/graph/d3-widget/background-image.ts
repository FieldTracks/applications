export class BackgroundImage {
  readonly background: HTMLImageElement = new Image();

  constructor(private repaintCb: () => void) {

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
      this.repaintCb()
    }
  }

  imageSrc() : string {
    return this.background.src
  }

  updateImage(src: string): void {
    this.background.src = src
  }

  paint(g: CanvasRenderingContext2D) {
    g.drawImage(this.background, 0,0)
  }
}
