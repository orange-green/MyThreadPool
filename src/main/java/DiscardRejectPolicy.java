import java.util.concurrent.BlockingQueue;

public class DiscardRejectPolicy implements RejectPolicy {
    @Override
    public void reject(Runnable task, BlockingQueue<Runnable> taskQueue) {

        // 展示被丢弃的任务的信息
        System.out.println("任务：" + task.hashCode() + " 被丢弃了");

    }
}