package WeirdFilter;
import java.util.ArrayList;
import java.util.List;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

/**
 * 
 * @author alan
 * 
 *Given the scenario that your room mate wanted to filter his image with random noise based on a
 *pixels surrounding 8 pixels, we've implemented this WeirdFilter class which implements the 
 *BufferedImageOp interface. This expands on the original weirdFilter class by making the process
 *data parallel rather than task parallel.
 * 
 */
public class DataParallelWeirdFilter extends Thread implements BufferedImageOp{

  int maxY;
  int maxX;
  int minY;
  int minX;
  int thread_count;
  
  /**
   * 
   * @author alan
   * this class is responsible for handling the threads produced by the parent class DataParallelWeirdFilter.
   * It calls the processpixel method for each pixel within parameters (passed in constructor).
   *
   */
  public class MyTask extends Thread {
    int maxY;
    int maxX;
    int minY;
    int minX;
    BufferedImage inputImage;
    BufferedImage outputImage;
    
    /**
     * 
     * @param minY - the min value of the x coordinate
     * @param maxY - the max value of the x coordinate
     * @param minX - the min value of the y coordinate
     * @param maxX - the max value of the y coordinate
     * @param input - the input image to be processed with the weird filter
     * @param output - the result image after processing
     */
    public MyTask(int minY, int maxY, int minX, int maxX, BufferedImage input, BufferedImage output) {
      this.maxY = maxY;
      this.maxX = maxX;
      this.minY = minY;
      this.minX = minX;
      this.inputImage = input;
      this.outputImage = output;
      
      //System.out.println("width :"+input.getWidth());
      //System.out.println("height :"+input.getHeight());
      //System.out.println("minY :"+minY);
      //System.out.println("maxY :"+maxY);
      //System.out.println("minX :"+minX);
      //System.out.println("maxX :"+maxX);
      //System.out.println("width :"+input.getWidth());
      //System.out.println("height :"+input.getHeight());
    }
    
    /**
     * threads run this when started, will compute 2D array of parameters passed in MyTask constructor
     */
    public void run() {
      //System.out.println("maxX in run method: "+maxX);
      for (int i = minX; i < maxX; i++) {
        for (int j = minY; j < maxY; j++) {
          //System.out.println("i: " +i +" j: "+ j);
          int temp = processPixel(inputImage, i, j, minY, maxY, minX, maxX);
          //System.out.println("temp values: "+temp);
            outputImage.setRGB(i, j, temp);
        }
      }
    }
  }
  /**
   * constructor (not used)
   * @param thread_count - number of threads used to process an image
   */
  public DataParallelWeirdFilter(int thread_count) {
    this.thread_count = thread_count;
    
  }
  
  
  /**
   * Filter method.
   * Distributes work of an image across all threads evenly to demonstrate data parallelism
   * Calls the processPixel method for each pixel within the picture.
   * @param inputImage - the input image that will be filtered with the "weird" filter
   * @param outputImage - the output image  with all pixels filtered
   * 
   */
  @Override
  public BufferedImage filter(BufferedImage inputImage, BufferedImage outputImage) {
    // TODO Auto-generated method stub
    int width = inputImage.getWidth();
    //int height = inputImage.getHeight();
    int thread_count = this.thread_count;
    int rows = inputImage.getHeight()/thread_count;
    int remainder = inputImage.getHeight()%thread_count;
    List<MyTask> threads = new ArrayList<MyTask>();
    
    //this for loop uses the offset_num variable to balance out the number of rows each thread processes
    int offset_num = 0; //keeps track of how many rows to offset
    int start_row_ind;  //index of start row
    int end_row_ind;    //index of end row
    for(int i = 0; i < thread_count; i++)
    {
      start_row_ind = i*rows;
      end_row_ind = (i+1)*rows - 1;
      if(remainder > 0) 
      {
        //System.out.println("remainder: "+remainder);
        MyTask thread = new MyTask(start_row_ind+offset_num, (end_row_ind + offset_num+1), 0, width-1 , inputImage, outputImage);
        threads.add(thread);
        remainder--;
        offset_num++;
      }
      else if(remainder == 0 && offset_num > 0)
      {
        //System.out.println("remainder: "+remainder + "offset: "+offset_num);
        MyTask thread = new MyTask(start_row_ind+offset_num, (end_row_ind + offset_num), 0, width-1 , inputImage, outputImage);
        threads.add(thread);
      }
      else
      {
        //System.out.println("else clause");
        MyTask thread = new MyTask(start_row_ind, end_row_ind, 0, width-1 , inputImage, outputImage);
        threads.add(thread);
      }
    }
    for (int i= 0; i < threads.size(); i++)
    {
      threads.get(i).start();
    }
    
    for (int i= 0; i < threads.size(); i++)
    {
      try {
        threads.get(i).join();
      }
      catch (InterruptedException e) {
        // TODO Auto-generated catch block
        System.out.println("couldn't join");
        e.printStackTrace();
      }
    }
    
    return outputImage;
    
  }

  @Override
  public Rectangle2D getBounds2D(BufferedImage src) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RenderingHints getRenderingHints() {
    // TODO Auto-generated method stub
    return null;
  }
  
  /**
   * this method processes the pixel RGB depending on the surrounding pixel RGB vlues.
   * @param image - the input image to be processed with the "weird" filter
   * @param x - the x coordinate of the pixel
   * @param y - the y coordinate of the pixel
   * @param minY - the min value of the y coordinate
   * @param maxY - the max value of the y coordinate
   * @param minX - the min value of the x coordinate
   * @param maxX - the max value of the x coordinate
   * @return int, that holds the RGB value for the pixel.
   */
  private static int processPixel(BufferedImage image, int x, int y, int minY, int maxY, int minX, int maxX) {
    byte[] new_pixel = new byte[3];
    byte[] neighbor = new byte[3];
   
  //looks through the surrounding pixels to calculate new pixel. Bounds are made to handle edges.
    for (int i = Math.max(x - 1, minX); i <= Math.min(x + 1, maxX); i++)
    {
      for (int j = Math.max(y - 1, minY); j <= Math.min(y + 1, maxY); j++) 
      {
        if (i != x || j != y)
        {
          neighbor = RGB.intToBytes(image.getRGB(i, j));
          new_pixel[0] += Math.max(neighbor[0], 40) + 10*Math.cos(neighbor[0]);
          new_pixel[1] += Math.min(neighbor[1], 100);
          new_pixel[2] += Math.min(Math.exp(neighbor[2]), 40);
        }
      }
    }
    
    return RGB.bytesToInt(new_pixel);
  }

}
