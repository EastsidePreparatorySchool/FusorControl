/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.cameras;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.util.ImageIOHelper;

/**
 *
 * @author tpylypenko
 */
public class ImageInterpreter {
    //static BufferedImage blockTest; //TODO remove this
    
    private static class Digit {
        BufferedImage image;
        int startRow;
        int startCol;
        int endRow;
        int endCol;
        int sW;
        double confidence;
        public Digit(BufferedImage bi) {
            this.image = bi;
            this.sW = 40; //TODO make this good
        }
        public double segmentConfidence(int sRow, int sCol, int eRow, int eCol) {
            double sum = 0;
            double total = (eRow - sRow + 1) * (eCol - sCol + 1);
            
            for (int row = sRow; row <= eRow; row++) {
                for (int col = sCol; col <= eCol; col++) {
                    Color color = new Color(image.getRGB(col, row)); //true when the pixel is black
                    boolean paulMacLean = color.getRed() == 0;
                    if(paulMacLean) {
                        sum++;
                    }
                    else {
                        sum--;
                    }
                }
            }
            
            /*
            int red = (new Color(255, 0, 0)).getRGB();
            for(int row = sRow; row < eRow; row++) {
                blockTest.setRGB(sCol, row, red);
                blockTest.setRGB(eCol, row, red);
            }
            for(int col = sCol; col < eCol; col++) {
                blockTest.setRGB(sRow, col, red);
                blockTest.setRGB(eRow, col, red);
            }
            /**/ //TODO remove this
            
            return sum/total;
        }
        public Result guessDigit() {
            int height = endRow - startRow + 1;
            int width = endCol - startCol + 1;
            double ratio = ((double) height)/width;
            //System.out.println("height: " + height + "\t width:" + width);
            if(ratio < 1) {
                return new Result(".", 1);
            }
            if(ratio > 2.5) {
                return new Result("1", 1);
            }
            /*
             0000
            1    2
            1    2
             3333
            4    5
            4    5
             6666
            */
            double[] sC = new double[7]; //confidence between zero and one that a given segment exists
            sC[0] = this.segmentConfidence(0, sW, sW, endCol - sW);
            sC[1] = this.segmentConfidence(sW, sW/2, startRow + (endRow-startRow-sW)/2, sW*3/2);
            sC[2] = this.segmentConfidence(sW, endCol - sW, startRow + (endRow-startRow-sW)/2, endCol);
            sC[3] = this.segmentConfidence(startRow + (endRow-startRow-sW)/2, sW, startRow + (endRow-startRow+sW)/2, endCol - sW);
            sC[4] = this.segmentConfidence(startRow + (endRow-startRow+sW)/2, 0, endRow - sW, sW);
            sC[5] = this.segmentConfidence(startRow + (endRow-startRow+sW)/2, endCol - (sW * 3/2), endRow - sW, endCol - sW/2);
            sC[6] = this.segmentConfidence(endRow - sW, sW, endRow, endCol - sW);
            
            double[] numberConfidence = new double[10];
            numberConfidence[0] = sC[0] + sC[1] + sC[2] - sC[3] + sC[4] + sC[5] + sC[6];
            numberConfidence[1] = 0;
            numberConfidence[2] = sC[0] - sC[1] + sC[2] + sC[3] + sC[4] - sC[5] + sC[6];
            numberConfidence[3] = sC[0] - sC[1] + sC[2] + sC[3] - sC[4] + sC[5] + sC[6];
            numberConfidence[4] = -sC[0] + sC[1] + sC[2] + sC[3] - sC[4] + sC[5] - sC[6];
            numberConfidence[5] = sC[0] + sC[1] - sC[2] + sC[3] - sC[4] + sC[5] + sC[6];
            numberConfidence[6] = sC[0] + sC[1] - sC[2] + sC[3] + sC[4] + sC[5] + sC[6];
            numberConfidence[7] = sC[0] - sC[1] + sC[2] - sC[3] - sC[4] + sC[5] - sC[6];
            numberConfidence[8] = sC[0] + sC[1] + sC[2] + sC[3] + sC[4] + sC[5] + sC[6];
            numberConfidence[9] = sC[0] + sC[1] + sC[2] + sC[3] + sC[4] - sC[5] + sC[6];
            for(int i = 0; i < 10; i++) {
                numberConfidence[i] /= 6;
            }
            
            int digit = 0;
            double bestConfidence = 0;
            for(int i = 0; i < 10; i++) {
                if(numberConfidence[i] > bestConfidence) {
                    digit = i;
                    bestConfidence = numberConfidence[i];
                }
            }
            return new Result("" + digit, bestConfidence);
        }
    }
    public static class Result {

        public String text;
        public double confidence;

        public Result(String text, double confidence) {
            this.text = text;
            this.confidence = confidence;
        }
    }
    
    public static void main(String[] args) {
        test();
    }
    
