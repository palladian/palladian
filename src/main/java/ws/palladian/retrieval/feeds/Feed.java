package ws.palladian.retrieval.feeds;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;
import ws.palladian.retrieval.feeds.evaluation.PollDataSeries;
import ws.palladian.retrieval.feeds.meta.FeedMetaInformation;

/**
 * Represents a news feed.
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * 
 */
public class Feed {

    private int id = -1;
    private String feedUrl = null;


    /** The items of this feed. */
    private List<FeedItem> items = new ArrayList<FeedItem>();

    /**
     * The total number of unique items downloaded so far. This value may differ from {@link #items}.size() since
     * {@link #items} may have been reseted by calling {@link #freeMemory()}.
     */
    private int numberOfItemsReceived = 0;



    /** The number of feed entries presented for each request, <code>null</code> if feed has never been parsed. */
    private Integer windowSize = null;

    /**
     * Set to <code>true</code> if the feed has a variable window size, <code>false</code> if not, <code>null</code> if
     * unknown.
     */
    private Boolean variableWindowSize = null;

    /**
     * For benchmarking purposes we need to know when the history file was read completely, that is the case if the last
     * entry has been read.
     */
    private boolean historyFileCompletelyRead = false;

    /**
     * Keep track of the timestamp for the lookup in the history file. We start with the minimum value to get the
     * first x entries where x is the window size.
     */
    private long benchmarkLookupTime = Long.MIN_VALUE;

    /**
     * Keep track of the last timestamp for the lookup in the history file. This way we can find out how many posts we
     * have missed in between lookups.
     */
    private long benchmarkLastLookupTime = Long.MIN_VALUE;

    /** number of times the feed has been retrieved and successfully read */
    private int checks = 0;

    /**
     * Time in minutes until it is expected to find at least one new entry in the feed.
     */
    private int updateInterval = 60;

    public static final int MIN_DELAY = 0;
    public static final int MAX_COVERAGE = 1;

    /** Either MIN_DELAY (minCheckInterval) or MAX_COVERAGE (maxCheckInterval). */
    private int updateMode = Feed.MIN_DELAY;

    /** Our internal hash of the most recent item */
    private String newestItemHash = "";

    /** number of times the feed was checked but could not be found. */
    private int unreachableCount = 0;

    /** number of times the feed was checked but could not be parsed. */
    private int unparsableCount = 0;

    /** Timestamp of the last feed entry found in this feed. */
    private Date lastFeedEntry = null;

    /** The HTTP header's last-modified value of the last poll. */
    private Date httpLastModified = null;

    /**
     * Record statistics about poll data for evaluation purposes.
     */
    private PollDataSeries pollDataSeries = new PollDataSeries();

    /**
     * Number of item that were posted in a certain minute of the day, minute of the day : frequency of posts; chances a
     * post could have appeared.
     */
    private Map<Integer, int[]> meticulousPostDistribution = new HashMap<Integer, int[]>();

    /**
     * Indicator whether we have seen one full day in the feed already. This is necessary to calculate the feed post
     * probability. Whenever we increase {@link checks} we set this value to null = unknown if it was not true yet.
     */
    private Boolean oneFullDayOfItemsSeen = null;

    /** The activity pattern of the feed is one of {@link FeedClassifier}s classes. */
    private int activityPattern = -1;

    /** The ETag that was send with the last request. This saves bandwidth for feeds that support ETags. */
    private String lastETag = null;

    /**
     * The date this feed was checked for updates the last time. This can be used to send last modified since requests.
     */
    private Date lastPollTime;


    /**
     * The raw XML markup for this feed.
     */
    // private Document document;

    /** Caching the raw xml markup of the document as string. */
    // private String rawMarkup;

    private double targetPercentageOfNewEntries = -1.0;

    /** Total time in milliseconds that has been spent on processing this feed. */
    private long totalProcessingTimeMS = 0;

    /**
     * Number of times that we found a MISS, that is, if we knew the feed's items before, checked it again and none of
     * the items has been seen before.
     */
    private int misses = 0;

    /**
     * The timestamp when we found the last MISS for this feed.
     */
    private Date lastMissTime = null;

