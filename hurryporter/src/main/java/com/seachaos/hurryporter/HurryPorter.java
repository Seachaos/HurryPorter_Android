package com.seachaos.hurryporter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class HurryPorter{
    public static final String ERROR_TAG = "[HP_ERR]";

    public static String Charset = "UTF-8";
    private HurryCallback userCallback = null;

    public interface HurryCallback{
        public JSONObject prepare(HurryPorter porter) throws JSONException;
        public void onSuccess(HurryPorter porter, JSONObject json, String raw);
        public void onFailed(HurryPorter porter, String raw);
    }

    public void makeRequest(final HurryCallback callback, final String url){
        new Thread(new Runnable() {
            @Override
            public void run() {
                _makeRequest(new HurryCallback() {
                    @Override
                    public JSONObject prepare(HurryPorter porter) throws JSONException {
                        return callback.prepare(porter);
                    }
                    @Override
                    public void onSuccess(HurryPorter porter, JSONObject json, String raw)  {
                        callback.onSuccess(porter, json, raw);
                    }

                    @Override
                    public void onFailed(HurryPorter porter, String raw) {
                        callback.onFailed(porter, raw);
                    }
                }, url);
            }
        });
    }

    public void makeRequestForTest(HurryCallback callback, String url){
        _makeRequest(callback, url);
    }

    private void _makeRequest(HurryCallback callback, String url){
        this.userCallback = callback;
        HttpWand wand = new HttpWand();
        JSONObject json = null;
        try {
            json = callback.prepare(this);
        } catch (JSONException e) {
            onFailed(ERROR_TAG+" prepare data failed:"+e.toString());
            return;
        }
        Iterator<String> keys = json.keys();
        // prepare datas
        while(keys.hasNext()){
            String name = keys.next();
            try {
                Object obj = json.get(name);
                if(obj instanceof String){
                    String value = (String) obj;
                    wand.addPost(name, value);
                }else if(obj instanceof Integer){
                    int value = (int) obj;
                    wand.addPost(name, value);
                }else{
                    String value = json.getString(name);
                    wand.addPost(name, value);
                }
            } catch (JSONException e) {
            }
        }
        String resp = wand.send(url);
        if(resp!=null){
            onReceiveResponse(callback, resp);
            return;
        }
        onFailed(ERROR_TAG + " response is null");
    }

    private void onReceiveResponse(HurryCallback callback, String resp) {
        if(userCallback==null){
            return;
        }
        JSONObject json = null;
        try {
            json = new JSONObject(resp);
        } catch (JSONException e) {
        }
        callback.onSuccess(this, json, resp);
    }

    private void onFailed(String reason){
        if(userCallback==null){
            return;
        }
        userCallback.onFailed(this, reason);
    }

    public JSONObject getBaseJSON(){
        return new JSONObject();
    }

}