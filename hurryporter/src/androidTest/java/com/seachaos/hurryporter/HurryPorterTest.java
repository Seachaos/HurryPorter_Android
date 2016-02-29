package com.seachaos.hurryporter;

import android.app.Application;
import android.test.ApplicationTestCase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class HurryPorterTest extends ApplicationTestCase<Application> {
    public HurryPorterTest() {
        super(Application.class);
    }

    public void testHttpWand(){
        HttpWand wand = new HttpWand();
        String resp = wand.send("http://www.myandroid.tw/test/post.php");
        assertNotNull(resp);
        assertFalse(resp.startsWith(HurryPorter.ERROR_TAG));
    }

    public void testHttpPost(){
        HttpWand wand = new HttpWand();
        wand.addPost("FirstName","Hurry");
        wand.addPost("LastName","Porter");
        String resp = wand.send("http://www.myandroid.tw/test/post.php");
        assertNotNull(resp);
        assertFalse(resp.startsWith(HurryPorter.ERROR_TAG));
    }

    public void testHurryPorterBasic(){
        // prepare test data
        final JSONObject testJSON = new JSONObject();
        try {
            testJSON.put("name","test");
            testJSON.put("int", 1234);
            testJSON.put("double", 123.456);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // do test
        HurryPorter porter = new HurryPorter();
        porter.makeRequestForTest(new HurryPorter.HurryCallback() {
            @Override
            public JSONObject prepare(HurryPorter porter) throws JSONException {
                return testJSON;
            }

            @Override
            public void onSuccess(HurryPorter porter, JSONObject json, String raw) {
                assertTrue(raw!=null);
            }

            @Override
            public void onFailed(HurryPorter porter, String raw) {
                assertFalse(true);
            }
        }, "http://www.myandroid.tw/test/post.php");
    }
}