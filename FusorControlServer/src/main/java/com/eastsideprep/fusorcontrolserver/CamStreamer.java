package com.eastsideprep.fusorcontrolserver;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryService;
import com.github.sarxos.webcam.WebcamStreamer;
import com.github.sarxos.webcam.WebcamResolution;
import java.util.List;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IRational;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CamStreamer {

    private WebcamStreamer ws;
    public int numCameras = 0;
    private ArrayList<Webcam> cams = new ArrayList<>();
    private ArrayList<Thread> camThreads = new ArrayList<>();
    private volatile boolean stopRecording = false;

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

            Dimension size = WebcamResolution.VGA.getSize();
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

        WebcamDiscoveryService discovery = Webcam.getDiscoveryService();
        discovery.stop();
    }

    void record(Webcam webcam, String fileName) {
        File file = new File(fileName + ".mp4");
        System.out.println("Recording cam " + webcam.getName() + " in file " + file.getAbsolutePath());

        // this is xuggler's weird way of specifying 5 frames per sec
        IRational FRAME_RATE = IRational.make(5, 1); 
        
        Dimension size = WebcamResolution.VGA.getSize();
        final IMediaWriter writer;
        try {
            writer = ToolFactory.makeWriter(file.getAbsolutePath());
            writer.addVideoStream(0, 0, FRAME_RATE, size.width, size.height);
        } catch (Throwable e) {
            System.out.println(e);
            System.out.println(e.getStackTrace());
            return;
        }

        long start = System.currentTimeMillis();

        Thread t = new Thread(() -> {
            boolean started = true;
            while (!stopRecording) {
                try {
                    try {
                        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_3BYTE_BGR);
                        image.getGraphics().drawImage(webcam.getImage(), 0, 0, null);
                        writer.encodeVideo(0, image, System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
                    } catch (Throwable ex) {
                        System.out.println(ex);
                        System.out.println(Arrays.toString(ex.getStackTrace()));
                        Thread.currentThread().interrupt();
                    }
                    Thread.sleep((long) (1000 / FRAME_RATE.getDouble()));
                } catch (InterruptedException ex) {
                }
            }
            // spend a final half-second giving the encoder time to catch up ...
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }
            //writer.flush();
            writer.close();
        });
        t.start();
        this.camThreads.add(t);
    }

    void startRecording(String filePrefix) {
        int i = 0;
        stopRecording = false;
        for (Webcam cam : this.cams) {
            record(cam, filePrefix + i);
            i++;
        }
    }

    void stopRecording() {
        System.out.print("Stopping camera recording ...");
        stopRecording = true;
        for (Thread t : camThreads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
            }
        }
        camThreads.clear();
        System.out.println(" complete.");
    }
}
