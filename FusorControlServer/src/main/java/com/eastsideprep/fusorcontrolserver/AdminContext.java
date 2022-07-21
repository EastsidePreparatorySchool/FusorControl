package com.eastsideprep.fusorcontrolserver;

import static com.eastsideprep.fusorcontrolserver.WebServer.cd;
import static com.eastsideprep.fusorcontrolserver.WebServer.cs;
import static com.eastsideprep.fusorcontrolserver.WebServer.dl;
import static com.eastsideprep.fusorcontrolserver.WebServer.dm;
import com.eastsideprep.fusorweblog.FusorWebLogEntry;
import com.eastsideprep.fusorweblog.FusorWebLogState;
import static spark.Spark.halt;
import static spark.Spark.stop;

public class AdminContext extends ObserverContext {

    AdminContext(String login, WebServer ws) {
        super(login, ws);
    }

    public void upgradeObserver(String obsname) {
        AdminContext ctx = new AdminContext(obsname, this.ws);
        ctx.name = ctx.login;
        synchronized (WebServer.class) {
            WebServer.upgrade = ctx;
        }
    }

    @Override
    String comment(spark.Request req) {
        String text = req.queryParams("text");

        String upgradeCommand = "#promote";
        String devicesCommand = "#devices";

        if (text.startsWith(upgradeCommand) 
                && (this.login.equals("gmein") 
                || this.login.equals("klewellen") 
                || this.login.equals("SYSTEM") 
                || this.login.equals("chuck"))) {
            String obs = text.substring(upgradeCommand.length()).trim();
            upgradeObserver(obs);
            logAdminCommand("#promote: " + obs);
            return "promotion scheduled";
        } else if (text.equals(devicesCommand)) {
            logAdminCommand("#devices: " + dm.getAllDeviceNames());
            return "ok";
        }

        return super.comment(req);
    }

    String killRoute() {
        if (this.login.equals("gmein")) {
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
        throw halt(401);
    }

    String getOneStatusRoute() {
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

    String startLogRoute(spark.Request req) {
        synchronized (ws) {
            if (dl != null) {
                dl.shutdown();
            }
            WebServer.log.clear(new FusorWebLogState(), new FusorWebLogEntry("<reset>", System.currentTimeMillis(), "{}"));
            dl = new DataLogger();
            dl.init(dm, cs, req.queryParams("filename"));

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
        
        if (!cd.variac.set("stop", true)) {
            throw halt("Variac stop failed");
        }
        
        if (cd.variac.setVoltage(variacValue)) {
            return "set value as " + req.queryParams("value");
        }
        throw halt("Variac control failed");
    }

    String variacStop(spark.Request req) {
        System.out.println("Received Variac Stop ");
        if (cd.variac.set("stop", true)) {
            return "variac stop ";
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

    String tmpLowRoute() {
        logAdminCommand("TMP low");
        if (cd.tmp.setLow()) {
            System.out.println("low speed success");
            return "set TMP to low speed";
        }
        System.out.println("low speed failed");
        throw halt(500, "TMP control failed");
    }

    String tmpHighRoute() {
        logAdminCommand("TMP high");
        if (cd.tmp.setHigh()) {
            System.out.println("high speed success");
            return "set TMP to high speed";
        }
        System.out.println("high speed failed");
        throw halt(500, "TMP control failed");
    }

    String hvOnRoute() {
        logAdminCommand("HV on");
        if (cd.hvrelay.on()) {
            return "turned on HV relay";
        }
        throw halt(500, "HV control failed");
    }

    String hvOffRoute() {
        logAdminCommand("HV off");
        if (cd.hvrelay.off()) {
            return "turned off hv relay";
        }
        throw halt(500, "HV control failed");
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

        if (cd.gas.setClosed()) {
            return "set solenoid to closed";
        }
        throw halt(500, "set solenoid failed");
    }

    String needleValveRoute(spark.Request req) {
        float value = Float.parseFloat(req.queryParams("value"));
        logAdminCommand("Set needle valve:" + value);
        System.out.println("Received needle valve Set " + value);
        if (cd.gas.set("nv_in", value)) {
            System.out.println("needle valve success");
            return "set needle valve value as " + value;
        }
        System.out.println("needle valve fail");
        throw halt(500, "set needle valve failed");
    }

    void logAdminCommand(String command) {
        System.out.println("Admin cmd: " + command);
        long millis = System.currentTimeMillis();
        dm.recordStatus("Command", millis, DataLogger.makeAdminCommandText(this.name, this.ip, command, millis));
    }
}
