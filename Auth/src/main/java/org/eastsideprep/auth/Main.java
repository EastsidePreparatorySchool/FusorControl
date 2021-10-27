package org.eastsideprep.auth;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import static spark.Spark.*;
import spark.servlet.SparkApplication;

public class Main implements SparkApplication {

//    public static void main(String[] args) {
//        Main app = new Main();
//        app.init();
////        try {
////            // loop until interrupted
////            while (true) {
////                Thread.sleep(1000);
////            }
////        } catch (Exception e) {
////            // catch and be quiet, then exit
////        }
//    }
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void init() {
        logger.info("EPSAuth: Initializing");
        System.out.println("EPSAuth: Initializing");

        port(80);
        get("name", (req, res) -> getLoginName(req));
        get("headers", (req, res) -> getHeaders(req));
        get("login", (req, res) -> login(req, res));
        staticFiles.location("/");
    }

    private static String login(Request req, Response res) {
        String login = req.headers("X-MS-CLIENT-PRINCIPAL-NAME");
        if (login != null) {
            String red = req.queryParams("url")
                    + "?" + req.queryParams("passthroughparam") + "=" + req.queryParams("passthrough")
                    + "&" + req.queryParams("loginparam") + "=" + login;
            logger.info("EPSAuth: redirecting: " + red);
            res.redirect(red, 302);
            return "ok";
        }
        throw halt(401);
    }

    private static String getLoginName(Request req) {
        String result = req.headers("X-MS-CLIENT-PRINCIPAL-NAME");
        if (result != null) {
            return result;
        }
        return "";
    }

    private static String getHeaders(Request req) {
        String result = "";

        for (String s : req.headers()) {
            result += s + ":" + req.headers(s) + "<br>";
        }

        return result;
    }
}
