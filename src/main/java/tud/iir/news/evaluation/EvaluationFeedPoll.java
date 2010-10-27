package tud.iir.news.evaluation;

/**
 * Represents one poll of the feed reader evaluation for www2011-paper.
 * 
 * @author Sandro Reichert 
 */
public class EvaluationFeedPoll {	
	
	/* FeedID */
	private int feedID = -1; 
	
	/* the feed's update class, e.g. zombie, constant */
	private int activityPattern = -1;
    
	/* true if the feed supports the eTag functionality */
	private Boolean supportsETag = false;
    
	/* true if the feed supports the conditional get functionality (HTTP 304)*/
	private Boolean supportsConditionalGet = false;
    
	/* size of the returned header in Bytes if supportsETag = true and no new entries found */
	private int eTagResponseSize = -1;
    
	/* size of the returned header in Bytes if supportsConditionalGet = true and feed has not changed */
	private int conditionalGetResponseSize = -1;
    
	/* the number of the current poll */ 
	private int numberOfPoll = -1;	
	
	/* the feed has been pooled at this TIMESTAMP, format milliseconds since 01.01.1970 00:00 */
	private long pollTimestamp = -1;                                                            
	
	/* the hour of the day the feed has been polled */
	private int pollHourOfDay = -1;
	
	/* the minute of the day the feed has been polled */
	private int pollMinuteOfDay = -1;                
	
	/* time in minutes we waited between last and current check */
	private float checkInterval = -1;
	
	/* the current size of the feed's window (number of items found) */
	private int windowSize = -1;
	
	/* the amount of bytes that has been downloadad */
	private float sizeOfPoll = -1;                 
	
	/* the number of new items we missed because there more new items since the last poll than fit into the window */
	private int numberMissedNewEntries = -1;
	
	/* the percentage of new entries within this poll. 1 means all but one entry is new, >1 means that all entries are new and we probably missed new entries */
	private float percentageNewEntries = -1;                                                                 
	
	/* late or early (negative value) in seconds, is the time span between timestamp poll and timestamp(s) next or last new entry(ies) */
	private double delay = -1;              
	
	/* the score in MAX mode = percentageNewEntries iff percentageNewEntries <=1 OR (1 - numberMissedNewEntries/windowSize) iff percentageNewEntries > 1 */
	private float scoreMax = -1;
	
	/* the score in MIN mode = (d/int + 1)^-1 ; score is in (0,1] 1 is perfect, 0.5 means culmulated delay (d) is equalt to current interval (int) */
    private float scoreMin = -1;
    
    
	
	public EvaluationFeedPoll() {
        super();
    }

    public final void feedID(int feedID) {
        this.feedID = feedID;
    }

    public final void setActivityPattern(int activityPattern) {
        this.activityPattern = activityPattern;
    }

    public final void setSupportsETag(Boolean supportsETag) {
        this.supportsETag = supportsETag;
    }

    public final void setSupportsConditionalGet(Boolean supportsConditionalGet) {
        this.supportsConditionalGet = supportsConditionalGet;
    }

    public final void seteTagResponseSize(int eTagResponseSize) {
        this.eTagResponseSize = eTagResponseSize;
    }

    public final void setConditionalGetResponseSize(int conditionalGetResponseSize) {
        this.conditionalGetResponseSize = conditionalGetResponseSize;
    }

    public final void setNumberOfPoll(int numberOfPoll) {
        this.numberOfPoll = numberOfPoll;
    }

    public final void setPollTimestamp(long pollTimestamp) {
        this.pollTimestamp = pollTimestamp;
    }

    public final void setPollHourOfDay(int pollHourOfDay) {
        this.pollHourOfDay = pollHourOfDay;
    }

    public final void setPollMinuteOfDay(int pollMinuteOfDay) {
        this.pollMinuteOfDay = pollMinuteOfDay;
    }

    public final void setCheckInterval(float checkInterval) {
        this.checkInterval = checkInterval;
    }

    public final void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public final void setSizeOfPoll(float sizeOfPoll) {
        this.sizeOfPoll = sizeOfPoll;
    }

    public final void setNumberMissedNewEntries(int numberMissedNewEntries) {
        this.numberMissedNewEntries = numberMissedNewEntries;
    }

    public final void setPercentageNewEntries(float percentageNewEntries) {
        this.percentageNewEntries = percentageNewEntries;
    }

    public final void setDelay(double delay) {
        this.delay = delay;
    }

    public final void setScoreMin(float scoreMin) {
        this.scoreMin = scoreMin;
    }

    public final void setScoreMax(float scoreMax) {
        this.scoreMax = scoreMax;
    }

    public final int getFeedID() {
		return feedID;
	}

	public final int getActivityPattern() {
		return activityPattern;
	}

	public final Boolean getSupportsETag() {
		return supportsETag;
	}

	public final Boolean getSupportsConditionalGet() {
		return supportsConditionalGet;
	}

	public final int geteTagResponseSize() {
		return eTagResponseSize;
	}

