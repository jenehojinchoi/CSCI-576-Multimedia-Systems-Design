
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class ImageDisplay {


    JFrame frame1; // original
    JFrame frame2; // processed
    JLabel lbIm1; // original
    JLabel lbIm2; // processed
    BufferedImage original_image;
    BufferedImage processed_image;
    int width = 1920; // default image width and height
    int height = 1080;

    int Y_input;
    int U_input;
    int V_input;

    /** Read Image RGB
     *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
     */
    private void readImageRGB(int width, int height, String imgPath)
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
                    // why needed?
                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind+height*width];
                    byte b = bytes[ind+height*width*2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                    original_image.setRGB(x,y,pix);
                    ind++;
                }
            }

            // ADDED: convert to YUV
            double[][] givenYUV = convert_to_YUV(bytes, width, height);

            // ADDED: subsampling
            double[][] subSampled_YUV = subSample(givenYUV, this.Y_input, this.U_input, this.V_input, width, height);

            // ADDED: upsampling
            double[][] upSampled_YUV = upSample(subSampled_YUV, width, height);

            // ADDED: convert to RGB
            // have to put upsampled YUV
            // but for now, to test if convertToRGB works, i'm using givenYUV as an input
            int[][] backToRGB = convert_to_RGB(upSampled_YUV, width, height);

            // next processing
            // backToRGB -> /Sw /Sh -> new_width = width * sw -> processed_image
            // int[][] scaled_RGB = scale(backToRGB)



            // antialiasing -> take average of all 9 backToRGB ->  /Sw /Sh

            // ADDED: image creation
            //subSampledImageCreation
            ind = 0;
            for(int y = 0; y < height; y++)
            {
                for(int x = 0; x < width; x++)
                {
                    int pix = 0xff000000 | (backToRGB[0][ind] << 16) | (backToRGB[1][ind] << 8) | backToRGB[2][ind];
                    processed_image.setRGB(x,y,pix);
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
        original_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // ADDED
        // create a processed image
        processed_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        readImageRGB(width, height, args[0]);


        // Use label to display the image
        // show original image
        frame1 = new JFrame();
        GridBagLayout gLayout1 = new GridBagLayout();
        frame1.getContentPane().setLayout(gLayout1);

        lbIm1 = new JLabel(new ImageIcon(original_image));

        JLabel lbText1 = new JLabel("Original image");
        lbText1.setHorizontalAlignment(SwingConstants.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        frame1.getContentPane().add(lbText1, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        frame1.getContentPane().add(lbIm1, c);

        frame1.pack();
        frame1.setVisible(true);


        // show original image
        frame2 = new JFrame();
        GridBagLayout gLayout2 = new GridBagLayout();
        frame2.getContentPane().setLayout(gLayout2);

        lbIm2 = new JLabel(new ImageIcon(processed_image));
        JLabel lbText2 = new JLabel("Processed image after subsampling");
        lbText2.setHorizontalAlignment(SwingConstants.CENTER);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        frame2.getContentPane().add(lbText2, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        frame2.getContentPane().add(lbIm2, c);

        frame2.pack();
        frame2.setVisible(true);
    }

    private double[][] convert_to_YUV(byte[] rgb, int width,int height){
        double[][] yuv = new double[3][rgb.length/3];
        int index = 0;
        for (int p = 0; p < height; p++) {
            for (int q = 0; q < width; q++) {
                int r = Byte.toUnsignedInt(rgb[index]);
                int g = Byte.toUnsignedInt(rgb[index+height*width]);
                int b = Byte.toUnsignedInt(rgb[index+height*width*2]);

                double y = (0.299 * r + 0.587 * g + 0.114 * b);
                double u = (0.596 * r - 0.274 * g - 0.322 * b);
                double v = (0.211 * r - 0.523 * g + 0.312 * b);

                yuv[0][index] = y;
                yuv[1][index] = u;
                yuv[2][index] = v;
                index++;
            }
        }
        return yuv;
    }

    private int[][] convert_to_RGB(double[][] yuv, int width, int height){
        int[][] processed_rgb = new int[3][yuv[0].length];
        int ind = 0;
        for (int p = 0; p < height; p++) {
            for (int q = 0; q < width; q++) {
                double y = yuv[0][ind];
                double u = yuv[1][ind];
                double v = yuv[2][ind];

                int r = Math.round((float)(1.000 * y + 0.956 * u + 0.621 * v));
                int g = Math.round((float)(1.000 * y - 0.272 * u - 0.647 * v));
                int b = Math.round((float)(1.000 * y - 1.106 * u + 1.703 * v));

                // clip values into [0,255]
                processed_rgb[0][ind] = Math.max(r, 0);
                processed_rgb[1][ind] = Math.max(g, 0);
                processed_rgb[2][ind] = Math.max(b, 0);

                processed_rgb[0][ind] = Math.min(processed_rgb[0][ind], 255);
                processed_rgb[1][ind] = Math.min(processed_rgb[1][ind], 255);
                processed_rgb[2][ind] = Math.min(processed_rgb[2][ind], 255);

                ind++;
            }
        }
        return processed_rgb;
    }

    private double[][] subSample(double[][] givenYUV, int y_input, int u_input, int v_input, int width, int height){
        double[][] subSampled_YUV = new double[3][givenYUV[0].length];
        int index = 0;
        for(int p = 0; p < height; p++)
        {
            for(int q = 0; q < width; q++)
            {
                double[] Y_subSampled = subSampled_YUV[0];
                double[] U_subSampled = subSampled_YUV[1];
                double[] V_subSampled = subSampled_YUV[2];

                Y_subSampled[index] = q % y_input == 0 ? givenYUV[0][index] : Integer.MIN_VALUE;
                U_subSampled[index] = q % u_input == 0 ? givenYUV[1][index] : Integer.MIN_VALUE;
                V_subSampled[index] = q % v_input == 0 ? givenYUV[2][index] : Integer.MIN_VALUE;

                index++;
            }
        }
        return subSampled_YUV;
    }

    private double[][] upSample(double[][] subSampled_YUV, int width, int height){
        double[][] upSampled_YUV = new double[3][subSampled_YUV[0].length];
        int index = 0;
        for (int p = 0; p < height; p++) {
            for (int q = 0; q < width; q++) {
                double[] Y_upSampled = upSampled_YUV[0];
                double[] U_upSampled = upSampled_YUV[1];
                double[] V_upSampled = upSampled_YUV[2];

                // if no value, get average.
                if (subSampled_YUV[0][index] == Integer.MIN_VALUE) {
                    Y_upSampled[index] = get_Average(subSampled_YUV[0], index);
                } else {
                    Y_upSampled[index] = subSampled_YUV[0][index];
                }

                if (subSampled_YUV[1][index] == Integer.MIN_VALUE) {
                    U_upSampled[index] = get_Average(subSampled_YUV[1], index);
                } else {
                    U_upSampled[index] = subSampled_YUV[1][index];
                }

                if (subSampled_YUV[2][index] == Integer.MIN_VALUE) {
                    V_upSampled[index] = get_Average(subSampled_YUV[2], index);
                } else {
                    V_upSampled[index] = subSampled_YUV[2][index];
                }
                index++;
            }
        }
        return upSampled_YUV;
    }

    private double get_Average(double[] arr, int index){
        double previous = Integer.MIN_VALUE;
        double next = Integer.MIN_VALUE;

        for (int i = index; i >= 0; --i) {
            if (arr[i] != Integer.MIN_VALUE) {
                previous = arr[i];
                break;
            }
        }
        for (int i = index; i < arr.length; ++i) {
            if (arr[i] != Integer.MIN_VALUE) {
                next = arr[i];
                break;
            }
        }

        // upsampling for values that are empty
        // if prev and next both exists
        if (previous != Integer.MIN_VALUE && next != Integer.MIN_VALUE) {
            return (previous + next)/2;
        }
        // if only prev exists
        else if (previous != Integer.MIN_VALUE) {
            return previous;
        }
        // if only next exists
        else {
            return next;
        }
    }


    public static void main(String[] args) {
        ImageDisplay ren = new ImageDisplay();
        ren.showIms(args);
    }

}