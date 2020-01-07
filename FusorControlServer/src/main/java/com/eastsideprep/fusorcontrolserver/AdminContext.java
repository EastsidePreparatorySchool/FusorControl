/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import static com.eastsideprep.fusorcontrolserver.WebServer.cd;
import static com.eastsideprep.fusorcontrolserver.WebServer.cs;
import static com.eastsideprep.fusorcontrolserver.WebServer.dl;
import static com.eastsideprep.fusorcontrolserver.WebServer.dm;
import com.eastsideprep.fusorweblog.FusorWebLogEntry;
import com.eastsideprep.fusorweblog.FusorWebLogState;
import java.io.IOException;
import java.util.HashMap;
import static spark.Spark.halt;
import static spark.Spark.stop;

/**
 *
 * @author gmein
 */
public class AdminContext extends ObserverContext {

    AdminContext(String login, WebServer ws) {
        super(login, ws);
    }

    public void upgradeObserver(String obsname) {
        AdminContext ctx = new AdminContext(obsname, this.ws);
        synchronized (WebServer.class) {
            WebServer.upgrade = ctx;
        }
    }

    @Override
    String comment(spark.Request req) {
        String text = req.queryParams("text");
        
        String upgradeCommand = "#upgrade";

        if (text.startsWith(upgradeCommand)) {
            upgradeObserver(text.substring(upgradeCommand.length()).trim());
        }

        return super.comment(req);
    }

    String killRoute() {
        logAdminCommand("shutdown");
        if (dl != null) {
            dl.shutdown();
        }
        dm.shutdown();

        stop();
        System.out.println("Server ended with /kill");
        System.exit(0);
        return "server ended";
    }

    String startLogRoute() {
        synchronized (ws) {
            if (dl != null) {
                dl.shutdown();
            }
            dl = new DataLogger();
            try {
                dl.init(dm, cs);
            } catch (IOException ex) {
                System.out.println("startLog IO exception: " + ex);
            }

            WebServer.log.clear(new FusorWebLogState(), new FusorWebLogEntry("<reset>", System.currentTimeMillis(), "{}"));

            dm.autoStatusOn();
            System.out.println("New log started");
            return "log started";

        }
    }

    String stopLogRoute() {
        logAdminCommand("stopLog");
        synchronized (ws) {
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
    }

    String variacRoute(spark.Request req) {
        int variacValue = Integer.parseInt(req.queryParams("value"));
        logAdminCommand("variac:set:" + variacValue);
        System.out.println("Received Variac Set " + variacValue);
        if (cd.variac.setVoltage(variacValue)) {
            return "set value as " + req.queryParams("value");
        }
        throw halt("Variac control failed");
    }

    String variacStop(spark.Request req) {
        int value = Integer.parseInt(req.queryParams("value"));
        System.out.println("Received Variac Stop " + value);
        if (cd.variac.set("stop", value)) {
            return "variac stop " + (value == 0 ? "(emergency)" : "(regular)");
        } else {
            throw halt("Variac stop failed");
        }
    }

    String tmpOnRoute() {
        logAdminCommand("TMP on");
        if (cd.tmp.setOn()) {
            return "turned on TMP";
        }
        throw halt(500, "TMP control failed");
    }

    String tmpOffRoute() {
        logAdminCommand("TMP off");
        if (cd.tmp.setOff()) {
            return "turned off TMP";
        }
        throw halt(500, "TMP control failed");
    }

    String solenoidOnRoute() {
        logAdminCommand("Solenoid open");

        if (cd.gas.setOpen()) {
            return "set solenoid to open";
        }
        throw halt(500, "set solenoid failed");
    }

    String solenoidOffRoute() {
        logAdminCommand("Solenoid closed");

        if (cd.gas.setOpen()) {
            return "set solenoid to open";
        }
        throw halt(500, "set solenoid failed");
    }

    String needleValveRoute(spark.Request req) {
        int value = Integer.parseInt(req.queryParams("value"));
        logAdminCommand("Set needle valve:" + value);
        System.out.println("Received needle valve Set " + value);
        if (cd.needle.set("needlevalve", value)) {
            System.out.println("needle valve success");
            return "set needle valve value as " + value;
        }
        System.out.println("needle valve fail");
        throw halt(500, "set needle valve failed");
    }

    void logAdminCommand(String command) {
        long millis = System.currentTimeMillis();
        dm.recordStatus("Command", millis, DataLogger.makeAdminCommandText(this.name, this.ip, command, millis));

    }
}
