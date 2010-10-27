/**
 * 
 */
package tud.iir.news.evaluation;

/**
 * @author Sandro Reichert
 *
 */
public class EvaluationItemIntervalItem {

    /* FeedID */
    private int feedID = -1; 
    
    /* the feed's update class, e.g. zombie, constant */
    private int activityPattern = -1;
    
    /* the average number of new entries per day */
    private double averageEntriesPerDay = -1;
    
    /* the feed's median item interval in minutes (several items may be updated at the same time) */
    private long medianItemInterval = -1;
    
    /* the feed's average update interval in minutes (one update may contain several items) */
    private String averageUpdateInterval = "-1";
    
    
    /**
     * 
     */
    public EvaluationItemIntervalItem() {
        // TODO Auto-generated constructor stub
    }


    /**
     * @return the feedID
     */
    public final int getFeedID() {
        return feedID;
    }


    /**
     * @param feedID the feedID to set
     */
    public final void setFeedID(int feedID) {
        this.feedID = feedID;
    }


    /**
     * the feed's update class,also called activityPattern, e.g. zombie, constant
     * @return the activityPattern
     */
    public final int getActivityPattern() {
        return activityPattern;
    }


    /**
     * the feed's update class,also called activityPattern, e.g. zombie, constant
     * @param activityPattern the activityPattern to set
     */
    public final void setActivityPattern(int activityPattern) {
        this.activityPattern = activityPattern;
    }


    /**
     * @return the averageEntriesPerDay
     */
    public final double getAverageEntriesPerDay() {
        return averageEntriesPerDay;
    }


    /**
     * @param averageEntriesPerDay the averageEntriesPerDay to set
     */
    public final void setAverageEntriesPerDay(double averageEntriesPerDay) {
        this.averageEntriesPerDay = averageEntriesPerDay;
    }


    /**
     * the feed's median item interval in minutes (several items may be updated at the same time) 
     * 
     * @return the feed's median item interval in minutes
     */
    public final long getMedianItemInterval() {
        return medianItemInterval;
    }


    /**
     * @param medianItemInterval the feed's median item interval in minutes
     */
    public final void setMedianItemInterval(long medianItemInterval) {
        this.medianItemInterval = medianItemInterval;
    }


    /**
     * the feed's average update interval in minutes (one update may contain several items)
     * @return the feeds average update interval in minutes
     */
    public final String getAverageUpdateInterval() {
        return averageUpdateInterval;
    }


    /**
     * @param averageUpdateInterval the feed's average update interval in minutes
     */
    public final void setAverageUpdateInterval(String averageUpdateInterval) {
        this.averageUpdateInterval = averageUpdateInterval;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + activityPattern;
        long temp;
        temp = Double.doubleToLongBits(averageEntriesPerDay);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((averageUpdateInterval == null) ? 0 : averageUpdateInterval.hashCode());
        result = prime * result + feedID;
        result = prime * result + (int) (medianItemInterval ^ (medianItemInterval >>> 32));
        return result;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EvaluationItemIntervalItem other = (EvaluationItemIntervalItem) obj;
        if (activityPattern != other.activityPattern)
            return false;
        if (Double.doubleToLongBits(averageEntriesPerDay) != Double.doubleToLongBits(other.averageEntriesPerDay))
            return false;
        if (averageUpdateInterval == null) {
            if (other.averageUpdateInterval != null)
                return false;
        } else if (!averageUpdateInterval.equals(other.averageUpdateInterval))
            return false;
        if (feedID != other.feedID)
            return false;
        if (medianItemInterval != other.medianItemInterval)
            return false;
        return true;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "EvaluationItemIntervals [feedID=" + feedID + ", activityPattern=" + activityPattern
                + ", averageEntriesPerDay=" + averageEntriesPerDay + ", medianItemInterval=" + medianItemInterval
                + ", averageUpdateInterval=" + averageUpdateInterval + "]";
    }

}
