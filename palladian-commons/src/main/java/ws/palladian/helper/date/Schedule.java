package ws.palladian.helper.date;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <p>A simple schedule. Any null field means it does not matter for matching with the date.</p>
 * Created by David on 10.07.2015.
 */
public class Schedule {

    private long lastRun = -1;

    private Integer dayOfYear;
    private Integer dayOfMonth;
    private Integer dayOfWeek;
    private Integer hourOfDay;
    private Integer minuteOfHour;

    public boolean onSchedule(Date date) {

        // we only resolve meticulous accuracy so the task must not have run within the last 60 seconds
        if (date.getTime() - TimeUnit.SECONDS.toMillis(60) < lastRun) {
            return false;
        }

        boolean onSchedule = true;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        if (dayOfYear != null) {
            if (calendar.get(Calendar.DAY_OF_YEAR) != dayOfYear) {
                onSchedule = false;
            }
        }
        if (dayOfMonth != null) {
            if (calendar.get(Calendar.DAY_OF_MONTH) != dayOfMonth) {
                onSchedule = false;
            }
        }
        if (dayOfWeek != null) {
            if (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                onSchedule = false;
            }
        }
        if (hourOfDay != null) {
            if (calendar.get(Calendar.HOUR_OF_DAY) != hourOfDay) {
                onSchedule = false;
            }
        }
        if (minuteOfHour != null) {
            if (calendar.get(Calendar.MINUTE) != minuteOfHour) {
                onSchedule = false;
            }
        }

        lastRun = System.currentTimeMillis();
        return onSchedule;
    }

    public Integer getDayOfYear() {
        return dayOfYear;
    }

    public void setDayOfYear(Integer dayOfYear) {
        this.dayOfYear = dayOfYear;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getHourOfDay() {
        return hourOfDay;
    }

    public void setHourOfDay(Integer hourOfDay) {
        this.hourOfDay = hourOfDay;
    }

    public Integer getMinuteOfHour() {
        return minuteOfHour;
    }

    public void setMinuteOfHour(Integer minuteOfHour) {
        this.minuteOfHour = minuteOfHour;
    }
}
