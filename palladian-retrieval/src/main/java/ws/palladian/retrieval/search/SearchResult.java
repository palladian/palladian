package ws.palladian.retrieval.search;

import java.util.Date;

/**
 * <p>
 * A {@link SearchResult} is a response that resulted from a search query.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public abstract class SearchResult {

    private final String title;
    private final String summary;
    private final Date date;
    private final String searchEngine;

    public SearchResult(String title, String summary, Date date) {
        this(title, summary, date, null);
    }

    public SearchResult(String title, String summary) {
        this(title, summary, null, null);
    }

    public SearchResult(String title, String summary, Date date, String searchEngine) {
        super();
        this.title = title;
        this.summary = summary;
        this.date = date;
        this.searchEngine = searchEngine;
    }

    public SearchResult(String title, String summary, String searchEngine) {
        this(title, summary, null, searchEngine);
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * <p>
     * Get the name of the search engine which provided this {@link SearchResult}. Return <code>null</code> when search
     * engine has not been specified.
     * </p>
     * 
     * @return
     */
    public String getSearchEngine() {
        return searchEngine;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        result = prime * result + ((summary == null) ? 0 : summary.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SearchResult other = (SearchResult) obj;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (summary == null) {
            if (other.summary != null)
                return false;
        } else if (!summary.equals(other.summary))
            return false;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SearchResult [title=");
        builder.append(title);
        builder.append(", summary=");
        builder.append(summary);
        builder.append(", date=");
        builder.append(date);
        builder.append(", search engine=");
        builder.append(searchEngine);
        builder.append("]");
        return builder.toString();
    }

}
