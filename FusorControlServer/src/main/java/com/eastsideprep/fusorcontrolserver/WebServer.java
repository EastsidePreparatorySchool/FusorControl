package com.eastsideprep.fusorcontrolserver;

import com.eastsideprep.cameras.CamStreamer;
import com.eastsideprep.serialdevice.DeviceManager;
import com.eastsideprep.serialdevice.CoreDevices;
import javax.servlet.MultipartConfigElement;
import static spark.Spark.*;
import spark.staticfiles.StaticFilesConfiguration;

public class WebServer {

    CamStreamer cs;
    DeviceManager dm;
    CoreDevices cd;
    DataLogger dl;

    public WebServer() {
    }

    public void init() {
        // housekeeping routes and filters
        port(80);

        post("/login", (req, res) -> login(req, res));
        post("/logout", (req, res) -> logout(req, res));
        get("/protected/name", (req, res) -> getName(req, res));

        // HTML pages use this to switch to the "expired" page
        get("/protected/checktimeout", (req, res) -> {
            Context ctx = getContextFromSession(req.session());
            if (ctx == null || ctx.checkExpired()) {
                System.out.println("filter: expired");
                internalLogout(req);
                return "expired";
            }
            return "alive";
        });

        // liveness check - this actually governs expiration
        before((req, res) -> {
//            System.out.println("filter: timer alive?" + req.url());
            Context ctx = getContextFromSession(req.session());
            if (ctx != null && ctx.checkExpired()) {
                internalLogout(req);
                res.redirect("/expired.html");
            }
        });

        before("/protected/*", (req, res) -> {
//            System.out.println("filter: /protected/*");
            if (req.session().attribute("context") == null) {
                System.out.println("unauthorized " + req.url());
                res.redirect("/unauthorized.html");
            }
        });
        before("/protected/admin/*", (req, res) -> {
//            System.out.println("filter: /protected/admin/*");
            Context ctx = getContextFromSession(req.session());
            if (ctx == null || !ctx.isAdmin) {
                System.out.println("unauthorized " + req.url());
                res.redirect("/unauthorized.html");
            }
        });
        
        // liveness timer - this keeps the context alive for valid pages and requests
        before((req, res) -> {
            Context ctx = getContextFromSession(req.session());
            if (ctx != null) {
                if (!req.url().endsWith("/protected/checktimeout")) {
//                    System.out.println("timer reset from URL: " + req.url());
                    ctx.updateTimer();
                }
            }
        });

        // Static files filter is LAST
        StaticFilesConfiguration staticHandler = new StaticFilesConfiguration();
        staticHandler.configure("/static");
        before((request, response) -> staticHandler.consume(request.raw(), response.raw()));

        //
        // setup all that fusor stuff
        //
        dm = new DeviceManager();
        cs = new CamStreamer(dm);
        cd = dm.init();

        // get the most important devices for our UI. If not present, halt. 
        if (!cd.complete()) {
            dm.shutdown();
            throw new IllegalArgumentException("missing core devices after dm.init()");
        }
        cd.variac.setVoltage(0);

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
                dl.init(dm, cs);
                dm.autoStatusOn();
                System.out.println("New log started");
                return "log started";

            }
        });

        get("/stoplog", (req, res) -> {
            synchronized (this) {
                if (dl != null) {
                    dm.autoStatusOff();
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
            if (cd.variac.setVoltage(variacValue)) {
                return "set value as " + req.queryParams("value");
            }
            halt("Variac control failed");
            return "";
        });

        //number of cameras streaming
        get("/numcameras", (req, res) -> {
            return Integer.toString(cs.numCameras);
        });

        //tmp control
        get("/tmpOn", (req, res) -> {
            if (cd.tmp.setOn()) {
                return "turned on TMP";
            }
            halt(500, "TMP control failed");
            return "";
        });

        get("/tmpOff", (req, res) -> {
            if (cd.tmp.setOff()) {
                return "turned off TMP";
            }
            halt(500, "TMP control failed");
            return "";
        });

        get("/needleValve", (req, res) -> {
            int value = Integer.parseInt(req.queryParams("value"));
            System.out.println("Received needle valve Set " + value);
            if (this.cd.needle.set("needlevalve", value)) {
                System.out.println("needle valve success");
                return "set needle valve value as " + value;
            }
            System.out.println("needle valve fail");
            halt(500, "set needle valve failed");
            return "";
        });

        //solenoid control
        get("/solenoidOn", (req, res) -> {
            if (cd.gas.setOpen()) {
                return "set solenoid to open";
            }
            halt(500, "set solenoid failed");
            return "";

        });

        get("/solenoidOff", (req, res) -> {
            if (cd.gas.setClosed()) {
                return "set solenoid to closed";
            } else {
                halt(500, "set solenoid faild");
                return "";
            }
        });

        //chatter control
        get("/verbose", (req, res) -> {
            FusorControlServer.config.verbose = true;
            return "verbose";
        });

        get("/quiet", (req, res) -> {
            FusorControlServer.config.verbose = false;
            return "quiet";
        });

        addGetStatusRoute();

        System.out.println("Initialized Web Server");
    }

    private void addGetStatusRoute() {
        get("/getstatus", (req, res) -> {
            //System.out.println("/getstatus");
            if (dl == null) {
                System.out.println("  logging inactive, poking bears ...");
                dm.getAllStatus();
                Thread.sleep(1000);
            }
            String s = dm.readStatusResults(FusorControlServer.config.includeCoreStatus);
            if (FusorControlServer.config.superVerbose) {
                System.out.println("  Status:" + s);
            }
            return s;
        });
    }

    // housekeeping:
    private static String login(spark.Request req, spark.Response res) {
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
        req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

        String login = req.queryParams("login");
        Context ctx = new Context(login);

        System.out.println("\"" + login + "\"");
        if (req.ip().equals("10.20.82.127") // GMEIN's LAPTOP
                || req.ip().equals("0:0:0:0:0:0:0:1")) {// LOCALHOST
            System.out.println("login: Admin: " + login);
            ctx.isAdmin = true;
            res.redirect("protected/admin/index.html");
        } else {
            res.redirect("protected/status.html");
        }

        ctx.name = login;
        ctx.id = 0;
        req.session().attribute("context", ctx);

//    if (ctx.db.queryName (ctx) 
//        .equals("unknown")) {
//            internalLogout(req);
//        res.redirect("login.html");
//        return "";
//    }
        System.out.println("login: " + login);
        return "ok";
    }

    private static void internalLogout(spark.Request req) {
        if (req.session().attribute("context") != null) {
            Context ctx = getContextFromSession(req.session());
            req.session().attribute("context", null);
            System.out.println("logged off.");
        }
    }

    private static String logout(spark.Request req, spark.Response res) {
        internalLogout(req);
        res.redirect("login.html");

        return "ok";
    }

    public static String getName(spark.Request req, spark.Response res) {
        Context ctx = getReqCtx(req);
        return ctx.name;
    }

    // this can be called even when there is no context
    private static Context getContextFromSession(spark.Session s) {
        Context ctx = s.attribute("context");
        return ctx;
    }

    // this should only be called when we know there is a context in the session
    private static Context getReqCtx(spark.Request req) {
        return getContextFromSession(req.session());
    }

}
