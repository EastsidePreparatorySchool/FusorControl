/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;
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
        
        System.out.println("Comment: "+logText);
        WebServer.dm.recordStatus("Comment", millis, logText);

        return "ok";
    }

}
