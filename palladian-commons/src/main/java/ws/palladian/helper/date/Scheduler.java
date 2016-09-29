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
     * Check scheduled tasks every 60 seconds.
     */
    private final int checkInterval = 60000;

    private Set<Pair<Runnable, Schedule>> tasks;

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

    private void runPeriodicTimeCheck() {

        (new Thread() {
            @Override
            public void run() {
                while (true) {
                    Date currentDate = new Date();

                    // check all tasks
                    for (Pair<Runnable, Schedule> task : tasks) {
                        if (task.getRight().onSchedule(currentDate)) {
                            task.getLeft().run();
                        }
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
