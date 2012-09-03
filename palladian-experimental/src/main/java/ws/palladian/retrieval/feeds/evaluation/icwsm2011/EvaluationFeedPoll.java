package ws.palladian.retrieval.feeds.evaluation.icwsm2011;

/**
 * Represents one poll of the feed reader evaluation for ICWSM2011-paper.
 * 
 * @author Sandro Reichert
 */
public class EvaluationFeedPoll {	
	
    /** FeedID */
	private int feedID = -1; 

    /** Specifies whether {@link EvaluationFeedPoll#feedID} is already set (read from data base) or not */
    private boolean feedIDValid = false;

    /** the feed's update class, e.g. zombie, constant */
    private int activityPattern = -1;

    /** Specifies whether {@link EvaluationFeedPoll#activityPattern} is already set (read from data base) or not */
    private boolean activityPatternValid = false;
    
    /** size of the returned header in Bytes if supportsConditionalGet = true and feed has not changed */
    private Integer conditionalGetResponseSize = null;

    /**
     * Specifies whether {@link EvaluationFeedPoll#conditionalGetResponseSize} is already set (read from data base) or
     * not
     */
    private boolean conditionalGetResponseSizeValid = false;
    
    /** true if the feed supports conditional GET or eTag */
    private boolean supportsConditionalGet = false;

    /**
     * Specifies whether {@link EvaluationFeedPoll#supportsConditionalGetSize} is already set (read from data base) and
     * not NULL
     */
    private boolean supportsConditionalGetValid = false;
    
    /** the number of the current poll */
    private int numberOfPoll = -1;

    /** Specifies whether {@link EvaluationFeedPoll#numberOfPoll} is already set (read from data base) or not */
    private boolean numberOfPollValid = false;
	
    /** the feed has been pooled at this TIMESTAMP, format milliseconds since 01.01.1970 00:00 */
    private long pollTimestamp = -1;

    /** Specifies whether {@link EvaluationFeedPoll#pollTimestamp} is already set (read from data base) or not */
    private boolean pollTimestampValid = false;
	
    /** time in minutes we waited between last and current check */
    private int checkInterval = -1;

    /** Specifies whether {@link EvaluationFeedPoll#checkInterval} is already set (read from data base) or not */
    private boolean checkIntervalValid = false;
	
    /** the current size of the feed's window (number of items found) */
    private int windowSize = -1;

    /** Specifies whether {@link EvaluationFeedPoll#windowSize} is already set (read from data base) or not */
    private boolean windowSizeValid = false;
	
    /** the amount of bytes that has been downloaded */
    private int sizeOfPoll = -1;

    /** Specifies whether {@link EvaluationFeedPoll#sizeOfPoll} is already set (read from data base) or not */
    private boolean sizeOfPollValid = false;
	
    /** the number of new items we missed because there more new items since the last poll than fit into the window */
    private int missedItems = -1;

    /** Specifies whether {@link EvaluationFeedPoll#missedItems} is already set (read from data base) or not */
    private boolean missedItemsValid = false;
	
    /** the number of new entries within this poll. */
    private int newWindowItems = -1;

    /** Specifies whether {@link EvaluationFeedPoll#newWindowItems} is already set (read from data base) or not */
    private boolean newWindowItemsValid = false;

    /**
     * late or early (negative value) in seconds, is the time span between timestamp poll and timestamp(s) next or last
     * new entry(ies)
     */
    private double cumulatedDelay = -1;

    /** Specifies whether {@link EvaluationFeedPoll#cumulatedDelay} is already set (read from data base) or not */
    private boolean cumulatedDelayValid = false;

    /** late in seconds, is the time span between timestamp poll and timestamp(s) next or last new entry(ies) */
    private double cumulatedLateDelay = -1;

    /** Specifies whether {@link EvaluationFeedPoll#cumulatedLateDelay} is already set (read from data base) or not */
    private boolean cumulatedLateDelayValid = false;
	
    /**
     * the score in MAX mode = percentageNewEntries iff percentageNewEntries <=1 OR (1 -
     * numberMissedNewEntries/windowSize) iff percentageNewEntries > 1
     */
    // TODO: David: ist das noch korrekt??
    private double timeliness = -1;