    public static Result extract(BufferedImage image) {
        int rows = image.getHeight();
        int cols = image.getWidth();
        
        List<Digit> digits = new ArrayList<Digit>(); 
        Digit currentDigit = null;
        boolean colHasText = false;
        boolean unmodified = false;
        //int startRow, startCol, endRow, endCol;
        for(int col = 0; col < cols; col++) {
            if(!colHasText && !unmodified) {
                if(currentDigit != null) {
                    digits.add(currentDigit);
                }
                currentDigit = new Digit(image);
                unmodified = true;
            }
            colHasText = false;
            for(int row = 0; row < rows; row++) {
                Color color = new Color(image.getRGB(col, row)); //true when the pixel is black
                //System.out.println(color.getRed());
                boolean paulMacLean = color.getRed() == 0;
                if(paulMacLean) {
                    colHasText = true;
                    if(unmodified) {
                        currentDigit.startRow = row;
                        currentDigit.startCol = col;
                        currentDigit.endRow = row;
                        currentDigit.endCol = col;
                        unmodified = false;
                    }
                    else {
                        if(row < currentDigit.startRow) {
                            currentDigit.startRow = row;
                        }
                        if(row > currentDigit.endRow) {
                            currentDigit.endRow = row;
                        }
                        if(col < currentDigit.startCol) {
                            currentDigit.startCol = col;
                        }
                        if(col > currentDigit.endCol) {
                            currentDigit.endCol = col;
                        }
                    }
                }
            }
        }
        
        //System.out.println("the number has " + digits.size() + " digits"); //the "." counts as a digit
        String output = "";
        double confidence = 0;
        for(Digit d : digits) {
            Result r = d.guessDigit();
            output += r.text;
            confidence += r.confidence;
        }
        confidence /= digits.size();
        
        return new Result(output, confidence);
    }
    
    public static ByteBuffer prepImageBuffer(ByteBuffer buf, int width, int height) {

//        // Check if the ColorSpace is RGB and the TransferType is BYTE. 
//        // Otherwise this fast method does not work as expected
//        ColorModel cm = image.getColorModel();
//        if (cm.getColorSpace().getType() == ColorSpace.TYPE_RGB && img.getRaster().getTransferType() == DataBuffer.TYPE_BYTE) {
        int bwidth = buf.limit() / height;
        int bytespp = bwidth / width;  //bytes per pixel
        //System.out.println("line is " + width + " pixels, " + bwidth + " bytes");

        byte[] bytes = new byte[buf.limit()];
        buf.get(bytes);
//        for (int i = 0; i < 10 * bytespp; i++) {
//            for (int j = 0; j < bytespp; j++) {
//                System.out.print(" x" + Integer.toHexString(Byte.toUnsignedInt(bytes[bytespp * i + j])));
//            }
//            System.out.println("");
//        }

        int cutoff = 170;
        cutoff *= cutoff;

        ByteBuffer bufNew = ByteBuffer.allocateDirect(buf.limit());
        byte[] bytesDest = new byte[bufNew.limit()];
        for (int y = 0; y < height; y++) {
            int lineStart = y * width * bytespp;
            int lineStartDest = y * width * 3;
            for (int pixel = 0; pixel < width; pixel++) {
                int val;    
                int valHigher = 0;
                int valLower = 0;
                int gap = (int)(10);

                val = Byte.toUnsignedInt(bytes[lineStart + (bytespp * pixel) + bytespp - 1]);
                val *= val;
                if (y > gap) {
                    valHigher = Byte.toUnsignedInt(bytes[(y - gap) * width * bytespp + (bytespp * pixel) + bytespp - 1]);
                    valHigher *= valHigher;
                }
                if (y < height - gap - 1) {
                    valLower = Byte.toUnsignedInt(bytes[(y + gap) * width * bytespp + (bytespp * pixel) + bytespp - 1]);
                    valLower *= valLower;
                }
                byte valNew = (byte) ((val > cutoff /*|| (valHigher > cutoff && valLower > cutoff)*/) ? 0 : (byte) 255);
                bytesDest[lineStartDest + (3 * pixel) + 0] = valNew;
                bytesDest[lineStartDest + (3 * pixel) + 1] = valNew;
                bytesDest[lineStartDest + (3 * pixel) + 2] = valNew;
            }
        }
        bufNew.put(bytesDest);
        bufNew.rewind();

        return bufNew;
    }
    
    public static BufferedImage prepImage(BufferedImage image) {
        ByteBuffer buf = ImageIOHelper.convertImageData(image);
        buf = prepImageBuffer(buf, image.getWidth(), image.getHeight());

        byte[] bytes = new byte[image.getWidth() * image.getHeight() * 3];
        buf.get(bytes);
        BufferedImage imageDest = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        imageDest.setData(Raster.createRaster(imageDest.getSampleModel(), new DataBufferByte(bytes, buf.limit()), new Point(0, 0)));
        return imageDest;
    }
    
    public static void test() {
        BufferedImage image = null;
        // load file for testing
        File inputFile = new File("C:\\Users\\tpylypenko\\Documents\\GitHub\\FusorControl\\FusorControlServer\\src\\main\\java\\com\\eastsideprep\\cameras\\pirani_test.png");
        try {
            image = ImageIO.read(inputFile);
        } catch (Throwable ex) {
            System.out.println(ex);
        }
        
        image = prepImage(image);
        //blockTest = image.getSubimage(0, 0, image.getWidth(), image.getHeight());

        Result result = extract(image);
        System.out.println("Result: '" + result.text + "', confidence: " + result.confidence);

        File outputfile = new File("pirani_test_bw.png");
        try {
            ImageIO.write(image, "png", outputfile);
        } catch (Throwable ex) {
            System.out.println("file neg ex");
        }
    }
}
