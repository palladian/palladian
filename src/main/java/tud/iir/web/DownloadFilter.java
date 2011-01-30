package tud.iir.web;

import java.util.HashSet;
import java.util.Set;

public class DownloadFilter {

    /** List of binary file extensions. */
    public static final String[] BINARY_FILE_TYPES = { "pdf", "doc", "ppt", "xls", "png", "jpg", "jpeg", "gif", "ai",
            "svg", "zip", "avi", "exe", "msi", "wav", "mp3", "wmv", "swf" };

    /** List of textual file extensions. */
    public static final String[] TEXTUAL_FILE_TYPES = { "cfm", "db", "svg", "txt" };

    /**
     * The file types to include. If this set is empty, all file types are included. If there are entries in the list
     * ONLY those file types are included.
     */
    private Set<String> includeFileTypes = new HashSet<String>();

    /** The file types to exclude. If this set is empty, no file types are excluded. */
    private Set<String> excludeFileTypes = new HashSet<String>();

    /**
     * The maximum file size which should be downloaded (TODO: only web documents?). -1 means no limit. If you think
     * that's a good idea, see: http://articles-articles-articles.com/index.php?page=mostpopulararticles Muahahahaha.
     */
    private long maxFileSize = 1000000;
    
    public Set<String> getIncludeFileTypes() {
        return includeFileTypes;
    }

    public void setIncludeFileTypes(Set<String> includeFileTypes) {
        this.includeFileTypes = includeFileTypes;
    }

    public Set<String> getExcludeFileTypes() {
        return excludeFileTypes;
    }

    public void setExcludeFileTypes(Set<String> excludeFileTypes) {
        this.excludeFileTypes = excludeFileTypes;
    }

    public void setExcludeFileTypes(String[] fileTypes) {
        for (String fileType : fileTypes) {
            this.excludeFileTypes.add(fileType);
        }
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }


}
