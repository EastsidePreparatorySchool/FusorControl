package com.eastsideprep.fusorcontrolserver;

import com.eastsideprep.cameras.CamStreamer;
import com.eastsideprep.fusorweblog.FusorWebLogState;
import com.eastsideprep.serialdevice.DeviceManager;
import com.eastsideprep.serialdevice.CoreDevices;
import com.eastsideprep.weblog.WebLog;
import java.util.HashMap;
import javax.servlet.MultipartConfigElement;
import static spark.Spark.*;
import spark.staticfiles.StaticFilesConfiguration;

public class WebServer {

    static WebServer instance;
    static CamStreamer cs;
    static DeviceManager dm;
    static CoreDevices cd;
    static DataLogger dl;
    static FusorWebLogState state;
    static WebLog log;

    public WebServer() {
        instance = this;
    }

    public void init() {
        // housekeeping routes and filters
        port(80);

        post("/login", (req, res) -> login(req, res));
        post("/logout", (req, res) -> logout(req, res));
        get("/client", (req, res) -> getClient(req, res));
        get("/resetobserver", (req, res) -> resetObserver(req, res));

        // HTML pages use this to switch to the "expired" page
        get("/protected/checktimeout", (req, res) -> {
            Context ctx = getCtx(req);
            if (ctx == null || (ctx.checkExpired() && !(ctx instanceof AdminContext))) {
                System.out.println("filter: expired");
                internalLogout(req);
                return "expired";
            }
            return "alive";
        });

        // Static files filter is first so we don't have to encode clientIDs in the page requests
        StaticFilesConfiguration staticHandler = new StaticFilesConfiguration();
        staticHandler.configure("/public");
        before((request, response) -> staticHandler.consume(request.raw(), response.raw()));

        before((req, res) -> {
//            System.out.println("filter: timer alive?" + req.url());
            Context ctx = getCtx(req);
            // liveness check - this actually governs expiration
            if (ctx != null && !(ctx instanceof AdminContext) && ctx.checkExpired()) {
                internalLogout(req);
                res.redirect("/expired.html");
                return;
            }
        });

        before("/protected/*", (req, res) -> {
            Context ctx = getCtx(req);
            if (ctx == null || !(ctx instanceof ObserverContext)) {
                System.out.println("filter: /protected/*");
                System.out.println("unauthorized " + req.uri());
                System.out.println("Ctx: " + ctx);
                if (ctx != null) {
                    System.out.println("ClientID: " + ctx.clientID);
                }
                res.redirect("/unauthorized.html");
                return;
            }
            // make sure everyone here has a log observer
            if (ctx.obs == null) {
                ctx.obs = log.addObserver(ctx.name);
            }
        });
        before("/protected/admin/*", (req, res) -> {
            Context ctx = getCtx(req);
            if (ctx == null || !(ctx instanceof AdminContext)) {
                System.out.println("filter: /protected/admin/*");
                System.out.println("unauthorized " + req.uri());
                System.out.println("Ctx: " + ctx);
                if (ctx != null) {
                    System.out.println("ClientID: " + ctx.clientID);
                }
                res.redirect("/unauthorized.html");
                return;
            }
            // make sure everyone here has a log observer
            if (ctx.obs == null) {
                ctx.obs = log.addObserver(ctx.name);
            }
        });

        // liveness timer - this keeps the context alive for valid pages and requests
        before((req, res) -> {
            Context ctx = getCtx(req);
            if (ctx != null) {
                if (!req.url().endsWith("/protected/checktimeout")) {
                    // System.out.println("timer reset from URL: " + req.url());
                    ctx.updateTimer();
                }
            }
        });

        //
        // setup all that fusor stuff
        //
        state = new FusorWebLogState();
        log = new WebLog(state);
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
        get("/", (req, res) -> "<h1><a href='index.html'>Go to index.html</a></h1>");

        //
        // Observer routes
        // anybody logged in can call these
        //
        get("/protected/getstatus", (req, res) -> getObserverCtx(req).getStatusRoute());
        get("/protected/numcameras", (req, res) -> Integer.toString(cs.numCameras));
        post("/protected/comment", (req, res) -> getObserverCtx(req).comment(req));

        //
        // Admin routes
        // need to be logged in as admin to call these
        //
        get("/protected/admin/kill", (req, res) -> getAdminCtx(req).killRoute());
        get("/protected/admin/startlog", (req, res) -> {
            return getAdminCtx(req).startLogRoute();
        });
        get("/protected/admin/stoplog", (req, res) -> getAdminCtx(req).stopLogRoute());
        get("/protected/admin/variac", (req, res) -> getAdminCtx(req).variacRoute(req));
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
        Context ctx;

        //System.out.println("\"" + login + "\"");
        if ((req.ip().equals("10.20.82.127") /* GMEIN's LAPTOP */
                || req.ip().equals("0:0:0:0:0:0:0:1") /* LOCALHOST */)
                && login.equalsIgnoreCase("gmein")) {
            System.out.println("login: Admin: " + login);
            ctx = new AdminContext(login, instance);
            ctx.isAdmin = true;
            res.redirect("protected/index.html");
        } else {
            ctx = new ObserverContext(login, instance);
            System.out.println("login: Observer: " + login);
            res.redirect("protected/index.html");
        }

        ctx.name = login;
        registerCtx(req, ctx);
        req.session().attribute("context", ctx);

        return "ok";
    }

