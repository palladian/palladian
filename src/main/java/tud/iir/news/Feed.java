package tud.iir.news;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.extraction.PageAnalyzer;
import tud.iir.helper.CollectionHelper;
import tud.iir.news.evaluation.PollDataSeries;
import tud.iir.web.Crawler;

/**
 * Represents a news feed.
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public class Feed {

    private static final Logger LOGGER = Logger.getLogger(Feed.class);

    /** Symbols to separate headlines. */
    public static final String TITLE_SEPARATION = "#-#";

    /**
     * Different formats of feeds; this has just informational character; the parser of the aggregator will determine
     * the feed's format automatically.
     */
    public static final int FORMAT_ATOM = 1;
    public static final int FORMAT_RSS = 2;

    // different text lengths in feeds
    public static final int TEXT_TYPE_UNDETERMINED = 0;
    public static final int TEXT_TYPE_NONE = 1;
    public static final int TEXT_TYPE_PARTIAL = 2;
    public static final int TEXT_TYPE_FULL = 3;

    private int id = -1;
    private String feedUrl;
    private String siteUrl;
    private String title;
    private int format;
    private Date added;
    private String language;
    private int textType = TEXT_TYPE_UNDETERMINED;

    /** The size of the feed in bytes. */
    private long byteSize = 0;

    private List<FeedItem> entries;

    /** The number of feed entries presented for each request. */
    private int windowSize = -1;

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

    /** number of times the feed has been retrieved and read */
    private int checks = 0;

    /**
     * Time in minutes until it is expected to find at least one new entry in the feed.
     */
    private int minCheckInterval = 60;

    /**
     * Time in minutes until it is expected to find only new but one new entries in the feed.
     */
    private int maxCheckInterval = 120;

    public static int MIN_DELAY = 0;
    public static int MAX_COVERAGE = 1;

    /** Either MIN_DELAY (minCheckInterval) or MAX_COVERAGE (maxCheckInterval). */
    private int updateMode = Feed.MIN_DELAY;

    /** a list of headlines that were found at the last check */
    private String lastHeadlines = "";

    /** number of times the feed was checked but could not be found or parsed */
    private int unreachableCount = 0;

    /** timestamp of the last feed entry found in this feed */
    private Date lastFeedEntry = null;

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

    /** Whether the feed supports ETags. */
    private Boolean eTagSupport;

    /** Whether the feed supports LastModifiedSince. */
    private Boolean lmsSupport;

    /** The header size of the feed when it supports a conditional get (ETag or LastModifiedSince). */
    private Integer cgHeaderSize;

    /**
     * The raw XML markup for this feed.
     */
    private String plainXML;

    private double targetPercentageOfNewEntries = -1;

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

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    public void setFeedUrl(String feedUrl, boolean setSiteURL) {
        this.feedUrl = feedUrl;
        if (setSiteURL) {
            setSiteUrl(Crawler.getDomain(feedUrl));
        }
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String pageUrl) {
        this.siteUrl = pageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getFormat() {
        return format;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public Date getAdded() {
        return added;
    }

    public Timestamp getAddedSQLTimestamp() {
        if (added != null) {
            return new Timestamp(added.getTime());
        }
        return null;
    }

    public void setAdded(Date added) {
        this.added = added;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getTextType() {
        return textType;
    }

    public void setTextType(int textType) {
        this.textType = textType;
    }

    public void setEntries(List<FeedItem> entries) {
        this.entries = entries;
    }

    public List<FeedItem> getEntries() {
        return entries;
    }

    public List<FeedItem> getEntries(Boolean update, Boolean usePageContentExtractor) {
        if (update) {
            updateEntries(usePageContentExtractor);
        }
        return getEntries();
    }

    // TODO move this to virtual class FeedFetcher
    public void updateEntries(Boolean usePageContentExtractor) {
        NewsAggregator aggregator = new NewsAggregator();
        aggregator.setDownloadPages(usePageContentExtractor);
        try {
            // FIXME
            Feed f = aggregator.downloadFeed(this);
            setEntries(f.getEntries());
        } catch (NewsAggregatorException e) {
            LOGGER.error("Unable to load entries for feed at address: " + getFeedUrl() + ", " + e.getMessage());
        }
        setPlainXML(PageAnalyzer.getRawMarkup(aggregator.getPlainXMLFeed()));
    }

    public void freeMemory() {
        setPlainXML("");
        setEntries(new ArrayList<FeedItem>());
    }

    public void setChecks(int checks) {
        this.checks = checks;
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
     * Set the max check interval.
     * Min = 4 minutes, max = 6 month (267840 minutes)
     * 
     * @param maxCheckInterval The max check interval in minutes.
     */
    public void setMaxCheckInterval(int maxCheckInterval) {
        maxCheckInterval = Math.max(4, maxCheckInterval);
        maxCheckInterval = Math.min(267840, maxCheckInterval);
        this.maxCheckInterval = maxCheckInterval;
    }

    public int getMaxCheckInterval() {
        return maxCheckInterval;
    }

    /**
     * Set the min check interval.
     * Min = 2 minutes, max = 1 month (44640 minutes)
     * 
     * @param minCheckInterval The min check interval in minutes.
     */
    public void setMinCheckInterval(int minCheckInterval) {
        minCheckInterval = Math.max(2, minCheckInterval);
        minCheckInterval = Math.min(44640, minCheckInterval);
        this.minCheckInterval = minCheckInterval;
    }

    public int getMinCheckInterval() {
        return minCheckInterval;
    }

    /**
     * Return either minCheckInterval of maxCheckInterval depending on updateMode.
     * 
     * @return The check interval depending on the updateMode.
     */
    public int getCheckInterval() {
        if (getUpdateMode() == Feed.MIN_DELAY) {
            return getMinCheckInterval();
        }

        return getMaxCheckInterval();
    }

    public void setLastHeadlines(String lastHeadlines) {
        this.lastHeadlines = lastHeadlines;
    }

    public String getLastHeadlines() {
        return lastHeadlines;
    }

    public void setUnreachableCount(int unreachableCount) {
        this.unreachableCount = unreachableCount;
    }

    public int getUnreachableCount() {
        return unreachableCount;
    }

    public void setLastFeedEntry(Date lastFeedEntry) {
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

    public void setActivityPattern(int activityPattern) {
        this.activityPattern = activityPattern;
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

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("Feed");
        // sb.append(" id:").append(id);
        sb.append(" feedUrl:").append(feedUrl);
        // sb.append(" siteUrl:").append(siteUrl);
        sb.append(" title:").append(title);
        // sb.append(" format:").append(format);
        // sb.append(" language:").append(language);
        // sb.append(" added:").append(added);

        return sb.toString();
    }

    public void setLastETag(String lastETag) {
        this.lastETag = lastETag;
    }

    public String getLastETag() {
        return lastETag;
    }

    /**
     * @return The date this feed was checked for updates the last time.
     */
    public final Date getLastPollTime() {
        return lastPollTime;
    }

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

    public void setByteSize(long byteSize) {
        this.byteSize = byteSize;
    }

    public long getByteSize() {
        return byteSize;
    }

    /**
     * <p>
     * Get a separated string with the headlines and links of all feed entries. The primary key is headline + link since
     * sometimes the headlines for several entries are the same but they point to different articles.
     * </p>
     * 
     * @param entries Feed entries.
     * @return A separated string with the headlines of all feed entries.
     */
    private StringBuilder getNewEntryTitles() {

        StringBuilder titles = new StringBuilder();
        for (FeedItem entry : getEntries()) {
            titles.append(entry.getTitle() + entry.getLink()).append(TITLE_SEPARATION);
        }

        return titles;
    }

    /**
     * Calculate the target percentage of new entries as follows: Percentage of new entries = pn = newEntries /
     * totalEntries Target Percentage = pTarget =
     * newEntries / (totalEntries - 1) A target percentage of 1 means that all entries but one are new and this is
     * exactly what we want.
     * 
     * Example 1: newEntries = 3, totalEntries = 4, pn = 0.75, pTarget = 3 / (4-1) = 1
     * 
     * Example 2: newEntries = 7, totalEntries = 10, pn = 0.7, pTarget = 7 / (10-1) ~ 0.78
     * 
     * The target percentage depends on the number of total entries and is not always the same as the examples show.
     * 
     * @return The percentage of news calculated as explained.
     */
    public double getTargetPercentageOfNewEntries() {

        if (targetPercentageOfNewEntries == -1) {

            // compare old and new entry titles to get percentage pn of new entries
            String[] oldTitlesArray = getLastHeadlines().split(TITLE_SEPARATION);
            Set<String> oldTitles = CollectionHelper.toHashSet(oldTitlesArray);

            // get new entry titles
            StringBuilder titles = getNewEntryTitles();
            Set<String> currentTitles = CollectionHelper.toHashSet(titles.toString().split(TITLE_SEPARATION));

            // count number of same titles
            int overlap = 0;
            for (String oldTitle : oldTitles) {
                for (String newTitle : currentTitles) {
                    if (oldTitle.equalsIgnoreCase(newTitle)) {
                        overlap++;
                        LOGGER.trace("equal headline: " + oldTitle);
                        LOGGER.trace("with headline:  " + newTitle);
                        break;
                    }
                }
            }

            // number of really new headlines
            int newEntries = Math.max(0, currentTitles.size() - overlap);

            // percentage of new entries - 1 entry, this is our target, if we know
            // at least one entry we know that we did not miss any
            double pnTarget = 1;

            if (currentTitles.size() > 1) {
                pnTarget = newEntries / ((double) currentTitles.size() - 1);
                // pnTarget = newEntries / ((double) currentTitles.size());
            } else {
                // in this special case we just look at the feed the default check time
                // pnTarget = -1;
                // LOGGER.warn(currentTitles.size() + " title(s) found in " + getId() + " ("+ getFeedUrl() + ")");
            }

            setLastHeadlines(titles.toString());

            targetPercentageOfNewEntries = pnTarget;
        }

        return targetPercentageOfNewEntries;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (added == null ? 0 : added.hashCode());
        result = prime * result + checks;
        result = prime * result + (entries == null ? 0 : entries.hashCode());
        result = prime * result + (feedUrl == null ? 0 : feedUrl.hashCode());
        result = prime * result + format;
        result = prime * result + id;
        result = prime * result + (language == null ? 0 : language.hashCode());
        result = prime * result + (lastPollTime == null ? 0 : lastPollTime.hashCode());
        result = prime * result + (lastFeedEntry == null ? 0 : lastFeedEntry.hashCode());
        result = prime * result + (lastHeadlines == null ? 0 : lastHeadlines.hashCode());
        result = prime * result + maxCheckInterval;
        result = prime * result + (meticulousPostDistribution == null ? 0 : meticulousPostDistribution.hashCode());
        result = prime * result + minCheckInterval;
        result = prime * result + (siteUrl == null ? 0 : siteUrl.hashCode());
        result = prime * result + textType;
        result = prime * result + (title == null ? 0 : title.hashCode());
        result = prime * result + unreachableCount;
        result = prime * result + activityPattern;
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Feed other = (Feed) obj;
        if (added == null) {
            if (other.added != null) {
                return false;
            }
        } else if (!added.equals(other.added)) {
            return false;
        }
        if (checks != other.checks) {
            return false;
        }
        if (entries == null) {
            if (other.entries != null) {
                return false;
            }
        } else if (!entries.equals(other.entries)) {
            return false;
        }
        if (feedUrl == null) {
            if (other.feedUrl != null) {
                return false;
            }
        } else if (!feedUrl.equals(other.feedUrl)) {
            return false;
        }
        if (format != other.format) {
            return false;
        }
        if (id != other.id) {
            return false;
        }
        if (language == null) {
            if (other.language != null) {
                return false;
            }
        } else if (!language.equals(other.language)) {
            return false;
        }
        if (lastPollTime == null) {
            if (other.lastPollTime != null) {
                return false;
            }
        } else if (!lastPollTime.equals(other.lastPollTime)) {
            return false;
        }
        if (lastFeedEntry == null) {
            if (other.lastFeedEntry != null) {
                return false;
            }
        } else if (!lastFeedEntry.equals(other.lastFeedEntry)) {
            return false;
        }
        if (lastHeadlines == null) {
            if (other.lastHeadlines != null) {
                return false;
            }
        } else if (!lastHeadlines.equals(other.lastHeadlines)) {
            return false;
        }
        if (maxCheckInterval != other.maxCheckInterval) {
            return false;
        }
        if (meticulousPostDistribution == null) {
            if (other.meticulousPostDistribution != null) {
                return false;
            }
        } else if (!meticulousPostDistribution.equals(other.meticulousPostDistribution)) {
            return false;
        }
        if (minCheckInterval != other.minCheckInterval) {
            return false;
        }
        if (siteUrl == null) {
            if (other.siteUrl != null) {
                return false;
            }
        } else if (!siteUrl.equals(other.siteUrl)) {
            return false;
        }
        if (textType != other.textType) {
            return false;
        }
        if (title == null) {
            if (other.title != null) {
                return false;
            }
        } else if (!title.equals(other.title)) {
            return false;
        }
        if (unreachableCount != other.unreachableCount) {
            return false;
        }
        if (activityPattern != other.activityPattern) {
            return false;
        }
        return true;
    }

    /**
     * @param plainXML The raw XML markup for this feed.
     */
    public void setPlainXML(String plainXML) {
        this.plainXML = plainXML;
    }

    /**
     * @return The raw XML markup for this feed.
     */
    public String getPlainXML() {
        return plainXML;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public int getWindowSize() {
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

    public void setETagSupport(Boolean eTagSupport) {
        this.eTagSupport = eTagSupport;
    }

    public Boolean getETagSupport() {
        return eTagSupport;
    }

    public void setLMSSupport(Boolean lmsSupport) {
        this.lmsSupport = lmsSupport;
    }

    public Boolean getLMSSupport() {
        return lmsSupport;
    }

    public void setCgHeaderSize(Integer cgHeaderSize) {
        this.cgHeaderSize = cgHeaderSize;
    }

    public Integer getCgHeaderSize() {
        if (cgHeaderSize != null && cgHeaderSize <= 0) {
            cgHeaderSize = null;
        }
        return cgHeaderSize;
    }

    public void setUpdateMode(int updateMode) {
        this.updateMode = updateMode;
    }

    public int getUpdateMode() {
        return updateMode;
    }
    
    
    public static void main(String[] args) throws Exception{
        
        NewsAggregator aggregator = new NewsAggregator();
        Feed feed = aggregator.downloadFeed("http://www.tagesschau.de/xml/rss2");
        System.out.println("# entries : " + feed.getEntries().size());

        feed.updateEntries(true);
        System.out.println("# entries after updateEntries : " + feed.getEntries().size());
    }

}