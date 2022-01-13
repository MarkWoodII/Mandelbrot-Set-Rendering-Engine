import g4p_controls.*;

double centerX, centerY;
double lowerX, upperX, lowerY, upperY;
double magnification, range;
int maxIterations;

double zx, zy, cx, cy, tmp;
color c;

int mili, renderIterations;

Chunk main;

CalculationThread[] givenData;
int acl;

float hue;
PVector cur;

int savedAs;
boolean saveShot;

void setup() {
  createGUI();
  customGUI();
  frameRate(30);
  fullScreen();
  pixelDensity(displayDensity());
  colorMode(HSB, 1);
  strokeWeight(1);
  noFill();
  Controls.setAlwaysOnTop(true);

  centerX = 0;//-0.8885416810711224;
  centerY = 0;//-0.10506944739156299;
  magnification = 1;//1/0.005;
  range = 1.7;
  lowerX = centerX - range/magnification;
  upperX = centerX + range/magnification;
  lowerY = centerY - range * height/width / magnification;
  upperY = centerY + range * height/width / magnification;

  maxIterations = 1000;
  acl = 10;
  hue = 0.62;

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

void draw() {
  updatePixels();
  if (saveShot) {
    saveShot = !saveShot;
    save("screenshot-"+savedAs+".png");
  }
}

void renderSet() {
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
  println("Finished calculating in " + (timer.lap()/1000.0));
  print("getting colors...   ");
  getColors(pixelIters);
  println("Finished getting colors in " + (timer.lap()/1000.0));
  double magni = upperX - lowerX;
  println("Done. Finished rendering in " + (timer.stopStopwatch()/1000.0) + "\nRendered at: "+centerX + ", " + centerY + "\nScale: " + magni + ", Magnification: " + magnification + "\nEscape Iterations: " + maxIterations);
}

void getColors(int[][] pixelIters) {
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
        pixels[i+(j*pixelWidth)] = color(hue, 1 - 0.9 * setColor[i][j], 0.1 + 0.9 * setColor[i][j]);
    }
  }
  updatePixels();
}

void go() {
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
