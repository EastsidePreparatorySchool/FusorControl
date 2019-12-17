package com.eastsideprep.fusorcontrolserver;

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

/**
 *
 * @author gmein
 */
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

    public TessWrapper() {
        api = TessAPI.INSTANCE;
        handle = api.TessBaseAPICreate();
    }

    public void init() {
        StringArray sarray = new StringArray(new String[0]);
        PointerByReference configs = new PointerByReference();
        configs.setPointer(sarray);
//        int result = api.TessBaseAPIInit1(handle, "tessdata", "eng", TessOcrEngineMode.OEM_DEFAULT, configs, 0);
        int result = api.TessBaseAPIInit1(handle, "tessdata", "letsgodigital", TessOcrEngineMode.OEM_DEFAULT, configs, 0);
        System.out.println("TessAPI init:" + result);
        api.TessBaseAPISetPageSegMode(handle, TessAPI.TessPageSegMode.PSM_SINGLE_LINE);
        //api.TessBaseAPISetRectangle(handle, int left, int top, int width, int height);
    }

    public Result extract(BufferedImage image) {
        ByteBuffer buf = ImageIOHelper.convertImageData(image);
        buf = prepImageBuffer(buf, image.getWidth(), image.getHeight());

        api.TessBaseAPISetImage(handle, buf, image.getWidth(), image.getHeight(), 3, image.getWidth() * 3);
        api.TessBaseAPISetSourceResolution(handle, 70);
        Pointer text = api.TessBaseAPIGetUTF8Text(handle);

        int confidence = api.TessBaseAPIMeanTextConf(handle);
        String strText = text.getString(0);
        Result result = new Result(strText, confidence);

        api.TessDeleteText(text);
        return result;
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

        int cutoff = 175;

        ByteBuffer bufNew = ByteBuffer.allocateDirect(buf.limit());
        byte[] bytesDest = new byte[bufNew.limit()];
        for (int pixel = 0; pixel < width * height; pixel++) {
            int val;
            val = Byte.toUnsignedInt(bytes[bytespp * pixel + bytespp - 1]);
            byte valNew = (byte) (val > cutoff ? 0 : (byte) 255);
            bytesDest[3 * pixel + 0] = valNew;
            bytesDest[3 * pixel + 1] = valNew;
            bytesDest[3 * pixel + 2] = valNew;
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
