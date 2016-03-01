package com.seachaos.hurryporter;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by seachaos on 2016/3/2.
 */
public class HurryPorterHook {
    static PrepareData globalPrepareData = null;
    static CheckResponse globalCheckResponse = null;


    public static void hookGlobalPrepareData(HurryPorterHook.PrepareData hook){
        globalPrepareData = hook;
    }
    public static void hookGlobalCheckResponse(HurryPorterHook.CheckResponse hook){
        globalCheckResponse = hook;
    }

    public interface PrepareData{
        public JSONObject willBeSent(HurryPorter porter, JSONObject json) throws JSONException;
    }

    public interface CheckResponse{
        public boolean verifyData(HurryPorter porter, JSONObject json, String raw) throws JSONException;
        public String errorMessage(HurryPorter porter, JSONObject json, String raw)  throws JSONException;
    }
}
