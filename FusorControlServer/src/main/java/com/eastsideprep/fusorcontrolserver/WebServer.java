/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import static spark.Spark.*;
import com.fazecast.jSerialComm.*;

import java.util.ArrayList;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author paul
 */
public class WebServer {
    
    SerialPort arduino;
    OutputStream os;
    String msgBuffer = "";
    Queue msgqueue;
    final int serialTimeout = 1500;
    
    public WebServer (){
        msgqueue = new LinkedBlockingQueue<String>();
    }
    
    public void init() {
        port(8080); //switch to 80 for regular use
        //sets default public location
        staticFiles.location("/public");
        
        //these set all the commands that are going to be sent from the client
        //ones like getstatus will have functions that communicate with the arduino
        get("/", (req, res) -> "<h1><a href='index.html'>Go to index.html</a></h1>");
        get("/kill", (req, res) -> {stop(); System.out.println("Server ended with /kill"); return "server ended";});
        get("/killSensor", (req,res) -> {killSensor(); return "reset Sensor Arduino";});
        get("/killControl", (req,res) -> {killControl(); return "reset Control Arduino";});
        get("/inita", (req, res) -> serialInit()?"success":"serial init failed");
        get("/getstatus", "application/json", (req, res) -> getStatus(req, res), new JSONRT());
        
        //variac control
        get("/variac", (req, res) -> {
            int variacValue = Integer.parseInt(req.queryParams("value"));
            System.out.println("Recieved Variac Set " + variacValue);
            sendVoltage(variacValue);
            return "set value as " + req.queryParams("value");
        });
        
        //tmp control
        get("/tmpOn", (req, res) -> {
            tmpOn();
            return "turned on TMP";
        });
        get("/tmpOff", (req, res) -> {
            tmpOff();
            return "turned off TMP";
        });

        //solenoid control
        get("/solenoidOn", (req, res) -> {
            solenoidOn();
            return "turned on Solenoid";
        });
        get("/solenoidOff", (req, res) -> {
            solenoidOff();
            return "turned off Solenoid";
        });
    }
    private void solenoidOn() {
        write("SETsolonEND");
    }
    private void solenoidOff() {
        write("SETsoloffEND");
    }
    private void tmpOn() {
        write("SETtmponEND");
    }
    private void tmpOff() {
        write("SETtmpoffEND");
    }
    
    public Object getStatus(spark.Request req, spark.Response res) {
        boolean arcon = arduino != null;
        long start = System.currentTimeMillis();
        String tmpv = "";
        String tmps = "";
        write("GETstatusEND");
        String message = "hold";
        List<Status> status = new ArrayList<>();
        
        //I plan on having a colon for between the key and number,
        //this takes all messages coming in on the buffer until 'statusend' and sends the correct values back to the client
        do {
            int coli = message.indexOf(":");
            if(message.startsWith("tmpv")) {
                tmpv = message.substring(coli+1);
            } if(message.startsWith("tmps")) {
                tmps = message.substring(coli+1);
            }
        } while(!message.equals("statusend")&&System.currentTimeMillis()-start<=serialTimeout);
        status.add(new Status((arcon?1:0), tmpv, tmps));
        return status;
    }
    
    public void killSensor() {
        write("KILEND"); //should write to sensor arduino
    }
    
    public void killControl() {
        write("KILEND"); //should write to control arduino
    }
    
    public boolean serialInit() {
        //sets up arduino serial communication
        try {
            System.out.println(Arrays.toString(SerialPort.getCommPorts()));
        arduino = SerialPort.getCommPorts()[0];
        arduino.openPort();
            System.out.println("port opened?");
            os = arduino.getOutputStream();
        arduino.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
            
            @Override
            //this is event that happens whenever something gets written from the arduino
            //it takes and characters and puts it into the handle function
            //important note, even if you write something as one message on the arduino, it might not come in all at once here
            //the handle function deals with this by looking for deliniators\
            //the current delineator 
            public void serialEvent(SerialPortEvent e) {
                //System.out.println(e.getEventType());
                if(e.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) 
                    return;
                byte[] data = new byte[arduino.bytesAvailable()];
                int count = arduino.readBytes(data, data.length);
                //System.out.println("Read " + count + " bytes from arduino");
                handle(new String(data));
            }
        });
        } catch (Exception e) {
            System.out.println("there was an error");
            return false;
        }
        return true;
    }
    
    //Handles the first command
    private void handle(String arg) {
        msgBuffer += arg;
        //this function takes all the raw input from the arduino and splits it up correctly into commands
        // 'msgBuffer' is a queue to hold commands
        while(msgBuffer.indexOf("END")!=-1) {
            int semi = msgBuffer.indexOf("END");
            System.out.println("arduino: " + msgBuffer.substring(0, semi));
            msgqueue.add(msgBuffer.substring(0, semi));
            msgBuffer = "";
        }
        
        
    }
    
    private void write(String arg) {
        byte[] bytes = arg.getBytes();
        try {
            os.write(bytes);
        } catch (IOException ex) {
            System.out.println("Serial comm exception: " + ex);
        }
    }
    
    public void test(){
        write("TESmemeEND");
    }
    
    private void sendVoltage(int v) {        
        write("SETvolt" + String.format("%03d", v) + "END");
    }
    
    
}
