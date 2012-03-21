package ws.palladian.helper;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

public class ThreadHelper {

    public static void deepSleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    public static void deepSleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    public static Set<Thread> getAllDaemonThreads() {
        Set<Thread> daemonThreads = new HashSet<Thread>();

        Set<Thread> threadSet = getAllThreads();
        for (Thread thread : threadSet) {
            if (thread.isDaemon()) {
                daemonThreads.add(thread);
            }
        }

        return daemonThreads;
    }

    public static Set<Thread> getAllNonDaemonThreads() {
        Set<Thread> nonDaemonThreads = new HashSet<Thread>();

        Set<Thread> threadSet = getAllThreads();
        for (Thread thread : threadSet) {
            if (!thread.isDaemon()) {
                nonDaemonThreads.add(thread);
            }
        }

        return nonDaemonThreads;
    }

    public static Set<Thread> getAllThreads() {
        // final ThreadGroup root = getRootThreadGroup();
        // final ThreadMXBean thbean = ManagementFactory.getThreadMXBean();
        // int nAlloc = thbean.getThreadCount();
        // int n = 0;
        // Thread[] threads;
        // do {
        // nAlloc *= 2;
        // threads = new Thread[nAlloc];
        // n = root.enumerate(threads, true);
        // } while (n == nAlloc);
        // return java.util.Arrays.copyOf(threads, n);
        return Thread.getAllStackTraces().keySet();
    }

    // public static Thread[] getAllThreads_(final Thread.State state) {
    // final Thread[] allThreads = getAllThreads();
    // final Thread[] found = new Thread[allThreads.length];
    // int nFound = 0;
    // for (Thread thread : allThreads) {
    // if (thread.getState() == state) {
    // found[nFound++] = thread;
    // }
    // }
    // return java.util.Arrays.copyOf(found, nFound);
    // }

    public static Set<Thread> getAllThreads(Thread.State state) {
        Set<Thread> stateThreads = new HashSet<Thread>();

        Set<Thread> threadSet = getAllThreads();
        for (Thread thread : threadSet) {
            if (thread.getState() == state) {
                stateThreads.add(thread);
            }
        }

        return stateThreads;
    }

    public static ThreadGroup getRootThreadGroup() {
        // if ( rootThreadGroup != null )
        // return rootThreadGroup;
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        ThreadGroup ptg;
        while ((ptg = tg.getParent()) != null) {
            tg = ptg;
        }
        return tg;
    }
}
