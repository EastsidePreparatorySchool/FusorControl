package com.eastsideprep.fusorcontrolserver;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamStreamer;
import java.util.List;

//import java.awt.Dimension;
//import java.awt.image.BufferedImage;
//import java.io.File;

//import com.xuggle.mediatool.IMediaWriter;
//import com.xuggle.mediatool.ToolFactory;
//import com.xuggle.xuggler.ICodec;
//import com.xuggle.xuggler.IPixelFormat;
//import com.xuggle.xuggler.IVideoPicture;
//import com.xuggle.xuggler.video.ConverterFactory;
//import com.xuggle.xuggler.video.IConverter;

public class CamStreamer {

    private WebcamStreamer ws;
    public int numCameras = 0;

    CamStreamer() {
        // old code Webcam webcam = Webcam.getDefault();
        List<Webcam> cams = Webcam.getWebcams();
        int count = 0;
        

        for (Webcam cam : cams) {
            if (cam == null) {
                System.out.println("webcam error");
                return;
            }
            cam.open();
            System.out.println("Webcam " + count + ", \"" + cam.getName() + "\" opened");

            ws = new WebcamStreamer(4567 + count, cam, 10, true);
            if (ws == null) {
                System.out.println("webstreamer error");
                return;
            }
            System.out.println("  WebStreamer started on port " + (4567 + count));
            count++;
        }
        
        numCameras = cams.size();
    }

//    void encode() {
//        File file = new File("output.ts");
//
//        IMediaWriter writer = ToolFactory.makeWriter(file.getName());
//        Dimension size = WebcamResolution.QVGA.getSize();
//
//        writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, size.width, size.height);
//
//        Webcam webcam = Webcam.getDefault();
//        webcam.setViewSize(size);
//        webcam.open(true);
//
//        long start = System.currentTimeMillis();
//
//        for (int i = 0; i < 50; i++) {
//
//            System.out.println("Capture frame " + i);
//
//            BufferedImage image = ConverterFactory.convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
//            IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);
//
//            IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis() - start) * 1000);
//            frame.setKeyFrame(i == 0);
//            frame.setQuality(0);
//
//            writer.encodeVideo(0, frame);
//
//            // 10 FPS
//            Thread.sleep(100);
//        }
//
//        writer.close();
//
//        System.out.println("Video recorded in file: " + file.getAbsolutePath());
//    }
}