    /**
     * If <code>true</code>, this feed is not used to create a data set by {@link DatasetCreator}.
     */
    private boolean blocked = false;

    /**
     * The timestamp this feed has been successfully checked the last time. A successful check happens if the feed is
     * reachable and parsable. This timestamp should be set every time {@link #checks} is increased.
     */
    private Date lastSuccessfulCheckTime = null;

    /**
     * Flag, to indicate if this feed contains a new item since the last update. If this value is <code>null</code>, we
     * need to re-determine if we have a new item, by comparing the last {@link FeedItem}'s hashes each.
     */
    private Boolean newItem = null;

    /** Allows to keep arbitrary, additional information. */
    private Map<String, Object> additionalData;

    /** The feed's meta information. */
    private FeedMetaInformation feedMetaInfo = new FeedMetaInformation();

    public Feed() {
        super();
    }

    public Feed(String feedUrl) {
        this();
        this.feedUrl = feedUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    /**
     * Set the feed's URL.
     * 
     * @param feedUrl
     */
    public void setFeedUrl(String feedUrl) {
        setFeedUrl(feedUrl, false);
    }

    /**
     * Set the feed's URL. Optionally, set the feed's domain as site url if the site url was null.
     * 
     * @param feedUrl
     */
    public void setFeedUrl(String feedUrl, boolean setSiteUrl) {
        this.feedUrl = feedUrl;
        if (getMetaInformation().getSiteUrl() == null) {
            String siteURL = UrlHelper.getDomain(feedUrl);
            if (!siteURL.isEmpty()) {
                getMetaInformation().setSiteUrl(siteURL);
            }
        }
    }


    public void setItems(List<FeedItem> items) {
        for (FeedItem feedItem : items) {
            feedItem.setFeed(this);
        }
        this.items = items;
        setNewItem(null);
    }

    public void addItem(FeedItem item) {
        if (items == null) {
            items = new ArrayList<FeedItem>();
        }
        items.add(item);
        item.setFeed(this);
        setNewItem(null);
    }

    public List<FeedItem> getItems() {
        return items;
    }

    /**
     * Free the memory because feed objects might be held in memory. Free the memory whenever you get the feed only once
     * and won't let the garbage collector take care of it.
     */
    public void freeMemory() {
        // rawMarkup = "";
        // document = null;
        setItems(new ArrayList<FeedItem>());
        // We need the last newestItemHash to find out whether we have new items or not. - Sandro
        // setNewestItemHash("");
    }

    public void setChecks(Integer checks) {
        if (checks != null) {
            this.checks = checks;
        }
    }

    public void increaseChecks() {
        // set back the target percentage to -1, which means we need to recalculate it
        targetPercentageOfNewEntries = -1;

        // if we haven't seen a full day yet, maybe in the next check
        if (oneFullDayOfItemsSeen != null && oneFullDayOfItemsSeen == false) {
            oneFullDayOfItemsSeen = null;
        }
        this.checks++;
    }

    public int getChecks() {
        return checks;
    }

    /**
     * Set the min check interval.
     * 
     * @param minCheckInterval The min check interval in minutes.
     */
    public void setUpdateInterval(Integer updateInterval) {
        if (updateInterval != null) {
        this.updateInterval = updateInterval;
        }
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setNewestItemHash(String newestItemHash) {
        if (newestItemHash != null) {
            this.newestItemHash = newestItemHash;
        }
    }

    /**
     * Return the newest item hash when the feed was checked the last time, but is not updated when its items are
     * updated. Don't never ever ever ever use this. This is meant to be used only by the persistence layer and
     * administrative authorities. And Chuck Norris.
     * 
     * @return
     */
    public String getNewestItemHash() {
        if (newestItemHash.isEmpty()) {
            calculateNewestItemHash();
        }
        return newestItemHash;
    }

    private void calculateNewestItemHash() {
        if (items.size() > 0) {
            FeedItem feedItem = items.get(0);
            newestItemHash = feedItem.getHash();
        }
    }

    public void setUnreachableCount(Integer unreachableCount) {
        if (unreachableCount != null) {
            this.unreachableCount = unreachableCount;
        }
    }

    public void incrementUnreachableCount() {
        unreachableCount++;
    }

    public int getUnreachableCount() {
        return unreachableCount;
    }

    public void setUnparsableCount(Integer unparsableCount) {
        if (unparsableCount != null) {
            this.unparsableCount = unparsableCount;
        }
    }

    public void incrementUnparsableCount() {
        unreachableCount++;
    }

    public int getUnparsableCount() {
        return unparsableCount;
    }

    /**
     * If date's year is > 9999, we set it to null!
     * 
     * @param lastFeedEntry
     */
    public void setLastFeedEntry(Date lastFeedEntry) {
        if (lastFeedEntry != null) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(lastFeedEntry);
            int year = cal.get(Calendar.YEAR);
            if (year >= 9999) {
                lastFeedEntry = null;
            }
        }
        this.lastFeedEntry = lastFeedEntry;
    }

    public Date getLastFeedEntry() {
        return lastFeedEntry;
    }

    public Timestamp getLastFeedEntrySQLTimestamp() {
        if (lastFeedEntry != null) {
            return new Timestamp(lastFeedEntry.getTime());
        }
        return null;
    }

    /**
     * If date's year is > 9999, we set it to null!
     * 
     * @param lastFeedEntry
     */
    public void setHttpLastModified(Date httpLastModified) {
        if (httpLastModified != null) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(httpLastModified);
            int year = cal.get(Calendar.YEAR);
            if (year >= 9999) {
                httpLastModified = null;
            }
        }
        this.httpLastModified = httpLastModified;
    }

