package com.seachaos.hurryporter;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.Runnable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public class HurryPorter{
    public static final String ERROR_TAG = "[HP_ERR]";

    public static String Charset = "UTF-8";
    public static boolean beforePostUseURLEncode = false;
    private HurryCallback userCallback = null;
    private Handler handler;
    private boolean useHandler = true;

    public int errorCode = 0;
    public int httpStatusCode = 0;
    public JSONObject responseJSON;
    public String responseContent;

    public HurryPorterHook.PrepareData hookPrepareData = null;
    public HurryPorterHook.CheckResponse hookCheckResponse = null;

    public String errorMessage;

    public interface HurryCallback{
        public JSONObject prepare(HurryPorter porter) throws JSONException;
        public void onSuccess(HurryPorter porter, JSONObject json, String raw);
        public void onFailed(HurryPorter porter, String raw, int errorCode);
    }

    public HurryPorter(){
        initHurry();
    }

    private void initHurry(){
        this.hookPrepareData = HurryPorterHook.globalPrepareData;
        this.hookCheckResponse = HurryPorterHook.globalCheckResponse;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public static String MD5(String msg){
        MessageDigest md5 = null;
        try
        {
            md5 = MessageDigest.getInstance("MD5");
        }catch(Exception e)
        {
            e.printStackTrace();
            return "";
        }

        char[] charArray = msg.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for(int i = 0; i < charArray.length; i++)
        {
            byteArray[i] = (byte)charArray[i];
        }
        byte[] md5Bytes = md5.digest(byteArray);

        StringBuffer hexValue = new StringBuffer();
        for( int i = 0; i < md5Bytes.length; i++)
        {
            int val = ((int)md5Bytes[i])&0xff;
            if(val < 16)
            {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    public static String SHA256(String msg){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(msg.getBytes("UTF-8"));
            byte[] digest = md.digest();
            return  String.format("%064x", new java.math.BigInteger(1, digest));
        } catch (UnsupportedEncodingException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
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
                    public void onFailed(final HurryPorter porter, final String raw, final int errorCode) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailed(porter, raw, errorCode);
                            }
                        });
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
            if(hookPrepareData!=null){
                json = hookPrepareData.willBeSent(this, json);
            }
        } catch (JSONException e) {
            onFailedCallback(ERROR_TAG + " prepare data failed:" + e.toString(), this.errorCode);
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
                }else if(obj instanceof Integer) {
                    int value = (int) obj;
                    wand.addPost(name, value);
                }else if(obj instanceof JSONObject){
                    JSONObject value = (JSONObject) obj;
                    wand.addPost(name, value.toString());
                }else if(obj instanceof JSONArray){
                    JSONArray value = (JSONArray) obj;
                    wand.addPost(name, value.toString());
                }else{
                    String value = json.getString(name);
                    wand.addPost(name, value);
                }
            } catch (JSONException e) {
            }
        }
        String resp = wand.send(url);
        this.httpStatusCode = wand.httpStatusCode;
        if(resp!=null){
            try {
                onReceiveResponse(callback, resp);
            } catch (JSONException e) {
                onFailedCallback(ERROR_TAG + " " + e.toString(), this.errorCode);
            }
            return;
        }
        onFailedCallback(ERROR_TAG + " response is null", this.errorCode);
    }

    private void onReceiveResponse(HurryCallback callback, String resp) throws JSONException {
        if(userCallback==null){
            return;
        }
        responseContent = resp;
        JSONObject json = null;
        try {
            json = new JSONObject(resp);
        } catch (JSONException e) {
        }
        responseJSON = json;
        if(hookCheckResponse!=null){
            if(hookCheckResponse.verifyData(this, json, resp)){
                callback.onSuccess(this, json, resp);
            }else{
                if(errorMessage==null){
                    errorMessage = hookCheckResponse.errorMessage(this, json, resp);
                }
                onFailedCallback(errorMessage, this.errorCode);
            }
            return;
        }
        callback.onSuccess(this, json, resp);
    }

    private void onFailedCallback(final String reason, final int _errorCode){
        if(userCallback==null){
            return;
        }
        this.errorCode = _errorCode;
        if(useHandler){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    userCallback.onFailed(HurryPorter.this, reason, _errorCode);
                }
            });
            return;
        }
        userCallback.onFailed(this, reason, _errorCode);
    }

    public JSONObject getBaseJSON(){
        return new JSONObject();
    }

}