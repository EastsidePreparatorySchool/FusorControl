package com.eastsideprep.fusorcontrolserver;

import com.google.gson.*;
import spark.*;

public class JSONRT implements ResponseTransformer {

    final static private Gson gson = new Gson();

    @Override
    public String render(Object o) {
        return gson.toJson(o);
    }

}