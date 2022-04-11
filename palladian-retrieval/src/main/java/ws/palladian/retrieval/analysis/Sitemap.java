package ws.palladian.retrieval.analysis;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * <p>
 * Represents a single sitemap.
 * </p>
 *
 * @author Jaroslav Vankat
 * @link https://www.sitemaps.org/protocol.html
 */
public class Sitemap {

    private LinkedHashSet<Entry> urlSet = new LinkedHashSet<>();

    public Sitemap() {

    }

    public Sitemap(LinkedHashSet<Entry> urlSet) {
        this.urlSet = urlSet;
    }

    public LinkedHashSet<Entry> getUrlSet() {
        return urlSet;
    }

    public void setUrlSet(LinkedHashSet<Entry> urlSet) {
        this.urlSet = urlSet;
    }

    public static class Entry {

        private String location;
        private Date lastModified;
        private Double priority;

        public Entry(String location, Date lastModified, Double priority) {
            this.location = location;
            this.lastModified = lastModified;
            this.priority = priority;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public Date getLastModified() {
            return lastModified;
        }

        public void setLastModified(Date lastModified) {
            this.lastModified = lastModified;
        }

        public Double getPriority() {
            return priority;
        }

        public void setPriority(Double priority) {
            this.priority = priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Entry that = (Entry) o;
            return Objects.equals(location, that.location) && Objects.equals(lastModified, that.lastModified) && Objects.equals(priority, that.priority);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, lastModified, priority);
        }

        @Override
        public String toString() {
            return "SitemapEntry{" + "location='" + location + '\'' + ", lastModified=" + lastModified + ", priority=" + priority +  '}';
        }
    }
}
