package ws.palladian.extraction.date.helper;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.ExtractedDateImpl;

/**
 * Helper functions for arrays consisting extracted dates or subclasses.
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public final class DateExtractionHelper {
    
    private DateExtractionHelper() {
        // utility class, no instances.
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExtractedDate> List<T> filter(List<? extends ExtractedDate> dates, Class<T> filter) {
        List<T> ret = new ArrayList<T>();
        for (ExtractedDate date : dates) {
            if (filter.isInstance(date)) {
                ret.add((T)date);
            }
        }
        return ret;
    }

    public static <T extends ExtractedDate> List<T> filterByRange(List<T> dates) {
        List<T> result = new ArrayList<T>(dates);
        CollectionHelper.filter(result, new Filter<T>() {
            @Override
            public boolean accept(T date) {
                return isDateInRange(date);
            }
        });
        return result;
    }

    public static <T extends ExtractedDate> List<T> filterFullDate(List<T> dates) {
        List<T> result = new ArrayList<T>(dates);
        CollectionHelper.filter(result, new Filter<T>() {
            @Override
            public boolean accept(T date) {
                return date.get(ExtractedDate.YEAR) != -1 && date.get(ExtractedDate.MONTH) != -1
                        && date.get(ExtractedDate.DAY) != -1;
            }
        });
        return result;
    }

    /**
     * Group equal dates in array lists. <br>
     * E.g. d1=May 2010; d2=05.2010; d3=01.05.10; d4=01st May '10 --> (d1&d2) &
     * (d3&d4). <br>
     * Every date can be only in one group.<br>
     * A group is a array list of dates.
     * 
     * @param <T>
     * @param dates
     *            Arraylist of dates.
     * @return A arraylist of groups, that are arraylists too.
     */
    public static <T extends ExtractedDate> List<List<T>> cluster(List<T> dates, DateExactness compareDepth) {
        List<List<T>> clusters = new ArrayList<List<T>>();
        DateComparator dc = new DateComparator(compareDepth);
        for (T date : dates) {
            boolean sameDatestamp = false;
            for (int resultIndex = 0; resultIndex < clusters.size(); resultIndex++) {
                T firstDate = clusters.get(resultIndex).get(0);
                int compare = dc.compare(firstDate, date);
                if (compare == 0) {
                    clusters.get(resultIndex).add(date);
                    sameDatestamp = true;
                    break;
                }
            }
            if (!sameDatestamp) {
                List<T> newDate = new ArrayList<T>();
                newDate.add(date);
                clusters.add(newDate);
            }
        }
        return clusters;
    }

    /**
     * Count how often a date is in a list.<br>
     * Dates will compared up to depth of stopFlag. <br>
     * Not the object, but the exact date.<br>
     * If the date-object is also in the list, it will not count.<br>
     * <br>
     * E.g.: list={date1,date2,date3} and date1 = date2 != date3. <br>
     * Look up for date1, the returning value will be 1 and not 2!
     * 
     * @param <T>
     * @param date
     * @param dates
     * @return
     */
    public static int countDates(ExtractedDate date, List<? extends ExtractedDate> dates, DateExactness exactness) {
        int count = 0;
        for (ExtractedDate currentDate : dates) {
            if (date.equals(currentDate)) {
                continue;
            }
            DateExactness thisExactness = exactness;
            if (exactness == DateExactness.UNSET) {
                thisExactness = DateExactness.getCommonExactness(date.getExactness(), currentDate.getExactness());
            }
            DateComparator dc = new DateComparator(thisExactness);
            if (dc.compare(date, currentDate) == 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns an array of dates, that have a given rate.
     * 
     * @param <T>
     * @param dates
     * @param rate
     * @return
     */
    public static <E extends ExtractedDate> List<E> getRatedDates(List<RatedDate<E>> dates, double rate) {
        List<E> result = new ArrayList<E>();
        for (RatedDate<E> date : dates) {
            if (date.getRate() == rate) {
                result.add(date.getDate());
            }
        }
        return result;
    }

    /**
     * Returns a hashmap of date are equal to given date.<br>
     * Date comparison stops at stopFlag.
     * 
     * @param <T>
     * @param date
     * @param dates
     * @return
     */
    public static <T extends ExtractedDate> List<T> getSameDatesMap(ExtractedDate date, List<T> dates,
            DateExactness compareDepth) {
        DateComparator dc = new DateComparator(compareDepth);
        List<T> result = CollectionHelper.newArrayList();
        for (T currentDate : dates) {
            if (dc.compare(date, currentDate) == 0) {
                result.add(currentDate);
            }
        }
        return result;
    }

    /**
     * Check if all values of hashmap are zero.
     * 
     * @param <T>
     * @param dates
     * @return
     */
    public static boolean isAllZero(List<? extends RatedDate<?>> dates) {
        for (RatedDate<?> date : dates) {
            if (date.getRate() > 0) {
                return false;
            }
        }
        return true;
    }

    public static <D extends ExtractedDate> List<D> filterExactest(List<D> dates) {
        DateExactness highestExactness = DateExactness.UNSET;
        for (D date : dates) {
            DateExactness currentExactness = date.getExactness();
            if (highestExactness.inRange(currentExactness)) {
                highestExactness = currentExactness;
            }
        }
        List<D> result = CollectionHelper.newArrayList();
        for (D date : dates) {
            if (date.getExactness() == highestExactness) {
                result.add(date);
            }
        }
        return result;
    }

    public static double getHighestRate(List<? extends RatedDate<?>> ratedDates) {
        double highest = 0;
        for (RatedDate<?> ratedDate : ratedDates) {
            highest = Math.max(highest, ratedDate.getRate());
        }
        return highest;
    }
    
    /**
     * Checks if a date is between 13th of November 1990, time 0:00 and now.
     * 
     * @param date
     * @return
     */
    public static boolean isDateInRange(ExtractedDate date) {
        ExtractedDate begin = DateParser.parseDate("1990-11-13T00:00:00Z", RegExp.DATE_ISO8601_YMD_T);
        ExtractedDate end = new ExtractedDateImpl();
        DateExactness compareDepth = DateExactness.DAY;
        if  (date.getExactness() != DateExactness.UNSET) {
            compareDepth = DateExactness.getCommonExactness(DateExactness.DAY, date.getExactness());
        }
        DateComparator dateComparator = new DateComparator(compareDepth);
        boolean gt = dateComparator.compare(begin, date) > -1;
        boolean lt = dateComparator.compare(date, end) > -1;
        return gt && lt;
    }



    /**
     * Sets for all dates from {@link List} the rate-value to given value in map.
     * 
     * @param <T>
     * @param dates
     * @param rate
     * @return 
     */
    public static <T extends ExtractedDate> List<RatedDate<T>> setRate(List<T> dates, double rate) {
        List<RatedDate<T>> result = CollectionHelper.newArrayList();
        for (int i = 0; i < dates.size(); i++) {
            result.add(RatedDate.create(dates.get(i), rate));
        }
        return result;
    }

}
