package tud.iir.news.evaluation;

/**
 * Represents one poll of the feed reader evaluation for www2011-paper.
 * 
 * @author Sandro Reichert 
 */
public class EvaluationFeedPoll {	
	
	/* FeedID */
	private int feedID = -1; 
    private boolean feedIDValid = false;
	
	/* the feed's update class, e.g. zombie, constant */
	private int activityPattern = -1;
    private boolean activityPatternValid = false;
    
	/* true if the feed supports the eTag functionality */
	private Boolean supportsETag = false;
    private boolean supportsETagValid = false;
    
	/* true if the feed supports the conditional get functionality (HTTP 304)*/
	private Boolean supportsConditionalGet = false;
    private boolean supportsConditionalGetValid = false;
    
	/* size of the returned header in Bytes if supportsETag = true and no new entries found */
	private int eTagResponseSize = -1;
    private boolean eTagResponseSizeValid = false;
    
	/* size of the returned header in Bytes if supportsConditionalGet = true and feed has not changed */
	private int conditionalGetResponseSize = -1;
    private boolean conditionalGetResponseSizeValid = false;
    
	/* the number of the current poll */ 
    private int numberOfPoll = -1;
    private boolean numberOfPollValid = false;
	
	/* the feed has been pooled at this TIMESTAMP, format milliseconds since 01.01.1970 00:00 */
    private long pollTimestamp = -1;
    private boolean pollTimestampValid = false;
	
	/* the hour of the day the feed has been polled */
	private int pollHourOfDay = -1;
    private boolean pollHourOfDayValid = false;
	
	/* the minute of the day the feed has been polled */
    private int pollMinuteOfDay = -1;
    private boolean pollMinuteOfDayValid = false;
	
	/* time in minutes we waited between last and current check */
    private int checkInterval = -1;
    private boolean checkIntervalValid = false;
	
	/* the current size of the feed's window (number of items found) */
	private int windowSize = -1;
    private boolean windowSizeValid = false;
	
	/* the amount of bytes that has been downloaded */
    private int sizeOfPoll = -1;
    private boolean sizeOfPollValid = false;
	
	/* the number of new items we missed because there more new items since the last poll than fit into the window */
	private int numberMissedNewEntries = -1;
    private boolean numberMissedNewEntriesValid = false;
	
	/* the percentage of new entries within this poll. 1 means all but one entry is new, >1 means that all entries are new and we probably missed new entries */
    private float percentageNewEntries = -1;
    private boolean percentageNewEntriesValid = false;
	
	/* late or early (negative value) in seconds, is the time span between timestamp poll and timestamp(s) next or last new entry(ies) */
    private double delay = -1;
    private boolean delayValid = false;
	
	/* the score in MAX mode = percentageNewEntries iff percentageNewEntries <=1 OR (1 - numberMissedNewEntries/windowSize) iff percentageNewEntries > 1 */
	private float scoreMax = -1;
    private boolean scoreMaxValid = false;
	
	/* the score in MIN mode = (d/int + 1)^-1 ; score is in (0,1] 1 is perfect, 0.5 means culmulated delay (d) is equalt to current interval (int) */
    private float scoreMin = -1;
    private boolean scoreMinValid = false;

    /* an average value like average scoreMin, scoreMax or percentageNewEntries, calculated for some diagrams */
    private double averageValue = -1;
    private boolean averageValueValid = false;
    
    private int dayOfYear = -1;
    private boolean dayOfYearValid = false;

    private long cumulatedSizeofPolls = -1;
    private boolean cumulatedSizeofPollsValid = false;
    
    private int hourOfExperiment = -1;
    private boolean hourOfExperimentValid = false;

    public EvaluationFeedPoll() {
        super();
    }

    /**
     * @return the averageValue
     */
    public final double getAverageValue() {
        if (!averageValueValid)
            throw new IllegalStateException("feeID " + feedID + ": averageValue not initialized!");
        return averageValue;
    }

    /**
     * @param averageValue the averageValue to set
     */
    public final void setAverageValue(double averageValue) {
        this.averageValue = averageValue;
        this.averageValueValid = true;
    }

