package com.example.seachaos.testlibrary;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.seachaos.hurryporter.HurryPorter;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends ActionBarActivity {

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
                        return json;
                    }

                    @Override
                    public void onSuccess(HurryPorter porter, JSONObject json, String raw) {
                        toast("success");
                        toast(raw);
                    }

                    @Override
                    public void onFailed(HurryPorter porter, String raw) {
                        toast("failed:"+raw);
                    }
                }, "http://www.myandroid.tw/test/post.php");
            }
        });
    }

    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
