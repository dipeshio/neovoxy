package me.cortex.neovoxy.common.thread;

import me.cortex.neovoxy.common.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread pool manager for Voxy background services.
 * 
 * <p>Manages worker threads for:
 * <ul>
 *   <li>Chunk voxelization</li>
 *   <li>LOD mesh generation</li>
 *   <li>Storage I/O</li>
 * </ul>
 */
public class ServiceManager {
    
    private final ExecutorService executor;
    private final int threadCount;
    private volatile boolean isShutdown = false;
    
    public ServiceManager(int threadCount) {
        this.threadCount = threadCount;
        
        AtomicInteger threadId = new AtomicInteger(0);
        this.executor = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "NeoVoxy-Worker-" + threadId.getAndIncrement());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        
        Logger.info("ServiceManager created with {} threads", threadCount);
    }
    
    /**
     * Submit a task for execution.
     */
    public Future<?> submit(Runnable task) {
        if (isShutdown) {
            throw new RejectedExecutionException("ServiceManager is shut down");
        }
        return executor.submit(task);
    }
    
    /**
     * Submit a task with result.
     */
    public <T> Future<T> submit(Callable<T> task) {
        if (isShutdown) {
            throw new RejectedExecutionException("ServiceManager is shut down");
        }
        return executor.submit(task);
    }
    
    /**
     * Get the number of worker threads.
     */
    public int getThreadCount() {
        return threadCount;
    }
    
    /**
     * Shutdown the service manager.
     */
    public void shutdown() {
        if (isShutdown) return;
        isShutdown = true;
        
        Logger.info("Shutting down ServiceManager...");
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    Logger.error("ServiceManager did not terminate cleanly");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        Logger.info("ServiceManager shut down");
    }
}
