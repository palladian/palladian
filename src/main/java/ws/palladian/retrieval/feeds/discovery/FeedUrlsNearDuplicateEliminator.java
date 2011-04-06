package ws.palladian.retrieval.feeds.discovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.commons.lang.ArrayUtils;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.LineAction;
import ws.palladian.helper.UrlHelper;

/**
 * Quickndirty implementation for Sandro's highly sophisticated Feed-URLs-Near-Duplicate-Detection-Algorithm(tm).
 * @author Philipp Katz
 */
public class FeedUrlsNearDuplicateEliminator {

    /** The logger for this class. */
    // private static final Logger LOGGER = Logger.getLogger(FeedUrlsNearDuplicateEliminator.class);

    // be sure, to sort the Strings in a way, so that no String in the Array is contained in its successor
    private static final String[] ATOM = new String[] { "atom10", "atom" };
    private static final String[] RSS = new String[] { "rss_2.0", "rss200", "rss2", "RSS2", "rss" };

    // Atom first, as it is the preferred format
    private static final String[] FORMATS = (String[]) ArrayUtils.addAll(ATOM, RSS);

    // place holder for temporary replacements
    private static final String FORMAT_PLACEHOLDER = "###FORMAT###";

    public static void main(String[] args) {

        final String inputFile = "/Users/pk/Desktop/FeedUrlNearDuplicateRemover/foundFeedsDeduplicated_Goldstandard_Input.txt";
        final String outputFile = "/Users/pk/Desktop/FeedUrlNearDuplicateRemover/foundFeedsDeduplicated_Result.txt";

        /** Collect links for each domain. */
        final Queue<String> linkQueue = new LinkedList<String>();

        LineAction lineAction = new LineAction() {

            String domain = null;

            @Override
            public void performAction(String line, int lineNumber) {
                String currentDomain = UrlHelper.getDomain(line);
                // the current Domain differs from the previous iteration;
                // do the de-duplication on the URLs in the queue and write them out
                boolean nextDomain = !currentDomain.equalsIgnoreCase(domain) && domain != null;
                if (nextDomain) {
                    List<String> deDuplicated = deDuplicate(linkQueue);
                    appendFile(outputFile, deDuplicated);
                    linkQueue.clear();
                }
                linkQueue.add(line);
                domain = currentDomain;
            }
        };
        FileHelper.delete(outputFile);
        FileHelper.performActionOnEveryLine(inputFile, lineAction);

        // write the rest of the URLs in the queue
        List<String> deDuplicated = deDuplicate(linkQueue);
        appendFile(outputFile, deDuplicated);

    }

    public static List<String> deDuplicate(Collection<String> linkQueue) {

        // list with result, candidates without explicitly given format are added directly
        List<String> result = new ArrayList<String>();

        // map contains [ url-with-placeholder ; [ format1; format2; format3; ...] ]
        MultiMap<String, String> temp = new MultiHashMap<String, String>();

        for (String link : linkQueue) {
            String format = null;
            link = link.trim();
            for (String s : FORMATS) {
                if (link.contains(s)) {
                    link = link.replace(s, FORMAT_PLACEHOLDER);
                    format = s;
                    break;
                }
            }
            if (format != null) {
                temp.put(link, format);
            } else {
                result.add(link);
            }
        }

        // remove "overlap", e.g. [ "http://peterdowdall.com/feed/", "http://peterdowdall.com/feed/atom/" ]
        // is reduced to ""http://peterdowdall.com/feed/atom/", because the whole string is contained in the other
        ListIterator<String> listIterator = result.listIterator();
        while (listIterator.hasNext()) {
            String current = listIterator.next();
            for (String key : temp.keySet()) {
                if (key.indexOf(current) != -1) {
                    listIterator.remove();
                }
            }
        }

        // find out the "best" alternative; if we have an Atom feed, take this,
        // elsewise take the first from what we have
        Set<Entry<String, Collection<String>>> entrySet = temp.entrySet();
        for (Entry<String, Collection<String>> entry : entrySet) {
            String link = entry.getKey();
            Collection<String> candidates = entry.getValue();
            for (String format : FORMATS) {
                if (candidates.contains(format)) {
                    link = link.replace(FORMAT_PLACEHOLDER, format);
                    result.add(link);
                    break;
                }
            }
        }

        // LOGGER.info(linkQueue + " -> " + result);
        return result;
    }

    private static void appendFile(String filePath, Collection<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        FileHelper.appendFile(filePath, sb.toString());
    }

}
