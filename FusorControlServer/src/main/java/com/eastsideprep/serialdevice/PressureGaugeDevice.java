/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.eastsideprep.serialdevice;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author gmein
 */
public class PressureGaugeDevice extends Arduino {

    float lastPressure;

    PressureGaugeDevice(Arduino sd) {
        super(sd);
        this.setStatus("{\"device\":\"PIRANI\"}");
        this.lastPressure = 0.0f;
    }

    public void setStatus(String s) {
        super.setStatus(s);
        if (s != null && s.length() > 25) {
//            System.out.println(s);
            try {
                JSONObject jo = new JSONObject(s);
                JSONObject p4 = jo.getJSONObject("p4");
                if (p4 != null) {
                    this.lastPressure = p4.getFloat("value") * 1000; // fine pressure in microns
                }
            } catch (JSONException ex) {
//                System.out.println("pressure parse failed");
            }
        }
    }

    public float getPressure() {
        return this.lastPressure;
    }
}
