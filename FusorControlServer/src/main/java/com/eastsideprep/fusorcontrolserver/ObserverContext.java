/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import static com.eastsideprep.fusorcontrolserver.WebServer.dl;
import static com.eastsideprep.fusorcontrolserver.WebServer.dm;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        if (dl == null) {
            System.out.println("  logging inactive, poking bears ...");
            dm.getAllStatus();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
            }
        }
        String s = dm.readStatusResults(FusorControlServer.config.includeCoreStatus);
        if (FusorControlServer.config.superVerbose) {
            System.out.println("  Status:" + s);
        }
        return s;
    }
}