    public Date getHttpLastModified() {
        return httpLastModified;
    }

    public Timestamp getHttpLastModifiedSQLTimestamp() {
        if (httpLastModified != null) {
            return new Timestamp(httpLastModified.getTime());
        }
        return null;
    }

    public void setMeticulousPostDistribution(Map<Integer, int[]> meticulousPostDistribution) {
        this.meticulousPostDistribution = meticulousPostDistribution;
    }

    public Map<Integer, int[]> getMeticulousPostDistribution() {
        return meticulousPostDistribution;
    }

    /**
     * Check whether the checked entries in the feed were spread over at least one day yet. That means in every minute
     * of the day the chances field should be
     * greater of equal to one.
     * 
     * @return True, if the entries span at least one day, false otherwise.
     */
    public Boolean oneFullDayHasBeenSeen() {

        // if we have calculated this value, just return it
        if (oneFullDayOfItemsSeen != null) {
            return oneFullDayOfItemsSeen;
        }

        oneFullDayOfItemsSeen = true;

        for (Entry<Integer, int[]> entry : meticulousPostDistribution.entrySet()) {
            // if feed had no chance of having a post entry in any minute of the day, no full day has been seen yet
            if (entry.getValue()[1] == 0) {
                oneFullDayOfItemsSeen = false;
                break;
            }
        }

        if (meticulousPostDistribution.isEmpty()) {
            oneFullDayOfItemsSeen = false;
        }

        return oneFullDayOfItemsSeen;
    }

    public void setActivityPattern(Integer activityPattern) {
        if (activityPattern != null) {
            this.activityPattern = activityPattern;
        }
    }

