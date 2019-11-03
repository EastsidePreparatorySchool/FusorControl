package com.eastsideprep.fusorcontrolserver;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamStreamer;

public class CamStreamer {

    WebcamStreamer ws;

    CamStreamer() {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            return; 
        }
        webcam.open();
        System.out.println("Webcam started");

        ws = new WebcamStreamer(4567, webcam, 10, true);
        if (ws == null) {
            return;
        }
        System.out.println("WebStreamer started");
    }

}
