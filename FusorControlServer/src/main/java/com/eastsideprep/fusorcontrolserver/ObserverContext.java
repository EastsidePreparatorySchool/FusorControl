/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import static com.eastsideprep.fusorcontrolserver.WebServer.cd;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletResponse;
import static spark.Spark.halt;

/**
 *
 * @author gmein
 */
public class ObserverContext extends Context {

    ObserverContext(String login, WebServer ws) {
        super(login, ws);
    }

    String getStatusRoute() {
        //System.out.println("/getstatus");
        if (WebServer.dl == null && this.isAdmin) {
            System.out.println("  logging inactive, poking bears ...");
            WebServer.dm.getAllStatus();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
            }
        }

        String s = DataLogger.getNewLogEntryBatch(obs);

        if (FusorControlServer.config.superVerbose) {
            System.out.println("  Status:" + s);
        }
        return s;
    }

    String comment(spark.Request req) {
        String text = req.queryParams("text");
        String ip = req.ip();

        long millis = System.currentTimeMillis();
        String logText = DataLogger.makeCommentDeviceText(this.name, ip, text, millis);

        System.out.println("Comment: " + logText);
        WebServer.dm.recordStatus("Comment", millis, logText);

        return "ok";
    }

    String variacEmergencyStop(spark.Request req) {
        System.out.println("Received Emergency Variac Stop ");
        long millis = System.currentTimeMillis();
        String logText = DataLogger.makeEmergencyStopDeviceText(this.name, ip, millis);
        WebServer.dm.recordStatus("Command", millis, logText);
        
        if (cd.variac.set("stop", 0)) {
            return "variac emergency stop ";
        } else {
            throw halt("Variac stop failed");
        }
    }

    ArrayList<String> getLogFileNames() {
        ArrayList<String> result = new ArrayList<>();
        File dir = new File(WebServer.logPath);
        File[] directoryListing = dir.listFiles();

        Arrays.sort(directoryListing, (a, b) -> Long.signum(b.lastModified() - a.lastModified()));

        if (directoryListing != null) {
            for (File child : directoryListing) {
                String name = child.getName().toLowerCase();
                if (name.endsWith(".json") && (WebServer.dl == null || !name.equals(WebServer.dl.currentFile))) {
                    result.add(name);
                }
            }
        }

        return result;
    }

    HttpServletResponse getLogFile(spark.Request req, spark.Response res) {
        String filename = req.queryParams("filename");
        if (filename == null || filename.equals("")) {
            System.out.println("getLogFile got no filename");
            return null;
        }

        System.out.println("logfile requested: " + filename);
        try {
            byte[] bytes;
            bytes = Files.readAllBytes(Paths.get(WebServer.logPath + filename));
            HttpServletResponse raw = res.raw();
            raw.getOutputStream().write(bytes);
            raw.getOutputStream().flush();
            raw.getOutputStream().close();
            res.header("Content-Type", "application/JSON");
            return raw;
        } catch (Exception e) {
            System.out.println("getLogFile: Exception: " + e);
            return null;
        }
    }

}
