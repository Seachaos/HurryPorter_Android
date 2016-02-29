package com.example.seachaos.examplehurryporter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.seachaos.hurryporter.HurryPorter;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        makeTestConnectionButton();
    }


    private void makeTestConnectionButton(){
        Button connectTest = (Button) findViewById(R.id.connectTest);
        connectTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast("Start connection...");
                HurryPorter porter = new HurryPorter();
                porter.makeRequest(new HurryPorter.HurryCallback() {
                    @Override
                    public JSONObject prepare(HurryPorter porter) throws JSONException {
                        JSONObject json = porter.getBaseJSON();
                        json.put("name", "value");
                        json.put("from", "EXAMPLE");
                        return json;
                    }

                    @Override
                    public void onSuccess(HurryPorter porter, JSONObject json, String raw) {
                        toast("success");
                        toast(raw);
                    }

                    @Override
                    public void onFailed(HurryPorter porter, String raw) {
                        toast("failed:" + raw);
                    }
                }, "http://www.myandroid.tw/test/post.php");
            }
        });
    }

    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
