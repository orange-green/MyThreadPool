import java.util.concurrent.BlockingQueue;


/**
 * 拒绝策略接口
 */
@FunctionalInterface
public interface RejectPolicy {
    void reject(Runnable task, BlockingQueue<Runnable> taskQueue);
}
