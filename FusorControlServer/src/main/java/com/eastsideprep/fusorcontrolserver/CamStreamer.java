package com.eastsideprep.fusorcontrolserver;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamStreamer;
import com.github.sarxos.webcam.WebcamResolution;
import java.util.List;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import java.util.ArrayList;

public class CamStreamer {

    private WebcamStreamer ws;
    public int numCameras = 0;
    private ArrayList<Webcam> cams = new ArrayList<>();
    private ArrayList<Thread> camThreads = new ArrayList<>();

    CamStreamer() {
        cams.clear();
        List<Webcam> camList = Webcam.getWebcams();
        int count = 0;
        int def = 0;

        for (Webcam cam : camList) {
            if (cam == null) {
                System.out.println("webcam error");
                return;
            }

            // keep track of it, for recording
            this.cams.add(cam);

            if (cam.getName().startsWith("EasyCamera")) {
                def = 1;
                continue;
            }

            Dimension size = WebcamResolution.QVGA.getSize();
            cam.setViewSize(size);
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

        numCameras = cams.size() - def;
    }

    void record(Webcam webcam, String fileName) {
        File file = new File(fileName);
        System.out.println("Recording cam " + webcam.getName() + " in file " + file.getAbsolutePath());

        Dimension size = WebcamResolution.QVGA.getSize();
        final IMediaWriter writer;
        try {
            writer = ToolFactory.makeWriter(file.getName());
        } catch (Throwable e) {
            System.out.println(e);
            return;
        }
        writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, size.width, size.height);

        long start = System.currentTimeMillis();

        Thread t = new Thread(() -> {
            boolean started = true;
            while (!Thread.interrupted()) {
                BufferedImage image = ConverterFactory.convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
                IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);
                IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis() - start) * 1000);
                frame.setKeyFrame(started);
                started = false;
                frame.setQuality(0);
                writer.encodeVideo(0, frame);
                try {
                    // 10 FPS
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }
            writer.close();
        });
        t.start();
        this.camThreads.add(t);
    }

    void startRecording(String filePrefix) {
        int i = 0;
        for (Webcam cam : this.cams) {
            record(cam, filePrefix + i);
            i++;
        }
    }

    void stopRecording() {
        for (Thread t : camThreads) {
            try {
                t.interrupt();
                t.join();
            } catch (InterruptedException ex) {
            }
        }
        camThreads.clear();
    }
}
