package ws.palladian.retrieval.wiki.data;

import java.util.Map;

/**
 * Helper to use page titles as keys in {@link Map}s.
 * 
 * @author Sandro Reichert
 */
public class PageTitle {

    /** The title of the page. */
    private final String title;

    /**
     * @param pageTitle Title of the page.
     */
    public PageTitle(final String pageTitle) {
        if (pageTitle == null || pageTitle.length() == 0) {
            throw new IllegalArgumentException("Value for pageTitle may not be null or empty string!");
        }
        this.title = pageTitle;
    }

    /**
     * Get the title of the page.
     * 
     * @return The title of the page. It is guaranteed that the return value is not <code>null</code> and of length > 0.
     */
    public String getpageTitle() {
        return title;
    }

    /**
     * {@code true} if the given object is also of type {@link PageTitle} and it's {@link #title} equals {@code this}.
     * {@link #title}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PageTitle)) {
            return false;
        }
        PageTitle pt = (PageTitle) obj;
        return this.title.equals(pt.title);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return title.hashCode();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "PageTitle [pageTitle=" + title + "]";
    }

}
