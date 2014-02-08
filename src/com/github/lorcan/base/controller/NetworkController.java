package com.github.lorcan.base.controller;

import android.content.Context;

import com.github.lorcan.base.model.TimeLineResponse;
import com.github.lorcan.base.network.ITaskFinishListener;
import com.github.lorcan.base.network.RequestTask;
import com.github.lorcan.base.network.TaskParams;
import com.github.lorcan.base.parser.BaseParser;
import com.github.lorcan.base.parser.IParser;

import org.apache.http.NameValuePair;
import org.apache.http.entity.mime.MultipartEntity;

import java.util.List;

/**
 * The controller of all net request
 *
 * @author lorcan
 */
public class NetworkController {

    public static void getPublishTimeLine(Context context, ITaskFinishListener listener) {
        String url = "https://api.weibo.com/2/statuses/public_timeline.json";

        doGet(context, url, new BaseParser(TimeLineResponse.class), listener);
    }


    /**
     * do get
     *
     * @param url
     * @param parser
     * @param listener
     */
    private static RequestTask doGet(Context context, String url, IParser parser, ITaskFinishListener listener) {
        RequestTask task = new RequestTask(context, parser, null);
        TaskParams params = new TaskParams();
        params.put(RequestTask.PARAM_URL, url);
        params.put(RequestTask.PARAM_HTTP_METHOD, RequestTask.HTTP_GET);
        task.setTaskFinishListener(listener);
        task.execute(params);

        return task;
    }

    /**
     * do get
     *
     * @param url
     * @param parser
     * @param listener
     */
    private static void doGet(Context context, String url, IParser parser, ITaskFinishListener listener, String type) {
        RequestTask task = new RequestTask(context, parser, null);
        task.setType(type);
        TaskParams params = new TaskParams();
        params.put(RequestTask.PARAM_URL, url);
        params.put(RequestTask.PARAM_HTTP_METHOD, RequestTask.HTTP_GET);
        task.setTaskFinishListener(listener);
        task.execute(params);
    }

    /**
     * do post
     *
     * @param url
     * @param pairs
     * @param parser
     * @param listener
     */
    private static void doPost(Context context, String url, List<NameValuePair> pairs, IParser parser, ITaskFinishListener listener) {
        RequestTask task = new RequestTask(context, parser, null);
        TaskParams params = new TaskParams();
        params.put(RequestTask.PARAM_URL, url);
        params.put(RequestTask.PARAM_HTTP_METHOD, RequestTask.HTTP_POST);
        task.setPostParams(pairs);
        task.setTaskFinishListener(listener);
        task.execute(params);
    }

    /**
     * do post multipartEntity
     *
     * @param url
     * @param parser
     * @param multipartEntity
     * @param listener
     */
    private static void doFilePost(Context context, String url, MultipartEntity multipartEntity, IParser parser, ITaskFinishListener listener) {
        RequestTask task = new RequestTask(context, parser, multipartEntity, null);
        TaskParams params = new TaskParams();
        params.put(RequestTask.PARAM_URL, url);
        params.put(RequestTask.PARAM_HTTP_METHOD, RequestTask.HTTP_POST_FILE);
        task.setTaskFinishListener(listener);

        task.execute(params);
    }

}
