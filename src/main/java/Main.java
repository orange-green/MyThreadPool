import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    // 任务计数器,用来生成任务ID，用原子类保证线程安全
    private static final AtomicInteger taskIdCounter = new AtomicInteger(0);


    public static void main(String[] args) throws InterruptedException {
        SimpleThreadPool threadPool = new SimpleThreadPool(2, 4, 3,1, new DiscardRejectPolicy());
        for (int i = 0; i < 20; i++) {
            final int taskId = taskIdCounter.getAndIncrement();
            threadPool.execute(() -> {
                System.out.println("任务：" + taskId + "，被执行，当前线程：" + Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
