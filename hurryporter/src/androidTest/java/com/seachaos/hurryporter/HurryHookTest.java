package com.seachaos.hurryporter;

import android.app.Application;
import android.test.ApplicationTestCase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by seachaos on 2016/3/2.
 */
public class HurryHookTest extends ApplicationTestCase<Application> {

    public HurryHookTest() {
        super(Application.class);
    }


    public void testHurryGlobalHooK(){
        // prepare test data

        HurryPorterHook.hookGlobalCheckResponse(new HurryPorterHook.CheckResponse() {
            @Override
            public boolean verifyData(HurryPorter porter, JSONObject json, String raw) throws JSONException {
                if("1".equals(json.getString("request"))){
                    return  true;
                }
                return false;
            }

            @Override
            public String errorMessage(HurryPorter porter, JSONObject json, String raw) throws JSONException {
                return "errorMessage_HPH";
            }
        });

        // do test
        HurryPorter porter = new HurryPorter();
        porter.makeRequestForTest(new HurryPorter.HurryCallback() {
            @Override
            public JSONObject prepare(HurryPorter porter) throws JSONException {
                JSONObject json = porter.getBaseJSON();
                json.put("request", "1");
                return json;
            }

            @Override
            public void onSuccess(HurryPorter porter, JSONObject json, String raw) {
                assertTrue(true);
            }

            @Override
            public void onFailed(HurryPorter porter, String raw , int errorCode) {
                assertTrue(false);
            }
        }, "http://www.myandroid.tw/test/post.php");


        // do test
        porter = new HurryPorter();
        porter.makeRequestForTest(new HurryPorter.HurryCallback() {
            @Override
            public JSONObject prepare(HurryPorter porter) throws JSONException {
                JSONObject json = porter.getBaseJSON();
                json.put("request", "0");
                return json;
            }

            @Override
            public void onSuccess(HurryPorter porter, JSONObject json, String raw) {
                assertTrue(false);
            }

            @Override
            public void onFailed(HurryPorter porter, String raw, int errorCode) {
                assertTrue(true);
                assertTrue("errorMessage_HPH".equals(raw));
            }
        }, "http://www.myandroid.tw/test/post.php");
    }

    public void testPrepareDataHook(){
        HurryPorter porter = new HurryPorter();
        porter.makeRequestForTest(new HurryPorter.HurryCallback() {
            @Override
            public JSONObject prepare(HurryPorter porter) throws JSONException {
                JSONObject json = porter.getBaseJSON();
                json.put("request", "1");
                porter.hookPrepareData = new HurryPorterHook.PrepareData() {
                    @Override
                    public JSONObject willBeSent(HurryPorter porter, JSONObject json) throws JSONException {
                        JSONObject resp = new JSONObject();
                        resp.put("warp_WBS", json);
                        resp.put("request", "1");
                        return resp;
                    }
                };
                return json;
            }

            @Override
            public void onSuccess(HurryPorter porter, JSONObject json, String raw) {
                assertTrue(raw.contains("warp_WBS"));
            }

            @Override
            public void onFailed(HurryPorter porter, String raw, int errorCode) {
                assertTrue(raw.contains("warp_WBS"));

            }
        }, "http://www.myandroid.tw/test/post.php");
    }
}

