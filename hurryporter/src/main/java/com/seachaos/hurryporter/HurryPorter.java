package com.seachaos.hurryporter;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.Runnable;
import java.util.Iterator;

public class HurryPorter{
    public static final String ERROR_TAG = "[HP_ERR]";

    public static String Charset = "UTF-8";
    private HurryCallback userCallback = null;
    private Handler handler;
    private boolean useHandler = true;

    public HurryPorterHook.PrepareData hookPrepareData = null;
    public HurryPorterHook.CheckResponse hookCheckResponse = null;

    public String errorMessage;

    public interface HurryCallback{
        public JSONObject prepare(HurryPorter porter) throws JSONException;
        public void onSuccess(HurryPorter porter, JSONObject json, String raw);
        public void onFailed(HurryPorter porter, String raw);
    }

    public HurryPorter(){
        initHurry();
    }

    private void initHurry(){
        this.hookPrepareData = HurryPorterHook.globalPrepareData;
        this.hookCheckResponse = HurryPorterHook.globalCheckResponse;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void makeRequest(final HurryCallback callback, final String url){
        useHandler = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                _makeRequest(new HurryCallback() {
                    @Override
                    public JSONObject prepare(HurryPorter porter) throws JSONException {
                        return callback.prepare(porter);
                    }
                    @Override
                    public void onSuccess(final HurryPorter porter,final JSONObject json,final String raw)  {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(porter,json, raw);
                            }
                        });
                    }

                    @Override
                    public void onFailed(HurryPorter porter, String raw) {
                        onFailedCallback(raw);
                    }
                }, url);
            }
        }).start();
    }

    public void makeRequestForTest(HurryCallback callback, String url){
        useHandler = false;
        _makeRequest(callback, url);
    }

    private void _makeRequest(HurryCallback callback, String url){
        this.userCallback = callback;
        HttpWand wand = new HttpWand();
        JSONObject json = null;
        try {
            json = callback.prepare(this);
        } catch (JSONException e) {
            onFailedCallback(ERROR_TAG + " prepare data failed:" + e.toString());
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
            try {
                onReceiveResponse(callback, resp);
            } catch (JSONException e) {
                onFailedCallback(ERROR_TAG + " " + e.toString());
            }
            return;
        }
        onFailedCallback(ERROR_TAG + " response is null");
    }

    private void onReceiveResponse(HurryCallback callback, String resp) throws JSONException {
        if(userCallback==null){
            return;
        }
        JSONObject json = null;
        try {
            json = new JSONObject(resp);
        } catch (JSONException e) {
        }
        if(hookCheckResponse!=null){
            if(hookCheckResponse.verifyData(this, json, resp)){
                callback.onSuccess(this, json, resp);
            }else{
                if(errorMessage==null){
                    errorMessage = hookCheckResponse.errorMessage(this, json, resp);
                }
                onFailedCallback(errorMessage);
            }
            return;
        }
        callback.onSuccess(this, json, resp);
    }

    private void onFailedCallback(final String reason){
        if(userCallback==null){
            return;
        }
        if(useHandler){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    userCallback.onFailed(HurryPorter.this, reason);
                }
            });
            return;
        }
        userCallback.onFailed(this, reason);
    }

    public JSONObject getBaseJSON(){
        return new JSONObject();
    }

}