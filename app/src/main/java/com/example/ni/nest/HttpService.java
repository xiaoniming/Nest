package com.example.ni.nest;

import android.os.AsyncTask;
import android.util.Log;

import com.example.ni.nest.Http.JSONStreamDecoder;
import com.example.ni.nest.Http.RequestMethod;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import static com.example.ni.nest.Application.NORMAL_DEBUG;

/**
 * Created by Ni on 2016/8/2.
 */
public class HttpService {
    public static final int CONNECTION_TIMEOUT = 5000;
    public static final int READ_TIMEOUT = 10000;
    private static final String TAG = "HttpService----->";

    private static HttpService sINSTANCE;

    public static synchronized HttpService get() {
        if (sINSTANCE == null) {
            sINSTANCE = new HttpService();
        }
        return sINSTANCE;
    }


    private static void setPropertiesForGet(HttpURLConnection conn) throws ProtocolException {
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setUseCaches(true);
    }

    private static void setPropertiesForPost(HttpURLConnection conn) throws ProtocolException {
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(true);
    }

    private static void L(String msg) {
        Log.d(TAG, msg);
    }

    public <T> T executeJsonPostSync(String url, String msg, Class<T> clazz) {
        return (T) executeHttpRequestSync(new JSONStreamDecoder(clazz), RequestMethod.POST, url, msg);
    }

    public <T> T executeJsonGetSync(String url, Class<T> clazz) {
        return (T) executeHttpRequestSync(new JSONStreamDecoder(clazz), RequestMethod.GET, url, null);
    }

    public <T> void executeJsonPostAsync(String url, String msg, Class<T> clazz, final HttpCallbackListener<T>
            callback) {
        executeHttpTaskAsyn(new JSONStreamDecoder(clazz), RequestMethod.POST, url, msg, callback);
    }

    public <T> void executeJsonGetAsync(String url, Class<T> clazz, final HttpCallbackListener<T> callback) {
        executeHttpTaskAsyn(new JSONStreamDecoder(clazz), RequestMethod.GET, url, null, callback);
    }

    public <T> void executeHttpTaskAsyn(JSONStreamDecoder decoder, RequestMethod method, String url, String msg,
                                        final HttpCallbackListener<T> callback) {
        if (callback == null) {
            throw new RuntimeException("Should provide a callback!");
        }
        HttpRequestTask<T> httpTask = new HttpRequestTask<>(callback, method, decoder);
        httpTask.execute(url, msg);
    }

    /**
     * This Method is sync, need to be called in a subThread!!!
     *
     * @param decoder
     * @param method
     * @param url
     * @param msg
     * @return
     */

    private Object executeHttpRequestSync(JSONStreamDecoder decoder, RequestMethod method, String url, String
            msg) {
//        if (method == RequestMethod.GET) {
//            url += ("?method=GET");
//        } else if (method == RequestMethod.POST) {
//            url += ("?method=POST");
//        }
        if (NORMAL_DEBUG) {
            L("url = " + url);
        }

        Object o = null;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            if (method == RequestMethod.GET) {
                setPropertiesForGet(con);
            } else if (method == RequestMethod.POST) {
                setPropertiesForPost(con);
                DataOutputStream dos = new DataOutputStream(con.getOutputStream());
                dos.write(msg.getBytes("UTF-8"));
                dos.flush();
                dos.close();
            }

            int respondeCode = con.getResponseCode();
            if (respondeCode == HttpURLConnection.HTTP_OK) {
                InputStream in = con.getInputStream();
                o = decoder.decode(in);
                in.close();
            } else {
                if (NORMAL_DEBUG) {
                    L("response code = " + respondeCode);
                    InputStream is = con.getErrorStream();
                    L("error response = " + decoder.decodeStringFromInputStream(is));
                    is.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) con.disconnect();
        }
        return o;
    }

    private class HttpRequestTask<T> extends AsyncTask<String, Void, T> {
        private WeakReference<HttpCallbackListener> callback;
        private RequestMethod method;
        private JSONStreamDecoder decoder;

        public HttpRequestTask(HttpCallbackListener callback, RequestMethod method, JSONStreamDecoder decoder) {
            this.callback = new WeakReference(callback);
            this.decoder = decoder;
            this.method = method;
        }

        @Override
        protected T doInBackground(String... params) {
            T t = (T) executeHttpRequestSync(decoder, method, params[0], params[1]);
            if (!isCancelled() && callback.get() != null) {
                if (t != null) {
                    callback.get().onFinish(t);
                } else if (decoder.getErrCode() != null) {
                    callback.get().onFail(decoder.getErrCode(), decoder.getErrMsg());
                } else {
                    callback.get().onFail("err code", "time out");
                }
                return t;
            }
            return null;
        }

        @Override
        protected void onPostExecute(T t) {

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }


}


