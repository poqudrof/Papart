public class MyApp extends PaperScreen {

  void setup() {
    setDrawingSize(297, 210);
    loadMarkerBoard(Papart.markerFolder + "A3-small1.cfg", 297, 210);
    drawOnPaperOnly();
  }

    void drawAroundPaper() {
        // background(0, 0, 0);
        fill(0, 100, 0);
        rect(-50, -50, drawingSize.x, drawingSize.y);
        fill(200, 100, 20);
        translate(0, 0, 1);
        rect(10, 10, 100, 30);
    }

    void drawOnPaper(){
        background(0, 200, 10);
    }

    // void draw() {
    //     beginDraw2D();
    //     clear();
    //     // background(0, 0, 0);
    //     fill(0, 100, 0);
    //     rect(-50, -50, drawingSize.x, drawingSize.y);
    //     fill(200, 100, 20);
    //     translate(0, 0, 1);
    //     rect(10, 10, 100, 30);
    //     endDraw();
    // }
}
