/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamStreamer;

/**
 *
 * @author gmein
 */
public class CamStreamer {
    
    WebcamStreamer ws;

    CamStreamer() {
        Webcam webcam = Webcam.getDefault();
        webcam.open();
        System.out.println("Webcam started");
        
        ws = new WebcamStreamer(4567, webcam, 10, true);
    }

}
