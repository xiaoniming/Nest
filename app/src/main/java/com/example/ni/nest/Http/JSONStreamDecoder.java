package com.example.ni.nest.Http;

import android.util.Log;

import com.example.ni.nest.Application;
import com.example.ni.nest.Http.javabeans.ErrorResponse;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created by ni on 2016/8/4.
 */

public class JSONStreamDecoder {

    private Class<?> clazz;
    private String errMsg;
    private String errCode;

    public JSONStreamDecoder(Class<?> clazz) {
        this.clazz = clazz;
    }

    protected static void L(String msg) {
        Log.d("decode stream----->", msg);
    }

    public String getErrMsg() {
        return errMsg;
    }

    public String getErrCode() {
        return errCode;
    }

    public Object decode(InputStream is) {
        String result = null;
        try {
            result = decodeStringFromInputStream(is);
            if (Application.NORMAL_DEBUG) {
                L(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (result != null) {
            return decodeObjectWithGson(result);
        }
        return null;
    }

    private Object decodeObjectWithGson(String json) {
        errCode = null;
        errMsg = null;
        ErrorResponse error;
        try {
            //We get ErrorResponse here
            error = new Gson().fromJson(json, ErrorResponse.class);
            errCode = error.errcode;
            errMsg = error.errmsg;
            if (errCode == null) {
                Object o = new Gson().fromJson(json, clazz);
                return new Gson().fromJson(json, clazz);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        errCode = "err code not defined";
        errMsg = "invalid json format";
        return null;
    }

    public String decodeStringFromInputStream(InputStream is) throws IOException {
        return decodeStringFromInputStream(is, "UTF-8");
    }

    public String decodeStringFromInputStream(InputStream is, String charset) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int index = -1;
        while ((index = is.read(buffer)) != -1) {
            baos.write(buffer, 0, index);
        }
        String result = baos.toString(charset);
        baos.close();
        return result;
    }

}
