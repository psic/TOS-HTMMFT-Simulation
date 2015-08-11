package com.htmmft.JSONObserver;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HTMMFTThreadPool extends ThreadPoolExecutor {
	
	public HTMMFTThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		System.out.println("Creating thread pool");
	}


	protected void afterExecute(Runnable r, Throwable t) {
		System.out.println("After executing thread");
		((JSONObserver) r).writeJsonFile();
		super.afterExecute(r, t);
	}	
}