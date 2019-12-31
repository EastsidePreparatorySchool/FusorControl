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

    private String makeCommentDeviceText(String observer, String ip, String text, long millis) {
         StringBuilder sb = DataLogger.startPseudoDeviceEntry(1000);
        DataLogger.addPseudoDeviceStringVariable(sb, "observer", observer, millis);
        DataLogger.addPseudoDeviceStringVariable(sb, "ip", ip, millis);
        DataLogger.addPseudoDeviceStringVariable(sb, "text", text, millis);
        return DataLogger.closePseudoDeviceEntry(sb, millis);
    }

    String comment(spark.Request req) {
//        System.out.println(""+req.body());
        
//        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
//        req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
//        
//        String body = req.body();

        String text = req.queryParams("text");
        String ip = req.ip();
        
        long millis = System.currentTimeMillis();
        String logText = makeCommentDeviceText(this.name, ip, text, millis);
        
        System.out.println("Comment: "+logText);
        WebServer.dm.recordStatus("Comment", millis, logText);

        return "ok";
    }

}
