package com.example.ni.nest;

/**
 * Created by Ni on 2016/8/2.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
