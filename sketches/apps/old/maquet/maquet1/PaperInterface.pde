class PaperInterface{

    Screen screen;
    MarkerBoard board;
    TouchElement touch;
    protected PVector drawingSize;
    Camera cameraTracking;
    Projector projector;
    float resolution;
    ArrayList<Button> buttons;
    PApplet parent;

    public PaperInterface(PApplet parent, MarkerBoard board, PVector size, Camera cam, float resolution, Projector proj){

	this.parent = parent;
	this.board = board;
	this.drawingSize = size.get();
	this.cameraTracking = cam;
	this.projector = proj;
	this.resolution = resolution;

	this.buttons = new ArrayList<Button>();
	
	this.screen = new Screen(parent, size, resolution);   
	screen.setAutoUpdatePos(cameraTracking, board);
	projector.addScreen(screen);

	board.setDrawingMode(cameraTracking, true, 10);
	board.setFiltering(cameraTracking, 30, 10);
    }


    ///// Load ressources ////////
    public void init(){

    }

    public void update(){
	screen.updatePos();
	screen.computeScreenPosTransform();
	touch = touchInput.projectTouchToScreen(screen, projector, 
						   true, true); 


	if(!touch.position2D.isEmpty()){
	    for(PVector v: touch.position2D){
		checkButtons(v.x *  drawingSize.x,
			     v.y *  drawingSize.y);
	    }
	}

    }


    private void checkButtons(float x, float y){
	for (Button b : buttons) {
	    if (b.isSelected(x, y, null)) {
		return;
	    }
	}
    }

    public void draw(){

	GLGraphicsOffScreen g = screen.getGraphics();
	g.beginDraw();
	g.clear(0, 0);
	g.scale(resolution);
	g.background(20, 20);
	g.endDraw();

    }

    protected void drawTouch(GLGraphicsOffScreen g, int ellipseSize){

	if(!touch.position2D.isEmpty()){
	    for(PVector v: touch.position2D){
		PVector p1 = new PVector(v.x *  drawingSize.x,
					 v.y *  drawingSize.y);
		g.ellipse(p1.x, p1.y, ellipseSize, ellipseSize);
	    }
	}

    }


}