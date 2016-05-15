package WeirdFilter;
/**
 * 
 * @author Henri Casanova
 *Provided methods by Dr. Henri Casanova to aid in converting RGB ints to bytes and vice versa.
 *
 */
public class RGB {

  /**
   * 
   * @param bytes - an array of bytes representing RGB values of a pixel to be converted to an integer value
   * @return - returns the integer value of RGB
   */
  public static int bytesToInt(byte bytes[]) {
    int value = 0;
    for (int i = 0; i < bytes.length; i++) {
      int shift = (bytes.length - 1 - i) * 8;
      value += (bytes[i] & 0x000000FF) << shift;
    }
    return value;
  }
/**
 * 
 * @param rgb - the int value of a pixel
 * @return - an array of 3 bytes representing the RGB values of a pixel
 */
  public static byte[] intToBytes(int rgb) {
    byte[] bytes = new byte[3];
    for (int i = 0; i < 3; i++) {
      int offset = (2 - i) * 8;
      bytes[i] = (byte) ((rgb >>> offset) & 0xFF);
    }
    return bytes;
  }
}