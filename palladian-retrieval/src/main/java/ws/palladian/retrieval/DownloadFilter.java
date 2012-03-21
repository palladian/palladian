package ws.palladian.retrieval;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * The download filter can be used in conjunction with the {@link DocumentRetriever} to specify, which file types should
 * be downloaded and which to ignore. The file type is determined with regards to the URL's file extension. Therefore it
 * provides a black and a white list. To determine, whether to reject a file, the following steps are considered:
 * <ul>
 * <li>If the white list is empty, all file types are included. If the white contains file types, <b>only</b> those are
 * accepted.</li>
 * <li>If the file is accepted according to the white list, the black list is considered. If the file type in question
 * is <b>not</b> on the black list, it is accepted.</li>
 * </ul>
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class DownloadFilter {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DownloadFilter.class);

    /** The file types to include. */
    private Set<String> whitelist;

    /** The file types to exclude. */
    private Set<String> blacklist;

    public DownloadFilter() {
        whitelist = new HashSet<String>();
        blacklist = new HashSet<String>();
    }

    public void setIncludeFileTypes(Collection<String> fileTypes) {
        whitelist = new HashSet<String>(fileTypes);
    }

    public void addIncludeFileTypes(String... fileTypes) {
        whitelist.addAll(Arrays.asList(fileTypes));
    }

    public void setIncludeFileTypes(String... fileTypes) {
        setIncludeFileTypes(Arrays.asList(fileTypes));
    }

    public void setExcludeFileTypes(Collection<String> fileTypes) {
        blacklist = new HashSet<String>(fileTypes);
    }

    public void addExcludeFileTypes(String... fileTypes) {
        blacklist.addAll(Arrays.asList(fileTypes));

    }

    public void setExcludeFileTypes(String... fileTypes) {
        setExcludeFileTypes(Arrays.asList(fileTypes));
    }

    public boolean isAcceptedFileType(String url) {
        String fileType = FileHelper.getFileType(url).toLowerCase();
        boolean whitelisted = whitelist.isEmpty() || whitelist.contains(fileType);
        boolean blacklisted = blacklist.contains(fileType);
        boolean accepted = whitelisted && !blacklisted;
        if (!accepted) {
            LOGGER.debug("rejected URL : " + url);
        }
        return accepted;
    }

}
