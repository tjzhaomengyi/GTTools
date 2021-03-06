/*
 * Tencent is pleased to support the open source community by making
 * Tencent GT (Version 2.4 and subsequent versions) available.
 *
 * Notwithstanding anything to the contrary herein, any previous version
 * of Tencent GT shall not be subject to the license hereunder.
 * All right, title, and interest, including all intellectual property rights,
 * in and to the previous version of Tencent GT (including any and all copies thereof)
 * shall be owned and retained by Tencent and subject to the license under the
 * Tencent GT End User License Agreement (http://gt.qq.com/wp-content/EULA_EN.html).
 *
 * Copyright (C) 2015 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.tencent.wstt.gt.datasource.engine;

import com.tencent.wstt.gt.datasource.util.SMUtils;

import java.lang.reflect.Field;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 流畅度数据采集引擎，用于采集本进程的流畅度数据
 */
public class SMTimerTask extends TimerTask {
	private boolean isGetPeriod = false; // 是否已获取执行间隔值
	private long thisPeriod = 1000; // 默认1000ms，在初次执行时要通过反射方法在超类中获取真实值

	private DataRefreshListener<Long> dataRefreshListener;

	private AtomicInteger count;

	/**
	 * 构造方法
	 * @param dataRefreshListener 数据的监听器
	 */
	public SMTimerTask(DataRefreshListener<Long> dataRefreshListener)
	{
		this.dataRefreshListener = dataRefreshListener;
		count = SMUtils.startSampleSM();
	}

	/*
	 * 主循环，1s执行一次
	 * @see java.util.TimerTask#run()
	 */
	public void run() {
		if (!SMUtils.isRunning())
		{
			return;
		}

		// Task的执行采样间隔
		if (! isGetPeriod)
		{
			Class<?> clz = TimerTask.class;
			Field superPeriod;
			try {
				superPeriod = clz.getDeclaredField("period");
				superPeriod.setAccessible(true);
				thisPeriod = superPeriod.getLong(this);
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			isGetPeriod = true;
		}

		int x = count.getAndSet(0);
		// 需要根据采样间隔对刷新次数进行放大或缩小
		dataRefreshListener.onRefresh(System.currentTimeMillis(), Long.valueOf(x * 1000 / thisPeriod));
	}

	public void stop()
	{
		this.cancel();
		SMUtils.stopSampleSM();
	}
}