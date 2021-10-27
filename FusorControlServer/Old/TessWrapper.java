package com.eastsideprep.cameras;

import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.PointerByReference;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.File;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITessAPI.TessOcrEngineMode;
import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.util.ImageIOHelper;

public class TessWrapper {

    public class Result {

        public String text;
        public int confidence;

        public Result(String text, int confidence) {
            this.text = text;
            this.confidence = confidence;
        }
    }

    TessAPI api;
    TessAPI.TessBaseAPI handle;
    static public int bytespp = 3;

    public TessWrapper() {
        api = TessAPI.INSTANCE;
        handle = api.TessBaseAPICreate();
    }

    public void init() {
        StringArray sarray = new StringArray(new String[0]);
        PointerByReference configs = new PointerByReference();
        configs.setPointer(sarray);
        int result = api.TessBaseAPIInit1(handle, "tessdata", "eng", TessOcrEngineMode.OEM_DEFAULT, configs, 0);
//        int result = api.TessBaseAPIInit1(handle, "tessdata", "letsgodigital", TessOcrEngineMode.OEM_DEFAULT, configs, 0);
        System.out.println("TessAPI init:" + result);
        api.TessBaseAPISetPageSegMode(handle, TessAPI.TessPageSegMode.PSM_SINGLE_LINE);
        //api.TessBaseAPISetRectangle(handle, int left, int top, int width, int height);
    }

    public Result extract(BufferedImage image) {
        synchronized (this) {
            ByteBuffer buf = ImageIOHelper.convertImageData(image);
            buf = prepImageBuffer(buf, image.getWidth(), image.getHeight());

            api.TessBaseAPISetImage(handle, buf, image.getWidth(), image.getHeight(), 3, image.getWidth() * 3);
            api.TessBaseAPISetSourceResolution(handle, 70);
            api.TessBaseAPISetRectangle(handle, image.getWidth()*5/100, image.getHeight()*5/100, image.getWidth()*65/100, image.getHeight()*40/100);
            Pointer text = api.TessBaseAPIGetUTF8Text(handle);
            // height 8% - 40%, width 15% - 70%

            int confidence = api.TessBaseAPIMeanTextConf(handle);
            String strText = text.getString(0);
            Result result = new Result(strText, confidence);

            api.TessDeleteText(text);
            return result;
        }
    }

    public void shutdown() {
        api.TessBaseAPIDelete(handle);
        handle = null;
        api = null;
    }

    public static ByteBuffer prepImageBuffer(ByteBuffer buf, int width, int height) {

//        // Check if the ColorSpace is RGB and the TransferType is BYTE. 
//        // Otherwise this fast method does not work as expected
//        ColorModel cm = image.getColorModel();
//        if (cm.getColorSpace().getType() == ColorSpace.TYPE_RGB && img.getRaster().getTransferType() == DataBuffer.TYPE_BYTE) {
        int bwidth = buf.limit() / height;
        int bytespp = bwidth / width;
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
                byte valNew = (byte) ((val > cutoff || (valHigher > cutoff && valLower > cutoff)) ? 0 : (byte) 255);
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
        TessWrapper tw = new TessWrapper();
        tw.init();
        BufferedImage image = null;
        // load file for testing
        File inputFile = new File("test.png");
        try {
            image = ImageIO.read(inputFile);
        } catch (Throwable ex) {
        }

        Result result = tw.extract(image);
        System.out.println("Result: '" + result.text + "', confidence: " + result.confidence);

        image = prepImage(image);

        File outputfile = new File("test_bw.png");
        try {
            ImageIO.write(image, "png", outputfile);
        } catch (Throwable ex) {
            System.out.println("file neg ex");
        }
        tw.shutdown();
    }
}
