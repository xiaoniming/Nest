package com.example.ni.nest;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final TextView mMessageWindow = (TextView) findViewById(R.id.tv_show);
        final EditText mInputPanle = (EditText) findViewById(R.id.edit_input);
        final Button mSendButton = (Button) findViewById(R.id.button_send);

        HttpUtils.sendHttpRequestGet("http://192.168.0.118:8080/NestServer/TestServlet", new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                Log.v("mainactivity----->", response);
                 mMessageWindow.append(response + "\n");//DO NOT DO THIS!! DubThread not UI
            }

            @Override
            public void onError(Exception e) {
                //mInputPanle.setText("Error: " + e.getMessage());
                Log.v("mainactivity----->", "error " + e.getMessage());
            }
        });

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
