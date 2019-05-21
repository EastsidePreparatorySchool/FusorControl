/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

/**
 *
 * @author fzhang
 */
public class Status {
    int arconValue;
    String tmpv;
    String tmps;
    
    public Status(int arconValue, String tmpv, String tmps) {
        this.arconValue = arconValue;
        this.tmpv = tmpv;
        this.tmps = tmps;
    }
}
