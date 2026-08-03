package ws.palladian.helper;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the stuck-pool give-up added to {@link ProcessHelper#waitForThreadPool}, which exists so that one wedged task
 * can no longer hang the calling thread forever (gamebrain update-service freezes on 2026-06-23 and 2026-08-02).
 */
public class ProcessHelperTest {
    @Test
    public void stuckRequiresTheWindowToHaveElapsed() {
        long window = TimeUnit.MINUTES.toMillis(10);
        assertFalse("just below the window is not stuck", ProcessHelper.isStuck(5, window - 1, window));
        assertFalse("exactly at the window is not stuck", ProcessHelper.isStuck(5, window, window));
        assertTrue("past the window is stuck", ProcessHelper.isStuck(5, window + 1, window));
    }

    @Test
    public void aPoolThatHasCompletedNothingCanStillBeStuck() {
        // the 2026-08-02 shape: a single-threaded pool whose only task wedged, so the counter never moved off 0
        assertTrue(ProcessHelper.isStuck(0, TimeUnit.MINUTES.toMillis(11), TimeUnit.MINUTES.toMillis(10)));
    }

    @Test
    public void deadlineCanBeDisabled() {
        assertFalse(ProcessHelper.isStuck(0, TimeUnit.DAYS.toMillis(7), 0));
        assertFalse(ProcessHelper.isStuck(0, TimeUnit.DAYS.toMillis(7), -1));
    }

    @Test
    public void unobservableProgressIsNeverReportedStuck() {
        // a false "stuck" on a pool we cannot measure would silently drop queued work, so we must not guess
        assertFalse(ProcessHelper.isStuck(-1, TimeUnit.DAYS.toMillis(7), TimeUnit.MINUTES.toMillis(10)));
    }

    @Test(timeout = 30000)
    public void waitGivesUpOnAWedgedTaskInsteadOfHangingForever() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        CountDownLatch taskStarted = new CountDownLatch(1);
        AtomicBoolean releaseTask = new AtomicBoolean(false);

        // an UNINTERRUPTIBLE wedge: it ignores interrupts, exactly like the socket read that caused the outage,
        // so shutdownNow() cannot end it - waitForThreadPool must abandon it and return anyway
        executor.submit(() -> {
            taskStarted.countDown();
            while (!releaseTask.get()) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    // deliberately swallowed
                }
            }
        });
        assertTrue(taskStarted.await(5, TimeUnit.SECONDS));

        try {
            long start = System.currentTimeMillis();
            boolean terminated = ProcessHelper.waitForThreadPool(executor, new StopWatch(), TimeUnit.SECONDS.toMillis(2));
            long elapsed = System.currentTimeMillis() - start;

            // the caller must be able to tell that queued work was dropped, not just that the wait ended
            assertFalse("abandoning a wedged pool must be reported as not-terminated", terminated);
            // it returned at all (the old code would loop here forever) and did not return before the window elapsed
            assertTrue("should not give up before the stuck window", elapsed >= 2000);
            assertTrue("should give up promptly after the stuck window, was " + elapsed + "ms", elapsed < 20000);
        } finally {
            releaseTask.set(true);
        }
    }

    /**
     * Documents the behaviour difference the fix makes. {@code stuckTimeoutMillis <= 0} is exactly the old,
     * unconditional {@code while (!awaitTermination(5s))} loop, so this test shows the old code still wedged on the
     * same input where the new code (see {@link #waitGivesUpOnAWedgedTaskInsteadOfHangingForever()}) returns.
     */
    @Test(timeout = 30000)
    public void withTheDeadlineDisabledTheWaitStillHangsAsItUsedTo() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        AtomicBoolean releaseTask = new AtomicBoolean(false);
        CountDownLatch waitReturned = new CountDownLatch(1);

        executor.submit(() -> {
            while (!releaseTask.get()) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    // deliberately swallowed
                }
            }
        });

        try {
            Thread waiter = new Thread(() -> {
                ProcessHelper.waitForThreadPool(executor, new StopWatch(), 0);
                waitReturned.countDown();
            });
            waiter.setDaemon(true);
            waiter.start();

            assertFalse("the old unbounded wait must NOT return while a task is wedged",
                    waitReturned.await(8, TimeUnit.SECONDS));
        } finally {
            // let the wedged task end so the waiter thread can finish too
            releaseTask.set(true);
        }
        assertTrue("once the task ends, the unbounded wait returns normally", waitReturned.await(15, TimeUnit.SECONDS));
    }

    @Test(timeout = 60000)
    public void aWedgedInnerPoolNoLongerFreezesTheOuterPool() throws Exception {
        // The exact shape of the 2026-08-02 outage, which had waitForThreadPool twice in one stack:
        //   outer worker pool -> inner per-item pool -> one uninterruptible task (a socket read that never returned).
        // Before the fix BOTH waits looped forever, so the outer caller (the service's main loop) never came back.
        AtomicBoolean releaseTask = new AtomicBoolean(false);
        CountDownLatch innerWaitReturned = new CountDownLatch(1);
        ExecutorService outer = Executors.newFixedThreadPool(2);

        try {
            // the wedged worker: its inner pool has a task that ignores interrupts
            outer.submit(() -> {
                ExecutorService inner = Executors.newFixedThreadPool(1);
                inner.submit(() -> {
                    while (!releaseTask.get()) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            // deliberately swallowed - an uninterruptible blocking call
                        }
                    }
                });
                ProcessHelper.waitForThreadPool(inner, new StopWatch(), TimeUnit.SECONDS.toMillis(2));
                innerWaitReturned.countDown();
            });
            // a healthy sibling worker, to prove the outer pool keeps making progress meanwhile
            outer.submit(() -> ThreadHelper.deepSleep(100));

            ProcessHelper.waitForThreadPool(outer, new StopWatch(), TimeUnit.SECONDS.toMillis(10));

            assertTrue("the inner wait must have abandoned its wedged task", innerWaitReturned.await(5, TimeUnit.SECONDS));
            assertTrue("the outer pool must have terminated normally", outer.isTerminated());
        } finally {
            releaseTask.set(true);
        }
    }

    @Test(timeout = 30000)
    public void waitDoesNotGiveUpWhileTasksKeepCompleting() throws Exception {
        // A pool that is slow but progressing must run to completion: total time (~3s) far exceeds the stuck window,
        // yet no single gap does. An absolute deadline would wrongly truncate this. The 1s window also keeps the poll
        // interval at 1s, so the progress-tracking branch of the wait loop is actually entered several times here -
        // with the default 5s poll the pool would terminate before the first poll and the loop body would never run.
        ExecutorService executor = Executors.newFixedThreadPool(1);
        int tasks = 10;
        for (int i = 0; i < tasks; i++) {
            executor.submit(() -> ThreadHelper.deepSleep(300));
        }

        assertTrue("the wait must report clean termination, not abandonment",
                ProcessHelper.waitForThreadPool(executor, new StopWatch(), TimeUnit.SECONDS.toMillis(1)));

        assertTrue("every task should have run to completion", executor.isTerminated());
        assertEquals(tasks, ((ThreadPoolExecutor) executor).getCompletedTaskCount());
    }
}
