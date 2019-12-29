/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import static com.eastsideprep.fusorcontrolserver.WebServer.dl;
import static com.eastsideprep.fusorcontrolserver.WebServer.dm;
import static com.eastsideprep.fusorcontrolserver.WebServer.instance;
import com.eastsideprep.fusorweblog.FusorWebLogEntry;
import com.eastsideprep.weblog.WebLogEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.MultipartConfigElement;

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
        if (dl == null && this.isAdmin) {
            System.out.println("  logging inactive, poking bears ...");
            dm.getAllStatus();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
            }
        }
        ArrayList<WebLogEntry> list = obs.getNewItems();
        if (FusorControlServer.config.superVerbose) {
            System.out.println("Obs: " + obs + ", updates: " + list.size());
        }
        StringBuilder sb = new StringBuilder();
        sb.ensureCapacity(10000);
        sb.append("[");

        for (WebLogEntry e : list) {
            FusorWebLogEntry fe = (FusorWebLogEntry) e;
            sb.append(DataLogger.makeLogResponse(fe.device, fe.serverTime, fe.data));
            sb.append(",\n");
        }

        sb.append("{\"status_complete\":\"");
        sb.append(((new Date()).toInstant().toString()));
        sb.append("\"}]");
        String s = sb.toString();

        if (FusorControlServer.config.superVerbose) {
            System.out.println("  Status:" + s);
        }
        return s;
    }

    String comment(spark.Request req) {
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
        req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

        StringBuilder sb = new StringBuilder(500);
        sb.append("{");
        sb.append("\"observer\":\"");
        sb.append(this.name);
        sb.append("\",\"ip\":\"");
        sb.append(req.ip());
        sb.append("\",\"text\":\"");
        sb.append(req.queryParams("text"));
        sb.append("\"}");
        
        dm.recordStatus("comment", System.currentTimeMillis(), sb.toString());

        return "ok";
    }

}
