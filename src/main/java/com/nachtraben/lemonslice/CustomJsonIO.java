package com.nachtraben.lemonslice;


import com.google.gson.JsonElement;

/**
 * Created by NachtDesk on 7/22/2016.
 */
public interface CustomJsonIO {
    public JsonElement write();
    public void read(JsonElement me);
}
