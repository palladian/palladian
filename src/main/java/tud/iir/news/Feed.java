package tud.iir.news;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents a news feed.
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * 
 */
public class Feed {

    /**
     * different formats of feeds; this has just informational character; the parser of the aggregator will determine the feed's format automatically.
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
    
    private List<FeedEntry> entries;

    /** number of times the feed has been retrieved and read */
    private int checks = 0;

    /**
     * time in minutes until it is expected to find at least one new entry in the feed
     */
    private int minCheckInterval = 30;

    /**
     * time in minutes until it is expected to find only new but one new entries in the feed
     */
    private int maxCheckInterval = 60;

    /** a list of headlines that were found at the last check */
    private String lastHeadlines = "";

    /** number of times the feed was checked but could not be found or parsed */
    private int unreachableCount = 0;

    /** timestamp of the last feed entry found in this feed */
    private Date lastFeedEntry = null;

    /**
     * number of news that were posted in a certain minute of the day, minute of the day : frequency of posts; chances a post could have appeared
     */
    private Map<Integer, int[]> meticulousPostDistribution = new HashMap<Integer, int[]>();

    /** the update class of the feed is one of {@link FeedClassifier}s classes */
    private int updateClass = -1;

    public Feed() {
    }

    public Feed(String feedUrl) {
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

    public void setEntries(List<FeedEntry> entries) {
        this.entries = entries;
    }

    public List<FeedEntry> getEntries() {
        return entries;
    }

    public void setChecks(int checks) {
        this.checks = checks;
    }

    public void increaseChecks() {
        this.checks++;
    }

    public int getChecks() {
        return checks;
    }

    public void setMaxCheckInterval(int maxCheckInterval) {
        maxCheckInterval = Math.max(1, maxCheckInterval);
        this.maxCheckInterval = maxCheckInterval;
    }

    public int getMaxCheckInterval() {
        return maxCheckInterval;
    }

    public void setMinCheckInterval(int minCheckInterval) {
        minCheckInterval = Math.max(1, minCheckInterval);
        this.minCheckInterval = minCheckInterval;
    }

    public int getMinCheckInterval() {
        return minCheckInterval;
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
     * Check whether the checked entries in the feed were spread over at least one day yet. That means in every minute of the day the chances field should be
     * greater of equal to one.
     * 
     * @return True, if the entries span at least one day, false otherwise.
     */
    public boolean oneFullDayHasBeenSeen() {
        boolean daySeen = true;

        for (Entry<Integer, int[]> entry : meticulousPostDistribution.entrySet()) {
            // if feed had no chance of having a post entry in any minute of the day, no full day has been seen yet
            if (entry.getValue()[1] == 0) {
                daySeen = false;
                break;
            }
        }

        if (meticulousPostDistribution.isEmpty()) {
            daySeen = false;
        }

        return daySeen;
    }

    public void setUpdateClass(int updateClass) {
        this.updateClass = updateClass;
    }

    /**
     * Returns the update class of the feed which is one of the following: {@link FeedClassifier#CLASS_CONSTANT}, {@link FeedClassifier#CLASS_CHUNKED},
     * {@link FeedClassifier#CLASS_SLICED} , {@link FeedClassifier#CLASS_ZOMBIE}, {@link FeedClassifier#CLASS_UNKNOWN} or
     * {@link FeedClassifier#CLASS_ON_THE_FLY}
     * 
     * @return The classID of the class. You can get the name using {@link FeedClassifier#getClassName()}
     */
    public int getUpdateClass() {
        return updateClass;
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
}