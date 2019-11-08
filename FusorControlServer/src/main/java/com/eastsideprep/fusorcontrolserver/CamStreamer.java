package com.eastsideprep.fusorcontrolserver;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamStreamer;
import java.util.List;

public class CamStreamer {

    WebcamStreamer ws;

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
            System.out.println("Webcam "+count+", \""+cam.getName()+"\" opened");

            ws = new WebcamStreamer(4567+count, cam, 10, true);
            if (ws == null) {
                System.out.println("webstreamer error");
                return;
            }
            System.out.println("  WebStreamer started on port "+(4567+count));
            count++;
        }
    }

}
