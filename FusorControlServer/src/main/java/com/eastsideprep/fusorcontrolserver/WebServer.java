package com.eastsideprep.fusorcontrolserver;

import com.eastsideprep.cameras.CamStreamer;
import com.eastsideprep.serialdevice.DeviceManager;
import com.eastsideprep.serialdevice.CoreDevices;
import javax.servlet.MultipartConfigElement;
import static spark.Spark.*;
import spark.staticfiles.StaticFilesConfiguration;

public class WebServer {

    static WebServer instance;
    static CamStreamer cs;
    static DeviceManager dm;
    static CoreDevices cd;
    static DataLogger dl;

    public WebServer() {
        instance = this;
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

        //chatter control
        get("/verbose", (req, res) -> {
            FusorControlServer.config.verbose = true;
            return "verbose";
        });

        get("/quiet", (req, res) -> {
            FusorControlServer.config.verbose = false;
            return "quiet";
        });

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

        //
        // routes
        //
        //these set all the commands that are going to be sent from the client
        get("/", (req, res) -> "<h1><a href='index.html'>Go to index.html</a></h1>");

        //
        // Observer routes
        // anybody logged in can call these
        //
        get("/protected/getstatus", (req, res) -> getObserverCtx(req).getStatusRoute());

        //
        // Admin routes
        // need to be logged in as admin to call these
        //
        get("/protected/admin/kill", (req, res) -> getAdminCtx(req).killRoute());
        get("/protected/admin/startlog", (req, res) -> getAdminCtx(req).startLogRoute());
        get("/protected/admin/stoplog", (req, res) -> getAdminCtx(req).stopLogRoute());
        get("/protected/admin/variac", (req, res) -> getAdminCtx(req).variacRoute(req));
        get("/protected/admin/numcameras", (req, res) -> Integer.toString(cs.numCameras));
        get("/protected/admin/tmpOn", (req, res) -> getAdminCtx(req).tmpOnRoute());
        get("/protected/admin/tmpOff", (req, res) -> getAdminCtx(req).tmpOffRoute());
        get("/protected/admin/needleValve", (req, res) -> getAdminCtx(req).needleValveRoute(req));
        get("/protected/admin/solenoidOn", (req, res) -> getAdminCtx(req).solenoidOnRoute());
        get("/protected/admin/solenoidOff", (req, res) -> getAdminCtx(req).solenoidOffRoute());

        System.out.println("Initialized Web Server");
    }

    //
    // housekeeping:
    //
    private static String login(spark.Request req, spark.Response res) {
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
        req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

        String login = req.queryParams("login");
        Context ctx = new Context(login, WebServer.instance);

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
        Context ctx = getCtx(req);
        return ctx.name;
    }

    // this can be called even when there is no context
    private static Context getContextFromSession(spark.Session s) {
        Context ctx = s.attribute("context");
        return ctx;
    }

    // this should only be called when we know there is a context in the session
    private static Context getCtx(spark.Request req) {
        return getContextFromSession(req.session());
    }

    // this should only be called when we know there is a context in the session
    private static ObserverContext getObserverCtx(spark.Request req) {
        return (ObserverContext) getContextFromSession(req.session());
    }

    // this should only be called when we know there is a context in the session
    private static AdminContext getAdminCtx(spark.Request req) {
        return (AdminContext) getContextFromSession(req.session());
    }

}
