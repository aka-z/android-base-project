package com.github.lorcan.base;

import android.os.Bundle;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.github.lorcan.base.controller.NetworkController;
import com.github.lorcan.base.image.ImageLoader;
import com.github.lorcan.base.model.TimeLineResponse;
import com.github.lorcan.base.network.ITaskFinishListener;
import com.github.lorcan.base.network.TaskResult;

public class MainActivity extends Activity {

    private Button mRequestBtn;

    private ImageView mImageView;

    private TextView mResponseText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRequestBtn = (Button) findViewById(R.id.test_request_btn);
        mImageView = (ImageView) findViewById(R.id.test_image_loader_img);
        mResponseText = (TextView) findViewById(R.id.response_text);

        DisplayMetrics dm = new DisplayMetrics();

        //取得窗口属性
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        //窗口的宽度
        int screenWidth = dm.widthPixels;

        //窗口高度
        int screenHeight = dm.heightPixels;
        //load image
        ImageLoader.getInstance(this).load("http://tp1.sinaimg.cn/1948832312/180/5643790868/1", mImageView, null,
                null, screenWidth, screenHeight);

        //send get request
        mRequestBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                NetworkController.getPublishTimeLine(MainActivity.this, new ITaskFinishListener() {
                    public void onTaskFinished(TaskResult taskResult) {
                        if (taskResult != null && taskResult.retObj != null) {
                            TimeLineResponse response = (TimeLineResponse) taskResult.retObj;
                            mResponseText.setText(response.getError() + "\n" + response.getError_code() + "\n" + response.getRequest());
                        }
                    }
                });
            }
        });
    }
}
