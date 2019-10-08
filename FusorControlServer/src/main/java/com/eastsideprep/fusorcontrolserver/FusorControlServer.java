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

    //static CamStreamer cs;
    static WebServer serv = new WebServer();

    public static void main(String[] args) {
        //serv = new WebServer();
        serv.initClient();
        //cs = new CamStreamer();
        serv.initPorts();
        serv.test();
    }
}