    private static void internalLogout(spark.Request req) {
        if (req.session().attribute("context") != null) {
            Context ctx = getCtx(req);
            req.session().attribute("context", null);
            System.out.println("logged off.");
        }
    }

    private static String logout(spark.Request req, spark.Response res) {
        internalLogout(req);
        res.redirect("login.html");

        return "ok";
    }

    public static String getClient(spark.Request req, spark.Response res) {
        Context ctx = getCtx(req);
        if (ctx == null) {
            return "invalid";
        } else if (ctx instanceof AdminContext) {
            return ctx.login + " (admin)";
        } else if (ctx instanceof ObserverContext) {
            return ctx.login + " (observer)";
        } else {
            return "invalid";
        }
    }

    public static String resetObserver(spark.Request req, spark.Response res) {
        Context ctx = getCtx(req);
        ctx.obs = null;
        return "ok";
    }

    private static void registerCtx(spark.Request req, Context ctx) {
        HashMap<String, Context> ctxMap = req.session().attribute("contexts");
        if (req.session().isNew() || ctxMap == null) {
            ctxMap = new HashMap<>();
            req.session().attribute("contexts", ctxMap);
        }

        String client = req.queryParams("clientID");
        ctx.clientID = client;
        if (!ctxMap.containsKey(client)) {
            System.out.println("New context: " + ctx + ", clientID " + client);
        }
        ctxMap.put(client, ctx);
    }

    // context helper
    private static Context getCtx(spark.Request req) {
        if (req.requestMethod().equals("POST")) {
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
            req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
        }

        HashMap<String, Context> ctxMap = req.session().attribute("contexts");
        if (req.session().isNew() || ctxMap == null) {
            ctxMap = new HashMap<>();
            req.session().attribute("contexts", ctxMap);
        }

        String client = req.queryParams("clientID");
        Context ctx = ctxMap.get(client);
        if (ctx == null) {
            return null;
        }

        // blow up stale contexts
        if (ctx.obs != null && ctx.obs.isStale()) {
            ctx.obs = null;
//            return null;
        }

        req.session().maxInactiveInterval(300); // kill this session after 5 minutes of inactivity
        return ctx;
    }

    // this should only be called when we know there is a context in the session
    private static ObserverContext getObserverCtx(spark.Request req) {
        return (ObserverContext) getCtx(req);
    }

    // this should only be called when we know there is a context in the session
    private static AdminContext getAdminCtx(spark.Request req) {
        return (AdminContext) getCtx(req);
    }

}
