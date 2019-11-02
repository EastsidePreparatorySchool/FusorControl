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
import java.util.HashMap;
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

    Device[] arduinos;
    SerialPort[] ports;
    public static HashMap<SerialPort,Device> portToArduino = new HashMap<>();

    String msgBuffer = "";
    Queue msgqueue;
    final int serialTimeout = 1500;

    public WebServer() {
        msgqueue = new LinkedBlockingQueue<String>();
    }

    public void initClient() {
        port(80); //switch to 80 for regular use
        //sets default public location
        staticFiles.location("/public");

        before("*", (req, res) -> {
            System.out.print("incoming from " + req.ip());
            // do not change this list without explicit approval from Mr. Mein!!!!
            if (!(req.ip().equals("10.20.84.153") // FUSOR-CLIENT
                    || req.ip().equals("10.20.84.166") // GMEIN's LAPTOP
                    || req.ip().equals("0:0:0:0:0:0:0:1"))) {   // LOCALHOST
                System.out.println(" ... denied.");
                halt(401, "Not authorized");
            }
            System.out.println(" ... allowed.");
        });

        //these set all the commands that are going to be sent from the client
        //ones like getstatus will have functions that communicate with the arduino
        get("/", (req, res) -> "<h1><a href='index.html'>Go to index.html</a></h1>");
        get("/kill", (req, res) -> {
            stop();
            System.out.println("Server ended with /kill");
            return "server ended";
        });
        get("/killSensor", (req, res) -> {
            killSensor();
            return "reset Sensor Arduino";
        });
        get("/killControl", (req, res) -> {
            killControl();
            return "reset Control Arduino";
        });
        get("/inita", (req, res) -> initPorts() ? "success" : "serial init failed");
        get("/getstatus", "application/json", (req, res) -> getStatus(req, res), new JSONRT());

        //variac control
        get("/variac", (req, res) -> {
            int variacValue = Integer.parseInt(req.queryParams("value"));
            System.out.println("Received Variac Set " + variacValue);
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
        System.out.println("Initialized Web Server");
    }

    
    static SerialPortDataListener connectionListener = new SerialPortDataListener() {
        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
        }

        HashMap<SerialPort, String> bufferState = new HashMap<>();

        @Override
        public void serialEvent(SerialPortEvent e) {
            if (e.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                return;
            }
            //System.out.println("Serial event happened");
            SerialPort port = e.getSerialPort();
            byte[] data = new byte[port.bytesAvailable()];
            port.readBytes(data, data.length);

            //System.out.println("Read " + count + " bytes from arduino");
            String buffer = new String(data);
            if (bufferState.containsKey(port)) {
                buffer = bufferState.get(port) + buffer;
            }
            String[] split;
            //System.out.println(buffer);
            while ((split = parse(buffer))[0] != null) { //loop breaks when there is NOT a complete message-delineator string
                //processSerialMessage(split[0], port);
                System.out.println("recieved: " + split[0] + " from port: " + port.getDescriptivePortName());
                //TODO store/process datata
                if(split[0].startsWith("IDE")){
                    identify(split[0],port);
                }
                buffer = split[1]; //the remainder of the buffer
            }
            bufferState.put(port, buffer);
        }
    };
    
    public boolean initPorts() {
        this.ports = SerialPort.getCommPorts();
        this.arduinos = new Device[64];
        int i = 0;
        System.out.println(this.arduinos.toString());
        System.out.println("Serial Ports Connected: ");
        for(SerialPort port : ports) {
            System.out.print(port.getDescriptivePortName()+", ");
        }
        System.out.println("");
        for (SerialPort port : ports) {
            //listen for response
            if(!port.getDescriptivePortName().contains("COM")) continue;
            System.out.println("opening port: " + port.getDescriptivePortName());
            port.openPort();
            port.addDataListener(connectionListener);
            boolean createArduino = true;
            //ask for identification
            if(createArduino){
                this.arduinos[i] = new Device(port);
                this.portToArduino.put(port, this.arduinos[i]);
                i++;
            }
            try {
                do{
                    writeToPort(port.getOutputStream(), "IDENTIFYEND");
                    //System.out.println("sent identify command to: " + port.getDescriptivePortName());
                    try{ Thread.sleep(100); } catch (InterruptedException e) {}
                }while(WebServer.portToArduino.get(port).name.equals("unknown"));
            } catch (IOException ex) {
                System.out.println(ex.getCause());
                //if this line is active, it prevents empty ports from being added as arduinos, but introduces potential for missing arduinos and makes it slower to add new ones
                //createArduino = false;
            }
            
        }
        return true; //this is currently just so it remains compaitble with the client until the client is updated
        
    }
    
    public static void identify(String message, SerialPort port){
        if(message.contains("IDECONTROL")){
            if(WebServer.portToArduino.containsKey(port)){
                WebServer.portToArduino.get(port).name = "CONTROL";
                System.out.println("Connected to Control Arduino");
            }
        }
        else if (message.contains("IDE")){
            if(WebServer.portToArduino.containsKey(port)){
                String name = message.substring(message.indexOf("IDE")+3);
                System.out.println("New Arduino connected: " + name);
                WebServer.portToArduino.get(port).name = name;
            }
        }
    }
    
    static void writeToPort(OutputStream os, String arg) throws IOException {
        byte[] bytes = (arg + "END").getBytes();
        os.write(bytes);
        //System.out.println("Wrote '" + arg + "' to an arduino");
    }
    
    private void solenoidOn() {
        writeControl("SETsolonEND");
    }

    private void solenoidOff() {
        writeControl("SETsoloffEND");
    }

    private void tmpOn() {
        writeControl("SETtmponEND");
    }

    private void tmpOff() {
        writeControl("SETtmpoffEND");
    }

    //nonfunctional
    
    public Object getStatus(spark.Request req, spark.Response res) {
        boolean arcon = false;
        long start = System.currentTimeMillis();
        String tmpv = "";
        String tmps = "";
        writeAll("GETstatusEND");
        String message = "hold";
        List<Status> status = new ArrayList<>();

        //I plan on having a colon for between the key and number,
        //this takes all messages coming in on the buffer until 'statusend' and sends the correct values back to the client
        do {
            int coli = message.indexOf(":");
            if (message.startsWith("tmpv")) {
                tmpv = message.substring(coli + 1);
            }
            if (message.startsWith("tmps")) {
                tmps = message.substring(coli + 1);
            }
        } while (!message.equals("statusend") && System.currentTimeMillis() - start <= serialTimeout);
        status.add(new Status((arcon ? 1 : 0), tmpv, tmps));
        return status;
    }

    public void killSensor() {
        writeSensor("KILEND"); //should write to sensor arduino
    }

    public void killControl() {
        writeControl("KILEND"); //should write to control arduino
    }

    /*public boolean serialInit() {
        //sets up arduino serial communication
        try {
            System.out.println(Arrays.toString(SerialPort.getCommPorts()));
            arduino = SerialPort.getCommPorts()[0];
            System.out.println("Port: " + arduino);
            if (!arduino.toString().equals("USBSER000")) {
                System.out.println("USBSER000 port not found in array slot 1");
                return false;
            }
            arduino.openPort();
            System.out.println("past openPort()");
            os = arduino.getOutputStream();
            arduino.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                }

                @Override
                //this is event that happens whenever something gets written from the arduino
                //it takes and characters and puts it into the handle function
                //important note, even if you write something as one message on the arduino, it might not come in all at once here
                //the handle function deals with this by looking for deliniators\
                //the current delineator 
                public void serialEvent(SerialPortEvent e) {
                    //System.out.println(e.getEventType());
                    if (e.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                        return;
                    }
                    byte[] data = new byte[arduino.bytesAvailable()];
                    int count = arduino.readBytes(data, data.length);
                    //System.out.println("Read " + count + " bytes from arduino");
                    handle(new String(data));
                }
            });
        } catch (Exception e) {
            System.out.println("there was an error:" + e);
            return false;
        }
        return true;
    }*/
    
     public static String[] parse(String s) {
        String target = "END";
        int location = s.indexOf(target);
        if (location == -1) {
            return new String[]{null, s};
        }
        return new String[]{
            s.substring(0, location),
            s.substring(location + target.length())
        };
    }

    private void writeSensor(String arg) {
        byte[] bytes = arg.getBytes();
        for (int i = 0; i < arduinos.length && arduinos[i]!=null; i++) {
            if(arduinos[i].name.equals("CONTROL")){
                continue;
            }
            try {
                arduinos[i].os.write(bytes);
            } catch (IOException ex) {
                System.out.println("Serial comm exception: " + ex + " " + "in port " + arduinos[i].port.getSystemPortName());
            }
        }
    }
    
    private void writeAll(String arg) {
        byte[] bytes = arg.getBytes();
        for (int i = 0; i < arduinos.length && arduinos[i]!=null; i++) {
            try {
                arduinos[i].os.write(bytes);
            } catch (IOException ex) {
                System.out.println("Serial comm exception: " + ex + " " + "in port " + arduinos[i].port.getSystemPortName());
            } catch (NullPointerException e) {
                System.out.println("Couldnt write, nullpointer?");
            }
        }
    }

    private void writeControl(String arg) {
        byte[] bytes = arg.getBytes();
        for (int i = 0; i < arduinos.length && arduinos[i]!=null; i++) {
            if(arduinos[i].name.equals("SENSOR")){
                continue;
            }
            try {
                arduinos[i].os.write(bytes);
            } catch (IOException ex) {
                System.out.println("Serial comm exception: " + ex + " " + "in port " + arduinos[i].port.getSystemPortName());
            } 
        }
    }

    public void test() {
        writeAll("TESmemeEND");
    }

    private void sendVoltage(int v) {
        writeControl("SETvolt" + String.format("%03d", v) + "END");
    }

}
