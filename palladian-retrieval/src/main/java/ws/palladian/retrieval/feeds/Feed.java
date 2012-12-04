package ws.palladian.retrieval.feeds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;
import ws.palladian.retrieval.feeds.meta.FeedMetaInformation;

/**
 * <p>
 * Represents a news feed. (A feed should be processed by a single thread only.)
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * 
 */
public class Feed {

    /** The logger for this class. */
    // private static final Logger LOGGER = Logger.getLogger(Feed.class);

    /** Internal database identifier. */
    private int id = -1;

    /** The URL of this feed. */
    private String feedUrl = null;

    /** The items of this feed. */
    private List<FeedItem> items = new ArrayList<FeedItem>();

    /**
     * A backup of the items' hashes and timestamps from the most recent window that is not deleted when
     * {@link #freeMemory()} is called. It can be used as a cache and read to update the checkInterval in case we did a
     * conditional get request and the feed has not been changed since the last request.
     */
    private Map<String, Date> itemCache = new HashMap<String, Date>();

    /**
     * The items that were new in the most recent poll.
     */
    private List<FeedItem> newItems = new ArrayList<FeedItem>();

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

    /** Number of times the feed has been retrieved and successfully read or polled and has not been modified. */
    private int checks = 0;

    /**
     * The default time in minutes until it is expected to find at least one new entry in the feed.
     */
    public static final int DEFAULT_UPDATE_INTERVAL = 60;

    /**
     * Time in minutes until it is expected to find at least one new entry in the feed.
     */
    private int updateInterval = DEFAULT_UPDATE_INTERVAL;

    /** Either MIN_DELAY (minCheckInterval) or MAX_COVERAGE (maxCheckInterval). */
    private FeedUpdateMode updateMode = FeedUpdateMode.MIN_DELAY;

    /** Our internal hash of the most recent item. <code>null</code> if we've never seen any item so far. */
    private String newestItemHash = null;

    /** number of times the feed was checked but could not be found. */
    private int unreachableCount = 0;

    /** number of times the feed was checked but could not be parsed. */
    private int unparsableCount = 0;

    /** Timestamp of the last feed entry found in this feed. */
    private Date lastFeedEntry = null;

    /** Timestamp of the last but one feed entry found in this feed. */
    private Date lastButOneFeedEntry = null;

    /**
     * If set to true, {@link #calculateNewestAndOldestItemHashAndDate()} has to be called to refresh
     * {@link #lastFeedEntry} etc.
     */
    private boolean recalculateDates = true;

    /**
     * The publish timestamp of the oldest entry in the most recent window. Value is not persisted in database.
     */
    private Date oldestFeedEntryCurrentWindow = null;

    /** The HTTP header's last-modified value of the last poll. */
    private Date httpLastModified = null;

    /** The HTTP header's date value of the last poll (The current system time of the feed server) */
    private Date httpDateLastPoll = null;

//    /**
//     * Record statistics about poll data for evaluation purposes.
//     */
//    private PollDataSeries pollDataSeries = new PollDataSeries();

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
    private FeedActivityPattern activityPattern = FeedActivityPattern.CLASS_UNKNOWN;

    /** The ETag that was send with the last request. This saves bandwidth for feeds that support ETags. */
    private String lastETag = null;

    /**
     * The date this feed was checked for updates the last time. This can be used to send last modified since requests.
     */
    private Date lastPollTime;

    /**
     * The date this feed was checked for updates the last but one time. This can be used to correct item timestamps.
     * It should be in the past of {@link #lastPollTime}.
     */
    private Date lastButOnePollTime = null;

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

    /** Allows to keep arbitrary, additional information. */
    private Map<String, Object> additionalData = null;

    /** The feed's meta information. */
    private FeedMetaInformation feedMetaInfo = new FeedMetaInformation();

