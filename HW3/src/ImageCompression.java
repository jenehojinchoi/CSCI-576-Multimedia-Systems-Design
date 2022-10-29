import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;

import javax.swing.*;

import static java.lang.Math.cos;

public class ImageCompression {
   public static void main(String[] args) {
      ImageCompression ren = new ImageCompression();
      ren.processDWT(args);
   }

   private static int WIDTH = 512;
   private static int HEIGHT = 512;
   private static int MAX_PIXEL_VAL = 255;
   private static int BLOCK_SIZE = 8;
   private static int BASE = 4096;

   BufferedImage originalImg = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

   int[][] red = new int[HEIGHT][WIDTH];
   int[][] green = new int[HEIGHT][WIDTH];
   int[][] blue = new int[HEIGHT][WIDTH];

   BufferedImage DWTImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
   double[][] rDWT = new double[HEIGHT][WIDTH];
   double[][] gDWT = new double[HEIGHT][WIDTH];
   double[][] bDWT = new double[HEIGHT][WIDTH];

   int[][] rIDWT = new int[HEIGHT][WIDTH];
   int[][] gIDWT = new int[HEIGHT][WIDTH];
   int[][] bIDWT = new int[HEIGHT][WIDTH];


   JFrame imageFrame = new JFrame();
   GridBagLayout gridBagLayout = new GridBagLayout();
   JLabel JLabel = new JLabel();
   JLabel JLabelText = new JLabel();
   static double[][] cosineBlockMatrix = new double[BLOCK_SIZE][BLOCK_SIZE];

