package sqsp;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
 
/**
 * An executor which blocks and prevents further tasks from
 * being submitted to the pool when all threads are working.
 * <p>
 * Based on BlockingExecutor example by Fahd Shariff.
 * http://fahdshariff.blogspot.de/2013/11/throttling-task-submission-with.html
 * <p>
 * Based on the BoundedExecutor example in:
 * Brian Goetz, 2006. Java Concurrency in Practice. (Listing 8.4)
 */
public class BlockingExecutor extends ThreadPoolExecutor 
{
  private final Semaphore _semaphore;
 
  /**
   * Creates a BlockingExecutor which will block and prevent further
   * submission to the pool when all threads are working.
   *
   * @param poolSize the number of the threads in the pool
   */
  public BlockingExecutor(final int poolSize) {
    super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
 
    // the semaphore is bounding the number of tasks currently executing
    _semaphore = new Semaphore(poolSize);
  }
 
  /**
   * Executes the given task.
   * This method will block when all threads are working.
   */
  @Override
  public void execute(final Runnable task) {
    boolean acquired = false;
    do {
        try {
            _semaphore.acquire();
            acquired = true;
        } catch (final InterruptedException e) {
            // try again
        }
    } while (!acquired);
 
    try {
        super.execute(task);
    } catch (final RejectedExecutionException e) {
        _semaphore.release();
        throw e;
    }
  }
 
  /**
   * Method invoked upon completion of execution of the given Runnable,
   * by the thread that executed the task.
   */
  @Override
  protected void afterExecute(final Runnable r, final Throwable t) {
    super.afterExecute(r, t);
    _semaphore.release();
  }
}