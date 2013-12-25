package com.github.lorcan.base.parser;

import android.text.TextUtils;


import com.github.lorcan.base.network.HttpUtil;
import com.github.lorcan.base.utils.JsonUtils;
import com.github.lorcan.base.utils.LogUtil;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;


public class BaseParser implements IParser {
    public static final String TAG = "Parser";

    private Class<?> classType;

    public BaseParser(Class<?> clz) {
        this.setClassType(clz);
    }

    public Object parse(InputStream in) {
        try {
            if (null == in) {
                return null;
            }
            String jsonString = HttpUtil.inputStream2String(in);
            LogUtil.d(TAG, jsonString);
            if (TextUtils.isEmpty(jsonString)) {
                return null;
            } else {
                return parseDataContent(jsonString);
            }
        } catch (IOException e) {
            LogUtil.e(TAG, e.toString(), e);
        } catch (JSONException e) {
            LogUtil.e(TAG, e.toString(), e);
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
            } catch (IOException e) {
                LogUtil.e(TAG, e.toString(), e);
            }
        }
        return null;
    }

    /**
     * @param jsonString
     * @return
     * @throws org.json.JSONException
     */
    protected Object parseDataContent(String jsonString) throws JSONException {
        return JsonUtils.fromJson(jsonString, getClassType());
    }

    public Class<?> getClassType() {
        return classType;
    }

    public void setClassType(Class<?> classType) {
        this.classType = classType;
    }
}
