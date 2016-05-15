package WeirdFilter;
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
 *BufferedImageOp interface. We'll only be overwriting the filter() method.
 */
public class WeirdFilter implements BufferedImageOp {

  public WeirdFilter() {
    
  }
  
  /**
   * Filter method.
   * Calls the processPixel method for each pixel within the picture.
   * @param inputImage - the input image that will be filtered with the "weird" filter
   * @param outputImage - the output image  with all pixels filtered
   * 
   */
  @Override
  public BufferedImage filter(BufferedImage inputImage, BufferedImage outputImage) {
    // TODO Auto-generated method stub
    int maxX = inputImage.getWidth();
    int maxY = inputImage.getHeight();
    
    for (int i = 0; i < maxX; i++) {
      for (int j = 0; j < maxY; j++) {
        outputImage.setRGB(i, j, processPixel(inputImage, i, j, 0, maxX -1, 0, maxY -1));
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
   * process
   * unused method, was used for a previous iteration of the assignment
   * @param inputImage
   * @param outputImage
   *
  private static void process(BufferedImage inputImage,
      BufferedImage outputImage) {
    int maxX = inputImage.getWidth();
    int maxY = inputImage.getHeight();
    
    for (int i = 0; i < maxX; i++) {
      for (int j = 0; j < maxY; j++) {
        outputImage.setRGB(i, j, processPixel(inputImage, i, j, 0, maxX, 0, maxY));
      }
    }
  }
  */
  
  /**
   * 
   * @param image - the input image to be processed with the "weird" filter
   * @param x - the x coordinate of the pixel
   * @param y - the y coordinate of the pixel
   * @param minX - the min value of the x coordinate
   * @param maxX - the max value of the x coordinate
   * @param minY - the min value of the y coordinate
   * @param maxY - the max value of the y coordinate
   * @return int, that holds the RGB value for the pixel.
   */
  private static int processPixel(BufferedImage image, int x, int y, int minX, int maxX, int minY, int maxY) {
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
