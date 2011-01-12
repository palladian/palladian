package tud.iir.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

import tud.iir.helper.DateArrayHelper;

/**
 * Template for rater classes.
 * Each technique that evaluates dates should implements this.
 * 
 * @author Martin Gregor
 * 
 * @param <T>
 */
public abstract class TechniqueDateRater<T> {

	
	
	protected PageDateType dateType;
	
	public TechniqueDateRater(PageDateType dateType) {
		this.dateType = dateType;
	}
	
	/**
	 * Rate-method fills this map for further use.
	 */
	protected HashMap<T, Double> ratedDates = new HashMap<T, Double>();
	
    /**
     * Enter a list of dates. <br>
     * These will be rated in dependency of date-technique.
     * 
     * @param list
     * @return
     */
    public abstract HashMap<T, Double> rate(ArrayList<T> list);
    
    /**
     * Returns best rated date of property "ratedDates". <br>
     * In case of more than one best date the first one will be returned. <br>
     * For other function override this method in subclasses.
     * @return
     */
    public T getBestDate(){
    	T date  = null;
    	if(this.ratedDates.size() > 0){
    		double rate  = DateArrayHelper.getHighestRate(this.ratedDates);
    		date = DateArrayHelper.getRatedDates(this.ratedDates, rate).get(0);
    	}
    	
    	return date;
    }
}