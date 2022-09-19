
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

    int scaled_width = width;
    int scaled_height = height;

    int Y_input;
    int U_input;
    int V_input;
    double Sw_input;
    double Sh_input;

    int A_input;

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

            // convert to YUV
            double[][] givenYUV = convert_to_YUV(bytes, width, height);

            // subsampling
            double[][] subSampled_YUV = subSample(givenYUV, this.Y_input, this.U_input, this.V_input, width, height);

            // upsampling
            double[][] upSampled_YUV = upSample(subSampled_YUV, width, height);

            // convert back to RGB
            int[][] coverted_RGB = convert_to_RGB(upSampled_YUV, width, height);

            // scaling
            int[][] scaled_RGB = scale_RGB(coverted_RGB);

            // if A_input is 1, do antialiasing
            if (A_input == 1) {
                scaled_RGB = antiAlias_RGB(scaled_RGB);
            }

            //subSampledImageCreation
            ind = 0;
            for(int y = 0; y < scaled_height; y++)
            {
                for(int x = 0; x < scaled_width; x++)
                {
                    int pix = 0xff000000 | (scaled_RGB[0][ind] << 16) | (scaled_RGB[1][ind] << 8) | scaled_RGB[2][ind];
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

        // Read parameters from command line
        this.Y_input = Integer.parseInt(args[1]);
        this.U_input = Integer.parseInt(args[2]);
        this.V_input = Integer.parseInt(args[3]);
        this.Sw_input = Double.parseDouble(args[4]);
        this.Sh_input = Double.parseDouble(args[5]);
        this.A_input = Integer.parseInt(args[6]);

        // Read in the specified image
        // create an original image
        original_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // create a processed image
        this.scaled_width = Math.round((float)(width * this.Sw_input));
        this.scaled_height = Math.round((float)(height * this.Sh_input));
        processed_image = new BufferedImage(scaled_width, scaled_height, BufferedImage.TYPE_INT_RGB);

        // call readImageRGB
        readImageRGB(width, height, args[0]);

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

        // show processed image in a different window
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
        int index = 0;
        for (int p = 0; p < height; p++) {
            for (int q = 0; q < width; q++) {
                double y = yuv[0][index];
                double u = yuv[1][index];
                double v = yuv[2][index];

                int r = Math.round((float)(1.000 * y + 0.956 * u + 0.621 * v));
                int g = Math.round((float)(1.000 * y - 0.272 * u - 0.647 * v));
                int b = Math.round((float)(1.000 * y - 1.106 * u + 1.703 * v));

                // clip values into [0,255]
                processed_rgb[0][index] = Math.max(r, 0);
                processed_rgb[1][index] = Math.max(g, 0);
                processed_rgb[2][index] = Math.max(b, 0);

                processed_rgb[0][index] = Math.min(processed_rgb[0][index], 255);
                processed_rgb[1][index] = Math.min(processed_rgb[1][index], 255);
                processed_rgb[2][index] = Math.min(processed_rgb[2][index], 255);

                index++;
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

                if (q % y_input == 0) {
                    Y_subSampled[index] = givenYUV[0][index];
                } else {
                    Y_subSampled[index] = Integer.MIN_VALUE;
                }

                if (q % u_input == 0) {
                    U_subSampled[index] = givenYUV[1][index];
                } else {
                    U_subSampled[index] = Integer.MIN_VALUE;
                }

                if (q % v_input == 0) {
                    V_subSampled[index] = givenYUV[2][index];
                } else {
                    V_subSampled[index] = Integer.MIN_VALUE;
                }
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


    private int[][] scale_RGB(int [][] given_RGB) {
        int [][] scaled_RGB = new int [3][scaled_width * scaled_height];
        // Sw_input and Sh_input are smaller than 1.0

        int Sw_reverse =  Math.round((float)(1/Sw_input));
        int Sh_reverse =  Math.round((float)(1/Sh_input));

        for (int p = 0; p < scaled_height; p++){
            for (int q = 0; q < scaled_width; q++){
                // map original_index to new_index
                int new_index = Math.round((float)p*scaled_width+q);
                int original_index = Math.round((float)(p*Sw_reverse)*width + q*Sh_reverse);

                scaled_RGB[0][new_index] = given_RGB[0][original_index];
                scaled_RGB[1][new_index] = given_RGB[1][original_index];
                scaled_RGB[2][new_index] = given_RGB[2][original_index];
            }
        }
        return scaled_RGB;
    }

    private int[][] antiAlias_RGB(int [][] scaled_RGB) {
        int [][] antiAliased_RGB = new int [3][scaled_width * scaled_height];

        int index = 0;
        for (int p = 0; p < scaled_height; p++){
            for (int q = 0; q < scaled_width; q++){
                antiAliased_RGB[0][index] = get_adjacent_values_average(scaled_RGB[0], p, q);
                antiAliased_RGB[1][index] = get_adjacent_values_average(scaled_RGB[1], p, q);
                antiAliased_RGB[2][index] = get_adjacent_values_average(scaled_RGB[2], p, q);
                index++;
            }
        }
        return antiAliased_RGB;
    }

    private int get_adjacent_values_average(int[] scaled_version, int p, int q) {
        // if p, q are in range and has adjacent 8 values
        if (p > 0 && p < scaled_height - 1 && q > 0 && q < scaled_width - 1) {
            int sum = 0;
            for (int j = p-1; j < p+2; j++) {
                for (int i = q-1; i < q+2; i++) {
                    sum += scaled_version[j*scaled_width+i];
                }
            }
            return sum / 9;
        }
        // if p, q are corner values (no 8 adjacent values)
        else {
            return scaled_version[p*scaled_width+q];
        }
    }

    public static void main(String[] args) {
        ImageDisplay ren = new ImageDisplay();
        ren.showIms(args);
    }
}