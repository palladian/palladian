package ws.palladian.helper.date;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ws.palladian.helper.ThreadHelper;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Run a certain task at a certain time.
 * </p>
 *
 * @author David Urbansky
 */
public class Scheduler {
    /**
     * Check scheduled tasks every 10 seconds.
     */
    private final long checkInterval = TimeUnit.SECONDS.toMillis(10);

    final Set<Pair<Runnable, Schedule>> tasks;
    Map<String, List<Runnable>> taggedTasks;

    /**
     * Collect errors.
     */
    private final List<Throwable> errors = new ArrayList<>();

    protected Scheduler() {
        tasks = Collections.synchronizedSet(new HashSet<>());
        taggedTasks = Collections.synchronizedMap(new HashMap<>());
        runPeriodicTimeCheck();
    }

    static class SingletonHolder {
        static Scheduler instance = new Scheduler();
    }

    public static Scheduler getInstance() {
        return SingletonHolder.instance;
    }

    public void addTask(Runnable runnable, Schedule schedule, String... tags) {
        Pair<Runnable, Schedule> pair = new ImmutablePair<>(runnable, schedule);
        for (String tag : tags) {
            List<Runnable> runnables = taggedTasks.computeIfAbsent(tag, k -> new ArrayList<>());
            runnables.add(runnable);
        }
        tasks.add(pair);
    }

    public void addTask(Pair<Runnable, Schedule> pair) {
        tasks.add(pair);
    }

    public List<Runnable> getTasks(String tag) {
        return taggedTasks.get(tag);
    }

    public void removeTasks(String tag) {
        List<Runnable> taggedRunnables = taggedTasks.get(tag);
        for (Runnable taggedRunnable : taggedRunnables) {
            List<Pair<Runnable, Schedule>> toRemove = new ArrayList<>();
            for (Pair<Runnable, Schedule> task : tasks) {
                if (task.getKey().equals(taggedRunnable)) {
                    toRemove.add(task);
                }
            }
            synchronized (tasks) {
                tasks.removeAll(toRemove);
            }
        }
        taggedTasks.remove(tag);
    }

    private void runPeriodicTimeCheck() {
        (new Thread() {
            @Override
            public void run() {
                while (true) {
                    Date currentDate = new Date();

                    // check all tasks
                    try {
                        tasks.stream().filter(task -> task.getRight().onSchedule(currentDate)).forEach(task -> {
                            try {
                                task.getLeft().run();
                            } catch (Exception e) {
                                errors.add(e);
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    ThreadHelper.deepSleep(checkInterval);
                }
            }
        }).start();
    }

    public List<Throwable> getErrors() {
        return errors;
    }

    public static void main(String[] args) {
        Schedule schedule = new Schedule();
        schedule.setHourOfDay(17);
        schedule.setMinuteOfHour(12);
        Schedule schedule2 = new Schedule();
        schedule2.setHourOfDay(17);
        schedule2.setMinuteOfHour(10);
        Schedule schedule3 = new Schedule();
        schedule3.setHourOfDay(17);
        schedule3.setMinuteOfHour(11);

        Thread runnable = new Thread() {
            @Override
            public void run() {
                System.out.println("I'm on schedule 1");
            }
        };
        Thread runnable2 = new Thread() {
            @Override
            public void run() {
                System.out.println("I'm on schedule 2");
            }
        };
        Thread runnable3 = new Thread() {
            @Override
            public void run() {
                System.out.println("I'm on schedule 3");
            }
        };

        Scheduler.getInstance().addTask(runnable, schedule);
        Scheduler.getInstance().addTask(runnable2, schedule2);
        Scheduler.getInstance().addTask(runnable3, schedule3);
        Scheduler.getInstance().addTask(runnable3, schedule3, "s3");

        Scheduler.getInstance().removeTasks("s3");
    }
}