	public final int getConditionalGetResponseSize() {
		return conditionalGetResponseSize;
	}

	public final int getNumberOfPoll() {
		return numberOfPoll;
	}

	public final long getPollTimestamp() {
		return pollTimestamp;
	}

	public final int getPollHourOfDay() {
		return pollHourOfDay;
	}

	public final int getPollMinuteOfDay() {
		return pollMinuteOfDay;
	}

	public final float getCheckInterval() {
		return checkInterval;
	}

	public final int getWindowSize() {
		return windowSize;
	}

	public final float getSizeOfPoll() {
		return sizeOfPoll;
	}

	public final int getNumberMissedNewEntries() {
		return numberMissedNewEntries;
	}

	public final float getPercentageNewEntries() {
		return percentageNewEntries;
	}

	public final double getDelay() {
		return delay;
	}

	public final float getScoreMin() {
		return scoreMin;
	}

	public final float getScoreMax() {
		return scoreMax;
	}

	
    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + activityPattern;
		result = prime * result + Float.floatToIntBits(checkInterval);
		result = prime * result + conditionalGetResponseSize;
		long temp;
		temp = Double.doubleToLongBits(delay);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + eTagResponseSize;
		result = prime * result + feedID;
		result = prime * result + numberMissedNewEntries;
		result = prime * result + numberOfPoll;
		result = prime * result + Float.floatToIntBits(percentageNewEntries);
		result = prime * result + pollHourOfDay;
		result = prime * result + pollMinuteOfDay;
		result = prime * result
				+ (int) (pollTimestamp ^ (pollTimestamp >>> 32));
		result = prime * result + Float.floatToIntBits(scoreMax);
		result = prime * result + Float.floatToIntBits(scoreMin);
		result = prime * result + Float.floatToIntBits(sizeOfPoll);
		result = prime
				* result
				+ ((supportsConditionalGet == null) ? 0
						: supportsConditionalGet.hashCode());
		result = prime * result
				+ ((supportsETag == null) ? 0 : supportsETag.hashCode());
		result = prime * result + windowSize;
		return result;
	}

	
	 /*
     * (non-Javadoc)
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
		EvaluationFeedPoll other = (EvaluationFeedPoll) obj;
		if (activityPattern != other.activityPattern)
			return false;
		if (Float.floatToIntBits(checkInterval) != Float
				.floatToIntBits(other.checkInterval))
			return false;
		if (conditionalGetResponseSize != other.conditionalGetResponseSize)
			return false;
		if (Double.doubleToLongBits(delay) != Double
				.doubleToLongBits(other.delay))
			return false;
		if (eTagResponseSize != other.eTagResponseSize)
			return false;
		if (feedID != other.feedID)
			return false;
		if (numberMissedNewEntries != other.numberMissedNewEntries)
			return false;
		if (numberOfPoll != other.numberOfPoll)
			return false;
		if (Float.floatToIntBits(percentageNewEntries) != Float
				.floatToIntBits(other.percentageNewEntries))
			return false;
		if (pollHourOfDay != other.pollHourOfDay)
			return false;
		if (pollMinuteOfDay != other.pollMinuteOfDay)
			return false;
		if (pollTimestamp != other.pollTimestamp)
			return false;
		if (Float.floatToIntBits(scoreMax) != Float
				.floatToIntBits(other.scoreMax))
			return false;
		if (Float.floatToIntBits(scoreMin) != Float
				.floatToIntBits(other.scoreMin))
			return false;
		if (Float.floatToIntBits(sizeOfPoll) != Float
				.floatToIntBits(other.sizeOfPoll))
			return false;
		if (supportsConditionalGet == null) {
			if (other.supportsConditionalGet != null)
				return false;
		} else if (!supportsConditionalGet.equals(other.supportsConditionalGet))
			return false;
		if (supportsETag == null) {
			if (other.supportsETag != null)
				return false;
		} else if (!supportsETag.equals(other.supportsETag))
			return false;
		if (windowSize != other.windowSize)
			return false;
		return true;
	}
	

    @Override
    public String toString() {
        return "EvaluationDataObject [feedID=" + feedID + ", activityPattern=" + activityPattern + ", supportsETag="
                + supportsETag + ", supportsConditionalGet=" + supportsConditionalGet + ", eTagResponseSize="
                + eTagResponseSize + ", conditionalGetResponseSize=" + conditionalGetResponseSize + ", numberOfPoll="
                + numberOfPoll + ", pollTimestamp=" + pollTimestamp + ", pollHourOfDay=" + pollHourOfDay
                + ", pollMinuteOfDay=" + pollMinuteOfDay + ", checkInterval=" + checkInterval + ", windowSize="
                + windowSize + ", sizeOfPoll=" + sizeOfPoll + ", numberMissedNewEntries=" + numberMissedNewEntries
                + ", percentageNewEntries=" + percentageNewEntries + ", delay=" + delay + ", scoreMin=" + scoreMin
                + ", scoreMax=" + scoreMax + "]";
    }

	
}
