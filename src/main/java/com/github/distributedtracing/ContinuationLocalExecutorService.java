package com.github.distributedtracing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ContinuationLocalExecutorService implements ExecutorService {
    private ExecutorService es;

    private ContinuationLocalExecutorService(ExecutorService es) {
        this.es = es;
    }

    public static ContinuationLocalExecutorService wrap(ExecutorService inner) {
        return new ContinuationLocalExecutorService(inner);
    }

    public void shutdown() {
        es.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return es.shutdownNow();
    }

    public boolean isShutdown() {
        return es.isShutdown();
    }

    public boolean isTerminated() {
        return es.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return es.awaitTermination(timeout, unit);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return es.submit(wrapCallable(task));
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return es.submit(wrapRunnable(task), result);
    }

    public Future<?> submit(Runnable task) {
        return es.submit(wrapRunnable(task));
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return es.invokeAll(wrapCollection(tasks));
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return es.invokeAll(wrapCollection(tasks), timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return es.invokeAny(wrapCollection(tasks));
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return es.invokeAny(wrapCollection(tasks), timeout, unit);
    }

    public void execute(final Runnable command) {
        es.execute(wrapRunnable(command));
    }

    private <T> Collection<? extends Callable<T>> wrapCollection(Collection<? extends Callable<T>> callables) {
        Collection<Callable<T>> wrapped = new ArrayList<Callable<T>>(callables.size());
        for(Callable<T> callable : callables) {
            wrapped.add(wrapCallable(callable));
        }
        return wrapped;
    }

    private Runnable wrapRunnable(final Runnable command) {
        final Object[] saved = ContinuationLocal.save();
        return new Runnable() {
            public void run() {
                Object[] ctx = ContinuationLocal.save();
                ContinuationLocal.restore(saved);
                try {
                    command.run();
                } finally {
                    ContinuationLocal.restore(ctx);
                }
            }
        };
    }

    private <T> Callable<T> wrapCallable(final Callable<T> callable) {
        final Object[] saved = ContinuationLocal.save();
        return new Callable<T>() {
            public T call() throws Exception {
                Object[] ctx = ContinuationLocal.save();
                ContinuationLocal.restore(saved);
                T result;
                try {
                    result = callable.call();
                } finally {
                    ContinuationLocal.restore(ctx);
                }
                return result;
            }
        };
    }
}
