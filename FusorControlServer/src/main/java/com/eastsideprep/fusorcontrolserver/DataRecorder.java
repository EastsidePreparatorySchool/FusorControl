package com.eastsideprep.fusorcontrolserver;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;

public class DataRecorder {
    FileWriter writer;
    
    public void init() throws IOException {
        //creates time stamp and appends to .json file, then creates a writer and inits headers
        Date date = new Date();
        Instant instant1 = date.toInstant();
        String ts = instant1.toString().replace(":", " ").replace(".", " ");
        String fileName = "Fusor_ " + ts + ".json";
        //makes inits fmile and Writer
        writer = new FileWriter(fileName);
        writer.flush();
    }
    
    public void write(String element) throws IOException {
        writer.append(element);
        writer.append("\n");
        writer.flush();
    }
    
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }
}