    /**
     * Returns the activity pattern of the feed which is one of the following: {@link FeedClassifier#CLASS_CONSTANT},
     * {@link FeedClassifier#CLASS_CHUNKED}, {@link FeedClassifier#CLASS_SLICED} , {@link FeedClassifier#CLASS_ZOMBIE},
     * {@link FeedClassifier#CLASS_UNKNOWN} or {@link FeedClassifier#CLASS_ON_THE_FLY}
     * 
     * @return The classID of the pattern. You can get the name using {@link FeedClassifier#getClassName()}
     */
    public int getActivityPattern() {
        return activityPattern;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Feed [id=");
        builder.append(id);
        builder.append(", feedUrl=");
        builder.append(feedUrl);
        builder.append(", items=");
        builder.append(items);
        builder.append(", numberOfItemsReceived=");
        builder.append(numberOfItemsReceived);
        builder.append(", windowSize=");
        builder.append(windowSize);
        builder.append(", variableWindowSize=");
        builder.append(variableWindowSize);
        builder.append(", historyFileCompletelyRead=");
        builder.append(historyFileCompletelyRead);
        builder.append(", benchmarkLookupTime=");
        builder.append(benchmarkLookupTime);
        builder.append(", benchmarkLastLookupTime=");
        builder.append(benchmarkLastLookupTime);
        builder.append(", checks=");
        builder.append(checks);
        builder.append(", updateInterval=");
        builder.append(updateInterval);
        builder.append(", updateMode=");
        builder.append(updateMode);
        builder.append(", newestItemHash=");
        builder.append(newestItemHash);
        builder.append(", unreachableCount=");
        builder.append(unreachableCount);
        builder.append(", unparsableCount=");
        builder.append(unparsableCount);
        builder.append(", lastFeedEntry=");
        builder.append(lastFeedEntry);
        builder.append(", httpLastModified=");
        builder.append(httpLastModified);
        builder.append(", pollDataSeries=");
        builder.append(pollDataSeries);
        builder.append(", meticulousPostDistribution=");
        builder.append(meticulousPostDistribution);
        builder.append(", oneFullDayOfItemsSeen=");
        builder.append(oneFullDayOfItemsSeen);
        builder.append(", activityPattern=");
        builder.append(activityPattern);
        builder.append(", lastETag=");
        builder.append(lastETag);
        builder.append(", lastPollTime=");
        builder.append(lastPollTime);
        builder.append(", targetPercentageOfNewEntries=");
        builder.append(targetPercentageOfNewEntries);
        builder.append(", totalProcessingTimeMS=");
        builder.append(totalProcessingTimeMS);
        builder.append(", misses=");
        builder.append(misses);
        builder.append(", lastMissTime=");
        builder.append(lastMissTime);
        builder.append(", blocked=");
        builder.append(blocked);
        builder.append(", lastSuccessfulCheckTime=");
        builder.append(lastSuccessfulCheckTime);
        builder.append(", newItem=");
        builder.append(newItem);
        builder.append(", additionalData=");
        builder.append(additionalData);
        builder.append(", feedMetaInfo=");
        builder.append(feedMetaInfo);
        builder.append("]");
        return builder.toString();
    }

    public void setLastETag(String lastETag) {
        this.lastETag = lastETag;
    }

    /**
     * @return The last ETag we got form the HTTP header. <code>null</code> if not provided.
     */
    public String getLastETag() {
        return lastETag;
    }

    /**
     * @return The date this feed was checked for updates the last time.
     */
    public final Date getLastPollTime() {
        return lastPollTime;
    }

    /**
     * @return The timestamp of the last poll or <code>null</code> if the feed has never been polled so far.
     */
    public Timestamp getLastPollTimeSQLTimestamp() {
        if (lastPollTime != null) {
            return new Timestamp(lastPollTime.getTime());
        }
        return null;
    }

    /**
     * @param lastChecked The date this feed was checked for updates the last time.
     */
    public final void setLastPollTime(Date lastPollTime) {
        this.lastPollTime = lastPollTime;
    }

    public void setNewItem(Boolean newItem) {
        this.newItem = newItem;
    }

    public Boolean hasNewItem() {
        // boolean newItem = false;
        //
        // if (items.size() > 0) {
        // FeedItem feedItem = items.get(0);
        // String hash = "";
        // hash += feedItem.getTitle();
        // hash += feedItem.getLink();
        // hash += feedItem.getRawId();
        // hash = StringHelper.sha1(hash);
        //
        // if (!hash.equals(getNewestItemHash())) {
        // newItem = true;
        // // who added this? setNewestItemHash(hash);
        // }
        // }
        //
        // return newItem;

        if (newItem == null) {
            String oldNewestItemHash = getNewestItemHash();
            calculateNewestItemHash();
            newItem = !oldNewestItemHash.equals(getNewestItemHash());
        }

        return newItem;
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
        result = prime * result + ((additionalData == null) ? 0 : additionalData.hashCode());
        result = prime * result + (int) (benchmarkLastLookupTime ^ (benchmarkLastLookupTime >>> 32));
        result = prime * result + (int) (benchmarkLookupTime ^ (benchmarkLookupTime >>> 32));
        result = prime * result + (blocked ? 1231 : 1237);
        result = prime * result + checks;
        result = prime * result + ((feedMetaInfo == null) ? 0 : feedMetaInfo.hashCode());
        result = prime * result + ((feedUrl == null) ? 0 : feedUrl.hashCode());
        result = prime * result + (historyFileCompletelyRead ? 1231 : 1237);
        result = prime * result + ((httpLastModified == null) ? 0 : httpLastModified.hashCode());
        result = prime * result + id;
        result = prime * result + ((items == null) ? 0 : items.hashCode());
        result = prime * result + ((lastETag == null) ? 0 : lastETag.hashCode());
        result = prime * result + ((lastFeedEntry == null) ? 0 : lastFeedEntry.hashCode());
        result = prime * result + ((lastMissTime == null) ? 0 : lastMissTime.hashCode());
        result = prime * result + ((lastPollTime == null) ? 0 : lastPollTime.hashCode());
        result = prime * result + ((lastSuccessfulCheckTime == null) ? 0 : lastSuccessfulCheckTime.hashCode());
        result = prime * result + ((meticulousPostDistribution == null) ? 0 : meticulousPostDistribution.hashCode());
        result = prime * result + misses;
        result = prime * result + ((newItem == null) ? 0 : newItem.hashCode());
        result = prime * result + ((newestItemHash == null) ? 0 : newestItemHash.hashCode());
        result = prime * result + numberOfItemsReceived;
        result = prime * result + ((oneFullDayOfItemsSeen == null) ? 0 : oneFullDayOfItemsSeen.hashCode());
        result = prime * result + ((pollDataSeries == null) ? 0 : pollDataSeries.hashCode());
        long temp;
        temp = Double.doubleToLongBits(targetPercentageOfNewEntries);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (int) (totalProcessingTimeMS ^ (totalProcessingTimeMS >>> 32));
        result = prime * result + unparsableCount;
        result = prime * result + unreachableCount;
        result = prime * result + updateInterval;
        result = prime * result + updateMode;
        result = prime * result + ((variableWindowSize == null) ? 0 : variableWindowSize.hashCode());
        result = prime * result + ((windowSize == null) ? 0 : windowSize.hashCode());
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
        Feed other = (Feed) obj;
        if (activityPattern != other.activityPattern)
            return false;
        if (additionalData == null) {
            if (other.additionalData != null)
                return false;
        } else if (!additionalData.equals(other.additionalData))
            return false;
        if (benchmarkLastLookupTime != other.benchmarkLastLookupTime)
            return false;
        if (benchmarkLookupTime != other.benchmarkLookupTime)
            return false;
        if (blocked != other.blocked)
            return false;
        if (checks != other.checks)
            return false;
        if (feedMetaInfo == null) {
            if (other.feedMetaInfo != null)
                return false;
        } else if (!feedMetaInfo.equals(other.feedMetaInfo))
            return false;
        if (feedUrl == null) {
            if (other.feedUrl != null)
                return false;
        } else if (!feedUrl.equals(other.feedUrl))
            return false;
        if (historyFileCompletelyRead != other.historyFileCompletelyRead)
            return false;
        if (httpLastModified == null) {
            if (other.httpLastModified != null)
                return false;
        } else if (!httpLastModified.equals(other.httpLastModified))
            return false;
        if (id != other.id)
            return false;
        if (items == null) {
            if (other.items != null)
                return false;
        } else if (!items.equals(other.items))
            return false;
        if (lastETag == null) {
            if (other.lastETag != null)
                return false;
        } else if (!lastETag.equals(other.lastETag))
            return false;
        if (lastFeedEntry == null) {
            if (other.lastFeedEntry != null)
                return false;
        } else if (!lastFeedEntry.equals(other.lastFeedEntry))
            return false;
        if (lastMissTime == null) {
            if (other.lastMissTime != null)
                return false;
        } else if (!lastMissTime.equals(other.lastMissTime))
            return false;
        if (lastPollTime == null) {
            if (other.lastPollTime != null)
                return false;
        } else if (!lastPollTime.equals(other.lastPollTime))
            return false;
        if (lastSuccessfulCheckTime == null) {
            if (other.lastSuccessfulCheckTime != null)
                return false;
        } else if (!lastSuccessfulCheckTime.equals(other.lastSuccessfulCheckTime))
            return false;
        if (meticulousPostDistribution == null) {
            if (other.meticulousPostDistribution != null)
                return false;
        } else if (!meticulousPostDistribution.equals(other.meticulousPostDistribution))
            return false;
        if (misses != other.misses)
            return false;
        if (newItem == null) {
            if (other.newItem != null)
                return false;
        } else if (!newItem.equals(other.newItem))
            return false;
        if (newestItemHash == null) {
            if (other.newestItemHash != null)
                return false;
        } else if (!newestItemHash.equals(other.newestItemHash))
            return false;
        if (numberOfItemsReceived != other.numberOfItemsReceived)
            return false;
        if (oneFullDayOfItemsSeen == null) {
            if (other.oneFullDayOfItemsSeen != null)
                return false;
        } else if (!oneFullDayOfItemsSeen.equals(other.oneFullDayOfItemsSeen))
            return false;
        if (pollDataSeries == null) {
            if (other.pollDataSeries != null)
                return false;
        } else if (!pollDataSeries.equals(other.pollDataSeries))
            return false;
        if (Double.doubleToLongBits(targetPercentageOfNewEntries) != Double
                .doubleToLongBits(other.targetPercentageOfNewEntries))
            return false;
        if (totalProcessingTimeMS != other.totalProcessingTimeMS)
            return false;
        if (unparsableCount != other.unparsableCount)
            return false;
        if (unreachableCount != other.unreachableCount)
            return false;
        if (updateInterval != other.updateInterval)
            return false;
        if (updateMode != other.updateMode)
            return false;
        if (variableWindowSize == null) {
            if (other.variableWindowSize != null)
                return false;
        } else if (!variableWindowSize.equals(other.variableWindowSize))
            return false;
        if (windowSize == null) {
            if (other.windowSize != null)
                return false;
        } else if (!windowSize.equals(other.windowSize))
            return false;
        return true;
    }

    /**
     * @return The raw XML markup for this feed.
     */
    // public String getRawMarkup() {
    // if (rawMarkup == null) {
    // rawMarkup = HTMLHelper.documentToHTMLString(getDocument());
    // }
    // return rawMarkup;
    // }

    // public Document getDocument() {
    // return document;
    // }

    // public void setDocument(Document document) {
    // this.document = document;
    // }

    /**
     * If the new windowSize is different to the previous size (exept the previous was null), the variable window size
     * flag is set.
     * 
     * @param windowSize
     */
    public void setWindowSize(Integer windowSize) {

        Integer oldWindowSize = getWindowSize();
        // do not set back to null
        if (windowSize != null) {
            this.windowSize = windowSize;

            // If windowSize has been set before and differs to current value, set flag to true.
            if (oldWindowSize != null && oldWindowSize.compareTo(windowSize) != 0) {
                setVariableWindowSize(true);

                // If flag has not been set before, and current and old windows have the same size, set flag to false.
            } else if (hasVariableWindowSize() == null) {
                setVariableWindowSize(false);
            }
        }
    }

    /**
     * @return The feed's window size. <code>null</code> if unknown.
     */
    public Integer getWindowSize() {
        return windowSize;
    }

    /**
     * Find out whether the history file has been read completely, a file is considered to be read completely when the
     * window reached the last feed post in the file.
     * This function is for benchmarking purposes only.
     * 
     * @return True if the window has read the last post entry of the history file, false otherwise.
     */
    public boolean historyFileCompletelyRead() {
        return historyFileCompletelyRead;
    }

    public void setHistoryFileCompletelyRead(boolean b) {
        historyFileCompletelyRead = b;
    }

    public void addToBenchmarkLookupTime(long checkInterval) {
        setBenchmarkLastLookupTime(benchmarkLookupTime);
        benchmarkLookupTime += checkInterval;
    }

    public void setPollDataSeries(PollDataSeries pollDataSeries) {
        this.pollDataSeries = pollDataSeries;
    }

    public PollDataSeries getPollDataSeries() {
        return pollDataSeries;
    }

    public long getBenchmarkLookupTime() {
        return benchmarkLookupTime;
    }

    public void setBenchmarkLookupTime(long benchmarkLookupTime) {
        this.benchmarkLookupTime = benchmarkLookupTime;
    }

    public void setBenchmarkLastLookupTime(long benchmarkLastLookupTime) {
        this.benchmarkLastLookupTime = benchmarkLastLookupTime;
    }

    public long getBenchmarkLastLookupTime() {
        return benchmarkLastLookupTime;
    }


    public void setUpdateMode(int updateMode) {
        this.updateMode = updateMode;
    }

    public int getUpdateMode() {
        return updateMode;
    }

    /**
     * Set the time in millisecond that has been spent on processing this feed.
     * 
     * @param totalProcessingTimeMS time in milliseconds. Ignored if smaller than zero.
     */
    public void setTotalProcessingTime(Long totalProcessingTimeMS) {
        if (totalProcessingTimeMS != null && totalProcessingTimeMS > 0) {
            this.totalProcessingTimeMS = totalProcessingTimeMS;
        }
    }

    /**
     * Get the time in milliseconds that has been spent on processing this feed.
     * 
     * @return time in milliseconds >= 0. Initially 0.
     */
    public long getTotalProcessingTime() {
        return totalProcessingTimeMS;
    }

    /**
     * Increases the time that has been spend on processing this feed by the given value.
     * 
     * @param processingTimeToAddMS time to add in millisecond.
     */
    public void increaseTotalProcessingTimeMS(long processingTimeToAddMS) {
        if (processingTimeToAddMS > 0) {
            setTotalProcessingTime(getTotalProcessingTime() + processingTimeToAddMS);
        }
    }

    /**
     * Get the average time in milliseconds that has been spent on processing this feed.
     * 
     * @return totalProcessingTime/(checks + unreachableCount)
     */
    public long getAverageProcessingTime() {
        return getTotalProcessingTime() / Math.max(1, (getChecks() + getUnreachableCount()));
    }

    /**
     * Number of times that we found a MISS, that is, if we knew the feed's items before, checked it again and none of
     * the items has been seen before.
     * 
     * @return the misses
     */
    public final int getMisses() {
        return misses;
    }

    /**
     * Set the number of times that we found a MISS, that is, if we knew the feed's items before, checked it again and
     * none of the items has been seen before.
     * 
     * @param misses The number of misses
     */
    public final void setMisses(Integer misses) {
        if (misses != null) {
            this.misses = misses;
        }
    }

    /**
     * Increases the the number of times that we found a MISS by 1. Sets lastMissTime to {@link #getLastPollTime()}.
     */
    public void increaseMisses() {
        setMisses(getMisses() + 1);
        setLastMissTime(getLastPollTime());
    }

    /**
     * Set the timestamp when we found the last MISS for this feed.
     * 
     * @param lastMissTime The timestamp we detected the last miss.
     */
    public void setLastMissTime(Date lastMissTime) {
        this.lastMissTime = lastMissTime;
    }

    /**
     * Get the timestamp when we found the last MISS for this feed.
     * 
     * @return The timestamp we detected the last miss, <code>null</code> if there has never been a MISS.
     */
    public Date getLastMissTime() {
        return lastMissTime;
    }

    /**
     * If true, do not use feed to create a data set, e.g. by {@link DatasetCreator}
     * 
     * @return If true, do not use feed to create a data set, e.g. by {@link DatasetCreator}
     */
    public final boolean isBlocked() {
        return blocked;
    }

    /**
     * Set to true to not use the feed to create a data set, e.g. by {@link DatasetCreator}
     * 
     * @param blocked set to true to block the feed.
     */
    public final void setBlocked(Boolean blocked) {
        if (blocked != null) {
            this.blocked = blocked;
        }
    }

    /**
     * The timestamp this feed has been successfully checked the last time. A successful check happens if the feed is
     * reachable and parsable.
     * 
     * @return The timestamp of the last successful check, <code>null</code> if there is none.
     */
    public final Date getLastSuccessfulCheckTime() {
        return lastSuccessfulCheckTime;
    }

    /**
     * The timestamp this feed has been successfully checked the last time. A successful check happens if the feed is
     * reachable and parsable. This timestamp should be set every time {@link #checks} is increased.
     * 
     * @param lastSuccessfulCheckTime The timestamp of the last successful check, <code>null</code> if there is none.
     */
    public final void setLastSuccessfulCheckTime(Date lastSuccessfulCheckTime) {
        this.lastSuccessfulCheckTime = lastSuccessfulCheckTime;
    }

    /**
     * Set to <code>true</code> if the feed has a variable window size. The variableWindowSize is initially
     * <code>null</code> (unknown). Once set to true, it can't be changed anymore.
     * 
     * @param variableWindowSize Set to <code>true</code> if the feed has a variable window size.
     */
    public void setVariableWindowSize(Boolean variableWindowSize) {
        if (variableWindowSize != null && (hasVariableWindowSize() == null || variableWindowSize == true)) {
            this.variableWindowSize = variableWindowSize;
        }
    }

    /**
     * @return <code>true</code> if the feed has a variable window size, <code>false</code> if not, <code>null</code> if
     *         unknown.
     */
    public Boolean hasVariableWindowSize() {
        return variableWindowSize;
    }

    /**
     * The number of unique items downloaded so far. This value may differ from {@link #items}.size() since
     * {@link #items} may have been reseted by calling {@link #freeMemory()}.
     * 
     * @param numberOfItemsReceived The number of unique items downloaded so far.
     */
    public void setNumberOfItemsReceived(Integer numberOfItemsReceived) {
        if (numberOfItemsReceived != null) {
            this.numberOfItemsReceived = numberOfItemsReceived;
        }
    }

    /**
     * @return The number of unique items downloaded so far. This value may differ from {@link #items}.size() since
     *         {@link #items} may have been reseted by calling {@link #freeMemory()}.
     */
    public int getNumberOfItemsReceived() {
        return numberOfItemsReceived;
    }

    /**
     * Increments the {@link #numberOfItemsReceived} so far by the given value.
     * 
     * @param numberOfNewItems Number of new items.
     */
    public void incrementNumberOfItemsReceived(int numberOfNewItems) {
        numberOfItemsReceived += numberOfNewItems;
    }

    /**
     * Print feed with content in human readable form.
     * 
     * @param includeText
     */
    public void print(boolean includeText) {

        StringBuilder builder = new StringBuilder();

        builder.append(getMetaInformation().getTitle()).append("\n");
        builder.append("feedUrl : ").append(getFeedUrl()).append("\n");
        builder.append("siteUrl : ").append(getMetaInformation().getSiteUrl()).append("\n");
        builder.append("-----------------------------------").append("\n");
        List<FeedItem> items = getItems();
        if (items != null) {
            for (FeedItem item : items) {
                builder.append(item.getTitle()).append("\t");
                if (includeText) {
                    builder.append(item.getDescription()).append("\t");
                    builder.append(item.getText()).append("\t");
                }
                builder.append(item.getLink()).append("\t");
                builder.append(item.getPublished()).append("\t");
                builder.append(item.getAuthors()).append("\n");
            }
            builder.append("-----------------------------------").append("\n");
            builder.append("# entries: ").append(items.size());
        }

        System.out.println(builder.toString());
    }

    /**
     * Print feed with content in human readable form.
     */
    public void print() {
        print(false);
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public Object getAdditionalData(String key) {
        return additionalData.get(key);
    }

    /**
     * @param feedMetaInfo the meta information to set
     */
    public void setFeedMetaInformation(FeedMetaInformation feedMetaInfo) {
        this.feedMetaInfo = feedMetaInfo;
    }

    /**
     * @return The feed's meta information.
     */
    public FeedMetaInformation getMetaInformation() {
        return feedMetaInfo;
    }

}