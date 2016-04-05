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

    public void testHttpsPost(){
        String target = "https://httpbin.org/post";
        HttpWand wand = new HttpWand();
        wand.addPost("FirstName","Hurry");
        wand.addPost("LastName","Porter");
        String resp = wand.send(target);
        assertNotNull(resp);
        assertFalse(resp.startsWith(HurryPorter.ERROR_TAG));
    }

    public void testHurryPorterBasic(){
        // prepare test data
        final JSONObject testJSON = getTestJSON();

        // do test
        HurryPorter porter = new HurryPorter();
        porter.makeRequestForTest(new HurryPorter.HurryCallback() {
            @Override
            public JSONObject prepare(HurryPorter porter) throws JSONException {
                return testJSON;
            }

            @Override
            public void onSuccess(HurryPorter porter, JSONObject json, String raw) {
                assertTrue(raw != null);
            }

            @Override
            public void onFailed(HurryPorter porter, String raw, int errorCode) {
                assertFalse(true);
            }
        }, "http://www.myandroid.tw/test/post.php");
    }

    public void testHurryHookAlwaysFailed(){
        // prepare test data
        final JSONObject testJSON = getTestJSON();

        // do test
        HurryPorter porter = new HurryPorter();
        porter.makeRequestForTest(new HurryPorter.HurryCallback() {
            @Override
            public JSONObject prepare(HurryPorter porter) throws JSONException {
                porter.hookCheckResponse = new HurryPorterHook.CheckResponse() {
                    String msg = null;
                    @Override
                    public boolean verifyData(HurryPorter porter, JSONObject json, String raw) throws JSONException {
                        msg = "just error";
                        return false;
                    }
                    @Override
                    public String errorMessage(HurryPorter porter, JSONObject json, String raw) throws JSONException {
                        return msg;
                    }
                };

                return testJSON;
            }

            @Override
            public void onSuccess(HurryPorter porter, JSONObject json, String raw) {
                assertTrue(false);
            }

            @Override
            public void onFailed(HurryPorter porter, String raw, int errorCode) {
                assertTrue(true);
                assertTrue("just error".equals(raw));
            }
        }, "http://www.myandroid.tw/test/post.php");
    }

    public JSONObject getTestJSON() {
        JSONObject testJSON = new JSONObject();
        try {
            testJSON.put("name","test");
            testJSON.put("int", 1234);
            testJSON.put("double", 123.456);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return testJSON;
    }

    public void testSHA256(){
        assertTrue("2c5bca2c7e6e42730134edb68fa01461d95216f7742799daa1f1314c8d7e207e".equals(HurryPorter.SHA256("aabbccdd")));
        assertTrue("d06e175d036e74ed0254d4a9bc59481745aaa0c197322b58f5d1b006d8106738".equals(HurryPorter.SHA256("f7742799")));
        assertFalse("d05e175d036e74ed0254d4a9bc59481745aaa0c197322b58f5d1b006d8106738".equals(HurryPorter.SHA256("f7742799")));

    }

    public void testMD5(){
        assertTrue("5734dadda4603f25bf4b4516927a87fb".equals(HurryPorter.MD5("here md5")));
        assertTrue("1cb251ec0d568de6a929b520c4aed8d1".equals(HurryPorter.MD5("text")));
        assertTrue("d41d8cd98f00b204e9800998ecf8427e".equals(HurryPorter.MD5("")));
    }
}