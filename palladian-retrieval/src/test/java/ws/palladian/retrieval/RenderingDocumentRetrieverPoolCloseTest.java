package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Locks down that a closed pool leaves no browser behind: every driver it created is closed exactly once, borrowed
 * ones included, without blocking on a driver that never comes back. A driver left running when its JVM exits is
 * reparented to init, where no reaper can tell it from a live one again (skycraft collected ~560 such processes).
 * No Chrome is launched: the retrievers only count their closes.
 */
public class RenderingDocumentRetrieverPoolCloseTest {
    private static final long PROCESS_WAIT_MILLIS = 5000;

    /** A retriever with no driver that counts how often it was closed. */
    private static final class CountingRetriever extends RenderingDocumentRetriever {
        final AtomicInteger closes = new AtomicInteger();

        CountingRetriever() {
            super((RemoteWebDriver) null);
        }

        @Override
        public boolean closeAndQuit() {
            closes.incrementAndGet();
            return true;
        }
    }

    private static class CountingPool extends RenderingDocumentRetrieverPool {
        /** No initializer on purpose: the super constructor fills the pool before subclass initializers run. */
        List<CountingRetriever> created;

        CountingPool(int size) {
            super(DriverManagerType.CHROME, size);
        }

        @Override
        protected synchronized RenderingDocumentRetriever createRetriever() {
            if (created == null) {
                created = Collections.synchronizedList(new ArrayList<>());
            }
            CountingRetriever retriever = new CountingRetriever();
            created.add(retriever);
            return retriever;
        }

        /** Puts a driver straight into the queue, as a hand-back that read {@code closed == false} just in time would. */
        void requeue(RenderingDocumentRetriever retriever) {
            pool.add(retriever);
        }

        int totalCloses() {
            int total = 0;
            synchronized (created) {
                for (CountingRetriever retriever : created) {
                    total += retriever.closes.get();
                }
            }
            return total;
        }
    }

    private static RenderingDocumentRetriever borrow(RenderingDocumentRetrieverPool pool) {
        RenderingDocumentRetriever retriever = pool.poll(1, TimeUnit.SECONDS);
        assertNotNull("pool should hand out a retriever", retriever);
        return retriever;
    }

    @Test
    public void closePoolClosesIdleDrivers() {
        CountingPool pool = new CountingPool(3);

        pool.closePool(0);

        for (CountingRetriever retriever : pool.created) {
            assertEquals(1, retriever.closes.get());
        }
    }

    /** The old closePool() took one driver per slot from the queue and blocked forever on one that was borrowed. */
    @Test(timeout = 10000)
    public void closePoolClosesABorrowedDriverThatIsNeverHandedBack() {
        CountingPool pool = new CountingPool(2);
        CountingRetriever held = (CountingRetriever) borrow(pool);

        pool.closePool(100);

        assertEquals("the borrowed driver must be closed too", 1, held.closes.get());
        assertEquals(2, pool.totalCloses());
    }

