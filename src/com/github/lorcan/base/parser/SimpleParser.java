package com.github.lorcan.base.parser;



import com.github.lorcan.base.network.HttpUtil;
import com.github.lorcan.base.utils.LogUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * 简单数据类型解析器
 *
 */
public class SimpleParser implements IParser {
    public static final String TAG = "SimpleParser";

	public SimpleParser() {
	}

	@Override
	public Object parse(InputStream in) {
		if (null == in) {
			return null;
		}

		Object result = null;
		try {
			String json = HttpUtil.inputStream2String(in);
			result = json;
		} catch (IOException e) {
			LogUtil.e(TAG, e.toString());
		} catch (Exception e) {
            LogUtil.e(TAG, e.toString());
		} finally {
			try {
				if (null != in) {
					in.close();
				}
			} catch (IOException e) {
                LogUtil.e(TAG, e.toString());
			}
		}

		return result;
	}
}
