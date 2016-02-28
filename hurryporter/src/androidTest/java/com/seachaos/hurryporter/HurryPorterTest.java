package com.seachaos.hurryporter;

import android.app.Application;
import android.test.ApplicationTestCase;

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
}