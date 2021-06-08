package com.eastsideprep.fusorcontrolserver;

import com.eastsideprep.cameras.CamStreamer;
import com.eastsideprep.fusorweblog.FusorWebLogEntry;
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
    public static DataLogger dl;
    static FusorWebLogState state;
    static WebLog log;
    static String logPath;
    static AdminContext upgrade;

    public WebServer() {
        instance = this;
    }

    public void init() {
        //
        // setup all that fusor stuff
        //

        // create a new log and reset it
        state = new FusorWebLogState();
        log = new WebLog(state);
        WebServer.log.clear(state, new FusorWebLogEntry("<reset>", System.currentTimeMillis(), "{}"));

        // create the serial device manager and camera streams
        dm = new DeviceManager();
        cs = new CamStreamer(dm);
        cd = dm.init();

        // create the data logger (an observer of the log that writes everything to disk)
        dl = new DataLogger();
        dl.init(dm, cs, null);
        dm.autoStatusOn();
        System.out.println("New log started");

        // get the most important devices for our UI. If not present, halt. 
        if (!cd.complete()) {
            dm.shutdown();
            throw new IllegalArgumentException("missing core devices after dm.init()");
        }
        cd.variac.setVoltage(-1, cd);

        // housekeeping routes and filters
        port(80);

        options("/*", (request, response) -> {
            response.header("Access-Control-Allow-Headers", "*");
            response.header("Access-Control-Allow-Methods", "*");
            response.header("Access-Control-Allow-Origin", "*");
            return "OK";
        });

        post("/login", (req, res) -> login(req, res));
        get("/autologin", (req, res) -> autoLogin(req, res));
        post("/logout", (req, res) -> logout(req, res));
        get("/numcameras", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            Context ctx = getCtx(req);
            if (ctx instanceof AdminContext) {
                return Integer.toString(cs.numCameras);
            }
            return 0;
        });
        get("/client", (req, res) -> getClient(req, res));
        get("/resetobserver", (req, res) -> resetObserver(req, res));

        // HTML pages use this to switch to the "expired" page
        get("/protected/checktimeout", (req, res) -> {
            Context ctx = getCtx(req);
            if (ctx == null || (ctx.checkExpired() && !(ctx instanceof AdminContext))) {
                System.out.println("filter: expired");
                internalLogout(req);
                throw halt(401, "expired");
            }
            return "alive";
        });

        before((req, res) -> {
            // System.out.println("filter: timer alive?" + req.url());
            Context ctx = getCtx(req);
            // liveness check - this actually governs expiration
            if (ctx != null && !(ctx instanceof AdminContext) && ctx.checkExpired()) {
                internalLogout(req);
                throw halt(401, "expired");
            }
        });

        before("/protected/*", (req, res) -> {
            Context ctx = getCtx(req);
            if (ctx == null || !(ctx instanceof ObserverContext)) {
//                System.out.println("filter: /protected/*");
//                System.out.println("unauthorized " + req.uri());
//                System.out.println("Ctx: " + ctx);
//                if (ctx != null) {
//                    System.out.println("ClientID: " + ctx.clientID);
//                }
                throw halt(401, "unauthorized");
            }
            // make sure everyone here has a log observer
            try {
                if (ctx.obs == null) {
                    ctx.obs = log.addObserver(ctx.name);
                }
            } catch (Exception e) {
                System.out.println("exception in /protected filter while adding observer");
            }

        });
        before("/protected/admin/*", (req, res) -> {
            try {
                Context ctx = getCtx(req);
                if (ctx == null || !(ctx instanceof AdminContext)) {
//                    System.out.println("filter: /protected/admin/*");
//                    System.out.println("unauthorized " + req.uri());
//                    System.out.println("Ctx: " + ctx);
//                    if (ctx != null) {
//                        System.out.println("ClientID: " + ctx.clientID);
//                    }
                    throw halt(401, "unauthorized");
                }
                // make sure everyone here has a log observer
                try {
                    if (ctx.obs == null) {
                        ctx.obs = log.addObserver(ctx.name);
                    }
                } catch (Exception e) {
                    System.out.println("exception in /protected filter while adding admin observer");
                }
            } catch (Throwable t) {
                System.out.println("exception in /protected/admin filter");
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

        // Static files filter is LAST 
        StaticFilesConfiguration staticHandler = new StaticFilesConfiguration();
        staticHandler.configure("/public");
        before((request, response) -> staticHandler.consume(request.raw(), response.raw()));

        //
        // Observer routes
        // anybody logged in can call these
        //
        get("/protected/getstatus", (req, res) -> getObserverCtx(req).getStatusRoute());
        post("/protected/comment", (req, res) -> getObserverCtx(req).comment(req));
        get("/protected/getlogfilenames", (req, res) -> getObserverCtx(req).getLogFileNames(), new JSONRT());
        get("/protected/getlogfile", (req, res) -> getObserverCtx(req).getLogFile(req, res));
        get("/protected/emergency_stop", (req, res) -> getObserverCtx(req).emergencyStop(req));

        //
        // Admin routes
        // need to be logged in as admin to call these
        //
        get("/protected/admin/kill", (req, res) -> getAdminCtx(req).killRoute());
        get("/protected/admin/startlog", (req, res) -> getAdminCtx(req).startLogRoute(req));
        get("/protected/getonestatus", (req, res) -> getAdminCtx(req).getOneStatusRoute());
        get("/protected/admin/stoplog", (req, res) -> getAdminCtx(req).stopLogRoute());
        get("/protected/admin/variac", (req, res) -> getAdminCtx(req).variacRoute(req));
        get("/protected/admin/variac_stop", (req, res) -> getAdminCtx(req).variacStop(req));
        get("/protected/admin/rpOn", (req, res) -> getAdminCtx(req).rpOnRoute());
        get("/protected/admin/rpOff", (req, res) -> getAdminCtx(req).rpOffRoute());
        get("/protected/admin/tmpOn", (req, res) -> getAdminCtx(req).tmpOnRoute());
        get("/protected/admin/tmpOff", (req, res) -> getAdminCtx(req).tmpOffRoute());
        get("/protected/admin/tmpLow", (req, res) -> getAdminCtx(req).tmpLowRoute());
        get("/protected/admin/tmpHigh", (req, res) -> getAdminCtx(req).tmpHighRoute());
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

        if (req.ip().equals("0:0:0:0:0:0:0:1")) {
            login = "SYSTEM";
        }

        //System.out.println("\"" + login + "\"");
        if ((req.ip().equals("10.20.82.127") /* GMEIN's LAPTOP */
                || req.ip().equals("0:0:0:0:0:0:0:1") /* LOCALHOST */)) {
            System.out.println("login: Admin: " + login);
            ctx = new AdminContext(login, instance);
            ctx.isAdmin = true;
            res.redirect("/console.html");
        } else {
            ctx = new ObserverContext(login, instance);
            System.out.println("login: Observer: " + login);
            res.redirect("/console.html");
        }

        ctx.name = login;

        registerCtx(req, ctx);

        long millis = System.currentTimeMillis();
        String logText = DataLogger.makeLoginCommandText(login, req.ip(), ctx.isAdmin ? 1 : 0, millis);

        WebServer.dm.recordStatus("Login", millis, logText);

        return "ok";
    }

    private static String autoLogin(spark.Request req, spark.Response res) {
        String login = req.queryParams("login");
        if (login.contains("@")) {
            login = login.substring(0, login.indexOf('@'));
        }

        if (req.ip().equals("0:0:0:0:0:0:0:1")) {
            login = "SYSTEM";
        }

        Context ctx;

        //System.out.println("\"" + login + "\"");
        if ((req.ip().equals("10.20.82.127") /* GMEIN's LAPTOP */
                || req.ip().equals("0:0:0:0:0:0:0:1") /* LOCALHOST */)) {
            System.out.println("login: Admin: " + login);
            ctx = new AdminContext(login, instance);
            ctx.isAdmin = true;
        } else {
            ctx = new ObserverContext(login, instance);
            System.out.println("login: Observer: " + login);
        }

        ctx.name = login;
        registerCtx(req, ctx);

        long millis = System.currentTimeMillis();
        String logText = DataLogger.makeLoginCommandText(login, req.ip(), ctx.isAdmin ? 1 : 0, millis);

        WebServer.dm.recordStatus("Login", millis, logText);
        res.redirect("/console.html");
        return "ok";
    }

    private static void internalLogout(spark.Request req) {
        Context ctx = getCtx(req);
        req.session().attribute("context", null);
        System.out.println("logged off.");
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
        ctx.ip = req.ip();
        if (!ctxMap.containsKey(client)) {
            System.out.println("New context: " + ctx + ", clientID " + client);
        }
        synchronized (WebServer.class) {
            ctxMap.put(client, ctx);
        }
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

        // if someone reloaded a tab, clientID will not be there. If they have only one tab open, just use that one
        if (client == null && ctxMap.size() == 1) {
            client = ctxMap.values().toArray(new Context[1])[0].clientID;
        }

        Context ctx = ctxMap.get(client);
        if (ctx == null) {
            return null;
        }

        // upgrades
        if (WebServer.upgrade != null) {
            if (ctx.login.equals(WebServer.upgrade.login)) {
                synchronized (WebServer.class) {
                    if (WebServer.upgrade
                            != null) {
                        AdminContext ctx2 = WebServer.upgrade;
                        ctx2.clientID = client;
                        ctx2.ip = req.ip();
                        ctx2.obs = ctx.obs;
                        ctx2.isAdmin = true;
                        WebServer.upgrade = null;
                        ctxMap.put(client, ctx2);
                        ctx = ctx2;
                        long millis = System.currentTimeMillis();
                        String logText = DataLogger.makeLoginCommandText(ctx.login, req.ip(), 1, millis);
                        WebServer.dm.recordStatus("<promote>", millis, logText);
                    }
                }
            }

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
