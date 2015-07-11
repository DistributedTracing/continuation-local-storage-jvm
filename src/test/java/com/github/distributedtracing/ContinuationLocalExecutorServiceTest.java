package com.github.distributedtracing;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ContinuationLocalExecutorServiceTest {
    @Test
    public void submit_task() throws ExecutionException, InterruptedException {
        // Set up a thread pool that has been tweaked, so that every thread that is started
        // clears its ContinuationLocal state. That way, we can verify that our wrap() method
        // builds a thread pool where the ContinuationLocal state is restored befure every task.
        final ContinuationLocalExecutorService service = ContinuationLocalExecutorService.wrap(
                new ScheduledThreadPoolExecutor(8, new ClearContinuationLocalThreadFactory()));

        final ContinuationLocal<Integer> value = new ContinuationLocal<Integer>();
        value.set(257);

        final boolean[] valueHasBeenOverwritten = new boolean[]{false};
        final long threadId = Thread.currentThread().getId();

        Future<Integer> futureInt = service.submit(new Callable<Integer>() {
            public Integer call() throws Exception {
                Thread.sleep(10);
                assert valueHasBeenOverwritten[0];
                assert threadId != Thread.currentThread().getId();
                return value.get();
            }
        });

        value.set(200);
        valueHasBeenOverwritten[0] = true;

        int result = futureInt.get();

        assertThat(result, is(257));
        assertThat(value.get(), is(200));
    }

    private static Runnable withClear(final Runnable r) {
        return new Runnable() {
            public void run() {
                ContinuationLocal.letAllClear(new Producer<Object>() {
                    public Object apply() {
                        r.run();
                        return null;
                    }
                });
            }
        };
    }

    private static final class ClearContinuationLocalThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            return new Thread(withClear(r));
        }
    }
}
