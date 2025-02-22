import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class SimpleThreadPool {

    // 任务队列
    private final BlockingQueue<Runnable> taskQueue;

    // 核心工作线程集合（长期存活的线程）
    private final List<WorkerThread> coreWorkers;

    // 辅助工作线程集合（可回收的临时线程）
    private final List<AuxWorkerThread> auxWorkers;


    // 核心线程数量参数
    private final int corePoolSize;

    // 最大线程数量参数
    private final int maxPoolSize;

    private int activeCount;

    // 辅助线程存活时间
    private final long keepAliveTime;


    // 线程池状态
    private volatile boolean isShutdown = false;

    // 拒绝策略
    private final RejectPolicy rejectPolicy;



    // 构造函数
    public SimpleThreadPool(int corePoolSize, int maxPoolSize, int queueCapacity, long keepAliveTime ,RejectPolicy policy) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.taskQueue = new LinkedBlockingDeque<>(queueCapacity);
        this.coreWorkers = new ArrayList<>(corePoolSize);
        this.auxWorkers = new ArrayList<>(maxPoolSize - corePoolSize);
        this.rejectPolicy = policy;


        // 初始化核心线程
        for (int i = 0; i < this.corePoolSize; i++) {
            WorkerThread worker = new WorkerThread("CoreWorker-" + i);
            worker.start();
            coreWorkers.add(worker);
            activeCount++;
        }
    }


    // 提交任务方法
    public void execute(Runnable task) throws InterruptedException {
        if (isShutdown) {
            throw new IllegalStateException("Thread pool is shutdown");
        }

        // 第一步尝试将任务添加到任务队列中
        if (taskQueue.offer(task)) {
            return;
        }

        // 第二步：队列已满，尝试创建新的辅助线程执行任务
        synchronized (this) {
            if (activeCount < maxPoolSize) {
                AuxWorkerThread auxWorker = new AuxWorkerThread("AuxWorker-" + activeCount);
                auxWorker.start();
                auxWorkers.add(auxWorker);
                activeCount++;
            }
        }

        // 第三步：辅助线程已满，执行拒绝策略
        rejectPolicy.reject(task, taskQueue);
    }

    // 关闭线程池
    public void shutdown() {
        // 中断所有线程
        isShutdown = true;
        coreWorkers.forEach(WorkerThread::interrupt);
        auxWorkers.forEach(AuxWorkerThread::interrupt);
    }

    // 核心工作线程内部类
    private class WorkerThread extends Thread {
        public WorkerThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (!isShutdown || !taskQueue.isEmpty()) {

                try {
                    Runnable task = taskQueue.take();
                    task.run();
                } catch (InterruptedException e) {
                    // 捕获到中断异常后，线程将退出
                    break;
                }

            }
        }
    }

    // 辅助工作线程内部类
    private class AuxWorkerThread extends Thread {
        public AuxWorkerThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            // 辅助线程空闲超时处理，直接回收线程
            while (!isShutdown || !taskQueue.isEmpty()) {

                try {
                    // 辅助线程空闲超时处理, 达到keepAliveTime时间后，线程将退出
                    // poll方法获取阻塞队列中的任务，如果队列为空，将等待keepAliveTime时间，如果超时还没有任务则返回null，线程用break退出
                    Runnable task = taskQueue.poll(keepAliveTime, TimeUnit.SECONDS);
                    if (task != null) {
                        task.run();
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                    // 捕获到中断异常后，线程将退出
                    break;
                }

            }
        }
    }
}
