package com.github.distributedtracing;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public final class ContinuationLocalTest {
    @Test
    public void should_be_undefined_by_default() {
        ContinuationLocal<Integer> local = new ContinuationLocal<Integer>();
        assertThat(local.get(), is(nullValue()));
    }

    @Test
    public void should_hold_on_to_values() {
        ContinuationLocal<Integer> local = new ContinuationLocal<Integer>();
        local.set(123);
        assertThat(local.get(), is(123));
    }

    @Test
    public void should_have_a_per_thread_definition() throws InterruptedException {
        final ContinuationLocal<Integer> local = new ContinuationLocal<Integer>();
        final Integer[] threadValue = new Integer[1];

        local.set(123);

        Thread t = new Thread() {
            @Override
            public void run() {
                assertThat(local.get(), is(123));
                local.set(333);
                threadValue[0] = local.get();
            }
        };

        t.start();
        t.join();

        assertThat(local.get(), is(123));
        assertThat(threadValue[0], is(333));
    }

    @Test
    public void should_maintain_value_definitions_when_other_continuation_locals_change() {
        ContinuationLocal<Integer> l0 = new ContinuationLocal<Integer>();
        l0.set(123);
        Object[] save0 = ContinuationLocal.save();
        ContinuationLocal<Integer> l1 = new ContinuationLocal<Integer>();
        assertThat(l0.get(), is(123));
        l1.set(333);
        assertThat(l1.get(), is(333));

        Object[] save1 = ContinuationLocal.save();
        ContinuationLocal.restore(save0);
        assertThat(l0.get(), is(123));
        assertThat(l1.get(), is(nullValue()));

        ContinuationLocal.restore(save1);
        assertThat(l0.get(), is(123));
        assertThat(l1.get(), is(333));
    }

    @Test
    public void restore_should_restore_saved_values() {
        ContinuationLocal<Integer> local = new ContinuationLocal<Integer>();
        local.set(123);
        Object[] saved = ContinuationLocal.save();
        local.set(321);

        ContinuationLocal.restore(saved);
        assertThat(local.get(), is(123));
    }

    @Test
    public void let_should_set_locals_and_restore_previous_value() {
        final ContinuationLocal<Integer> l1 = new ContinuationLocal<Integer>();
        final ContinuationLocal<Integer> l2 = new ContinuationLocal<Integer>();
        l1.set(1);
        l2.set(2);
        Object[] ctx = ContinuationLocal.save();
        l1.set(2);
        l2.set(4);
        final int[] executeCount = {0};

        ContinuationLocal.let(ctx, new Producer<Object>() {
            public Object apply() {
                assertThat(l1.get(), is(1));
                assertThat(l2.get(), is(2));
                executeCount[0] += 1;
                return null;
            }
        });

        assertThat(l1.get(), is(2));
        assertThat(l2.get(), is(4));
        assertThat(executeCount[0], is(1));
    }

    @Test
    public void letAllClear_should_clear_all_locals_and_restore_previous_value() {
        final ContinuationLocal<Integer> l1 = new ContinuationLocal<Integer>();
        final ContinuationLocal<Integer> l2 = new ContinuationLocal<Integer>();
        l1.set(1);
        l2.set(2);

        ContinuationLocal.letAllClear(new Producer<Object>() {
            public Object apply() {
                assertThat(l1.get(), is(nullValue()));
                assertThat(l2.get(), is(nullValue()));
                return null;
            }
        });

        assertThat(l1.get(), is(1));
        assertThat(l2.get(), is(2));
    }

    @Test
    public void restore_should_unset_undefined_variables_when_restoring() {
        ContinuationLocal<Integer> local = new ContinuationLocal<Integer>();
        Object[] saved = ContinuationLocal.save();
        local.set(123);
        ContinuationLocal.restore(saved);

        assertThat(local.get(), is(nullValue()));
    }

    @Test
    public void restore_should_not_restore_cleared_variables() {
        ContinuationLocal<Integer> local = new ContinuationLocal<Integer>();
        local.set(123);
        ContinuationLocal.save(); // to trigger caching
        local.clear();
        ContinuationLocal.restore(ContinuationLocal.save());
        assertThat(local.get(), is(nullValue()));
    }

    @Test
    public void let_should_scope_with_a_value_and_restore_previous_value() {
        final ContinuationLocal<Integer> local = new ContinuationLocal<Integer>();
        local.set(123);
        local.let(321, new Producer<Object>() {
            public Object apply() {
                assertThat(local.get(), is(321));
                return null;
            }
        });
        assertThat(local.get(), is(123));
    }

    @Test
    public void letClear_should_clear_ContinuationLocal_and_restore_previous_value() {
        final ContinuationLocal<Integer> local = new ContinuationLocal<Integer>();
        local.set(123);
        local.letClear(new Producer<Object>() {
            public Object apply() {
                assertThat(local.get(), is(nullValue()));
                return null;
            }
        });
        assertThat(local.get(), is(123));
    }

    @Test
    public void clear_should_make_a_copy_when_clearing() {
        ContinuationLocal<Integer> l = new ContinuationLocal<Integer>();
        l.set(1);
        Object[] save0 = ContinuationLocal.save();
        l.clear();
        assertThat(l.get(), is(nullValue()));
        ContinuationLocal.restore(save0);
        assertThat(l.get(), is(1));
    }

    @Test
    public void closed() {
        final ContinuationLocal<Integer> l = new ContinuationLocal<Integer>();
        l.set(1);
        Producer<Integer> adder = new Producer<Integer>() {
            public Integer apply() {
                int rv = 100 + l.get();
                l.set(10000);
                return rv;
            }
        };
        Producer<Integer> fn = ContinuationLocal.closed(adder);
        l.set(100);
        assertThat(fn.apply(), is(101));
        assertThat(l.get(), is(100));
        assertThat(fn.apply(), is(101));
    }
}
