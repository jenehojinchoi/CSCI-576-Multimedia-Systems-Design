import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import javax.swing.*;
import static java.lang.Math.cos;

public class ImageCompression {
   private static int width = 512;
   private static int height = 512;
   private static int BASE = 4096;
   int totalNum;
   BufferedImage originalImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

   int[][] red = new int[height][width];
   int[][] green = new int[height][width];
   int[][] blue = new int[height][width];

   BufferedImage DWTImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
   double[][] rDWT = new double[height][width];
   double[][] gDWT = new double[height][width];
   double[][] bDWT = new double[height][width];

   int[][] rIDWT = new int[height][width];
   int[][] gIDWT = new int[height][width];
   int[][] bIDWT = new int[height][width];


   JFrame imageFrame = new JFrame();
   GridBagLayout gridBagLayout = new GridBagLayout();
   JLabel JLabel = new JLabel();
   JLabel JLabelText = new JLabel();
   static double[][] cosineMatrix = new double[8][8];

   private void readImageRGB(File file) {
      try {
         InputStream inputStream = new FileInputStream(file);

         long fileLength = file.length();
         byte[] bytes = new byte[(int) fileLength];

         int offset = 0;
         int readCount = 0;
         while (offset < bytes.length && (readCount = inputStream.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += readCount;
         }
         calculateCosineMatrix();

         int index = 0;
         for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
               int r = bytes[index];
               int g = bytes[index + height * width];
               int b = bytes[index + height * width * 2];

               r = r & 0xFF;
               g = g & 0xFF;
               b = b & 0xFF;

               red[row][column] = r;
               green[row][column] = g;
               blue[row][column] = b;

               int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
               originalImg.setRGB(column, row, pix);
               index++;
            }
         }
      }
      catch (FileNotFoundException e) {
         e.printStackTrace();

      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void calculateCosineMatrix() {
      for (int r = 0; r < 8; r++)
         for (int c = 0; c < 8; c++)
            cosineMatrix[r][c] = cos((2*r+1) * c * Math.PI / 16.00);
   }

   /**
    * @param matrix : Matrix A
    * @return Transposed Matrix of A
    */

   private static double[][] transpose(double[][] matrix) {
      double[][] temp = new double[height][width];
      for (int r = 0; r < height; r++)
         for (int c = 0; c < width; c++)
            temp[r][c] = matrix[c][r];
      return temp;
   }

   public void processDWT(String[] args) {
      try {
         File file = new File(args[0]);
         int lowPassCoefficient = Integer.parseInt(args[1]);
         readImageRGB(file);

         if (lowPassCoefficient >= 0) {
            totalNum = (int) Math.pow(2, lowPassCoefficient*2);

            rDWT = DWTDecomposition(red, totalNum);
            gDWT = DWTDecomposition(green, totalNum);
            bDWT = DWTDecomposition(blue, totalNum);

            rIDWT = IDWTComposition(rDWT);
            gIDWT = IDWTComposition(gDWT);
            bIDWT = IDWTComposition(bDWT);
            showRecoveredDWT(-1);
         }
         else {
            for (int idx = 1; idx < 10; idx ++) {
               int level = idx;
               totalNum = (int) Math.pow(2, level *2);

               rDWT = DWTDecomposition(red, totalNum);
               gDWT = DWTDecomposition(green, totalNum);
               bDWT = DWTDecomposition(blue, totalNum);

               rIDWT = IDWTComposition(rDWT);
               gIDWT = IDWTComposition(gDWT);
               bIDWT = IDWTComposition(bDWT);

               try {
                  Thread.sleep(3000);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
               showRecoveredDWT(idx);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void showRecoveredDWT(int idx) {
      for (int r = 0; r < height; r++) {
         for (int c = 0; c < width; c++) {
            int rr = rIDWT[r][c] & 0xff;
            int gg = gIDWT[r][c] & 0xff;
            int bb = bIDWT[r][c] & 0xff;

            int pixValue = 0xff000000 | ((rr << 16) | ((gg << 8) | bb));
            DWTImage.setRGB(c, r, pixValue);
         }
      }

      imageFrame.getContentPane().setLayout(gridBagLayout);

      int step = idx-1;
      JLabelText.setText(idx != -1 ? "DWT View (Step: " + step + ")" : "DWT View ");
      JLabelText.setHorizontalAlignment(SwingConstants.CENTER);
      JLabel.setIcon(new ImageIcon(DWTImage));

      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.anchor = GridBagConstraints.CENTER;
      gridBagConstraints.weightx = 0.5;
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 0;
      imageFrame.getContentPane().add(JLabelText, gridBagConstraints);

      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 1;
      imageFrame.getContentPane().add(JLabel, gridBagConstraints);

      imageFrame.pack();
      imageFrame.setVisible(true);
   }

   private double[][] DWTDecomposition(int[][] matrix, int n) {
      double[][] DWTMatrix = new double[height][width];
      for (int r = 0; r < height; r++)
         for (int c = 0; c < width; c++)
            DWTMatrix[r][c] = matrix[r][c];

      for (int r = 0; r < width; r++)
         DWTMatrix[r] = getDecompositionArray(DWTMatrix[r]);
      DWTMatrix = transpose(DWTMatrix);

      for (int c = 0; c < width; c++)
         DWTMatrix[c] = getDecompositionArray(DWTMatrix[c]);
      DWTMatrix = transpose(DWTMatrix);

      DWTMatrix = zigZagTraversal(DWTMatrix, n);

      return DWTMatrix;
   }


   private double[] getDecompositionArray(double[] array) {
      int height = array.length;
      while (height > 0) {
         array = decompositionStep(array, height);
         height = height / 2;
      }
      return array;
   }

   private double[] decompositionStep(double[] array, int height) {
      double[] decomposedArray = Arrays.copyOf(array, array.length);
      for (int i = 0; i < height / 2; i++) {
         // low
         decomposedArray[i] = (array[2*i] + array[2*i+1]) / 2;
         // high
         decomposedArray[height / 2 + i] = (array[2*i] - array[2*i+1]) / 2;
      }
      return decomposedArray;
   }


   private static int[][] IDWTComposition(double[][] matrix) {
      int[][] IDWTMatrix = new int[height][width];

      // all columns
      matrix = transpose(matrix);
      for (int r = 0; r < width; r++) {
         matrix[r] = getCompositionArray(matrix[r]);
      }

      // all rows
      matrix = transpose(matrix);
      for (int c = 0; c < height; c++) {
         matrix[c] = getCompositionArray(matrix[c]);
      }

      for (int r = 0; r < height; r++) {
         for (int c = 0; c < width; c++) {
            IDWTMatrix[r][c] = (int) Math.round(matrix[r][c]);
            if (IDWTMatrix[r][c] < 0) {
               IDWTMatrix[r][c] = 0;
            }
            if (IDWTMatrix[r][c] > 255) {
               IDWTMatrix[r][c] = 255;
            }
         }
      }
      return IDWTMatrix;
   }

   private static double[] getCompositionArray(double[] array) {
      int height = 1;
      while (height <= array.length) {
         array = compositionStep(array, height);
         height = height * 2;
      }
      return array;
   }

   private static double[] compositionStep(double[] array, int height) {
      double[] composedArray = Arrays.copyOf(array, array.length);
      for (int i = 0; i < height / 2; i++) {
         composedArray[2*i] = array[i] + array[height / 2 + i];
         composedArray[2*i + 1] = array[i] - array[height / 2 + i];
      }
      return composedArray;
   }

   public double[][] zigZagTraversal(double[][] matrix, int m) {
      int r = 0; int c = 0;
      int length = matrix.length - 1;
      int count = 1;

      matrix[r][c] = count > m ? 0 : matrix[r][c];
      count++;

      while (true) {
         c++;
         matrix[r][c] = count > m ? 0 : matrix[r][c];
         count++;
         while (c != 0) {
            r++;
            c--;
            matrix[r][c] = count > m ? 0 : matrix[r][c];
            count++;
         }
         r++;
         if (r > length) {
            r--;
            break;
         }
         matrix[r][c] = count > m ? 0 : matrix[r][c];
         count++;
         while (r != 0) {
            r--;
            c++;
            matrix[r][c] = count > m ? 0 : matrix[r][c];
            count++;
         }
      }

      while (true) {
         c++;
         count++;
         if (count > m) {
            matrix[r][c] = 0;
         }
         while (c != length) {
            c++;
            r--;
            matrix[r][c] = count > m ? 0 : matrix[r][c];
            count++;
         }
         r++;
         if (r > length) {
            r--;
            break;
         }
         matrix[r][c] = count > m ? 0 : matrix[r][c];
         count++;
         while (r < length) {
            r++;
            c--;
            matrix[r][c] = count > m ? 0 : matrix[r][c];
            count++;
         }
      }
      return matrix;
   }

   public static void main(String[] args) {
      ImageCompression imageCompress = new ImageCompression();
      imageCompress.processDWT(args);
   }
}