    /**
     * @param eTagResponseSize the eTagResponseSize to set
     */
    public final void seteTagResponseSize(int eTagResponseSize) {
        this.eTagResponseSize = eTagResponseSize;
        this.eTagResponseSizeValid = true;
    }

    
    /**
     * @return the hourOfExperiment
     */
    public final int getHourOfExperiment() {
        if (!hourOfExperimentValid)
            throw new IllegalStateException("feeID " + feedID + ": hourOfExperiment not initialized!");
        return hourOfExperiment;
    }

    /**
     * @param hourOfExperiment the hourOfExperiment to set
     */
    public final void setHourOfExperiment(int hourOfExperiment) {
        this.hourOfExperiment = hourOfExperiment;
        this.hourOfExperimentValid = true;
    }

    /**
     * @return the dayOfYear
     */
    public final int getDayOfYear() {
        if (!dayOfYearValid)
            throw new IllegalStateException("feeID " + feedID + ": dayOfYear not initialized!");
        return dayOfYear;
    }

    /**
     * @param dayOfYear the dayOfYear to set
     */
    public final void setDayOfYear(int dayOfYear) {
        this.dayOfYear = dayOfYear;
        this.dayOfYearValid = true;
    }

    /**
     * @return the culmulatedSizeofPolls
     */
    public final long getCulmulatedSizeofPolls() {
        if (!cumulatedSizeofPollsValid)
            throw new IllegalStateException("feeID " + feedID + ": culmulatedSizeofPolls not initialized!");
        return cumulatedSizeofPolls;
    }

    /**
     * @param culmulatedSizeofPolls the culmulatedSizeofPolls to set
     */
    public final void setCumulatedSizeofPolls(long culmulatedSizeofPolls) {
        this.cumulatedSizeofPolls = culmulatedSizeofPolls;
        this.cumulatedSizeofPollsValid = true;
    }


    public final void setFeedID(int feedID) {
        this.feedID = feedID;
        this.feedIDValid = true;
    }

    public final void setActivityPattern(int activityPattern) {
        this.activityPattern = activityPattern;
        this.activityPatternValid = true;
    }

    public final void setSupportsETag(Boolean supportsETag) {
        this.supportsETag = supportsETag;
        this.supportsETagValid = true;
    }

    public final void setSupportsConditionalGet(Boolean supportsConditionalGet) {
        this.supportsConditionalGet = supportsConditionalGet;
        this.supportsConditionalGetValid = true;
    }

    public final void setETagResponseSize(int eTagResponseSize) {
        this.eTagResponseSize = eTagResponseSize;
        this.eTagResponseSizeValid = true;
    }

    public final void setConditionalGetResponseSize(int conditionalGetResponseSize) {
        this.conditionalGetResponseSize = conditionalGetResponseSize;
        this.conditionalGetResponseSizeValid = true;
    }
    
    public final void setNumberOfPoll(int numberOfPoll) {
        this.numberOfPoll = numberOfPoll;
        this.numberOfPollValid = true;
    }

    public final void setPollTimestamp(long pollTimestamp) {
        this.pollTimestamp = pollTimestamp;
        this.pollTimestampValid = true;
    }

    public final void setPollHourOfDay(int pollHourOfDay) {
        this.pollHourOfDay = pollHourOfDay;
        this.pollHourOfDayValid = true;
    }

    public final void setPollMinuteOfDay(int pollMinuteOfDay) {
        this.pollMinuteOfDay = pollMinuteOfDay;
        this.pollMinuteOfDayValid = true;
    }

