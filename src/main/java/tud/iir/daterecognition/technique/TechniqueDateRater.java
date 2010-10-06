package tud.iir.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Template for rater classes.
 * Each technique that evaluates dates should implements this.
 * 
 * @author Martin Gregor
 * 
 * @param <T>
 */
public abstract class TechniqueDateRater<T> {

    /**
     * Enter a list of dates. <br>
     * These will be rated in dependency of date-technique.
     * 
     * @param list
     * @return
     */
    public abstract HashMap<T, Double> rate(ArrayList<T> list);
}