   private void readImageRGB(File file, int coefficientNum) {
      try {
         InputStream inputStream = new FileInputStream(file);

         long fileLength = file.length();
         byte[] bytes = new byte[(int) fileLength];

         int offset = 0;
         int readCount = 0;
         while (offset < bytes.length && (readCount = inputStream.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += readCount;
         }

         for (int row = 0; row < BLOCK_SIZE; row++)
            for (int column = 0; column < BLOCK_SIZE; column++)
               cosineBlockMatrix[row][column] = cos((2 * row + 1) * column * 3.14159 / 16.00);

         int index = 0;
         for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
               int r = bytes[index];
               int g = bytes[index + HEIGHT * WIDTH];
               int b = bytes[index + HEIGHT * WIDTH * 2];

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
   public void processDWT(String[] args) {

      try {
         File file = new File(args[0]);
         int coefficientNum = Integer.parseInt(args[1]);
         readImageRGB(file, coefficientNum);

         if (coefficientNum >= 0) {
            int totalNum = (int) Math.pow(2, coefficientNum*2);
            System.out.println(totalNum);
            int m = totalNum / BASE;

            rDWT = decomposeDWT(red, totalNum);
            gDWT = decomposeDWT(green, totalNum);
            bDWT = decomposeDWT(blue, totalNum);

            rIDWT = IDWTComposition(rDWT);
            gIDWT = IDWTComposition(gDWT);
            bIDWT = IDWTComposition(bDWT);

            showRecoveredDWT(-1);
         } else {
            for (int idx = 0; idx < 10; idx ++) {
               int level = idx;
               int totalNum = (int) Math.pow(2, level *2);
               int m = totalNum / BASE;

               rDWT = decomposeDWT(red, totalNum);
               gDWT = decomposeDWT(green, totalNum);
               bDWT = decomposeDWT(blue, totalNum);

               rIDWT = IDWTComposition(rDWT);
               gIDWT = IDWTComposition(gDWT);
               bIDWT = IDWTComposition(bDWT);

               try {
                  Thread.sleep(500);
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


   /**
    * Show DWT transformed image for a level
    * @param idx : number of level
    */
   private void showRecoveredDWT(int idx) {
      for (int row = 0; row < HEIGHT; row++) {
         for (int column = 0; column < WIDTH; column++) {
            int rr = rIDWT[row][column] & 0xff;
            int gg = gIDWT[row][column] & 0xff;
            int bb = bIDWT[row][column] & 0xff;

            int pixValue = 0xff000000 | ((rr << 16) | ((gg << 8) | bb));
            DWTImage.setRGB(column, row, pixValue);
         }
      }

      imageFrame.getContentPane().setLayout(gridBagLayout);

      JLabelText.setText(idx != -1 ? "DWT View (n : " + idx + "/9)" : "DWT View ");
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

   /**
    * @param matrix : Matrix A
    * @return Transposed Matrix of A
    */

   private static double[][] performMatrixTranspose(double[][] matrix) {
      double[][] tempMat = new double[HEIGHT][WIDTH];
      for (int row = 0; row < HEIGHT; row++)
         for (int column = 0; column < WIDTH; column++)
            tempMat[row][column] = matrix[column][row];

      return tempMat;
   }


   private double[][] decomposeDWT(int[][] matrix, int n) {
      double[][] DWTMatrix = new double[HEIGHT][WIDTH];

      for (int row = 0; row < HEIGHT; row++)
         for (int column = 0; column < WIDTH; column++)
            DWTMatrix[row][column] = matrix[row][column];

      for (int row = 0; row < WIDTH; row++)
         DWTMatrix[row] = getDecompositionArray(DWTMatrix[row]);

      DWTMatrix = performMatrixTranspose(DWTMatrix);
      for (int col = 0; col < HEIGHT; col++)
         DWTMatrix[col] = getDecompositionArray(DWTMatrix[col]);

      DWTMatrix = performMatrixTranspose(DWTMatrix);
      DWTMatrix = doZigZagTraversal(DWTMatrix, n);

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

   /**
    * @return Low and High Pass decomposition
    */

   private double[] decompositionStep(double[] array, int height) {
      double[] dArray = Arrays.copyOf(array, array.length);
      for (int index = 0; index < height / 2; index++) {
         dArray[index] = (array[2 * index] + array[2 * index + 1]) / 2;
         dArray[height / 2 + index] = (array[2 * index] - array[2 * index + 1]) / 2;
      }
      return dArray;
   }


   /**
    * Recreating the image after DWT operation
    *
    * @param matrix : DWT matrix
    * @return : possible original image
    */

   private static int[][] IDWTComposition(double[][] matrix) {
      int[][] IDWTMatrix = new int[HEIGHT][WIDTH];

      matrix = performMatrixTranspose(matrix);
      for (int row = 0; row < WIDTH; row++) {
         matrix[row] = getCompositionArray(matrix[row]);
      }

      matrix = performMatrixTranspose(matrix);
      for (int col = 0; col < HEIGHT; col++) {
         matrix[col] = getCompositionArray(matrix[col]);
      }

      for (int row = 0; row < HEIGHT; row++) {
         for (int column = 0; column < WIDTH; column++) {
            IDWTMatrix[row][column] = (int) Math.round(matrix[row][column]);
            IDWTMatrix[row][column] = IDWTMatrix[row][column] < 0 ? 0 : (IDWTMatrix[row][column] > MAX_PIXEL_VAL ? MAX_PIXEL_VAL : IDWTMatrix[row][column]);
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
      double[] dArray = Arrays.copyOf(array, array.length);
      for (int index = 0; index < height / 2; index++) {
         dArray[2 * index] = array[index] + array[height / 2 + index];
         dArray[2 * index + 1] = array[index] - array[height / 2 + index];
      }
      return dArray;
   }


   /**
    * coefficient counting
    *
    * @param matrix : image matrix
    * @param m      : n/4096
    * @return : matrix after zig zag traversal
    */

   public double[][] doZigZagTraversal(double[][] matrix, int m) {
      int row = 0;
      int column = 0;
      int length = matrix.length - 1;
      int count = 1;

      matrix[row][column] = count > m ? 0 : matrix[row][column];
      count++;

      while (true) {

         column++;
         matrix[row][column] = count > m ? 0 : matrix[row][column];
         count++;

         while (column != 0) {
            row++;
            column--;
            matrix[row][column] = count > m ? 0 : matrix[row][column];
            count++;
         }
         row++;
         if (row > length) {
            row--;
            break;
         }

         matrix[row][column] = count > m ? 0 : matrix[row][column];
         count++;

         while (row != 0) {
            row--;
            column++;
            matrix[row][column] = count > m ? 0 : matrix[row][column];
            count++;
         }
      }

      while (true) {
         column++;
         count++;

         if (count > m) {
            matrix[row][column] = 0;
         }

         while (column != length) {
            column++;
            row--;
            matrix[row][column] = count > m ? 0 : matrix[row][column];
            count++;
         }

         row++;
         if (row > length) {
            row--;
            break;
         }
         matrix[row][column] = count > m ? 0 : matrix[row][column];
         count++;

         while (row < length) {
            row++;
            column--;
            matrix[row][column] = count > m ? 0 : matrix[row][column];
            count++;
         }
      }
      return matrix;
   }
}