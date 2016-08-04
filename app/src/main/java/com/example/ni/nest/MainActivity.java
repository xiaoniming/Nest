package com.example.ni.nest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.ni.nest.Http.javabeans.UserCommentResponse;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final static String TAG = "MainActivity----->";
    HttpService mHttpService;

    Button mScanButton;
    Button mBookButton;
    Button mFeedbackButton;

    private static void L(String msg) {
        Log.d(TAG, msg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mScanButton = (Button) findViewById(R.id.bt_scan);
        mBookButton = (Button) findViewById(R.id.bt_book);
        mFeedbackButton = (Button) findViewById(R.id.bt_feedback);
        mScanButton.setOnClickListener(this);
        mBookButton.setOnClickListener(this);
        mFeedbackButton.setOnClickListener(this);


        mHttpService = HttpService.get();
        String url = "http://192.168.0.118:8080/NestServer/TestServlet";
        mHttpService.executeJsonGetAsync(url, UserCommentResponse.class, new
                HttpCallbackListener<UserCommentResponse>() {
                    @Override
                    public void onFinish(UserCommentResponse userCommentResponse) {
                        Log.v("mainactivity----->", userCommentResponse.toString());
                    }

                    @Override
                    public void onFail(String code, String msg) {
                        Log.v("mainactivity----->", "error code" + code + "  errMsg: " + msg);
                    }
                });

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.bt_book:
                //TODO
                break;
            case R.id.bt_scan:
                int requestCode = Cons.REQUEST_SCAN;
                Intent intent = new Intent(this, ScaneActivity.class);
                intent.putExtra(Cons.ARG_ACTION, Cons.ACTION_CAPTURE);
                startActivityForResult(intent, requestCode);
                break;
            case R.id.bt_feedback:
                //TODO
                break;
            default:
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Cons.REQUEST_SCAN:
                if (resultCode == RESULT_OK) {

                    String authCode = data.getStringExtra(Cons.ARG_SCAN_RESULT);
                    //TODO Print userInfo
                    L("Scan succeed: " + authCode);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
