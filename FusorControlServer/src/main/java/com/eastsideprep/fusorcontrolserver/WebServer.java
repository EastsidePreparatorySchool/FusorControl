package com.eastsideprep.fusorcontrolserver;

import static spark.Spark.*;
import java.util.ArrayList;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebServer {

    CamStreamer cs;
    DeviceManager dm;
    CoreDevices cd;

    String msgBuffer = "";
    Queue msgqueue;
    final int serialTimeout = 1500;

    public WebServer() {
        msgqueue = new LinkedBlockingQueue<String>();
    }

    public void init() {
        cs = new CamStreamer();
        dm = new DeviceManager();

        // initialize the connections to all serial devices
        cd = dm.init();

        // get the most important devices for our UI. If not present, halt. 
        if (!cd.complete()) {
            dm.shutdown();
            throw new IllegalArgumentException("missing core devices after dm.init()");
        }
        
        dm.getAll();
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
        }

        dm.get("GENERIC").set("bar", "Let's see whether this works");
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
        }

        dm.get("GENERIC").get("bar");
        dm.getAll();
        
        

        port(80); //switch to 80 for regular use
        //sets default public location
        staticFiles.location("/public");

        before("*", (req, res) -> {
            System.out.print("incoming from " + req.ip());
            // do not change this list without explicit approval from Mr. Mein!!!!
            if (!(req.ip().equals("10.20.84.127") // GMEIN's LAPTOP
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
//        get("/killSensor", (req, res) -> {
//            killSensor();
//            return "reset Sensor Arduino";
//        });
//        get("/killControl", (req, res) -> {
//            killControl();
//            return "reset Control Arduino";
//        });
//        get("/inita", (req, res) -> initPorts() ? "success" : "serial init failed");
        get("/getstatus", "application/json", (req, res) -> getStatus(req, res), new JSONRT());

        //variac control
        get("/variac", (req, res) -> {
            int variacValue = Integer.parseInt(req.queryParams("value"));
            System.out.println("Received Variac Set " + variacValue);
            cd.variac.setVoltage(variacValue);
            return "set value as " + req.queryParams("value");
        });

        //tmp control
        get("/tmpOn", (req, res) -> {
            cd.tmp.setOn();
            return "turned on TMP";
        });
        get("/tmpOff", (req, res) -> {
            cd.tmp.setOff();
            return "turned off TMP";
        });

        //solenoid control
        get("/solenoidOn", (req, res) -> {
            cd.solenoid.setOpen();
            return "set solenoid to open";
        });
        get("/solenoidOff", (req, res) -> {
            cd.solenoid.setClosed();
            return "set solenoid to closed";
        });
        System.out.println("Initialized Web Server");
    }

    //nonfunctional
    public Object getStatus(spark.Request req, spark.Response res) {
        boolean arcon = false;
        long start = System.currentTimeMillis();
        String tmpv = "";
        String tmps = "";
        //dm.updateAllStatus(); // todo redesign and reimplement
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

 
}
