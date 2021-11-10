package com.eastsideprep.fusorcontrolserver;

import com.eastsideprep.weblog.WebLogObserver;

public class Context {
    String login;
    boolean isAdmin;
    String name;
    String clientID;
    String ip;
    long timeLastSeen;
    WebServer ws;
    WebLogObserver obs;

    Context(String login, WebServer ws) {
        this.login = login;
        this.name = null;
        this.timeLastSeen = System.currentTimeMillis();
        this.ws = ws;
    }

    void updateTimer() {
        timeLastSeen = System.currentTimeMillis();
    }

    boolean checkExpired() {
        return (System.currentTimeMillis() - timeLastSeen >= (5 * 60 * 1000)); // if it has been more than 5 minutes
    }
}