    public final void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
        this.checkIntervalValid = true;
    }

    public final void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
        this.windowSizeValid = true;
    }

    public final void setSizeOfPoll(int sizeOfPoll) {
        this.sizeOfPoll = sizeOfPoll;
        this.sizeOfPollValid = true;
    }

    public final void setNumberMissedNewEntries(int numberMissedNewEntries) {
        this.numberMissedNewEntries = numberMissedNewEntries;
        this.numberMissedNewEntriesValid = true;
    }

    public final void setPercentageNewEntries(float percentageNewEntries) {
        this.percentageNewEntries = percentageNewEntries;
        this.percentageNewEntriesValid = true;
    }

    public final void setDelay(double delay) {
        this.delay = delay;
        this.delayValid = true;
    }

    public final void setScoreMin(float scoreMin) {
        this.scoreMin = scoreMin;
        this.scoreMinValid = true;
    }

    public final void setScoreMax(float scoreMax) {
        this.scoreMax = scoreMax;
        this.scoreMaxValid = true;
    }

    public final int getFeedID() {
        if (!feedIDValid)
            throw new IllegalStateException("feeID has not been initialized!");
        return feedID;
	}

	public final int getActivityPattern() {
        if (!activityPatternValid)
            throw new IllegalStateException("feeID " + feedID + ": activityPattern not initialized!");
		return activityPattern;
	}

	public final Boolean getSupportsETag() {
        if (!supportsETagValid)
            throw new IllegalStateException("feeID " + feedID + ": supportsETag not initialized!");
		return supportsETag;
	}

	public final Boolean getSupportsConditionalGet() {
        if (!supportsConditionalGetValid)
            throw new IllegalStateException("feeID " + feedID + ": supportsConditionalGet not initialized!");
		return supportsConditionalGet;
	}

	public final int geteTagResponseSize() {
        if (!eTagResponseSizeValid)
            throw new IllegalStateException("feeID " + feedID + ": eTagResponseSize not initialized!");
		return eTagResponseSize;
	}

	public final int getConditionalGetResponseSize() {
        if (!conditionalGetResponseSizeValid)
            throw new IllegalStateException("feeID " + feedID + ": conditionalGetResponseSize not initialized!");
		return conditionalGetResponseSize;
	}

	public final int getNumberOfPoll() {
        if (!numberOfPollValid)
            throw new IllegalStateException("feeID " + feedID + ": numberOfPoll not initialized!");
		return numberOfPoll;
	}

	public final long getPollTimestamp() {
        if (!pollTimestampValid)
            throw new IllegalStateException("feeID " + feedID + ": pollTimestamp not initialized!");
		return pollTimestamp;
	}

	public final int getPollHourOfDay() {
        if (!pollHourOfDayValid)
            throw new IllegalStateException("feeID " + feedID + ": pollHourOfDay not initialized!");
		return pollHourOfDay;
	}

	public final int getPollMinuteOfDay() {
        if (!pollMinuteOfDayValid)
            throw new IllegalStateException("feeID " + feedID + ": pollMinuteOfDay not initialized!");
		return pollMinuteOfDay;
	}

    public final int getCheckInterval() {
        if (!checkIntervalValid)
            throw new IllegalStateException("feeID " + feedID + ": checkInterval not initialized!");
        if (checkInterval <= 0)
            throw new IllegalStateException("feeID " + feedID + ": checkInterval out of range");
        return checkInterval;
	}

	public final int getWindowSize() {
        if (!windowSizeValid)
            throw new IllegalStateException("feeID " + feedID + ": windowSize not initialized!");
		return windowSize;
	}

	/**
	 *  the amount of bytes that has been downloaded 
	 */
    public final int getSizeOfPoll() {
        if (!sizeOfPollValid)
            throw new IllegalStateException("feeID " + feedID + ": sizeOfPoll not initialized!");
		return sizeOfPoll;
	}

	public final int getNumberMissedNewEntries() {
        if (!numberMissedNewEntriesValid)
            throw new IllegalStateException("feeID " + feedID + ": numberMissedNewEntries not initialized!");
		return numberMissedNewEntries;
	}

	public final float getPercentageNewEntries() {
        if (!percentageNewEntriesValid)
            throw new IllegalStateException("feeID " + feedID + ": percentageNewEntries not initialized!");
		return percentageNewEntries;
	}

	public final double getDelay() {
        if (!delayValid)
            throw new IllegalStateException("feeID " + feedID + ": delay not initialized!");
		return delay;
	}

	public final float getScoreMin() {
        if (!scoreMinValid)
            throw new IllegalStateException("feeID " + feedID + ": scoreMin not initialized!");
		return scoreMin;
	}

	public final float getScoreMax() {
        if (!scoreMaxValid)
            throw new IllegalStateException("feeID " + feedID + ": scoreMax not initialized!");
		return scoreMax;
	}
	


	
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + activityPattern;
        result = prime * result + (activityPatternValid ? 1231 : 1237);
        long temp;
        temp = Double.doubleToLongBits(averageValue);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (averageValueValid ? 1231 : 1237);
        result = prime * result + checkInterval;
        result = prime * result + (checkIntervalValid ? 1231 : 1237);
        result = prime * result + conditionalGetResponseSize;
        result = prime * result + (conditionalGetResponseSizeValid ? 1231 : 1237);
        result = prime * result + (int) (cumulatedSizeofPolls ^ (cumulatedSizeofPolls >>> 32));
        result = prime * result + (cumulatedSizeofPollsValid ? 1231 : 1237);
        result = prime * result + dayOfYear;
        result = prime * result + (dayOfYearValid ? 1231 : 1237);
        temp = Double.doubleToLongBits(delay);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (delayValid ? 1231 : 1237);
        result = prime * result + eTagResponseSize;
        result = prime * result + (eTagResponseSizeValid ? 1231 : 1237);
        result = prime * result + feedID;
        result = prime * result + (feedIDValid ? 1231 : 1237);
        result = prime * result + hourOfExperiment;
        result = prime * result + (hourOfExperimentValid ? 1231 : 1237);
        result = prime * result + numberMissedNewEntries;
        result = prime * result + (numberMissedNewEntriesValid ? 1231 : 1237);
        result = prime * result + numberOfPoll;
        result = prime * result + (numberOfPollValid ? 1231 : 1237);
        result = prime * result + Float.floatToIntBits(percentageNewEntries);
        result = prime * result + (percentageNewEntriesValid ? 1231 : 1237);
        result = prime * result + pollHourOfDay;
        result = prime * result + (pollHourOfDayValid ? 1231 : 1237);
        result = prime * result + pollMinuteOfDay;
        result = prime * result + (pollMinuteOfDayValid ? 1231 : 1237);
        result = prime * result + (int) (pollTimestamp ^ (pollTimestamp >>> 32));
        result = prime * result + (pollTimestampValid ? 1231 : 1237);
        result = prime * result + Float.floatToIntBits(scoreMax);
        result = prime * result + (scoreMaxValid ? 1231 : 1237);
        result = prime * result + Float.floatToIntBits(scoreMin);
        result = prime * result + (scoreMinValid ? 1231 : 1237);
        result = prime * result + sizeOfPoll;
        result = prime * result + (sizeOfPollValid ? 1231 : 1237);
        result = prime * result + ((supportsConditionalGet == null) ? 0 : supportsConditionalGet.hashCode());
        result = prime * result + (supportsConditionalGetValid ? 1231 : 1237);
        result = prime * result + ((supportsETag == null) ? 0 : supportsETag.hashCode());
        result = prime * result + (supportsETagValid ? 1231 : 1237);
        result = prime * result + windowSize;
        result = prime * result + (windowSizeValid ? 1231 : 1237);
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
        EvaluationFeedPoll other = (EvaluationFeedPoll) obj;
        if (activityPattern != other.activityPattern)
            return false;
        if (activityPatternValid != other.activityPatternValid)
            return false;
        if (Double.doubleToLongBits(averageValue) != Double.doubleToLongBits(other.averageValue))
            return false;
        if (averageValueValid != other.averageValueValid)
            return false;
        if (checkInterval != other.checkInterval)
            return false;
        if (checkIntervalValid != other.checkIntervalValid)
            return false;
        if (conditionalGetResponseSize != other.conditionalGetResponseSize)
            return false;
        if (conditionalGetResponseSizeValid != other.conditionalGetResponseSizeValid)
            return false;
        if (cumulatedSizeofPolls != other.cumulatedSizeofPolls)
            return false;
        if (cumulatedSizeofPollsValid != other.cumulatedSizeofPollsValid)
            return false;
        if (dayOfYear != other.dayOfYear)
            return false;
        if (dayOfYearValid != other.dayOfYearValid)
            return false;
        if (Double.doubleToLongBits(delay) != Double.doubleToLongBits(other.delay))
            return false;
        if (delayValid != other.delayValid)
            return false;
        if (eTagResponseSize != other.eTagResponseSize)
            return false;
        if (eTagResponseSizeValid != other.eTagResponseSizeValid)
            return false;
        if (feedID != other.feedID)
            return false;
        if (feedIDValid != other.feedIDValid)
            return false;
        if (hourOfExperiment != other.hourOfExperiment)
            return false;
        if (hourOfExperimentValid != other.hourOfExperimentValid)
            return false;
        if (numberMissedNewEntries != other.numberMissedNewEntries)
            return false;
        if (numberMissedNewEntriesValid != other.numberMissedNewEntriesValid)
            return false;
        if (numberOfPoll != other.numberOfPoll)
            return false;
        if (numberOfPollValid != other.numberOfPollValid)
            return false;
        if (Float.floatToIntBits(percentageNewEntries) != Float.floatToIntBits(other.percentageNewEntries))
            return false;
        if (percentageNewEntriesValid != other.percentageNewEntriesValid)
            return false;
        if (pollHourOfDay != other.pollHourOfDay)
            return false;
        if (pollHourOfDayValid != other.pollHourOfDayValid)
            return false;
        if (pollMinuteOfDay != other.pollMinuteOfDay)
            return false;
        if (pollMinuteOfDayValid != other.pollMinuteOfDayValid)
            return false;
        if (pollTimestamp != other.pollTimestamp)
            return false;
        if (pollTimestampValid != other.pollTimestampValid)
            return false;
        if (Float.floatToIntBits(scoreMax) != Float.floatToIntBits(other.scoreMax))
            return false;
        if (scoreMaxValid != other.scoreMaxValid)
            return false;
        if (Float.floatToIntBits(scoreMin) != Float.floatToIntBits(other.scoreMin))
            return false;
        if (scoreMinValid != other.scoreMinValid)
            return false;
        if (sizeOfPoll != other.sizeOfPoll)
            return false;
        if (sizeOfPollValid != other.sizeOfPollValid)
            return false;
        if (supportsConditionalGet == null) {
            if (other.supportsConditionalGet != null)
                return false;
        } else if (!supportsConditionalGet.equals(other.supportsConditionalGet))
            return false;
        if (supportsConditionalGetValid != other.supportsConditionalGetValid)
            return false;
        if (supportsETag == null) {
            if (other.supportsETag != null)
                return false;
        } else if (!supportsETag.equals(other.supportsETag))
            return false;
        if (supportsETagValid != other.supportsETagValid)
            return false;
        if (windowSize != other.windowSize)
            return false;
        if (windowSizeValid != other.windowSizeValid)
            return false;
        return true;
    }
	

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "EvaluationFeedPoll [feedID=" + feedID + ", feedIDValid=" + feedIDValid + ", activityPattern="
                + activityPattern + ", activityPatternValid=" + activityPatternValid + ", supportsETag=" + supportsETag
                + ", supportsETagValid=" + supportsETagValid + ", supportsConditionalGet=" + supportsConditionalGet
                + ", supportsConditionalGetValid=" + supportsConditionalGetValid + ", eTagResponseSize="
                + eTagResponseSize + ", eTagResponseSizeValid=" + eTagResponseSizeValid
                + ", conditionalGetResponseSize=" + conditionalGetResponseSize + ", conditionalGetResponseSizeValid="
                + conditionalGetResponseSizeValid + ", numberOfPoll=" + numberOfPoll + ", numberOfPollValid="
                + numberOfPollValid + ", pollTimestamp=" + pollTimestamp + ", pollTimestampValid=" + pollTimestampValid
                + ", pollHourOfDay=" + pollHourOfDay + ", pollHourOfDayValid=" + pollHourOfDayValid
                + ", pollMinuteOfDay=" + pollMinuteOfDay + ", pollMinuteOfDayValid=" + pollMinuteOfDayValid
                + ", checkInterval=" + checkInterval + ", checkIntervalValid=" + checkIntervalValid + ", windowSize="
                + windowSize + ", windowSizeValid=" + windowSizeValid + ", sizeOfPoll=" + sizeOfPoll
                + ", sizeOfPollValid=" + sizeOfPollValid + ", numberMissedNewEntries=" + numberMissedNewEntries
                + ", numberMissedNewEntriesValid=" + numberMissedNewEntriesValid + ", percentageNewEntries="
                + percentageNewEntries + ", percentageNewEntriesValid=" + percentageNewEntriesValid + ", delay="
                + delay + ", delayValid=" + delayValid + ", scoreMax=" + scoreMax + ", scoreMaxValid=" + scoreMaxValid
                + ", scoreMin=" + scoreMin + ", scoreMinValid=" + scoreMinValid + ", averageValue=" + averageValue
                + ", averageValueValid=" + averageValueValid + ", dayOfYear=" + dayOfYear + ", dayOfYearValid="
                + dayOfYearValid + ", cumulatedSizeofPolls=" + cumulatedSizeofPolls + ", cumulatedSizeofPollsValid="
                + cumulatedSizeofPollsValid + ", hourOfExperiment=" + hourOfExperiment + ", hourOfExperimentValid="
                + hourOfExperimentValid + "]";
    }

	
}
