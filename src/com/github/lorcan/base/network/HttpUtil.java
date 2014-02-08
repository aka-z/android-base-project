package com.github.lorcan.base.network;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Proxy;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;


import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;

/**
 * http请求相关的工具方法
 *
 * @author Tsmile
 * @modify lorcan.luo@gmail.com
 */
public class HttpUtil {

    private static final String TAG = "HttpUtil";

    private static final int TIME_MAX_WAIT_OUT_CONNECTION = 60000;

    private static final int TIME_OUT_CONNECTION = 60000;

    private static final int TIME_OUT_SOCKET = 60000;

    public static final String BOUNDARY = "7cd4a6d158c";

    public static final String MP_BOUNDARY = "--" + BOUNDARY;

    public static final String END_MP_BOUNDARY = "--" + BOUNDARY + "--";

    public static final String MULTIPART_FORM_DATA = "multipart/form-data";


    public static String inputStream2String(InputStream in) throws IOException {
        if (in == null)
            return "";

        final int size = 128;
        byte[] buffer = new byte[size];

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int cnt = 0;
        while ((cnt = in.read(buffer)) > -1) {
            baos.write(buffer, 0, cnt);
        }
        baos.flush();

        in.close();
        baos.close();

        return baos.toString();
    }

    /**
     * 获取网络状态
     *
     * @return 网络状态：State.*
     */
    public static State getConnectionState(Context context) {
        ConnectivityManager sManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = sManager.getActiveNetworkInfo();
        if (info != null) {
            return info.getState();
        }
        return State.UNKNOWN;
    }

    /**
     * 网络连接是否已连接好
     *
     * @return
     */
    public static boolean isConnected(Context context) {
        return State.CONNECTED.equals(getConnectionState(context));
    }

    /**
     * 获取httpclient进行网络请求
     */
    public static HttpClient getHttpClient(Context context) throws IOException {
        NetworkState state = getNetworkState(context);
        HttpClient client = createHttpClient();

        if (state == NetworkState.NOTHING) {
            throw new IOException("NoSignalException");
        } else if (state == NetworkState.MOBILE) {
            APNWrapper wrapper = null;
            wrapper = getAPN(context);
            if (!TextUtils.isEmpty(wrapper.proxy)) {
                client.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY,
                        new HttpHost(wrapper.proxy, wrapper.port));
            }
        }

