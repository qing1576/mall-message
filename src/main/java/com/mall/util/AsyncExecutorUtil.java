package com.mall.util;

import com.mall.config.PublicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <b><u>AsyncUtil功能说明：</u></b>
 * <p>通过阻塞队列，异步执行任务。</p>
 * <p>当队列中任务过多时，将无法实时执行，会出现阻塞。<b>如果对实时性要求比较高时，请慎用。</b></p>
 * @author
 * 2021-07-19 14:01
 */
@Component
public class AsyncExecutorUtil {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 核心线程数
     */
    private final int CORE_POOL_SIZE = 10;

    /**
     * 最大线程数
     */
    private final int MAXIMUM_POOL_SIZE = 20;

    /**
     * 超过 corePoolSize 线程数量的线程最大空闲时间
     */
    private final int KEEP_ALIVE_TIME = 20;

    /**
     * 缓冲队列数
     */
    private final int QUEUE_CAPACITY_SIZE = 100;

    /**
     * 自定义拒绝策略。当线程池满了之后，将任务加入到缓冲队列中
     * 一旦抛出RejectedExecutionException异常，主线程后面的任务也不会执行了，但是不会影响线程池里其它任务的运行
     */
    private final RejectedExecutionHandler rejectedExecutionHandler = (r, executor) -> {
        MDC.put(PublicConfig.THREAD_NAME, Thread.currentThread().getName());
        try {
            logger.info("即将再次重试加入任务队列：{}, {}", Thread.currentThread().getName(), r.toString());
            // 参考activeMq的拒绝策略，最大努力执行任务型。当触发拒绝策略时，会在10秒内再次尝试将任务塞进任务队列。如果还是失败，则抛出异常
            boolean b = executor.getQueue().offer(r, 10, TimeUnit.SECONDS);
            if (!b) {
                throw new RejectedExecutionException("AsyncExecutorUtil Timed Out while attempting to enqueue Task.");
            }
        } catch (InterruptedException e) {
            logger.error("", e);
            throw new RejectedExecutionException("Interrupted waiting for AsyncExecutorUtil.worker");
        }
    };

    /**
     * 创建线程池
     */
    private final static ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

    private final ThreadFactory threadFactory = new ThreadFactory() {

        final AtomicInteger threadCount = new AtomicInteger();
        @Override
        public Thread newThread(Runnable r) {
            String threadNamePrefix = "myAsyncUtil-";
            return new Thread(null, r, threadNamePrefix + threadCount.incrementAndGet());
        }
    };

    public ThreadPoolTaskExecutor getThreadPoolTaskExecutor() {
        return threadPoolTaskExecutor;
    }

    /**
     * <b><u>init方法说明：</u></b>
     * <p>初始化线程</p>
     * @author
     * 2023/12/13 15:56
     */
    private void init() {
        logger.info("开始初始化线程池……");
        // 核心线程数
        threadPoolTaskExecutor.setCorePoolSize(CORE_POOL_SIZE);
        threadPoolTaskExecutor.setMaxPoolSize(MAXIMUM_POOL_SIZE);
        threadPoolTaskExecutor.setKeepAliveSeconds(KEEP_ALIVE_TIME);
        threadPoolTaskExecutor.setQueueCapacity(QUEUE_CAPACITY_SIZE);
        threadPoolTaskExecutor.setThreadNamePrefix("myAsyncUtil-");
        threadPoolTaskExecutor.setThreadFactory(threadFactory);
        threadPoolTaskExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);
    }

    public AsyncExecutorUtil() {
        // 机器的CPU核心数
        init();
        threadPoolTaskExecutor.initialize();
    }

    /**
     * <b><u>execute方法说明：</u></b>
     * <p>无返回值的执行任务。不会堵塞主线程</p>
     * Runnable执行run时遇到异常并不会抛出。
     * @param runnable 提交的任务
     * @author
     * 2021-08-02 16:00
     */
    public static void execute(Runnable runnable) {
        MDC.put(PublicConfig.THREAD_NAME, Thread.currentThread().getName());
        threadPoolTaskExecutor.execute(runnable);
    }

    /**
     * <b><u>execute方法说明：</u></b>
     * <p>无返回值的执行任务。通过CountDownLatch来堵塞主线程，达到批量数据拆分执行的效果</p>
     * @param runnable       提交的任务
     * @param countDownLatch 信号量
     * @author
     * 2023/8/3 11:24 AM
     */
    public static void execute(Runnable runnable, CountDownLatch countDownLatch) {
        try {
            threadPoolTaskExecutor.execute(() -> {
                MDC.put(PublicConfig.THREAD_NAME, Thread.currentThread().getName());
                runnable.run();
            });
        } finally {
            countDownLatch.countDown();
        }
    }

    /**
     * <b><u>submit方法说明：</u></b>
     * <p>有返回值的执行任务。</p>
     * 如果提交任务之后，没有执行取返回值的方法，即get()，则不堵塞主线程；
     * 如果提交任务之后，需要取返回值，则会堵塞主线程
     * Callable执行call时遇到异常会抛出。如果需要回滚，需要用submit
     * @param task  提交的任务
     * @author
     * 2021-08-02 16:01
     */
    public static <T> Future<T> submit(Callable<T> task) {
        MDC.put(PublicConfig.THREAD_NAME, Thread.currentThread().getName());
        return threadPoolTaskExecutor.submit(task);
    }

    /**
     * <b><u>submit方法说明：</u></b>
     * <p>有返回值的执行任务。通过CountDownLatch来堵塞主线程，达到批量数据拆分执行的效果</p>
     * <p>需要有线程池事务的，需要使用该方法。在主线程中，通过捕获get()方法抛出的异常，来回滚事务</p>
     * @param task           提交的任务
     * @param countDownLatch 信号量
     * @return java.util.concurrent.Future<T>
     * @author
     * 2023/8/11 2:54 PM
     */
    public static  <T> Future<T> submit(Callable<T> task, CountDownLatch countDownLatch) {
        MDC.put(PublicConfig.THREAD_NAME, Thread.currentThread().getName());
        try {
            return threadPoolTaskExecutor.submit(task);
        } finally {
            countDownLatch.countDown();
        }
    }
}
