package com.eastsideprep.fusorcontrolserver;

import static spark.Spark.*;

public class WebServer {

    CamStreamer cs;
    DeviceManager dm;
    CoreDevices cd;
    DataLogger dl;

    public WebServer() {
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
        cd.variac.setVoltage(0);

//        dl = new DataLogger();
//        try {
//            dl.init(dm);
//        } catch (IOException ex) {
//            System.out.println("DataLogger:Exception on init:" + ex);
//        }
        port(80); //switch to 80 for regular use
        //sets default public location
        staticFiles.location("/public");

        before("*", (req, res) -> {
            // do not change this list without explicit approval from Mr. Mein!!!!
            if (!(req.ip().equals("10.20.84.127") // GMEIN's LAPTOP
                    || req.ip().equals("0:0:0:0:0:0:0:1"))) {   // LOCALHOST
                System.out.print("incoming from " + req.ip() + ": " + req.url());
                System.out.println(" ... denied.");
                throw halt(401, "Not authorized");
            }
            //System.out.print("incoming from " + req.ip()+": "+req.url());
            //System.out.println(" ... allowed.");
        });

        //these set all the commands that are going to be sent from the client
        get("/", (req, res) -> "<h1><a href='index.html'>Go to index.html</a></h1>");
        get("/kill", (req, res) -> {
            if (dl != null) {
                dl.shutdown();
            }
            dm.shutdown();

            stop();
            System.out.println("Server ended with /kill");
            System.exit(0);
            return "server ended";
        });

        get("/startlog", (req, res) -> {
            synchronized (this) {
                if (dl != null) {
                    dl.shutdown();
                }
                dl = new DataLogger();
                dl.init(dm);
                System.out.println("New log started");
                return "log started";

            }
        });

        get("/stoplog", (req, res) -> {
            synchronized (this) {
                if (dl != null) {
                    dl.shutdown();
                    dl = null;
                    System.out.println("Log stopped");
                    return "log stopped";
                } else {
                    return "log not running";
                }
            }
        });

        //variac control
        get("/variac", (req, res) -> {
            int variacValue = Integer.parseInt(req.queryParams("value"));
            System.out.println("Received Variac Set " + variacValue);
            cd.variac.setVoltage(variacValue);
            return "set value as " + req.queryParams("value");
        });

        //number of cameras streaming
        get("/cameras", (req, res) -> {
            return Integer.toString(cs.numCameras);
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

        //chatter control
        get("/verbose", (req, res) -> {
            FusorControlServer.verbose = true;
            return "verbose";
        });

        get("/quiet", (req, res) -> {
            FusorControlServer.verbose = false;
            return "quiet";
        });

        get("/getstatus", (req, res) -> {
            //System.out.println("/getstatus");
            if (dl == null) {
                System.out.println("  logging inactive, poking bears ...");
                dm.getAllStatus();
                Thread.sleep(1000);
            }
            String s = dm.readStatusResults(FusorControlServer.includeCoreStatus);
            if (FusorControlServer.superVerbose) {
                System.out.println("  Status:" + s);
            }
            return s;
        });

        System.out.println("Initialized Web Server");
    }
}