    /** Specifies whether {@link EvaluationFeedPoll#timeliness} is already set (read from data base) or not */
    private boolean timelinessValid = false;
	
    /**
     * the score in MIN mode = (d/int + 1)^-1 ; score is in (0,1] 1 is perfect, 0.5 means culmulated delay (d) is equalt
     * to current interval (int)
     */
    // TODO: David: ist das noch korrekt??
    private double timelinessLate = -1;

    /** Specifies whether {@link EvaluationFeedPoll#timelinessLate} is already set (read from data base) or not */
    private boolean timelinessLateValid = false;

    /** an average value like average scoreMin, scoreMax or percentageNewEntries, calculated for some diagrams */
    private double averageValue = -1;

    /** Specifies whether {@link EvaluationFeedPoll#averageValue} is already set (read from data base) or not */
    private boolean averageValueValid = false;
    
    /** size of polls preaggregated by data base */
    private long cumulatedSizeofPolls = -1;

    /** Specifies whether {@link EvaluationFeedPoll#cumulatedSizeofPolls} is already set (read from data base) or not */
    private boolean cumulatedSizeofPollsValid = false;

    /** The day of the year this poll has been made */
    private int dayOfYear = -1;

    /** Specifies whether {@link EvaluationFeedPoll#dayOfYear} is already set (read from data base) or not */
    private boolean dayOfYearValid = false;
    
    /** The hour within the experiment this poll has been made */
    private int hourOfExperiment = -1;

    /** Specifies whether {@link EvaluationFeedPoll#hourOfExperiment} is already set (read from data base) or not */
    private boolean hourOfExperimentValid = false;

    /**
     * guess :)
     */
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


    /**
     * Sets the feedID and marks it as valid.
     * 
     * @param feedID the feedID to set.
     */
    public final void setFeedID(int feedID) {
        this.feedID = feedID;
        this.feedIDValid = true;
    }

    /**
     * Sets the activityPattern and marks it as valid.
     * 
     * @param activityPattern the activityPattern to set.
     */
    public final void setActivityPattern(int activityPattern) {
        this.activityPattern = activityPattern;
        this.activityPatternValid = true;
    }


    /**
     * Sets the conditionalGetResponseSize and marks it as valid.
     * 
     * @param conditionalGetResponseSize the conditionalGetResponseSize to set.
     */
    public final void setConditionalGetResponseSize(Integer conditionalGetResponseSize) {
        this.conditionalGetResponseSize = conditionalGetResponseSize;
        this.conditionalGetResponseSizeValid = true;
        if (conditionalGetResponseSize != null)
            setSupportsConditionalGet(true);
    }
    
    /**
     * Sets the numberOfPoll and marks it as valid.
     * 
     * @param numberOfPoll the numberOfPoll to set.
     */
    public final void setNumberOfPoll(int numberOfPoll) {
        this.numberOfPoll = numberOfPoll;
        this.numberOfPollValid = true;
    }

    /**
     * Sets the pollTimestamp and marks it as valid.
     * 
     * @param pollTimestamp the pollTimestamp to set.
     */
    public final void setPollTimestamp(long pollTimestamp) {
        this.pollTimestamp = pollTimestamp;
        this.pollTimestampValid = true;
    }

