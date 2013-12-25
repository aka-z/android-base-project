package com.github.lorcan.base.parser;

import java.io.InputStream;

/**
 * 解析器接口
 *
 */
public interface IParser {

	/**
	 * 解析
	 *
	 * @param in
	 *            ：被解析的输入流
	 */
	public Object parse(InputStream in);
}
