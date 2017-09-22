package ws.palladian.helper.date;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ws.palladian.helper.ThreadHelper;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * Run a certain task at a certain time.
 * </p>
 *
 * @author David Urbansky
 */
public class Scheduler {

    /**
     * Check scheduled tasks every 20 seconds.
     */
    private final int checkInterval = 20000;

    private final Set<Pair<Runnable, Schedule>> tasks;

    private Scheduler() {
        tasks = Collections.synchronizedSet(new HashSet<>());
        runPeriodicTimeCheck();
    }

    static class SingletonHolder {
        static Scheduler instance = new Scheduler();
    }

    public static Scheduler getInstance() {
        return SingletonHolder.instance;
    }

    public void addTask(Runnable runnable, Schedule schedule) {
        Pair<Runnable, Schedule> pair = new ImmutablePair<>(runnable, schedule);
        tasks.add(pair);
    }

    public void addTask(Pair<Runnable, Schedule> pair) {
        tasks.add(pair);
    }

    private void runPeriodicTimeCheck() {

        (new Thread() {
            @Override
            public void run() {
                while (true) {
                    Date currentDate = new Date();

                    synchronized (tasks) {
                        // check all tasks
                        tasks.stream().filter(task -> task.getRight().onSchedule(currentDate)).forEach(task -> {
                            task.getLeft().run();
                        });
                    }

                    ThreadHelper.deepSleep(checkInterval);
                }
            }
        }).start();

    }

    public static void main(String[] args) {
        Schedule schedule = new Schedule();
        schedule.setHourOfDay(23);
        schedule.setMinuteOfHour(21);

        Thread runnable = new Thread() {
            @Override
            public void run() {
                System.out.println("I'm on schedule!!!");
            }
        };

        Scheduler.getInstance().addTask(runnable, schedule);
    }

}