    @Test(timeout = 10000)
    public void aDriverHandedBackDuringTheGraceIsClosedOnce() throws Exception {
        CountingPool pool = new CountingPool(2);
        CountingRetriever held = (CountingRetriever) borrow(pool);
        Thread borrower = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            pool.recycle(held);
        });
        borrower.start();

        pool.closePool(5000);
        borrower.join();

        assertEquals(1, held.closes.get());
        assertEquals(2, pool.totalCloses());
    }

    /** A quit that takes a while, like a real one, and notices being interrupted (which loses its chromedriver). */
    private static final class SlowRetriever extends RenderingDocumentRetriever {
        final AtomicBoolean finished = new AtomicBoolean();
        final AtomicBoolean interrupted = new AtomicBoolean();

        SlowRetriever() {
            super((RemoteWebDriver) null);
        }

        @Override
        public boolean closeAndQuit() {
            try {
                Thread.sleep(500);
                finished.set(true);
                return true;
            } catch (InterruptedException e) {
                interrupted.set(true);
                return false;
            }
        }
    }

    private static final class SlowPool extends RenderingDocumentRetrieverPool {
        /** No initializer on purpose: the super constructor fills the pool before subclass initializers run. */
        List<SlowRetriever> created;

        SlowPool(int size) {
            super(DriverManagerType.CHROME, size);
        }

        @Override
        protected synchronized RenderingDocumentRetriever createRetriever() {
            if (created == null) {
                created = Collections.synchronizedList(new ArrayList<>());
            }
            SlowRetriever retriever = new SlowRetriever();
            created.add(retriever);
            return retriever;
        }
    }

    /** closePool() used to return once the hand-back had claimed the driver and then interrupt its running quit. */
    @Test(timeout = 10000)
    public void aSlowCloseStartedByAHandBackIsAwaitedNotInterrupted() throws Exception {
        SlowPool pool = new SlowPool(1);
        SlowRetriever held = (SlowRetriever) borrow(pool);
        Thread borrower = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            pool.recycle(held);
        });
        borrower.start();

        pool.closePool(5000);
        boolean finishedWhenClosePoolReturned = held.finished.get();
        borrower.join();

        assertTrue("closePool must wait for the borrower's quit to finish", finishedWhenClosePoolReturned);
        assertFalse("the borrower's quit must not be interrupted", held.interrupted.get());
    }

    /**
     * WebKnox's own hook closes its pools alongside the JVM-wide one. The JVM-wide hook must still wait for such a
     * pool before its final sweep, or the sweep kills drivers that are being quit.
     */
    @Test(timeout = 10000)
    public void theShutdownHookWaitsForAPoolAnotherThreadIsClosing() throws Exception {
        SlowPool pool = new SlowPool(1);
        Thread otherHook = new Thread(() -> pool.closePool(0));
        otherHook.start();
        Thread.sleep(100); // the other hook has claimed the pool and is quitting its driver

        // the real hook minus its process sweep, which would kill chromedriver children of the whole test JVM
        AtomicBoolean finishedWhenTheSweepRan = new AtomicBoolean();
        RenderingDocumentRetrieverPool.closeAllPoolsThenSweep(() -> {
            finishedWhenTheSweepRan.set(pool.created.get(0).finished.get());
            return 0;
        });
        otherHook.join();

        assertTrue("the sweep ran before the pool another thread was closing had finished", finishedWhenTheSweepRan.get());
    }

    /** createObject() doubles as a factory (WebKnox's NewsFrontpageDownloader) whose callers quit the driver themselves. */
    @Test(timeout = 10000)
    public void aRetrieverClosedOutsideThePoolIsNoLongerWaitedFor() {
        RenderingDocumentRetrieverPool pool = new RenderingDocumentRetrieverPool(DriverManagerType.CHROME, 1) {
            @Override
            protected RenderingDocumentRetriever createRetriever() {
                return new RenderingDocumentRetriever((RemoteWebDriver) null);
            }
        };
        RenderingDocumentRetriever direct = pool.createObject();
        direct.closeAndQuit();

        long start = System.currentTimeMillis();
        pool.closePool(3000);

        assertTrue("closePool waited out its grace for a retriever that was already closed", System.currentTimeMillis() - start < 2000);
    }

    /** A hand-back that races closePool()'s drain must not leave a closed retriever in the queue. */
    @Test
    public void aDriverQueuedAfterTheDrainIsTakenBackAndClosed() {
        CountingPool pool = new CountingPool(1);
        CountingRetriever held = (CountingRetriever) borrow(pool);
        pool.closePool(0);
        // what recycle() does when it read closed == false just before closePool() drained the queue
        pool.requeue(held);

        pool.takeBackIfClosed(held);

        assertNull("a closed pool must not hand the driver out again", pool.poll(10, TimeUnit.MILLISECONDS));
        assertEquals("already force-closed, it must not be closed twice", 1, held.closes.get());
    }

    @Test
    public void recycleIntoAClosedPoolDoesNotCloseTwiceOrPoolTheDriver() {
        CountingPool pool = new CountingPool(1);
        CountingRetriever held = (CountingRetriever) borrow(pool);
        pool.closePool(0);

        pool.recycle(held);

        assertEquals(1, held.closes.get());
        assertNull("a closed pool must not hand the driver out again", pool.poll(10, TimeUnit.MILLISECONDS));
    }

    @Test
    public void replaceOnAClosedPoolStartsNoNewDriver() {
        CountingPool pool = new CountingPool(1);
        RenderingDocumentRetriever held = borrow(pool);
        pool.closePool(0);

        pool.replace(held, RenderingDocumentRetrieverPool.ReplaceReason.INVALIDATED);

        assertEquals("nothing would ever close a browser started now", 1, pool.getCreatedDriverCount());
    }

    @Test(timeout = 20000)
    public void concurrentClosesCloseEveryDriverExactlyOnce() throws Exception {
        CountingPool pool = new CountingPool(4);
        borrow(pool);
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> closers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Thread closer = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                pool.closePool(100);
            });
            closer.start();
            closers.add(closer);
        }
        start.countDown();
        for (Thread closer : closers) {
            closer.join();
        }

        for (CountingRetriever retriever : pool.created) {
            assertEquals(1, retriever.closes.get());
        }
    }

    @Test
    public void aFailedInitialFillClosesTheDriversItStarted() {
        List<CountingRetriever> started = new ArrayList<>();
        try {
            new RenderingDocumentRetrieverPool(DriverManagerType.CHROME, 3) {
                @Override
                protected RenderingDocumentRetriever createRetriever() {
                    if (started.size() == 2) {
                        throw new IllegalStateException("chrome would not start");
                    }
                    CountingRetriever retriever = new CountingRetriever();
                    started.add(retriever);
                    return retriever;
                }
            };
            fail("the constructor must report the failed fill");
        } catch (IllegalStateException expected) {
            // expected
        }

        assertEquals(2, started.size());
        for (CountingRetriever retriever : started) {
            assertEquals(1, retriever.closes.get());
        }
    }

    /** The shutdown sweep kills a chromedriver even while it is registered as owned: the JVM is going away. */
    @Test
    public void terminateChromedriverChildrenKillsOwnedDriversToo() throws Exception {
        Assume.assumeTrue("/bin/sh must exist", Files.exists(Paths.get("/bin/sh")));
        int port = 45873;
        Path tempDir = Files.createTempDirectory("palladian-chromedriver-test");
        Process process = null;
        try {
            Path executable = tempDir.resolve("chromedriver-test");
            Files.writeString(executable, "#!/bin/sh\nsleep 60 & wait\n");
            Assume.assumeTrue(executable.toFile().setExecutable(true));
            process = new ProcessBuilder(executable.toString(), "--port=" + port).start();
            ProcessHandle driver = process.toHandle();
            ProcessHandle browser = waitForChild(driver);
            ChromedriverProcessRegistry.registerPort(port);

            int killed = RenderingDocumentRetrieverPool.terminateChromedriverChildren(ProcessHandle.current());

            assertTrue(killed >= 1);
            assertProcessDead(driver);
            assertProcessDead(browser);
        } finally {
            ChromedriverProcessRegistry.unregisterPort(port);
            if (process != null) {
                RenderingDocumentRetrieverPool.terminateProcessTree(process.toHandle());
            }
            deleteRecursively(tempDir);
        }
    }

    private static ProcessHandle waitForChild(ProcessHandle parent) throws InterruptedException {
        long deadline = System.currentTimeMillis() + PROCESS_WAIT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            ProcessHandle child = parent.children().findFirst().orElse(null);
            if (child != null) {
                return child;
            }
            Thread.sleep(25);
        }
        fail("Timed out waiting for child process of pid=" + parent.pid());
        return null;
    }

    private static void assertProcessDead(ProcessHandle process) throws InterruptedException {
        long deadline = System.currentTimeMillis() + PROCESS_WAIT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                return;
            }
            Thread.sleep(25);
        }
        fail("Process still alive pid=" + process.pid());
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        List<Path> paths = new ArrayList<>();
        Files.walk(dir).forEach(paths::add);
        Collections.reverse(paths);
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }
}
