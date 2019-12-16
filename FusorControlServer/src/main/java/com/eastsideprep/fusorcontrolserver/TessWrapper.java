/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.PointerByReference;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITessAPI.TessOcrEngineMode;
import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.util.ImageIOHelper;

/**
 *
 * @author gmein
 */
public class TessWrapper {

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
        int result = api.TessBaseAPIInit1(handle, "tessdata", "letsgodigital", TessOcrEngineMode.OEM_DEFAULT, configs, 0);
        System.out.println("TessAPI init:" + result);
    }

    public String extract(BufferedImage image) {
        String result = "";

        // gray scale
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp op = new ColorConvertOp(colorSpace, null);
        BufferedImage dstImage = op.filter(image, null);

        //  enhance contrast
        RescaleOp op2 = new RescaleOp(-3.0f, 255.0f, null);
        image = op2.filter(dstImage, null);

        // make binary
        BufferedImage bw = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics gfx = bw.getGraphics();
        gfx.drawImage(image, 0, 0, null);
        
//        // save for testing
//        File outputfile = new File("test.jpg");
//        try {
//            ImageIO.write(bw, "jpg", outputfile);
//        } catch (IOException ex) {
//        }

        ByteBuffer buf = ImageIOHelper.convertImageData(image);
        api.TessBaseAPISetImage(handle, buf, image.getWidth(), image.getHeight(), 1, image.getWidth());
        api.TessBaseAPISetPageSegMode(handle, TessAPI.TessPageSegMode.PSM_SINGLE_CHAR);
        Pointer text = api.TessBaseAPIGetUTF8Text(handle);
        int confidence = api.TessBaseAPIMeanTextConf(handle);
        System.out.println("Confidence: " + confidence);
        result = text.getString(0);
        api.TessDeleteText(text);
        return result;
    }

    public void shutdown() {
        api.TessBaseAPIDelete(handle);
        handle = null;
        api = null;
    }
}
