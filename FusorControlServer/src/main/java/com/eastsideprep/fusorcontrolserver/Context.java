/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

/**
 *
 * @author gmein
 */
public class Context {
    int messagesSeen;
    String login;
    boolean isAdmin;
    int id;
    String name;
    long timeLastSeen;

    Context(String login) {
        this.login = login;
        this.id = 0;
        this.name = null;
        this.timeLastSeen = System.currentTimeMillis();
        this.isAdmin = false;
    }

    void updateTimer() {
        timeLastSeen = System.currentTimeMillis();
    }

    boolean checkExpired() {
        return (System.currentTimeMillis() - timeLastSeen >= (5 * 60 * 1000)); // if it has been more than 5 minutes
    }
}
