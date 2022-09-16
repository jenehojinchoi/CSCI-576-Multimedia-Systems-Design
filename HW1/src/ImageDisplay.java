
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class ImageDisplay {

    JFrame frame;
    JLabel lbIm1;
    BufferedImage imgOne;
    BufferedImage processedImg;
    int width = 1920; // default image width and height
    int height = 1080;

    int Y_input;
    int U_input;
    int V_input;

    /** Read Image RGB
     *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
     */
    private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
    {
        try
        {
            int frameLength = width*height*3;

            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            long len = frameLength;
            byte[] bytes = new byte[(int) len];

            raf.read(bytes);

            int ind = 0;
            for(int y = 0; y < height; y++)
            {
                for(int x = 0; x < width; x++)
                {
                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind+height*width];
                    byte b = bytes[ind+height*width*2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                    img.setRGB(x,y,pix);
                    ind++;
                }
            }

            // ADDED: convert to YUV
            int[][] givenYUV = convertToYUV(bytes, width, height);

            // ADDED: subsampling
            int[][] subSampledYUV = subSample(givenYUV, this.Y_input, this.U_input, this.V_input, width, height);

            // ADDED: convert to RGB
            // have to put upsampled YUV
            // but for now, to test if convertToRGB works, i'm using givenYUV as an input
            int[][] backToRGB = convertToRGB(givenYUV, width, height);

            // ADDED: image creation
            //subSampledImageCreation
            ind = 0;
            for(int y = 0; y < height; y++)
            {
                for(int x = 0; x < width; x++)
                {
                    int pix = 0xff000000 | (backToRGB[0][ind] << 16) | (backToRGB[1][ind] << 8) | backToRGB[2][ind];
                    this.processedImg.setRGB(x,y,pix);
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

    public void showIms(String[] args){

        // ADDED
        // Read a parameter from command line
        this.Y_input = Integer.parseInt(args[1]);
        this.U_input = Integer.parseInt(args[2]);
        this.V_input = Integer.parseInt(args[3]);

        // Read in the specified image
        imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // ADDED
        // create a processed image
        processedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        readImageRGB(width, height, args[0], imgOne);


        // Use label to display the image
        frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        lbIm1 = new JLabel(new ImageIcon(imgOne));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        frame.getContentPane().add(lbIm1, c);

        frame.pack();
        frame.setVisible(true);
    }

    private int[][] convertToYUV(byte[] rgb, int width,int height){
        int[][] yuv = new int[3][rgb.length/3];
        int index = 0;
        for(int p = 0; p < height; p++){
            for(int q = 0; q < width; q++){
                int r = Byte.toUnsignedInt(rgb[index]);
                int g = Byte.toUnsignedInt(rgb[index+height*width]);
                int b = Byte.toUnsignedInt(rgb[index+height*width*2]);

                int y = Math.round((float)(0.299*r + 0.587*g + 0.114*b));
                int u = Math.round((float)(0.596*r - 0.274*g - 0.322*b));
                int v = Math.round((float)(0.211*r - 0.523*g + 0.312*b));

                yuv[0][index] = y;
                yuv[1][index] = u;
                yuv[2][index] = v;
                index++;
            }
        }
        return yuv;
    }

    private int[][] subSample(int[][] givenYUV, int y_input, int u_input, int v_input, int width, int height){
        int[][] subSampled_YUV = new int[3][givenYUV[0].length];
        int index = 0;
        for(int p = 0; p < height; p++)
        {
            for(int q = 0; q < width; q++)
            {
                subSampled_YUV[0][index] = q % y_input == 0 ? givenYUV[0][index] : Integer.MIN_VALUE;
                subSampled_YUV[1][index] = q % u_input == 0 ? givenYUV[1][index] : Integer.MIN_VALUE;
                subSampled_YUV[2][index] = q % v_input == 0 ? givenYUV[2][index] : Integer.MIN_VALUE;
                index++;
            }
        }
        return subSampled_YUV;
    }

    private int[][] convertToRGB(int[][] yuv, int width, int height){
        int[][] rgb = new int[3][yuv[0].length];
        int ind = 0;
        for(int p = 0; p < height; p++)
        {
            for(int q = 0; q < width; q++)
            {
                int y = yuv[0][ind];
                int u = yuv[1][ind];
                int v = yuv[2][ind];

                int r = Math.round((float)(1.000 * y + 0.956 * u + 0.621 * v));
                int g = Math.round((float)(1.000 * y - 0.272 * u - 0.647 * v));
                int b = Math.round((float)(1.000 * y - 1.106 * u + 1.703 * v));

                rgb[0][ind] = r>=0?r:0;
                rgb[1][ind] = g>=0?g:0;
                rgb[2][ind] = b>=0?b:0;
                ind++;
            }
        }
        return rgb;
    }

    public static void main(String[] args) {
        ImageDisplay ren = new ImageDisplay();
        ren.showIms(args);
    }

}
