package com.github.lorcan.base.network;

/**
 * Task执行完毕后返回的结果
 *
 * @author Tsimle
 *
 */
public class TaskResult {

	public int stateCode; // 状态码，一般为HTTP的响应码
	public GenericTask task; // 任务对象本身
	public Object retObj; // 任务处理结果对象。也可以是错误消息

	public TaskResult() {

	}

	public TaskResult(int stateCode, GenericTask task, Object result) {
		super();
		this.stateCode = stateCode;
		this.task = task;
		this.retObj = result;
	}

}
