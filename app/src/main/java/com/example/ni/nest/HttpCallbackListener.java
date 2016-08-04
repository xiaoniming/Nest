package com.example.ni.nest;

/**
 * Created by Ni on 2016/8/2.
 */
public interface HttpCallbackListener<T> {
    void onFinish(T t);
    void onFail(String code, String msg);
}
