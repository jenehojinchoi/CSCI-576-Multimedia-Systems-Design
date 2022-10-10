
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

   int GREEN_PIXEL = 0xff000000 | (0 << 16) | (255 << 8) | 51;
   int mode;

   int [][] prevRGB = new int[width*height][3];
   int [][] currRGB = new int[width*height][3];
   float [][] prevHSV = new float[width*height][3];
   float [][] currHSV = new float[width*height][3];

   ArrayList<BufferedImage> frames;
   private static int DELAY = 41;

   /** Read Image RGB
    *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
    */
   private void readImageRGBMode1(int width, int height, String foregroundPath, String backgroundPath, BufferedImage tempImage)
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

   private void readImageRGBMode0(int width, int height, String prevFramePath, String currFramePath, String backgroundFramePath, BufferedImage tempImage)
   {
      try
      {
         int frameLength = width*height*3;

         File prevFrameFile = new File(prevFramePath);
         File currFrameFile = new File(currFramePath);
         File backFrameFile = new File(backgroundFramePath);

         RandomAccessFile rafPrev = new RandomAccessFile(prevFrameFile, "r");
         RandomAccessFile rafCurr = new RandomAccessFile(currFrameFile, "r");
         RandomAccessFile rafBack = new RandomAccessFile(backFrameFile, "r");
         rafPrev.seek(0);
         rafCurr.seek(0);
         rafBack.seek(0);

         long len = frameLength;
         byte[] prevBytes = new byte[(int) len];
         byte[] currBytes = new byte[(int) len];
         byte[] backBytes = new byte[(int) len];

         rafPrev.read(prevBytes);
         rafCurr.read(currBytes);
         rafBack.read(backBytes);

         int ind = 0;
         for(int y = 0; y < height; y++)
         {
            for(int x = 0; x < width; x++)
            {
               byte a = 0;
               byte r = prevBytes[ind];
               byte g = prevBytes[ind+height*width];
               byte b = prevBytes[ind+height*width*2];

               int prevR = (r & 0xff);
               int prevG = (g & 0xff);
               int prevB = (b & 0xff);

               byte r2 = currBytes[ind];
               byte g2 = currBytes[ind+height*width];
               byte b2 = currBytes[ind+height*width*2];

               int currR = (r2 & 0xff);
               int currG = (g2 & 0xff);
               int currB = (b2 & 0xff);

               byte rBack = backBytes[ind];
               byte gBack = backBytes[ind+height*width];
               byte bBack = backBytes[ind+height*width*2];

               int backR = (rBack & 0xff);
               int backG = (gBack & 0xff);
               int backB = (bBack & 0xff);

               prevRGB[ind][0] = prevR;
               prevRGB[ind][1] = prevG;
               prevRGB[ind][2] = prevB;

               currRGB[ind][0] = currR;
               currRGB[ind][1] = currG;
               currRGB[ind][2] = currB;

               int currPix = 0xff000000 | (currR << 16) | (currG << 8) | currB;
               int backPix = 0xff000000 | (backR << 16) | (backG << 8) | backB;

               if (Math.abs(prevR - currR) <= 7 && Math.abs(prevG - currG) <= 7 && Math.abs(prevB - currB) <= 7) {
                  tempImage.setRGB(x, y, backPix);
               } else {
                  tempImage.setRGB(x, y, currPix);
               }

               ind++;
            }
         }
         prevHSV = currHSV;
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
      mode = Integer.parseInt(args[2]);
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


      showIms(totalFrame, totalLabel);

      // another option: class video play extends timedTask
      if (mode == 1) {
         int i = 0;
         System.out.println("mode 1");
         while (i < numFiles) {
            String foregroundFileName = foregroundFileNames[i];
            String foregroundFamePath = foregroundVideoFilesPath + "/" + foregroundFileName;

            String backgroundFileName = backgroundFileNames[i];
            String backgroundFamePath = backgroundVideoFilesPath + "/" + backgroundFileName;

            BufferedImage tempImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            readImageRGBMode1(width, height, foregroundFamePath, backgroundFamePath, tempImage);
            frames.add(tempImage);
            i++;
         }
      }
      else {
         System.out.println("mode 0");
         int i = 1;
         while (i < numFiles) {
            String prevFileName = foregroundFileNames[i-1];
            String prevFramePath = foregroundVideoFilesPath + "/" + prevFileName;
            String currFileName = foregroundFileNames[i];
            String currFramePath = foregroundVideoFilesPath + "/" + currFileName;
            String backgroundFileName = backgroundFileNames[i];
            String backgroundFramePath = backgroundVideoFilesPath + "/" + backgroundFileName;

            BufferedImage tempImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            BufferedImage backgroundImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            readImageRGBMode0(width, height, prevFramePath, currFramePath, backgroundFramePath, tempImage);
            frames.add(tempImage);
            i++;
         }
      }

      runVideo(numFiles);
   }

   private void runVideo (int numFiles) {
      int i = 1;
      totalFrame.pack();
      totalFrame.setVisible(true);

      while (i < numFiles-1) {
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