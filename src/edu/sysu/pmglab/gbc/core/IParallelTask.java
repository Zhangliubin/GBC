package edu.sysu.pmglab.gbc.core;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.check.Value;
import edu.sysu.pmglab.easytools.ValueUtils;

/**
 * @author suranyi
 * @description 并行线程任务
 */

public interface IParallelTask {
    /**
     * 最大并行线程数
     */
    int AVAILABLE_PROCESSORS = ValueUtils.max(1, Runtime.getRuntime().availableProcessors());
    int INIT_THREADS = Value.of(4, 1, AVAILABLE_PROCESSORS);
    int MIN_THREADS = 1;

    /**
     * 设置并行线程数
     *
     * @param threads 并行线程数
     * @return 任务本类
     */
    default IParallelTask setThreads(int threads) {
        return setParallel(threads);
    }

    /**
     * 设置并行线程数
     *
     * @param threads 并行线程数
     * @return 任务本类
     */
    IParallelTask setParallel(int threads);

    /**
     * 添加线程数
     *
     * @param threads 要添加的并行线程数
     * @return 任务本类
     */
    default IParallelTask addThreads(int threads) {
        Assert.that(threads >= 0);

        if (threads > 0) {
            return setThreads(getThreads() + threads);
        }

        return this;
    }

    /**
     * 减少线程数
     *
     * @param threads 要减少的并行线程数
     * @return 任务本类
     */
    default IParallelTask reduceThreads(int threads) {
        Assert.that(threads >= 0);

        if (threads > 0) {
            return setThreads(getThreads() - threads);
        }

        return this;
    }

    /**
     * 检查并行线程数是否合法
     *
     * @param threads 并行线程数
     * @return 无效线程数抛错, 有效则返回值
     */
    static int checkParallel(int threads) {
        threads = threads == -1 ? INIT_THREADS : threads;
        return Value.of(threads, 1, AVAILABLE_PROCESSORS);
    }

    /**
     * 获取线程数
     *
     * @return 线程数
     */
    int getThreads();
}
