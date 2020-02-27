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
import net.sourceforge.tess4j.Tesseract;

public class CamStreamer {

    private WebcamStreamer ws;
    public int numCameras = 0;
    private ArrayList<Webcam> cams = new ArrayList<>();
    private ArrayList<Thread> camThreads = new ArrayList<>();
    private volatile boolean stopRecording = false;
    private Tesseract tesseract;
    private TessWrapper tw;
    private DeviceManager dm;

    public CamStreamer(DeviceManager dm) {
        cams.clear();
        List<Webcam> camList = Webcam.getWebcams();
        int count = 0;
        int def = 0;
        this.dm = dm;

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
            if (cam.getName().startsWith("Integrated Camera 0")) { // built-in,  camera on x1-gmein laptop
                def = 1;
                continue;
            }

            // keep track of it, for recording
            this.cams.add(cam);

            SerialDevice sd = new NullSerialDevice("OCR");
            dm.register(sd);

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

        numCameras = cams.size();

        WebcamDiscoveryService discovery = Webcam.getDiscoveryService();
        discovery.stop();

        tesseract = new Tesseract();
        tesseract.setDatapath("tessdata");
        tesseract.setLanguage("letsgodigital");

        tw = new TessWrapper();
        tw.init();
    }

    void record(Webcam webcam, String fileName, long baseTime) {
        File file = new File(fileName + ".mp4");
        System.out.println("Recording cam " + webcam.getName() + " in file " + file.getAbsolutePath());

        // this is xuggler's weird way of specifying frames per sec
        IRational FRAME_RATE = IRational.make(20, 1);

        // get the writer prepared
        Dimension size = WebcamResolution.QVGA.getSize();
        final IMediaWriter writer;
        try {
            writer = ToolFactory.makeWriter(file.getAbsolutePath());
            writer.addVideoStream(0, 0, FRAME_RATE, size.width, size.height);
        } catch (Throwable e) {
            System.out.println(e);
            System.out.println(e.getStackTrace());
            return;
        }

        // access our fake serial device in case of OCR
        SerialDevice sd = dm.get("OCR");

        // this thread will do its thing until the logger sets the flag
        Thread t = new Thread(() -> {
            long start = System.currentTimeMillis();
            while (!stopRecording) {
                try {
                    try {
                        //get timestamp, get image into bufferImage
                        long millis = System.currentTimeMillis();
                        double secs = ((millis - baseTime) / 100) / 10.0;
                        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_3BYTE_BGR);
                        Graphics gfx = image.getGraphics();
                        //Graphics2D gfx = image.createGraphics();
                        gfx.drawImage(webcam.getImage(), 0, 0, null);
//                        gfx.setComposite(AlphaComposite.SrcOver.derive(0.1f));
//                        for (int i = 0; i < 20; i++) {
//                            gfx.drawImage(webcam.getImage(), 0, i, null);
//                        }
//                        gfx.dispose();

                        extractNumber(image, millis, sd);

                        if (FusorControlServer.config.saveProcessedVideo) {
                            image = TessWrapper.prepImage(image);
                        }
                        image.getGraphics().drawString(Double.toString(secs), 10, 20);
                        image.getGraphics().drawString(file.getName(), 10, size.height - 10);
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

    void extractNumber(BufferedImage image, long millis, SerialDevice sd) {
        if (true)return;

        TessWrapper.Result result = tw.extract(image);
        if (result.confidence < 80) {
            return;
        }
        String s = result.text;

        if (s != null && s.length() > 0) {
            String log = "{";
            //System.out.println("Recognized: " + s);
            s = s.replaceAll("[^\\x00-\\x7F]", "");
            // erases all the ASCII control characters
            s = s.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
            // removes non-printable characters from Unicode
            s = s.replaceAll("\\p{C}", "");
            s = s.replaceAll("\\. ", ".");
            s = s.trim();

            //System.out.println("OCR: " + s);
            // check if we have an actual number of the right format
            if (s.startsWith(".")) {
                if (s.length() != 4) {
                    return;
                }
                if (!s.matches("\\.\\d\\d\\d")) {
                    return;
                }
                s = "0" + s;
            } else {
                if (s.length() != 3) {
                    return;
                }
                if (!s.matches("\\d\\d\\d")) {
                    return;
                }
            }

            log += "\"text\":{\"value\":\"" + s + "\",\"vartime\":" + millis + "}";
            log += ",\"confidence\":{\"value\":" + result.confidence + ",\"vartime\":" + millis + "}";

            double d;
            try {
                d = Double.parseDouble(s);
                log += ",\"double\":{\"value\":" + d + ",\"vartime\":" + millis + "}";
            } catch (Exception e) {
                //return;
            }
            log += ",\"devicetime\":" + millis;

            log += "}";
            dm.recordStatusForDevice(sd, millis, log);
        }
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
