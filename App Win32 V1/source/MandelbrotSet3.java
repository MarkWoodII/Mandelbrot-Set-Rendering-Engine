import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import g4p_controls.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class MandelbrotSet3 extends PApplet {



double centerX, centerY;
double lowerX, upperX, lowerY, upperY;
double magnification, range;
int maxIterations;

double zx, zy, cx, cy, tmp;
int c;

int mili, renderIterations;

Chunk main;

CalculationThread[] givenData;
int acl;

float hue;
PVector cur;

int savedAs;
boolean saveShot;

public void setup() {
  createGUI();
  customGUI();
  frameRate(30);
  
  
  colorMode(HSB, 1);
  strokeWeight(1);
  noFill();
  Controls.setAlwaysOnTop(true);

  centerX = 0;//-0.8885416810711224;
  centerY = 0;//-0.10506944739156299;
  magnification = 1;//1/0.005;
  range = 1.7f;
  lowerX = centerX - range/magnification;
  upperX = centerX + range/magnification;
  lowerY = centerY - range * height/width / magnification;
  upperY = centerY + range * height/width / magnification;

  maxIterations = 1000;
  acl = 10;
  hue = 0.62f;

  cur = new PVector(width/2, height/2);
  savedAs = 0;
  saveShot = false;
  renderIterations = 0;
  renderSet();

  for (int i = 1; i < 21; i++) {
    println(i + ":  " + pixelWidth % i + ", " + pixelHeight % i);
  }
}

public void customGUI() {
}

public void draw() {
  updatePixels();
  if (saveShot) {
    saveShot = !saveShot;
    save("screenshot-"+savedAs+".png");
  }
}

public void renderSet() {
  Timer timer = new Timer();
  timer.startStopwatch();
  println("\nStarted Calculating");

  //this function works up to about 2.0 * 10^-5 zoom, and then it's too little

  int[][] pixelIters = new int[pixelWidth][pixelHeight];
  Chunk[] activeChunks = new Chunk[acl*acl];
  double xRange = upperX - lowerX;
  double yRange = upperY - lowerY;
  for (int i = 0; i < acl; i++) {
    for (int j = 0; j < acl; j++) {
      activeChunks[i*acl + j] = new Chunk(lowerX + i * xRange/acl, lowerY + j * yRange/acl, xRange/acl, yRange/acl);
    }
  }
  //activeChunks[0] = new Chunk(lowerX, lowerY, upperX - lowerX, upperY - lowerY);

  //SET UP THREADING
  CalculationThread[] threads = new CalculationThread[acl*acl];
  for (int c = 0; c < acl*acl; c++) {
    threads[c] = activeChunks[c].calculateData();
  }

  //HAVE FUNCTION WAIT TILL ALL THREADS ARE FINISHED
  for (int c = 0; c < acl * acl; c++) {
    try { 
      threads[c].join();
    }
    catch(InterruptedException e) {
      e.printStackTrace();
    }
  }

  //THIS MESS STITCHES ALL THE CHUNKS BACK TOGETHER IN PIXELITERS
  //I KNOW FOUR NESTED LOOPS IS BAD BUT I NEEDED TWO FOR THE X AND Y OF PIXELITERS
  //AND TWO FOR THE X AND Y OF THREAD ITERS
  for (int c = 0; c < acl; c++) {
    for (int d = 0; d < acl; d++) {
      for (int i = 0; i < pixelWidth/acl; i++) {
        for (int j = 0; j < pixelHeight/acl; j++) {
          pixelIters[i + c * pixelWidth/acl][j + d * pixelHeight/acl] = threads[c*acl + d].iters[i][j];
        }
      }
    }
  }

  givenData = threads;
  println("Finished calculating in " + (timer.lap()/1000.0f));
  print("getting colors...   ");
  getColors(pixelIters);
  println("Finished getting colors in " + (timer.lap()/1000.0f));
  double magni = upperX - lowerX;
  println("Done. Finished rendering in " + (timer.stopStopwatch()/1000.0f) + "\nRendered at: "+centerX + ", " + centerY + "\nScale: " + magni + ", Magnification: " + magnification + "\nEscape Iterations: " + maxIterations);
}

public void getColors(int[][] pixelIters) {
  //float total = 0;
  int[] histogram = new int[pixelWidth * pixelHeight - 1];
  for (int i = 0; i < pixelWidth; i++) {
    for (int j = 0; j < pixelHeight; j++) {
      int index = pixelIters[i][j];
      if (index != maxIterations)
        histogram[index] += 1;
      //total += 1;
    }
  }
  float total = 0;
  for (int i = 0; i < histogram.length; i++) {
    total += histogram[i];
  }
  float[] divdHist = new float[histogram.length];
  divdHist[0] = histogram[0] / total;
  for (int i = 1; i < divdHist.length; i++) {
    divdHist[i] = divdHist[i-1] + histogram[i] / total;
  }

  float[][] setColor = new float[pixelWidth][pixelHeight];
  for (int i = 0; i < pixelWidth; i++) {
    for (int j = 0; j < pixelHeight; j++) {
      int iteration = pixelIters[i][j];
      setColor[i][j] = divdHist[iteration];
    }
  }
  loadPixels();
  for (int i = 0; i < pixelWidth; i++) {
    for (int j = 0; j < pixelHeight; j++) {
      if (pixelIters[i][j] == maxIterations) 
        pixels[i+(j*pixelWidth)] = color(0, 0, 0);
      else
        //pixels[i+(j*pixelWidth)] = color((pixelIters[i][j] % 100)/100.0,1,1);
        pixels[i+(j*pixelWidth)] = color(hue, 1 - 0.9f * setColor[i][j], 0.1f + 0.9f * setColor[i][j]);
    }
  }
  updatePixels();
}

public void go() {
  mili = millis();
  centerX = lowerX + (upperX - lowerX) * cur.x/width;
  centerY = lowerY + (upperY - lowerY) * cur.y/height;

  //magnification *= 2;
  lowerX = centerX - range/magnification;
  upperX = centerX + range/magnification;
  lowerY = centerY - range * height/width / magnification;
  upperY = centerY + range * height/width / magnification;
  
  renderSet();
}

/*
Timer: acts as a stopwatch for processes being run
 */
class Timer {
  int initiationTime;
  int startTime;
  int lastLapTime;
  int stopTime;

  public void Timer() {
    initiationTime = millis();
    startTime = -1;
    lastLapTime = -1;
  }

  public void startStopwatch() {
    startTime = millis();
    lastLapTime = 0;
  }

  public int lap() {
    lastLapTime = millis() - startTime - lastLapTime;
    return lastLapTime;
  }

  public int stopStopwatch() {
    stopTime = millis() - startTime;
    return stopTime;
  }
}

/* 
 Point: struct-like object used to store x and y values of points in double precision
 */
class Point {
  double x;
  double y;
  Point(double x, double y) {
    this.x = x;
    this.y = y;
  }
}

/*
 Chunk: 
 holds and operates on a group of points. May initialize and determine if points are
 within set
 UNSUPPORTED: 
 chunk-trees. Used to save previous chunks and operate on sub-chunks to increase
 LOD in areas of interest
 */
class Chunk {
  private int resX = pixelWidth/acl;
  private int resY = pixelHeight/acl;

  private double x, y, w, h;
  private int[] data;

  public Chunk[] leafs;

  /*
   Chunk(): initialize chunk with data and range
   x_: x coordinate of upper left point
   y_: y coordinate of upper left point
   w_: width of chunk
   h_: height of chunk
   */
  Chunk(double x_, double y_, double w_, double h_) {
    data = new int[resX*resY];
    x = x_;
    y = y_;
    w = w_;
    h = h_;

    leafs = new Chunk[4];
  }

  /*
    UNSUPPORTED
   */
  private void generateSubChunks() {
    leafs[0] = new Chunk(x, y, w/2, h/2);
    leafs[1] = new Chunk(x+w/2, y, w/2, h/2);
    leafs[2] = new Chunk(x, y+h/2, w/2, h/2);
    leafs[3] = new Chunk(x+w/2, y+h/2, w/2, h/2);
  }

  /*
  calculateData(): creates an object to calculate on each point within the chunk 
   to see if it's in the set or not. Chunk is divided up into resX by resY number
   of points
   returns: the asynchronous thread used to actually perform the calculation
   */
  public CalculationThread calculateData() {
    Point[][] allPoints = new Point[resX][resY];

    for (int i = 0; i < resX; i++) {
      for (int j = 0; j < resY; j++) {
        allPoints[i][j] = new Point(x+(w*i/resX), y+(h*j/resY));
      }
    }

    CalculationThread thread = new CalculationThread(allPoints, w/pixelWidth);
    thread.start();
    return thread;
  }
}

/*
CalculationThread: holds data points and may calculate on them asynchronously
 */
class CalculationThread extends Thread {

  int[][] iters;
  Point[][] range;
  double approx;

  /*
   CalculationThread(): initialize asynchronous thread to be ready to run
   range: all points that the thread will calculate on. These must be in one continous group
   approx: "error margin" of point. For accuracy, set to size of one pixel
   */
  public CalculationThread(Point[][] range, double approx) {
    this.range = range;
    iters = new int[range.length][range[0].length];
    this.approx = approx;
  }

  /* 
   run(): determine the iterations of all points in range
   runs asynchronously to main loop
   */
  public void run() {
    //The first two looping blocks wrap around the outside of the chunk to see if the pixels are
    //all at max iterations
    boolean allMax = true;
    for (int r = 0; r < 2; r++) {
      for (int i = 0; i < iters.length; i++) {
        int j = r * (iters[i].length - 1);
        iters[i][j] = determinePoint(range[i][j].x, range[i][j].y, approx);
        allMax = allMax && iters[i][j] == maxIterations;
      }
    }

    for (int r = 0; r < 2; r++) {
      for (int j = 1; j < iters[0].length - 1; j++) {
        int i = r * (iters.length - 1);
        iters[i][j] = determinePoint(range[i][j].x, range[i][j].y, approx);
        allMax = allMax && iters[i][j] == maxIterations;
      }
    }
    /*if all the outside pixels are at max iterations, default to setting all the pixels to max iterations
     this is a reasonable assumption since it is highly unlikely that the sampling would happen to land on
     points within the set without once hitting an unbounded point. There are also no points outside the set
     enclosed by the set */
    if (allMax) {
      for (int i = 1; i < iters.length - 1; i++) {
        for (int j = 1; j < iters[i].length - 1; j++) {
          iters[i][j] = maxIterations;
        }
      }
    } else {
      for (int i = 1; i < iters.length - 1; i++) {
        for (int j = 1; j < iters[i].length - 1; j++) {
          iters[i][j] = determinePoint(range[i][j].x, range[i][j].y, approx);
        }
      }
    }
  }

  /*
   determinPoint(): determines the number of escape iterations of a given point
   cX: the x coordinate of the point
   cY: the y coordinate of the point
   approx: "error margin" of point. For accuracy, set to size of one pixel
   returns: the number of iterations (up to maxIterations) before point escapes set boundary
   */
  private int determinePoint(double cX, double cY, double approx) {
    double zx = 0.0f;
    double zy = 0.0f;
    int iteration = 0;
    double zx2 = 0;
    double zy2 = 0;
    int max_iteration = maxIterations;
    double xold = 0;
    double yold = 0;
    int period = 0;
    while (zx2 + zy2 <= 4 && iteration < max_iteration) {
      zy = (zx + zx) * zy + cX;
      zx = zx2 - zy2 + cY;
      zx2 = zx*zx;
      zy2 = zy*zy;
      iteration++;

      if (Math.abs(zx - xold) < approx && Math.abs(zy - yold) < approx) {
        iteration = max_iteration;    /* Set to max for the color plotting */
        break;       /* We are inside the Mandelbrot set, leave the while loop */
      }
      period = period + 1;
      if (period > 20) {
        period = 0;
        xold = zx;
        yold = zy;
      }
    }
    return iteration;
  }
}
/* =========================================================
 * ====                   WARNING                        ===
 * =========================================================
 * The code in this tab has been generated from the GUI form
 * designer and care should be taken when editing this file.
 * Only add/edit code inside the event handlers i.e. only
 * use lines between the matching comment tags. e.g.

 void myBtnEvents(GButton button) { //_CODE_:button1:12356:
     // It is safe to enter your event code here  
 } //_CODE_:button1:12356:
 
 * Do not rename this tab!
 * =========================================================
 */

public void mainSliderChange(GSlider2D source, GEvent event) { //_CODE_:MainSlider:576398:
  cur.x = MainSlider.getValueXF() * width;
  cur.y = MainSlider.getValueYF() * height;
  //println("slider2d1 - GSlider2D >> GEvent." + event + " @ " + millis());
} //_CODE_:MainSlider:576398:

synchronized public void win_draw1(PApplet appc, GWinData data) { //_CODE_:Controls:409002:
  appc.background(230);
} //_CODE_:Controls:409002:

public void button1_click2(GButton source, GEvent event) { //_CODE_:Recalculate:704829:
  magnification = magnification * Math.pow(2, zoomLevel.getValueF());
  go();
  MainSlider.setValueXY(0.5f, 0.5f);

  if (stickyZoom.isSelected() == false) {
    zoomLevel.setValue(0);
    zoomLabel.setText(""+((int)(100 * pow(2, zoomLevel.getValueF()))/100.0f));
  }
} //_CODE_:Recalculate:704829:

public void button1_click4(GButton source, GEvent event) { //_CODE_:saving:293264:
  saveShot = true;
  savedAs++;
} //_CODE_:saving:293264:

public void slider1_change2(GSlider source, GEvent event) { //_CODE_:iterSlider:879533:
  float newMaxIters = maxIterations;
  if (iterSlider.getValueF() > 0) {
    newMaxIters = maxIterations * iterSlider.getValueF();
  } else if (iterSlider.getValueF() < 0) {
    newMaxIters = maxIterations / abs(iterSlider.getValueF());
  }

  if (event == GEvent.RELEASED) {
    maxIterations = (int)newMaxIters; 
    iterSlider.setValue(0);
  }

  putIters.setText(""+(int)newMaxIters);
} //_CODE_:iterSlider:879533:

public void chunks2change(GSlider source, GEvent event) { //_CODE_:Chunks2:612255:
  acl = Chunks2.getValueI();
  while (pixelWidth % acl != 0 && pixelHeight % acl != 0) {
    acl--;
  }
  chunkLabel.setText(""+acl);
} //_CODE_:Chunks2:612255:

public void colors2change(GSlider source, GEvent event) { //_CODE_:colors2:940355:
  hue = colors2.getValueF();
  colorsLabel.setText(""+(int)(hue * 100)/100.0f);
} //_CODE_:colors2:940355:

public void slider1_change1(GSlider source, GEvent event) { //_CODE_:zoomLevel:206728:
  //double newZoomLevel = 1;

  //newZoomLevel = magnification * Math.pow(2, zoomLevel.getValueF());
  zoomLabel.setText(""+((int)(100 * pow(2, zoomLevel.getValueF()))/100.0f));


  //if (event == GEvent.RELEASED) {
  //  magnification = newZoomLevel;
  //}
} //_CODE_:zoomLevel:206728:

public void checkbox1_clicked1(GCheckbox source, GEvent event) { //_CODE_:stickyZoom:294669:
  if (stickyZoom.isSelected() == false)
    zoomLevel.setValue(0);
} //_CODE_:stickyZoom:294669:



// Create all the GUI controls. 
// autogenerated do not edit
public void createGUI(){
  G4P.messagesEnabled(false);
  G4P.setGlobalColorScheme(GCScheme.BLUE_SCHEME);
  G4P.setMouseOverEnabled(false);
  surface.setTitle("Sketch Window");
  MainSlider = new GSlider2D(this, 0, 0, width, height);
  MainSlider.setLimitsX(0.5f, 0.0f, 1.0f);
  MainSlider.setLimitsY(0.5f, 0.0f, 1.0f);
  MainSlider.setNumberFormat(G4P.DECIMAL, 2);
  MainSlider.setOpaque(false);
  MainSlider.addEventHandler(this, "mainSliderChange");
  Controls = GWindow.getWindow(this, "Controls", 0, 0, 300, 360, JAVA2D);
  Controls.noLoop();
  Controls.setActionOnClose(G4P.KEEP_OPEN);
  Controls.addDrawHandler(this, "win_draw1");
  Recalculate = new GButton(Controls, 20, 300, 260, 50);
  Recalculate.setText("Recalculate");
  Recalculate.addEventHandler(this, "button1_click2");
  saving = new GButton(Controls, 20, 10, 260, 40);
  saving.setText("Save Screen Shot");
  saving.addEventHandler(this, "button1_click4");
  escapeIters = new GLabel(Controls, 170, 60, 110, 20);
  escapeIters.setText("Escape Iterations:");
  escapeIters.setOpaque(false);
  putIters = new GLabel(Controls, 170, 80, 110, 20);
  putIters.setText("1000");
  putIters.setOpaque(false);
  iterSlider = new GSlider(Controls, 20, 60, 150, 40, 10.0f);
  iterSlider.setLimits(0.1f, -10.0f, 10.0f);
  iterSlider.setNumberFormat(G4P.DECIMAL, 2);
  iterSlider.setOpaque(false);
  iterSlider.addEventHandler(this, "slider1_change2");
  Chunks2 = new GSlider(Controls, 20, 110, 150, 50, 10.0f);
  Chunks2.setLimits(10, 1, 20);
  Chunks2.setNumberFormat(G4P.INTEGER, 0);
  Chunks2.setOpaque(false);
  Chunks2.addEventHandler(this, "chunks2change");
  chunkLabel = new GLabel(Controls, 170, 130, 80, 20);
  chunkLabel.setText("10");
  chunkLabel.setOpaque(false);
  label1 = new GLabel(Controls, 170, 110, 120, 20);
  label1.setText("Number of Chunks:");
  label1.setOpaque(false);
  colors2 = new GSlider(Controls, 20, 170, 150, 50, 10.0f);
  colors2.setLimits(0.62f, 0.0f, 1.0f);
  colors2.setNumberFormat(G4P.DECIMAL, 2);
  colors2.setOpaque(false);
  colors2.addEventHandler(this, "colors2change");
  colorsLabel = new GLabel(Controls, 170, 190, 80, 20);
  colorsLabel.setText("0.62");
  colorsLabel.setOpaque(false);
  colorsHolderidk = new GLabel(Controls, 170, 170, 110, 20);
  colorsHolderidk.setText("Color (hue space):");
  colorsHolderidk.setOpaque(false);
  zoomLevel = new GSlider(Controls, 20, 230, 150, 50, 10.0f);
  zoomLevel.setLimits(0.0f, -1.0f, 1.0f);
  zoomLevel.setNumberFormat(G4P.DECIMAL, 2);
  zoomLevel.setOpaque(false);
  zoomLevel.addEventHandler(this, "slider1_change1");
  zoomLabel = new GLabel(Controls, 170, 250, 80, 20);
  zoomLabel.setText("1");
  zoomLabel.setOpaque(false);
  zoomHolder = new GLabel(Controls, 170, 230, 80, 20);
  zoomHolder.setText("Zoom Level:");
  zoomHolder.setOpaque(false);
  stickyZoom = new GCheckbox(Controls, 170, 270, 80, 20);
  stickyZoom.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  stickyZoom.setText("Sticky");
  stickyZoom.setOpaque(false);
  stickyZoom.addEventHandler(this, "checkbox1_clicked1");
  stickyZoom.setSelected(true);
  Controls.loop();
}

// Variable declarations 
// autogenerated do not edit
GSlider2D MainSlider; 
GWindow Controls;
GButton Recalculate; 
GButton saving; 
GLabel escapeIters; 
GLabel putIters; 
GSlider iterSlider; 
GSlider Chunks2; 
GLabel chunkLabel; 
GLabel label1; 
GSlider colors2; 
GLabel colorsLabel; 
GLabel colorsHolderidk; 
GSlider zoomLevel; 
GLabel zoomLabel; 
GLabel zoomHolder; 
GCheckbox stickyZoom; 

  public void settings() {  fullScreen();  pixelDensity(displayDensity()); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "MandelbrotSet3" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
