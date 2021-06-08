package com.eastsideprep.cameras;

import com.eastsideprep.fusorcontrolserver.FusorControlServer;
import com.eastsideprep.serialdevice.DeviceManager;
import com.eastsideprep.serialdevice.NullSerialDevice;
import com.eastsideprep.serialdevice.SerialDevice;
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
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CamStreamer {

    private WebcamStreamer ws;
    public int numCameras = 0;
    private ArrayList<Webcam> cams = new ArrayList<>();
    private ArrayList<Thread> camThreads = new ArrayList<>();
    private volatile boolean stopRecording = false;
    private DeviceManager dm;

    public CamStreamer(DeviceManager dm) {
        cams.clear();
        List<Webcam> camList = Webcam.getWebcams();
        int count = 0;
        int def = 0;
        this.dm = dm;

        numCameras = 0;

        if (FusorControlServer.config.noCameras) {
            return;
        }

        for (Webcam cam : camList) {
            if (cam == null) {
                System.out.println("webcam error");
                return;
            }

            if (cam.getName().startsWith("EasyCamera")) { // built-in, useless camera on FUSOR2 laptop
                def = 1;
                continue;
            }
            if (cam.getName().startsWith("Integrated Camera")) { // built-in,  camera on x1-gmein laptop
                def = 1;
                continue;
            }

            // keep track of it, for recording
            this.cams.add(cam);

            Dimension size = WebcamResolution.QVGA.getSize();
            if (cam.getName().startsWith("Logitech")) {
                size = WebcamResolution.VGA.getSize();
            }
            cam.setViewSize(size);
            cam.open();
            System.out.println("Webcam " + count + ", \"" + cam.getName() + "\" opened");

            if (!FusorControlServer.config.noCameraStreaming) {
                ws = new WebcamStreamer(4567 + count, cam, 10, true);
                if (ws == null) {
                    System.out.println("webstreamer error");
                    return;
                }
                System.out.println("  WebStreamer started on port " + (4567 + count));
            }
            count++;
        }

        numCameras = cams.size();

        WebcamDiscoveryService discovery = Webcam.getDiscoveryService();
        discovery.stop();
    }

    void record(Webcam webcam, String fileName, long baseTime) {
        File file = new File(fileName + ".mp4");
        System.out.println("Recording cam " + webcam.getName() + " in file " + file.getAbsolutePath());

        // this is xuggler's weird way of specifying frames per sec
        IRational FRAME_RATE = IRational.make(20, 1);

        // get the writer prepared
        Dimension size = WebcamResolution.QVGA.getSize();
        if (webcam.getName().startsWith("Logitech")) {
            size = WebcamResolution.VGA.getSize();
        }
        final Dimension fSize = size;
        final IMediaWriter writer;
        try {
            writer = ToolFactory.makeWriter(file.getAbsolutePath());
            writer.addVideoStream(0, 0, FRAME_RATE, size.width, size.height);
        } catch (Throwable e) {
            System.out.println(e);
            System.out.println(e.getStackTrace());
            return;
        }

        // this thread will do its thing until the logger sets the flag
        Thread t = new Thread(() -> {
            long start = System.currentTimeMillis();
            while (!stopRecording) {
                try {
                    try {
                        //get timestamp, get image into bufferImage
                        long millis = System.currentTimeMillis();
                        double secs = ((millis - baseTime) / 100) / 10.0;
                        BufferedImage image = new BufferedImage(fSize.width, fSize.height, BufferedImage.TYPE_3BYTE_BGR);
                        Graphics gfx = image.getGraphics();
                        gfx.drawImage(webcam.getImage(), 0, 0, null);
                        image.getGraphics().drawString(Double.toString(secs), 10, 20);
                        image.getGraphics().drawString(file.getName(), 10, fSize.height - 10);
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

    public void startRecording(String filePrefix, long baseTime) {
        int i = 0;
        stopRecording = false;
        for (Webcam cam : this.cams) {
            record(cam, filePrefix + i, baseTime);
            i++;
        }
    }

    public void stopRecording() {
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
