package com.github.lorcan.base.network;

import android.content.Context;


import com.github.lorcan.base.parser.IParser;
import com.github.lorcan.base.utils.LogUtil;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.mime.MultipartEntity;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class RequestTask extends GenericTask {

    private static final String TAG = "RequestTask";

    /**
     * 一般get请求
     */
    public static final String HTTP_GET = "GET";

    /**
     * 一般post请求
     */
    public static final String HTTP_POST = "POST";

    /**
     * post请求,传json参数
     */
    public static final String HTTP_POST_JSON = "POST_JSON";

    public static final String PARAM_URL = "url";

    public static final String PARAM_HTTP_METHOD = "httpmethod";


    /**
     * 解析器
     */
    private IParser mParser;

    private List<NameValuePair> mPostParams;

    private JSONObject mJSONParams;

    private TaskParams mParams;

    private String mType;

    private String mUrl;

    private String mImage;

    private String mAudio;

    private String mCheckInId;

    private Header[] mHeaders;


    private Context mContext;

    /**
     * 相关对象的引用
     */
    private Object mExtra;

    private MultipartEntity mMultipartEntity;

    public RequestTask(Context context, IParser parser, Header[] headers) {
        super();
        this.mContext = context;
        this.mParser = parser;
        this.mHeaders = headers;
    }

    public RequestTask(Context context, IParser parser, String image, Header[] headers) {
        this(context, parser, headers);
        this.mImage = image;
    }

    public RequestTask(Context context, IParser parser, MultipartEntity multipartEntity, Header[] headers) {
        this(context, parser, headers);
        this.mMultipartEntity = multipartEntity;
    }


    public void setPostParams(List<NameValuePair> params) {
        mPostParams = params;
    }

    public TaskParams getParams() {
        return mParams;
    }

    public JSONObject getmJSONParams() {
        return mJSONParams;
    }

    public void setmJSONParams(JSONObject mJSONParams) {
        this.mJSONParams = mJSONParams;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public String getRequestUrl() {
        return mUrl;
    }

    public Object getExtra() {
        return mExtra;
    }

    public void setExtra(Object relativeObj) {
        this.mExtra = relativeObj;
    }

    @Override
    protected void onPostExecute(TaskResult result) {
        // 当task没被取消时，调用父类回调taskFinished
        if (!isCancelled()) {
            super.onPostExecute(result);
        }
    }

    public void cancel() {
        onCancelled();
    }

    @Override
    protected TaskResult doInBackground(TaskParams... params) {
        TaskResult result = new TaskResult(-1, this, null);
        mParams = params[0];
        if (mParams == null) {
            LogUtil.e(TAG, "params is null");
            return result;
        }


        mUrl = mParams.getString(PARAM_URL);
        LogUtil.i(TAG, "request url: " + mUrl);

        HttpClient client = null;
        HttpResponse response = null;
        HttpEntity entity = null;
        try {

            client = HttpUtil.getHttpClient(mContext);
            if (HTTP_POST.equals(mParams.getString(PARAM_HTTP_METHOD))) {
                if (mImage != null) {
                    response = HttpUtil.doFilePostRequest(client, mUrl, mMultipartEntity, mHeaders);
                } else {
                    response = HttpUtil.doPostRequest(client, mUrl, mPostParams, mHeaders);
                }
            } else if (HTTP_POST_JSON.equals(mParams
                    .getString(PARAM_HTTP_METHOD))) {
                response = HttpUtil.doPostRequest(client, mUrl, mJSONParams, mHeaders);
            } else {
                response = HttpUtil.doGetRequest(client, mUrl, mHeaders);
            }
            int stateCode = response.getStatusLine().getStatusCode();
            result.stateCode = stateCode;
            if (stateCode == HttpStatus.SC_OK
                    || stateCode == HttpStatus.SC_PARTIAL_CONTENT) {
                entity = response.getEntity();
                InputStream inputStream = entity.getContent();
                if (inputStream != null && mParser != null) {
                    Object obj = mParser.parse(inputStream);
                    result.stateCode = HttpStatus.SC_OK;
                    result.retObj = obj;
                }
            } else {
                if (stateCode == HttpStatus.SC_BAD_REQUEST) {
                    entity = response.getEntity();
                    InputStream inputStream = entity.getContent();
                    if (inputStream != null && mParser != null) {
                        Object obj = mParser.parse(inputStream);
                        result.retObj = obj;
                    }
                }
            }
            LogUtil.d(TAG, "result.stateCode:" + stateCode);
        } catch (IOException e) {
            LogUtil.e(TAG, e.toString());
        } catch (Exception e) {
            LogUtil.e(TAG, e.toString());
        } finally {
            if (client != null) {
                client.getConnectionManager().shutdown();
            }
        }
        return result;
    }

}
