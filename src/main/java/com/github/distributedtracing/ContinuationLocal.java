package com.github.distributedtracing;

public final class ContinuationLocal<T> {
    private static InheritableThreadLocal<Object[]> localCtx =
            new InheritableThreadLocal<Object[]>();
    private static volatile int size = 0;

    /**
     * Return a snapshot of the current ContinuationLocal state.
     */
    public static Object[] save() {
        return localCtx.get();
    }

    /**
     * Restore the ContinuationLocal state to a given set of values.
     */
    public static void restore(Object[] saved) {
        localCtx.set(saved);
    }

    private static synchronized int add() {
        size += 1;
        return size - 1;
    }

    private static void set(int i, Object v) {
        assert i < size;
        Object[] ctx = localCtx.get();

        if(ctx == null) {
            ctx = new Object[size];
        } else {
            Object[] oldCtx = ctx;
            ctx = new Object[size];
            System.arraycopy(oldCtx, 0, ctx, 0, oldCtx.length);
        }

        ctx[i] = v;
        localCtx.set(ctx);
    }

    private static Object get(int i) {
        Object[] ctx = localCtx.get();
        if(ctx == null || ctx.length <= i) {
            return null;
        }

        return ctx[i];
    }

    private static void clear(int i) {
        set(i, null);
    }

    /**
     * Clear all locals in the current context.
     */
    private static void clearAll() {
        localCtx.set(null);
    }

    /**
     * Execute a block with the given ContinuationLocals,
     * restoring current values upon completion.
     */
    public static <U> U let(Object[] ctx, Producer<U> f) {
        Object[] saved = save();
        restore(ctx);
        U result;
        try {
            result = f.apply();
        } finally {
            restore(saved);
        }
        return result;
    }

    /**
     * Execute a block with all ContinuationLocals clear, restoring
     * current values upon completion.
     */
    public static <U> U letAllClear(Producer<U> f) {
        return ContinuationLocal.let(null, f);
    }

    /**
     * Convert a Producer&lt;R&gt; into another producer of the same
     * type whose ContinuationLocal context is saved when calling `closed`
     * and restored upon invocation.
     */
    public static <R> Producer<R> closed(final Producer<R> fn) {
        final Object[] closure = ContinuationLocal.save();
        return new Producer<R>() {
            public R apply() {
                Object[] save = ContinuationLocal.save();
                ContinuationLocal.restore(closure);
                R result;
                try {
                    result = fn.apply();
                } finally {
                    ContinuationLocal.restore(save);
                }
                return result;
            }
        };
    }

    private final int me = add();

    /**
     * Update the ContinuationLocal with the given value.
     */
    public void set(T value) {
        set(me, value);
    }

    /**
     * Get the ContinuationLocal's value.
     */
    public T get() {
        return (T)get(me);
    }

    /**
     * Alias for get()
     */
    public T apply() {
        return get();
    }

    /**
     * Execute a block with a specific ContinuationLocal value,
     * restoring the current state upon completion.
     */
    public <U> U let(T value, Producer<U> f) {
        T saved = get();
        set(value);
        U result;
        try {
            result = f.apply();
        } finally {
            set(saved);
        }
        return result;
    }

    /**
     * Execute a block with the ContinuationLocal cleared, restoring
     * the current state upon completion.
     */
    public <U> U letClear(Producer<U> f) {
        T saved = get();
        clear();
        U result;
        try {
            result = f.apply();
        } finally {
            set(saved);
        }
        return result;
    }

    /**
     * Clear the ContinuationLocal's value. Other ContinuationLocal's are not modified.
     */
    public void clear() {
        ContinuationLocal.clear(me);
    }
}