        // HttpConnectionParamBean paramHelper = new
        // HttpConnectionParamBean(client.getParams());
        // paramHelper.setSoTimeout(TIME_OUT_CONNECTION);
        // paramHelper.setConnectionTimeout(TIME_OUT_SOCKET);
        return client;
    }

    /**
     * 使用httpclient进行post请求
     */
    public static HttpResponse doFilePostRequest(HttpClient client, String url, MultipartEntity multipartEntity,
                                                 Header[] headers) throws Exception {
        HttpPost httpPostRequest = new HttpPost(url);
        if (headers != null) {
            httpPostRequest.setHeaders(headers);
        }
        client.getParams().setParameter(HttpConnectionParams.SO_TIMEOUT, 60000);
        httpPostRequest.setEntity(multipartEntity);

        return client.execute(httpPostRequest);
    }

    public static HttpResponse doJsonPostRequest(HttpClient client, String url,
                                             JSONObject postJSON, Header[] headers) throws IOException {
        HttpPost httpPostRequest = new HttpPost(url);
        if (headers != null) {
            httpPostRequest.setHeaders(headers);
        }
        HttpEntity entity = null;
        if (postJSON != null) {
            entity = new StringEntity(postJSON.toString());
            httpPostRequest.setEntity(entity);
        }
        return client.execute(httpPostRequest);
    }

    public static HttpResponse doPostRequest(HttpClient client, String url,
                                             List<NameValuePair> postParams, Header[] headers) throws IOException {
        HttpPost httpPostRequest = new HttpPost(url);

        if (headers != null) {
            httpPostRequest.setHeaders(headers);
        }

        HttpEntity entity = null;
        if (postParams != null && postParams.size() > 0) {
            entity = new UrlEncodedFormEntity(postParams, HTTP.UTF_8);
            httpPostRequest.setEntity(entity);
        }
        return client.execute(httpPostRequest);
    }

    /**
     * 使用httpclient进行get请求
     */
    public static HttpResponse doGetRequest(HttpClient client, String url, Header[] headers)
            throws IOException {
        HttpGet httpGetRequest = new HttpGet(url);
        if (headers != null) {
            httpGetRequest.setHeaders(headers);
        }
        return client.execute(httpGetRequest);
    }

    public enum NetworkState {
        NOTHING, MOBILE, WIFI
    }

    public static NetworkState getNetworkState(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || !info.isAvailable()) {
            return NetworkState.NOTHING;
        } else {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                return NetworkState.MOBILE;
            } else {
                return NetworkState.WIFI;
            }
        }
    }

    public static String[] getProxyHostAndPort(Context context) {
        if (getNetworkState(context) == NetworkState.WIFI) {
            return new String[]{"", "-1"};
        } else {
            return new String[]{Proxy.getDefaultHost(),
                    "" + Proxy.getDefaultPort()};
        }
    }

    public static boolean isWapNet(Context context) {
        String currentAPN = "";
        ConnectivityManager conManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = conManager.getActiveNetworkInfo();
        if (info == null || !info.isAvailable()) {
            return false;
        }
        if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            return false;
        }
        currentAPN = info.getExtraInfo();
        if (currentAPN == null || currentAPN.equals("")) {
            return false;
        } else {
            if (currentAPN.equals("cmwap") || currentAPN.equals("uniwap")
                    || currentAPN.equals("3gwap")) {

                return true;
            } else {
                return false;
            }
        }
    }

    public static class APNWrapper {
        public String name;

        public String apn;

        public String proxy;

        public int port;

        public String getApn() {
            return apn;
        }

        public String getName() {
            return name;
        }

        public int getPort() {
            return port;
        }

        public String getProxy() {
            return proxy;
        }

        APNWrapper() {
        }

        public String toString() {
            return "{name=" + name + ";apn=" + apn + ";proxy=" + proxy
                    + ";port=" + port + "}";
        }
    }

    public static APNWrapper getAPN(Context ctx) {
        APNWrapper wrapper = new APNWrapper();
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(
                    Uri.parse("content://telephony/carriers/preferapn"),
                    new String[]{"name", "apn", "proxy", "port"}, null,
                    null, null);
        } catch (Exception e) {
            // 为了解决在4.2系统上禁止非系统进程获取apn相关信息，会抛出安全异常
            // java.lang.SecurityException: No permission to write APN settings
        }
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.isAfterLast()) {
                wrapper.name = "N/A";
                wrapper.apn = "N/A";
            } else {
                wrapper.name = cursor.getString(0) == null ? "" : cursor
                        .getString(0).trim();
                wrapper.apn = cursor.getString(1) == null ? "" : cursor
                        .getString(1).trim();
            }
            cursor.close();
        } else {
            wrapper.name = "N/A";
            wrapper.apn = "N/A";
        }
        wrapper.proxy = Proxy.getDefaultHost();
        wrapper.proxy = TextUtils.isEmpty(wrapper.proxy) ? "" : wrapper.proxy;
        wrapper.port = Proxy.getDefaultPort();
        wrapper.port = wrapper.port > 0 ? wrapper.port : 80;
        return wrapper;
    }

    protected static DefaultHttpClient createHttpClient() {

        // sets up parameters
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "utf-8");
        params.setBooleanParameter("http.protocol.expect-continue", false);

        // 从这里开始是进行下载，使用了多线程执行请求
        ConnManagerParams.setMaxConnectionsPerRoute(params,
                new ConnPerRouteBean(50));// 设置并发数

        // 设置连接最大等待时间
        ConnManagerParams.setTimeout(params, TIME_MAX_WAIT_OUT_CONNECTION);
        /* 连接超时 */
        HttpConnectionParams.setConnectionTimeout(params, TIME_OUT_CONNECTION);
        /* 请求读取超时 */
        HttpConnectionParams.setSoTimeout(params, TIME_OUT_SOCKET);

        // registers schemes for both http and https
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory
                .getSocketFactory(), 80));
        try {

            KeyStore trustStore = KeyStore.getInstance(KeyStore
                    .getDefaultType());
            trustStore.load(null, null);

            EasySSLSocketFactory sf = new EasySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            registry.register(new Scheme("https", sf, 443));
        } catch (Exception e) {
            Log.e(TAG, "https:", e);
        }
        ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(
                params, registry);
        return new DefaultHttpClient(manager, params);
    }
}
