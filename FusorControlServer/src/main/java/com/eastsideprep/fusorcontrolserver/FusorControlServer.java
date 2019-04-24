/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

/**
 *
 * @author Administrator
 */
public class FusorControlServer {
    public static void main(String[] args) {
        WebServer serv = new WebServer();
        serv.init();
    }
}