    /**
     * Sets the checkInterval and marks it as valid.
     * 
     * @param checkInterval the checkInterval to set.
     */
    public final void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
        this.checkIntervalValid = true;
    }

    /**
     * Sets the windowSize and marks it as valid.
     * 
     * @param windowSize the windowSize to set.
     */
    public final void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
        this.windowSizeValid = true;
    }

    /**
     * Sets the sizeOfPoll and marks it as valid.
     * 
     * @param sizeOfPoll the sizeOfPoll to set.
     */
    public final void setSizeOfPoll(int sizeOfPoll) {
        this.sizeOfPoll = sizeOfPoll;
        this.sizeOfPollValid = true;
    }

    /**
     * Sets the missedItems and marks it as valid.
     * 
     * @param missedItems the missedItems to set.
     */
    public final void setMissedItems(int missedItems) {
        this.missedItems = missedItems;
        this.missedItemsValid = true;
    }

    /**
     * Sets the newWindowItems and marks it as valid.
     * 
     * @param newWindowItems the newWindowItems to set.
     */
    public final void setPercentageNewEntries(int numberNewEntries) {
        this.newWindowItems = numberNewEntries;
        this.newWindowItemsValid = true;
    }

    /**
     * Sets the cumulatedDelay and marks it as valid.
     * 
     * @param cumulatedDelay the cumulatedDelay to set.
     */
    public final void setDelay(double delay) {
        this.cumulatedDelay = delay;
        this.cumulatedDelayValid = true;
    }

    /**
     * Sets the timelinessLate and marks it as valid.
     * 
     * @param timelinessLate the timelinessLate to set.
     */
    public final void setScoreMin(float scoreMin) {
        this.timelinessLate = scoreMin;
        this.timelinessLateValid = true;
    }

    /**
     * returns the feedID if it has been set, otherwise it throws a {@link IllegalStateException}
     * 
     * @return the poll's feedID if it has been set.
     */
    public final int getFeedID() {
        if (!feedIDValid)
            throw new IllegalStateException("feeID has not been initialized!");
        return feedID;
	}

    /**
     * returns the activityPattern if it has been set, otherwise it throws a {@link IllegalStateException}
     * 
     * @return the poll's activityPattern if it has been set.
     */
    public final int getActivityPattern() {
        if (!activityPatternValid)
            throw new IllegalStateException("feeID " + feedID + ": activityPattern not initialized!");
		return activityPattern;
	}

    /**
     * Returns true supportsConditionalGet if the feed supports conditional get requests or eTag, or false, if the feed
     * doesn't support one of both.
     * A {@link IllegalStateException} is thrown if it is unknown.
     * 
     * @return the poll's activityPattern if it has been set.
     */
    public final Boolean getSupportsConditionalGet() {
        if (!supportsConditionalGetValid)
            throw new IllegalStateException("feeID " + feedID + ": supportsConditionalGet not initialized!");
		return supportsConditionalGet;
	}

    /**
     * Returns the conditionalGetResponseSize if it has been set, otherwise it throws a {@link IllegalStateException}.
     * 
     * @return the poll's conditionalGetResponseSize if it has been set.
     */
    public final int getConditionalGetResponseSize() {
        if (!conditionalGetResponseSizeValid)
            throw new IllegalStateException("feeID " + feedID + ": conditionalGetResponseSize not initialized!");
		return conditionalGetResponseSize;
	}

    /**
     * Returns the numberOfPoll if it has been set, otherwise it throws a {@link IllegalStateException}.
     * 
     * @return the poll's numberOfPoll if it has been set.
     */
	public final int getNumberOfPoll() {
        if (!numberOfPollValid)
            throw new IllegalStateException("feeID " + feedID + ": numberOfPoll not initialized!");
		return numberOfPoll;
	}

    /**
     * Returns the pollTimestamp if it has been set, otherwise it throws a {@link IllegalStateException}.
     * 
     * @return the poll's pollTimestamp if it has been set.
     */
	public final long getPollTimestamp() {
        if (!pollTimestampValid)
            throw new IllegalStateException("feeID " + feedID + ": pollTimestamp not initialized!");
		return pollTimestamp;
	}

    /**
     * Returns the checkInterval if it has been set, otherwise it throws a {@link IllegalStateException}.
     * 
     * @return the poll's checkInterval if it has been set.
     */
    public final int getCheckInterval() {
        if (!checkIntervalValid)
            throw new IllegalStateException("feeID " + feedID + ": checkInterval not initialized!");
        if (checkInterval <= 0)
            throw new IllegalStateException("feeID " + feedID + ": checkInterval out of range");
        return checkInterval;
	}

    /**
     * Returns the windowSize if it has been set, otherwise it throws a {@link IllegalStateException}.
     * 
     * @return the poll's windowSize if it has been set.
     */
    public final int getWindowSize() {
        if (!windowSizeValid)
            throw new IllegalStateException("feeID " + feedID + ": windowSize not initialized!");
		return windowSize;
	}

    /**
     * If it has been set, it returns the amount of bytes that has been downloaded in this poll, otherwise it throws a
     * {@link IllegalStateException}.
     * 
     * @return the amount of bytes that has been downloaded in this poll.
     */
    public final int getSizeOfPoll() {
        if (!sizeOfPollValid)
            throw new IllegalStateException("feeID " + feedID + ": sizeOfPoll not initialized!");
		return sizeOfPoll;
	}

    /**
     * Returns the number of missedItems if it has been set, otherwise it throws a {@link IllegalStateException}.
     * 
     * @return the number of missedItems if it has been set.
     */
    public final int getMissedItems() {
        if (!missedItemsValid)
            throw new IllegalStateException("feeID " + feedID + ": missedItems not initialized!");
		return missedItems;
	}

    /**
     * Returns the number of newWindowItems if it has been set, otherwise it throws a {@link IllegalStateException}.
     * 
     * @return the number of newWindowItems if it has been set.
     */
    public final int getNewWindowItems() {
        if (!newWindowItemsValid)
            throw new IllegalStateException("feeID " + feedID + ": newWindowItems not initialized!");
        return newWindowItems;
	}

    /**
     * Returns the cumulatedDelay in if it has been set, otherwise it throws a {@link IllegalStateException}.
     * The cumulatedDelay is late or early (negative value) in seconds, it is the time span between timestamp poll and
     * timestamp(s) next or last new entry(ies)
     * 
     * @return the cumulatedDelay if it has been set.
     */
    public final double getDelay() {
        if (!cumulatedDelayValid)
            throw new IllegalStateException("feeID " + feedID + ": delay not initialized!");
		return cumulatedDelay;
	}

    /**
     * Returns the timelinessLate if it has been set, otherwise it throws a {@link IllegalStateException}.
     * 
     * @return the timelinessLate if it has been set.
     */
    public final double getTimelinessLate() {
        if (!timelinessLateValid)
            throw new IllegalStateException("feeID " + feedID + ": timelinessLate not initialized!");
        return timelinessLate;
	}

    /**
     * Returns the number of new items within the window if the value has been set, otherwise it throws a
     * {@link IllegalStateException}.
     * 
     * @return the newWindowItems if it has been set.
     */
    public final int getNumberNewEntries() {
        if (!newWindowItemsValid)
            throw new IllegalStateException("feeID " + feedID + ": numberNewEntries not initialized!");
        return newWindowItems;
    }

    /**
     * Sets the number of new items in the current poll and {@link newWindowItems} as valid.
     * 
     * @param newWindowItems the newWindowItems to set.
     */
    public final void setNewWindowItems(int newWindowItems) {
        this.newWindowItems = newWindowItems;
        this.newWindowItemsValid = true;
    }

    /**
     * @return the cumulatedDelay
     */
    public final double getCumulatedDelay() {
        if (!cumulatedDelayValid)
            throw new IllegalStateException("feeID " + feedID + ": cumulatedDelay not initialized!");
        return cumulatedDelay;
    }

    /**
     * @param cumulatedDelay the cumulatedDelay to set
     */
    public final void setCumulatedDelay(double cumulatedDelay) {
        this.cumulatedDelay = cumulatedDelay;
        this.cumulatedDelayValid = true;
    }

    /**
     * @return the cumulatedLateDelay
     */
    public final double getCumulatedLateDelay() {
        if (!cumulatedLateDelayValid)
            throw new IllegalStateException("feeID " + feedID + ": cumulatedLateDelay not initialized!");
        return cumulatedLateDelay;
    }

    /**
     * @param cumulatedLateDelay the cumulatedLateDelay to set
     */
    public final void setCumulatedLateDelay(double cumulatedLateDelay) {
        this.cumulatedLateDelay = cumulatedLateDelay;
        this.cumulatedLateDelayValid = true;
    }

    /**
     * @return the timeliness
     */
    public final double getTimeliness() {
        if (!timelinessValid)
            throw new IllegalStateException("feeID " + feedID + ": timeliness not initialized!");
        return timeliness;
    }

    /**
     * @param timeliness the timeliness to set
     */
    public final void setTimeliness(double timeliness) {
        this.timeliness = timeliness;
        this.timelinessValid = true;
    }

    /**
     * @param timelinessLate the timelinessLate to set
     */
    public final void setTimelinessLate(double timelinessLate) {
        this.timelinessLate = timelinessLate;
        this.timelinessLateValid = true;
    }

    /**
     * @return the cumulatedSizeofPolls
     */
    public final long getCumulatedSizeofPolls() {
        if (!cumulatedSizeofPollsValid)
            throw new IllegalStateException("feeID " + feedID + ": cumulatedSizeofPolls not initialized!");
        return cumulatedSizeofPolls;
    }

    /**
     * @param supportsConditionalGet the supportsConditionalGet to set
     */
    private final void setSupportsConditionalGet(boolean supportsConditionalGet) {
        this.supportsConditionalGet = supportsConditionalGet;
        this.supportsConditionalGetValid = true;
    }

    /**
     * (non-Javadoc)
     * 
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
        result = prime * result + ((conditionalGetResponseSize == null) ? 0 : conditionalGetResponseSize.hashCode());
        result = prime * result + (conditionalGetResponseSizeValid ? 1231 : 1237);
        temp = Double.doubleToLongBits(cumulatedDelay);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (cumulatedDelayValid ? 1231 : 1237);
        temp = Double.doubleToLongBits(cumulatedLateDelay);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (cumulatedLateDelayValid ? 1231 : 1237);
        result = prime * result + (int) (cumulatedSizeofPolls ^ (cumulatedSizeofPolls >>> 32));
        result = prime * result + (cumulatedSizeofPollsValid ? 1231 : 1237);
        result = prime * result + dayOfYear;
        result = prime * result + (dayOfYearValid ? 1231 : 1237);
        result = prime * result + feedID;
        result = prime * result + (feedIDValid ? 1231 : 1237);
        result = prime * result + hourOfExperiment;
        result = prime * result + (hourOfExperimentValid ? 1231 : 1237);
        result = prime * result + missedItems;
        result = prime * result + (missedItemsValid ? 1231 : 1237);
        result = prime * result + newWindowItems;
        result = prime * result + (newWindowItemsValid ? 1231 : 1237);
        result = prime * result + numberOfPoll;
        result = prime * result + (numberOfPollValid ? 1231 : 1237);
        result = prime * result + (int) (pollTimestamp ^ (pollTimestamp >>> 32));
        result = prime * result + (pollTimestampValid ? 1231 : 1237);
        result = prime * result + sizeOfPoll;
        result = prime * result + (sizeOfPollValid ? 1231 : 1237);
        result = prime * result + (supportsConditionalGet ? 1231 : 1237);
        result = prime * result + (supportsConditionalGetValid ? 1231 : 1237);
        temp = Double.doubleToLongBits(timeliness);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(timelinessLate);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (timelinessLateValid ? 1231 : 1237);
        result = prime * result + (timelinessValid ? 1231 : 1237);
        result = prime * result + windowSize;
        result = prime * result + (windowSizeValid ? 1231 : 1237);
        return result;
    }

    /**
     * (non-Javadoc)
     * 
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
        if (conditionalGetResponseSize == null) {
            if (other.conditionalGetResponseSize != null)
                return false;
        } else if (!conditionalGetResponseSize.equals(other.conditionalGetResponseSize))
            return false;
        if (conditionalGetResponseSizeValid != other.conditionalGetResponseSizeValid)
            return false;
        if (Double.doubleToLongBits(cumulatedDelay) != Double.doubleToLongBits(other.cumulatedDelay))
            return false;
        if (cumulatedDelayValid != other.cumulatedDelayValid)
            return false;
        if (Double.doubleToLongBits(cumulatedLateDelay) != Double.doubleToLongBits(other.cumulatedLateDelay))
            return false;
        if (cumulatedLateDelayValid != other.cumulatedLateDelayValid)
            return false;
        if (cumulatedSizeofPolls != other.cumulatedSizeofPolls)
            return false;
        if (cumulatedSizeofPollsValid != other.cumulatedSizeofPollsValid)
            return false;
        if (dayOfYear != other.dayOfYear)
            return false;
        if (dayOfYearValid != other.dayOfYearValid)
            return false;
        if (feedID != other.feedID)
            return false;
        if (feedIDValid != other.feedIDValid)
            return false;
        if (hourOfExperiment != other.hourOfExperiment)
            return false;
        if (hourOfExperimentValid != other.hourOfExperimentValid)
            return false;
        if (missedItems != other.missedItems)
            return false;
        if (missedItemsValid != other.missedItemsValid)
            return false;
        if (newWindowItems != other.newWindowItems)
            return false;
        if (newWindowItemsValid != other.newWindowItemsValid)
            return false;
        if (numberOfPoll != other.numberOfPoll)
            return false;
        if (numberOfPollValid != other.numberOfPollValid)
            return false;
        if (pollTimestamp != other.pollTimestamp)
            return false;
        if (pollTimestampValid != other.pollTimestampValid)
            return false;
        if (sizeOfPoll != other.sizeOfPoll)
            return false;
        if (sizeOfPollValid != other.sizeOfPollValid)
            return false;
        if (supportsConditionalGet != other.supportsConditionalGet)
            return false;
        if (supportsConditionalGetValid != other.supportsConditionalGetValid)
            return false;
        if (Double.doubleToLongBits(timeliness) != Double.doubleToLongBits(other.timeliness))
            return false;
        if (Double.doubleToLongBits(timelinessLate) != Double.doubleToLongBits(other.timelinessLate))
            return false;
        if (timelinessLateValid != other.timelinessLateValid)
            return false;
        if (timelinessValid != other.timelinessValid)
            return false;
        if (windowSize != other.windowSize)
            return false;
        if (windowSizeValid != other.windowSizeValid)
            return false;
        return true;
    }

    /**
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "EvaluationFeedPoll [feedID=" + feedID + ", feedIDValid=" + feedIDValid + ", activityPattern="
                + activityPattern + ", activityPatternValid=" + activityPatternValid + ", conditionalGetResponseSize="
                + conditionalGetResponseSize + ", conditionalGetResponseSizeValid=" + conditionalGetResponseSizeValid
                + ", supportsConditionalGet=" + supportsConditionalGet + ", supportsConditionalGetValid="
                + supportsConditionalGetValid + ", numberOfPoll=" + numberOfPoll + ", numberOfPollValid="
                + numberOfPollValid + ", pollTimestamp=" + pollTimestamp + ", pollTimestampValid=" + pollTimestampValid
                + ", checkInterval=" + checkInterval + ", checkIntervalValid=" + checkIntervalValid + ", windowSize="
                + windowSize + ", windowSizeValid=" + windowSizeValid + ", sizeOfPoll=" + sizeOfPoll
                + ", sizeOfPollValid=" + sizeOfPollValid + ", missedItems=" + missedItems + ", missedItemsValid="
                + missedItemsValid + ", newWindowItems=" + newWindowItems + ", newWindowItemsValid="
                + newWindowItemsValid + ", cumulatedDelay=" + cumulatedDelay + ", cumulatedDelayValid="
                + cumulatedDelayValid + ", cumulatedLateDelay=" + cumulatedLateDelay + ", cumulatedLateDelayValid="
                + cumulatedLateDelayValid + ", timeliness=" + timeliness + ", timelinessValid=" + timelinessValid
                + ", timelinessLate=" + timelinessLate + ", timelinessLateValid=" + timelinessLateValid
                + ", averageValue=" + averageValue + ", averageValueValid=" + averageValueValid
                + ", cumulatedSizeofPolls=" + cumulatedSizeofPolls + ", cumulatedSizeofPollsValid="
                + cumulatedSizeofPollsValid + ", dayOfYear=" + dayOfYear + ", dayOfYearValid=" + dayOfYearValid
                + ", hourOfExperiment=" + hourOfExperiment + ", hourOfExperimentValid=" + hourOfExperimentValid + "]";
    }

	
}
