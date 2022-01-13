
/*
Timer: acts as a stopwatch for processes being run
 */
class Timer {
  int initiationTime;
  int startTime;
  int lastLapTime;
  int stopTime;

  void Timer() {
    initiationTime = millis();
    startTime = -1;
    lastLapTime = -1;
  }

  void startStopwatch() {
    startTime = millis();
    lastLapTime = 0;
  }

  int lap() {
    lastLapTime = millis() - startTime - lastLapTime;
    return lastLapTime;
  }

  int stopStopwatch() {
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
  CalculationThread calculateData() {
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
    double zx = 0.0;
    double zy = 0.0;
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
