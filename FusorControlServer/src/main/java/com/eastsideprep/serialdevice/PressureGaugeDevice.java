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
public class PressureGaugeDevice extends Arduino{
    float lastPressure;
    
    PressureGaugeDevice(Arduino sd) {
        super(sd);
        this.setStatus("{\"device\":\"PIRANI\"}");
        this.lastPressure = 0.0f;
    }

   
    public float getPressure() {
       String status = this.currentStatus;
        try {
            JSONObject jo = new JSONObject(status);
            this.lastPressure = jo.getJSONObject("p4").getFloat("value")*1000; // fine pressure in microns
        } catch (JSONException ex) {
            System.out.println("pressure parse failed");
        }
        return this.lastPressure;
    }
}
