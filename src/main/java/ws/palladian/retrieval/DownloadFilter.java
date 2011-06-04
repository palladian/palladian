package ws.palladian.retrieval;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.math.SizeUnit;

public class DownloadFilter {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DownloadFilter.class);

    /** List of binary file extensions. */
    public static final String[] BINARY_FILE_TYPES = { "pdf", "doc", "ppt", "xls", "png", "jpg", "jpeg", "gif", "ai",
            "svg", "zip", "avi", "exe", "msi", "wav", "mp3", "wmv", "swf" };

    /** List of textual file extensions. */
    public static final String[] TEXTUAL_FILE_TYPES = { "cfm", "db", "svg", "txt" };

    /**
     * The file types to include. If this set is empty, all file types are included. If there are entries in the list
     * ONLY those file types are included.
     */
    private Set<String> includeFileTypes;

    /** The file types to exclude. If this set is empty, no file types are excluded. */
    private Set<String> excludeFileTypes;

    /**
     * The maximum file size in bytes which should be downloaded. A value of -1 means no limit. If you
     * think that's a good idea, see: http://articles-articles-articles.com/index.php?page=mostpopulararticles
     * Muahahahaha.
     */
    private long maxFileSize;

    public DownloadFilter() {
        includeFileTypes = new HashSet<String>();
        excludeFileTypes = new HashSet<String>();
        maxFileSize = SizeUnit.MEGABYTES.toBytes(1);
    }

    public void setIncludeFileTypes(Collection<String> fileTypes) {
        includeFileTypes = new HashSet<String>(fileTypes);
    }

    public void setIncludeFileTypes(String[] fileTypes) {
        setIncludeFileTypes(Arrays.asList(fileTypes));
    }

    public void setExcludeFileTypes(Collection<String> fileTypes) {
        excludeFileTypes = new HashSet<String>(fileTypes);
    }

    public void setExcludeFileTypes(String[] fileTypes) {
        setExcludeFileTypes(Arrays.asList(fileTypes));
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public boolean isAcceptedFileType(String url) {
        String fileType = FileHelper.getFileType(url);
        boolean whiteListed = includeFileTypes.isEmpty() || includeFileTypes.contains(fileType);
        boolean blackListed = excludeFileTypes.contains(fileType);
        boolean accepted = whiteListed && !blackListed;
        if (!accepted) {
            LOGGER.debug("rejected URL : " + url);
        }
        return accepted;
    }

}
