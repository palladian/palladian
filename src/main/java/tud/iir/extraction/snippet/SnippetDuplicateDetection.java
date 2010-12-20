package tud.iir.extraction.snippet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tud.iir.knowledge.Snippet;
import tud.iir.persistence.DatabaseManager;

/**
 * This class provides simple de-duplication techniques to eliminate duplicated snippets.
 * 
 * @author Christopher Friedrich
 * @author David Urbansky
 */
public class SnippetDuplicateDetection {

    /**
     * This method removes the exact duplicates from a list of snippets, which are either within the same list or in the database.
     * 
     * @param snippets - The List of snippets
     */
    public static void removeDuplicates(List<Snippet> snippets) {

        // remove duplicates in snippets list (from this run)
        Map<String, Snippet> findings = new HashMap<String, Snippet>();
        for (Snippet snippet : snippets) {
            String key = snippet.getText();

            // take the first match
            if (!findings.containsKey(key)) {
                findings.put(key, snippet);
            }
        }

        ArrayList<Snippet> list = new ArrayList<Snippet>();
        for (Entry<String, Snippet> finding : findings.entrySet()) {
            list.add(finding.getValue());
        }
        snippets.retainAll(list);

        // check for duplicates in database (from former run)
        DatabaseManager dm = DatabaseManager.getInstance();

        ArrayList<Snippet> dblist = new ArrayList<Snippet>();
        for (Snippet snippet : snippets) {
            // check if snippet exists for entity
            if (!dm.snippetExists(snippet)) {
                dblist.add(snippet);
            }
        }
        snippets.retainAll(dblist);

    }

}
