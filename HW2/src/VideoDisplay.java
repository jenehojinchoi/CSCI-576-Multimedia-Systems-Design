
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.nio.Buffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class VideoDisplay {
   JFrame totalFrame;
   JLabel totalLabel;
   int width = 640; // default image width and height
   int height = 480;

   ArrayList<BufferedImage> frames;
   private static int DELAY = 42;

   /** Read Image RGB
    *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
    */
   private void readImageRGB(int width, int height, String foregroundPath, String backgroundPath, BufferedImage tempImage)
   {
      try
      {
         int frameLength = width*height*3;

         File foregroundFile = new File(foregroundPath);
         File backgroundFile = new File(backgroundPath);

         RandomAccessFile rafFore = new RandomAccessFile(foregroundFile, "r");
         RandomAccessFile rafBack = new RandomAccessFile(backgroundFile, "r");
         rafFore.seek(0);
         rafBack.seek(0);

         long len = frameLength;
         byte[] foreBytes = new byte[(int) len];
         float[][] HSV = new float[width*height][3];
         byte[] backBytes = new byte[(int) len];

         rafFore.read(foreBytes);
         rafBack.read(backBytes);

         int ind = 0;
         for(int y = 0; y < height; y++)
         {
            for(int x = 0; x < width; x++)
            {
               byte a = 0;
               byte r = foreBytes[ind];
               byte g = foreBytes[ind+height*width];
               byte b = foreBytes[ind+height*width*2];

               byte rBack = backBytes[ind];
               byte gBack = backBytes[ind+height*width];
               byte bBack = backBytes[ind+height*width*2];

               int R = (r & 0xff);
               int G = (g & 0xff);
               int B = (b & 0xff);

               int pix = 0xff000000 | (R << 16) | (G << 8) | B;
               int pixBack = 0xff000000 | ((rBack & 0xff) << 16) | ((gBack & 0xff) << 8) | (bBack & 0xff);

               float[] oneRowHSV = Color.RGBtoHSB(R, G, B, null);
               HSV[ind] = oneRowHSV;

               float H = oneRowHSV[0] * 360;
               float S = oneRowHSV[1] * 100;
               float V = oneRowHSV[2] * 100;

               if (88 <= H && H <= 173 && V >= 43){
                  tempImage.setRGB(x, y, pixBack);
               } else {
                  tempImage.setRGB(x, y, pix);
               }

               ind++;
            }
         }
      }
      catch (FileNotFoundException e)
      {
         e.printStackTrace();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
   }

   public void showVideo(String[] args){
      // Read videoFilePaths for foreground and background
      String foregroundVideoFilesPath = args[0];
      String backgroundVideoFilesPath = args[1];
      String mode = args[2];
      System.out.println(mode);

      // foreground, background files
      File foregroundVideoDirectory = new File(foregroundVideoFilesPath);
      String [] foregroundFileNames = foregroundVideoDirectory.list();
      Arrays.sort(foregroundFileNames);

      File backgroundVideoDirectory = new File(backgroundVideoFilesPath);
      String [] backgroundFileNames = backgroundVideoDirectory.list();
      Arrays.sort(backgroundFileNames);

      int numFiles = foregroundFileNames.length;
      frames = new ArrayList<BufferedImage>(numFiles);
      totalFrame = new JFrame();
      totalLabel = new JLabel();

      int i = 0;

      showIms(totalFrame, totalLabel);

      // another option: class video play extends timedTask
      while (i < numFiles) {
         String foregroundFileName = foregroundFileNames[i];
         String foregroundFamePath = foregroundVideoFilesPath + "/" + foregroundFileName;

         String backgroundFileName = backgroundFileNames[i];
         String backgroundFamePath = backgroundVideoFilesPath + "/" + backgroundFileName;

         BufferedImage tempImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

         readImageRGB(width, height, foregroundFamePath, backgroundFamePath, tempImage);
         frames.add(tempImage);
         i++;
      }
      runVideo(numFiles);
   }

   private void runVideo (int numFiles) {
      int i = 0;
      totalFrame.pack();
      totalFrame.setVisible(true);

      while (i < numFiles) {
         BufferedImage tempFrame = frames.get(i);
         totalLabel.setIcon(new ImageIcon(tempFrame));

         try {
            Thread.sleep(DELAY);
         } catch (InterruptedException e) {
            throw new RuntimeException(e);
         }
         i++;
      }
   }

   private void showIms (JFrame totalFrame, JLabel totalLabel) {
      BufferedImage bufferedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      GridBagLayout gLayout1 = new GridBagLayout();
      totalFrame.getContentPane().setLayout(gLayout1);

      totalLabel.setIcon(new ImageIcon(bufferedImg));

      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.anchor = GridBagConstraints.CENTER;
      c.weightx = 0.5;
      c.gridx = 0;
      c.gridy = 0;

      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridx = 0;
      c.gridy = 1;
      totalFrame.getContentPane().add(totalLabel, c);
   }


   public static void main(String[] args) {
      VideoDisplay ren = new VideoDisplay();
      ren.showVideo(args);
   }
}