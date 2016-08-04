package com.example.ni.nest;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by Ni on 2016/8/2.
 */
public class HttpUtils {
    private static final String MSG = "HttpUtils----->";

    public static void sendHttpRequestGet(final String address, final HttpCallbackListener listener) {
        if (listener == null) {
            Log.v(MSG, "listener cannot be null");
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                try {
                    URL url = new URL(address);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setReadTimeout(8000);
                    con.setConnectTimeout(8000);
                    con.setRequestProperty("contentType", "GBK");

                    int responseCode = con.getResponseCode();
                    if (responseCode == 200) {
                        InputStream in = con.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "GBK"));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();
                        Log.v(MSG, response.toString());
                        listener.onFinish(response.toString());
                    } else {
                        listener.onError(new Exception("Response code: " + responseCode));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onError(e);
                } finally {
                    if (con != null) con.disconnect();
                }
            }
        }).start();
    }

    //TODO not enough!

    public static void sendHttpRequestPost(final String address, final String msg, final HttpCallbackListener listener) {
        if (listener == null) {
            Log.v(MSG, "listener cannot be null");
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                try {
                    URL url = new URL(address);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setReadTimeout(8000);
                    con.setConnectTimeout(8000);
                    con.setDoInput(true);
                    con.setDoOutput(true);

                    // con.setRequestProperty("Connection", "keep-alive");
                    OutputStream os = con.getOutputStream();
                    os.write(URLEncoder.encode(msg, "UTF-8").getBytes());
                    os.flush();
                    os.close();
                    int respondeCode = con.getResponseCode();
                    if (respondeCode == 200) {
                        InputStream in = con.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        listener.onFinish(response.toString());
                    } else {
                        listener.onError(new Exception("Response code: " + respondeCode));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onError(e);
                } finally {
                    if (con != null) con.disconnect();
                }
            }
        }).start();
    }


}