    /** The result of the most recent feed task. */
    private FeedTaskResult lastFeedTaskResult = null;

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
        this.feedUrl = feedUrl;
    }

    /**
     * Replace the feed's items with the provided items. Make sure the items' properties such as windowSize or httpDate
     * are already set. It is not assumed that the list contains exactly one complete poll.
     * 
     * @param items All items to set.
     */
    public void setItems(List<FeedItem> items) {

        ArrayList<FeedItem> newItemsTemp = new ArrayList<FeedItem>();
        Map<String, Date> itemCacheTemp = new HashMap<String, Date>();
        recalculateDates = true;

        for (FeedItem feedItem : items) {
            feedItem.setFeed(this);
            String hash = feedItem.getHash();
            if (isNewItem(hash)) {
                // correct timestamp only in case this hasn't been done before.
                // if (feedItem.getCorrectedPublishedDate() == null) {
                Date correctedTimestamp = correctedTimestamp(feedItem.getPublished(), getLastPollTime(),
                        getLastButOnePollTime(), feedItem.toString(), false);
                feedItem.setCorrectedPublishedDate(correctedTimestamp);
                // }

                itemCacheTemp.put(hash, feedItem.getCorrectedPublishedDate());
                newItemsTemp.add(feedItem);
            } else {
                itemCacheTemp.put(hash, getCachedItemTimestamp(hash));
            }
        }

        // Prevent cache from being reset to empty cache. Some feeds suddenly have empty windows for a short period of
        // time, afterwards their windows contain items seen before. If we delete the cache when receiving an empty
        // window, we loose the ability to do the duplicate detection, all items seem to be new, we have a MISS.
        if (getCachedItems().isEmpty() || !itemCacheTemp.isEmpty()) {
            setCachedItems(itemCacheTemp);
        }

        setNewItems(newItemsTemp);
        this.items = items;
    }

    public void addItem(FeedItem item) {
        if (items == null) {
            items = new ArrayList<FeedItem>();
        }
        recalculateDates = true;
        items.add(item);
        item.setFeed(this);

        String hash = item.getHash();
        if (isNewItem(hash)) {
            // correct timestamp only in case this hasn't been done before.
            // if (item.getCorrectedPublishedDate() == null) {
            Date correctedTimestamp = correctedTimestamp(item.getPublished(), getLastPollTime(),
                    getLastButOnePollTime(), item.toString(), false);
            item.setCorrectedPublishedDate(correctedTimestamp);
            // }
            addCacheItem(hash, item.getCorrectedPublishedDate());
            addNewItem(item);
        } else {
            addCacheItem(hash, getCachedItemTimestamp(hash));
        }

    }

    public List<FeedItem> getItems() {
        return items;
    }

    /**
     * TODO remove param logWarnings, put to config file and set to true by default?
     * Correct publish dates of entries. It is assumed, that the entry to check has been fetched at lastPollTimeFeed
     * and it has not been seen before (not identified as duplicate). Therefore, it's timestamp has to be older than
     * the last poll, and if we already polled the feed twice or more, the item timestamp must be newer than the last
     * but one poll (otherwise we would have seen the item before).<br />
     * <br />
     * 
     * Get the publish date from the entry. In case an entry has no timestamp, its timestamp is in the future of
     * lastPollTimeFeed, older than the the last but one poll (lastButOnePollTimeFeed) or older than 01.01.1990 00:00
     * (Unix 631152000), the last poll timestamp (lastPollTimeFeed) is used instead.
     * 
     * @param entryPublishDate The entry's publish date to correct.
     * @param lastPollTimeFeed The time the feed has been polled the last time, that is, the time of the current poll
     * @param lastButOnePollTimeFeed The time of the last but one poll. This is the poll done before lastPollTimeFeed.
     *            May be <code>null</code> if there was no such poll.
     * @param logMessage Message to write to logfile in case the date has been corrected. Useful to know which item has
     *            been corrected when reading the logfile.
     * @param logWarnings If <code>true</code>, warnings are logged in case the entry has no or an illegal timestamp.
     *            Use with caution, this will generate massive log traffic...
     * @return the corrected publish date.
     */
    public static Date correctedTimestamp(Date entryPublishDate, Date lastPollTimeFeed, Date lastButOnePollTimeFeed,
            String logMessage, boolean logWarnings) {
        StringBuilder warnings = new StringBuilder();

        // get poll timestamp, if not present, use current time as estimation.
        long pollTime = 0;
        String timestampUsed = "";
        if (lastPollTimeFeed != null) {
            pollTime = lastPollTimeFeed.getTime();
            timestampUsed = ". Setting poll timestamp instead.";
        } else {
            pollTime = System.currentTimeMillis();
            timestampUsed = ". Setting current system timestamp instead.";
        }

        // Is the pubDate provided by feed? Check for 'illegal' date in future.
        // Future publish dates are allowed in RSS 2.0 but are useless for predicting updates.
        Date pubDate = null;
        if (entryPublishDate != null) {
            pubDate = new Date(entryPublishDate.getTime());

            // check for date in future of last (=current) poll
            if (pubDate.getTime() > pollTime) {
                pubDate = new Date(pollTime);
                warnings.append("Entry has a pub date in the future, feed entry : ").append(logMessage)
                        .append(timestampUsed);
                // is entry older than last but one poll? If so, we should have seen it before so pubDate must be wrong.
            } else if (lastButOnePollTimeFeed != null && !pubDate.after(lastButOnePollTimeFeed)) {
                pubDate = new Date(pollTime);
                warnings.append("Entry has a pub date in the past of the last but one poll, feed entry : ")
                        .append(logMessage).append(timestampUsed);

                // Entry has a pub date older than 01.01.1990 00:00 (Unix 631152000), date must be wrong
            } else if (pubDate.getTime() < 631152000) {
                pubDate = new Date(pollTime);
                warnings.append("Entry has a pub date older than 01.01.1990 00:00 (Unix 631152000), feed entry : ")
                        .append(logMessage).append(timestampUsed);
            }

            // no pubDate provided, use poll timestamp
        } else {
            warnings.append("Entry has no pub date, feed entry : ").append(logMessage).append(timestampUsed);
            pubDate = new Date(pollTime);
        }
        if (logWarnings && warnings.length() > 0) {
            FeedReader.LOGGER.warn(warnings);
        }

        return pubDate;
    }

    /**
     * Locally store the item timestamp. Make sure that this date is not newer than the feed's poll timestamp.
     * 
     * @param pubDate a item timestamp.
     */
    private void addCacheItem(String hash, Date pubDate) {
        this.itemCache.put(hash, pubDate);
    }

    /**
     * Replaces the current item cache with the given one. Don't never ever ever ever use this. This is meant to be used
     * only by the persistence layer and administrative authorities. And Chuck Norris.
     * 
     * @param toCache The new item cache to set.
     */
    public void setCachedItems(Map<String, Date> toCache) {
        this.itemCache = toCache;
    }

    /**
     * Get all cached item hashes and their associated corrected publish dates. These are the items of the most recent
     * feed window that was not empty. Caution! In case the feeds last window was empty but the one before contained
     * items, the cache contains these items and does not reflect the current polls content (which would be empty).
     * 
     * @return all cached item hashes and their associated publish dates.
     */
    public Map<String, Date> getCachedItems() {
        return itemCache;
    }

    /**
     * Get the cached, (corrected) publish date that is associated to the provided item hash.
     * 
     * @param hash The {@link FeedItem}'s hash to get the publish date for.
     * @return the cached, (corrected) publish date that is associated to the provided item hash or <code>null</code> if
     *         the hash is unknown.
     */
    private Date getCachedItemTimestamp(String hash) {
        return itemCache.get(hash);
    }

    /**
     * Checks whether the provided item hash is already in the cache. If so, the item is already known.
     * 
     * @param hash The item's hash to check.
     * @return <code>true</code> if the hash is already in {@link #itemCache}, <code>false</code> else wise.
     */
    private boolean isNewItem(String hash) {
        return !itemCache.containsKey(hash);
    }

    /**
     * Get the item's corrected timestamps of the most recent poll. Usually, these are the same timestamps as
     * iterating over {@link #getItems()} and get their pubDate. In case {@link #getItems()} returns an empty list, it
     * may have been reset by {@link #freeMemory()}, the timestamps provided by this method will not be removed. They
     * are replaced as soon as the feed is downloaded the next time.
     * <p>
     * Since some feeds do not provide timestamps for some or all of their items and publish dates may be in the future,
     * missing and future timestamps are replaced by the feed's poll timestamp.
     * </p>
     * 
     * @return Corrected item timestamps.
     */
    public Collection<Date> getCorrectedItemTimestamps() {
        return itemCache.values();
    }

    /**
     * Get all items that were new in the most recent poll.
     * 
     * @return the newItems, never <code>null</code>.
     */
    public List<FeedItem> getNewItems() {
        return newItems;
    }

    /**
     * @param newItems the newItems to set
     */
    private void setNewItems(List<FeedItem> newItems) {
        if (newItems != null) {
            this.newItems = newItems;
            incrementNumberOfItemsReceived(newItems.size());
        }
    }

    /**
     * Add the item to the list of items that are known to be new (not contained in the previous poll of this feed). If
     * you want to add an item to this feed, use {@link #addItem(FeedItem)} instead.
     * 
     * @param newItem A item that is known to be new.
     */
    private void addNewItem(FeedItem newItem) {
        this.newItems.add(newItem);
        incrementNumberOfItemsReceived(1);
    }

    /**
     * Free the memory because feed objects might be held in memory. Free the memory whenever you get the feed only once
     * and won't let the garbage collector take care of it.
     */
    public void freeMemory() {
        this.items = new ArrayList<FeedItem>();
        this.newItems = new ArrayList<FeedItem>();
    }

    public void setChecks(Integer checks) {
        if (checks != null) {
            this.checks = checks;
        }
    }

    /**
     * Increase the number of times the feed has a) been retrieved and successfully read or b) polled and has not been
     * modified.
     */
    public void increaseChecks() {
        // set back the target percentage to -1, which means we need to recalculate it
        targetPercentageOfNewEntries = -1;

        // if we haven't seen a full day yet, maybe in the next check
        if (oneFullDayOfItemsSeen != null && oneFullDayOfItemsSeen == false) {
            oneFullDayOfItemsSeen = null;
        }
        this.checks++;
    }

    /**
     * Number of times the feed has been retrieved and successfully read or polled and has not been modified.
     * 
     * @return Number of times the feed has been retrieved and successfully read or polled and has not been modified.
     */
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

    /**
     * Time in minutes until it is expected to find at least one new entry in the feed.
     */
    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setNewestItemHash(String newestItemHash) {
        this.newestItemHash = newestItemHash;
    }

    /**
     * Return the newest item hash when the feed was checked the last time, but is not updated when its items are
     * updated. Don't never ever ever ever use this. This is meant to be used only by the persistence layer and
     * administrative authorities. And Chuck Norris.
     * 
     * @return
     */
    public String getNewestItemHash() {
        if (recalculateDates) {
            calculateNewestAndOldestItemHashAndDate();
        }
        return newestItemHash;
    }

    /**
     * Calculates and sets the hash of the newest, second newest and oldest item and its corrected publish date. In case
     * we haven't seen any items so far, there is no hash or date so we set them to <code>null</code>.
     */
    private void calculateNewestAndOldestItemHashAndDate() {
        Map<String, Date> cache = getCachedItems();
        String tempNewestHash = null;
        Date tempNewestDate = lastFeedEntry;
        Date tempSecondNewestDate = lastButOneFeedEntry;
        Date tempOldestDate = null;

        for (String hash : cache.keySet()) {
            long currentElement = cache.get(hash).getTime();

            if (tempNewestDate == null) {
                tempNewestDate = cache.get(hash);
                tempNewestHash = hash;
            }
            if (tempNewestDate.getTime() < currentElement) {
                tempSecondNewestDate = tempNewestDate;
                tempNewestDate = cache.get(hash);
                tempNewestHash = hash;
            }
            if (tempNewestDate != null && currentElement < tempNewestDate.getTime()
                    && (tempSecondNewestDate == null || currentElement > tempSecondNewestDate.getTime())) {
                tempSecondNewestDate = cache.get(hash);
            }

            if (tempOldestDate == null || tempOldestDate.getTime() > cache.get(hash).getTime()) {
                tempOldestDate = cache.get(hash);
            }

        }
        setLastFeedEntry(tempNewestDate);
        setLastButOneFeedEntry(tempSecondNewestDate);
        setNewestItemHash(tempNewestHash);
        setOldestFeedEntryCurrentWindow(tempOldestDate);
        recalculateDates = false;
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
        unparsableCount++;
    }

    public int getUnparsableCount() {
        return unparsableCount;
    }

    /**
     * If date's year is > 9999, we set it to null! Do not set this value, it is calculated by the feed itself. The
     * setter is to be used by the persistence layer only!
     * 
     * @param lastFeedEntry
     */
    public void setLastFeedEntry(Date lastFeedEntry) {
        this.lastFeedEntry = DateHelper.validateYear(lastFeedEntry, 9999);
    }

    /**
     * @return The timestamp of the most recent item.
     */
    public Date getLastFeedEntry() {
        if (recalculateDates) {
            calculateNewestAndOldestItemHashAndDate();
        }
        return lastFeedEntry;
    }

    /**
     * The Date of the second newest entry. Might be same as {@link #getLastFeedEntry()} in case the two newest entries
     * have the same publish date. Use with caution, this value is automatically updated.
     * 
     * @param lastButOneFeedEntry
     */
    public void setLastButOneFeedEntry(Date lastButOneFeedEntry) {
        this.lastButOneFeedEntry = DateHelper.validateYear(lastButOneFeedEntry, 9999);
    }

    /**
     * The Date of the second newest entry. It is guaranteed that the returned value is before
     * {@link #getLastFeedEntry()} or <code>null</code> if there is no such date. The returned date might be from a
     * previous poll. Example with two poll and windowSize of 2:<br />
     * <br />
     * Poll 1, Entry A: 01.01.2000 00:00<br />
     * Poll 1, Entry B: 01.01.2000 00:30<br />
     * Poll 2, Entry C: 01.01.2000 01:00<br />
     * Poll 2, Entry D: 01.01.2000 01:00<br />
     * <br />
     * Since distinct dates are forced, entry B is returned.
     * 
     * @return The Date of the second newest entry or <code>null</code> if there is no such date.
     */
    public Date getLastButOneFeedEntry() {
        if (recalculateDates) {
            calculateNewestAndOldestItemHashAndDate();
        }
        return lastButOneFeedEntry;
    }

    /**
     * @return The publish timestamp of the oldest entry in the most recent window.
     */
    public final Date getOldestFeedEntryCurrentWindow() {
        if (recalculateDates) {
            calculateNewestAndOldestItemHashAndDate();
        }
        return oldestFeedEntryCurrentWindow;
    }

    /**
     * The publish timestamp of the oldest entry in the most recent window. If date's year is > 9999, we set it to null!
     * Do not set this value, it is calculated by the feed itself. The setter is to be used by the persistence layer
     * only!
     * 
     * @param oldestFeedEntryCurrentWindow The publish timestamp of the oldest entry in the most recent window.
     */
    public final void setOldestFeedEntryCurrentWindow(Date oldestFeedEntryCurrentWindow) {
        this.oldestFeedEntryCurrentWindow = DateHelper.validateYear(oldestFeedEntryCurrentWindow, 9999);
    }

    /**
     * If date's year is > 9999, we set it to null!
     * 
     * @param lastFeedEntry
     */
    public void setHttpLastModified(Date httpLastModified) {
        this.httpLastModified = DateHelper.validateYear(httpLastModified, 9999);
    }

    public Date getHttpLastModified() {
        return httpLastModified;
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

    public void setActivityPattern(FeedActivityPattern activityPattern) {
        if (activityPattern != null) {
            this.activityPattern = activityPattern;
        }
    }

    /**
     * Returns the activity pattern of the feed which is one of the following: {@link FeedClassifier#CLASS_CONSTANT},
     * {@link FeedClassifier#CLASS_CHUNKED}, {@link FeedClassifier#CLASS_SLICED} , {@link FeedClassifier#CLASS_ZOMBIE},
     * {@link FeedClassifier#CLASS_UNKNOWN} or {@link FeedClassifier#CLASS_ON_THE_FLY}
     * 
     * @return The {@link FeedActivityPattern}. You can get the name using {@link FeedClassifier#getClassName()}
     */
    public FeedActivityPattern getActivityPattern() {
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
     * Sets the time as lastPollTime and sets the old value as {@link #setLastButOnePollTime(Date)}.
     * 
     * @param lastChecked The date this feed was checked for updates the last time.
     */
    public final void setLastPollTime(Date lastPollTime) {
        setLastButOnePollTime(this.lastPollTime);
        this.lastPollTime = lastPollTime;
    }

    /**
     * 
     * The date this feed was checked for updates the last but one time. This can be used to correct item timestamps.
     * It should be in the past of {@link #getLastPollTime()}.
     * 
     * @return the lastButOnePollTime, might be <code>null</code> if there was no such poll.
     */
    public final Date getLastButOnePollTime() {
        return lastButOnePollTime;
    }

    /**
     * The date this feed was checked for updates the last but one time. This can be used to correct item timestamps.
     * It should be in the past of {@link #getLastPollTime()}.
     * 
     * @param lastButOnePollTime the lastButOnePollTime to set
     */
    public final void setLastButOnePollTime(Date lastButOnePollTime) {
        this.lastButOnePollTime = lastButOnePollTime;
    }

    public Boolean hasNewItem() {
        return getNewItems().size() > 0;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((activityPattern == null) ? 0 : activityPattern.hashCode());
        result = prime * result + ((additionalData == null) ? 0 : additionalData.hashCode());
        result = prime * result + (int)(benchmarkLastLookupTime ^ (benchmarkLastLookupTime >>> 32));
        result = prime * result + (int)(benchmarkLookupTime ^ (benchmarkLookupTime >>> 32));
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
        result = prime * result + ((newestItemHash == null) ? 0 : newestItemHash.hashCode());
        result = prime * result + numberOfItemsReceived;
        result = prime * result + ((oneFullDayOfItemsSeen == null) ? 0 : oneFullDayOfItemsSeen.hashCode());
        long temp;
        temp = Double.doubleToLongBits(targetPercentageOfNewEntries);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + (int)(totalProcessingTimeMS ^ (totalProcessingTimeMS >>> 32));
        result = prime * result + unparsableCount;
        result = prime * result + unreachableCount;
        result = prime * result + updateInterval;
        result = prime * result + ((updateMode == null) ? 0 : updateMode.hashCode());
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
        Feed other = (Feed)obj;
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
     * If the new windowSize is different to the previous size (except the previous was null), the variable window size
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

    public void setUpdateMode(FeedUpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    public FeedUpdateMode getUpdateMode() {
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
    private void incrementNumberOfItemsReceived(int numberOfNewItems) {
        numberOfItemsReceived += numberOfNewItems;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    /**
     * @return The feed's additional data or an empty map if there is none
     */
    public Map<String, Object> getAdditionalData() {
        if (additionalData == null) {
            additionalData = new HashMap<String, Object>();
        }
        return additionalData;
    }

    public Object getAdditionalData(String key) {
        if (additionalData == null) {
            return null;
        }
        return additionalData.get(key);
    }

    /**
     * Add additional data, identified by key.
     * 
     * @param key Identifier to be used to access this data.
     * @param data the additional data to store.
     * @return The additional data previously associated with key, or <code>null</code> if there was no mapping for key.
     */
    public Object addAdditionalData(String key, Object data) {
        return getAdditionalData().put(key, data);
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

    /**
     * @return the FeedTaskResult of the most recent FeedTask. <code>null</code> if the FeedTask has never been run on
     *         this feed.
     */
    public final FeedTaskResult getLastFeedTaskResult() {
        return lastFeedTaskResult;
    }

    /**
     * @param lastFeedTaskResult the recentFeedTaskResult to set
     */
    public final void setLastFeedTaskResult(FeedTaskResult lastFeedTaskResult) {
        this.lastFeedTaskResult = lastFeedTaskResult;
    }

    /**
     * The HTTP header's date value of the last poll (The current system time of the feed server)
     * 
     * @return the httpDateLastPoll
     */
    public final Date getHttpDateLastPoll() {
        return httpDateLastPoll;
    }

    /**
     * The HTTP header's date value of the last poll (The current system time of the feed server)
     * If date's year is > 9999, we set it to null!
     * 
     * @param httpDateLastPoll the httpDateLastPoll to set
     */
    public final void setHttpDateLastPoll(Date httpDateLastPoll) {
        this.httpDateLastPoll = DateHelper.validateYear(httpDateLastPoll, 9999);
    }

}