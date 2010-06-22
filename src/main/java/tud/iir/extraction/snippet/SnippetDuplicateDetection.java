package tud.iir.extraction.snippet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tud.iir.knowledge.Snippet;
import tud.iir.persistence.DatabaseManager;

/**
 * This class provides different de-duplication techniques to eliminate duplicated or near-duplicated snippets.
 * 
 * This class is described in detail in "Friedrich, Christopher. WebSnippets - Extracting and Ranking of entity-centric knowledge from the Web. Diploma thesis,
 * Technische Universität Dresden, April 2010".
 * 
 * @author Christopher Friedrich
 */
public class SnippetDuplicateDetection {

    public static final int PLAIN = 0;
    public static final int SHINGLES = 1;

    /**
     * Remove the duplicates from a list of snippets, which are either within the same list or in the database. This might vary by technique.
     * 
     * Depending on the technique specified, these are either exact or near duplicates.
     * 
     * @param snippets - List of snippets
     * @param method - Technique used to remove duplicates, currently implemented is PLAIN.
     */
    public static void removeDuplicates(List<Snippet> snippets, int method) {
        // TODO: filter (near) duplicates
        switch (method) {
            case PLAIN:
                removeDuplicatesPlain(snippets);
                // case SHINGLES:
                // removeDuplicatesShingles(snippets);
        }
    }

    /**
     * This method removes the exact duplicates from a list of snippets, which are either within the same list or in the database.
     * 
     * @param snippets - The List of snippets
     */
    private static void removeDuplicatesPlain(List<Snippet> snippets) {

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
                // snippets.remove(snippet);
                dblist.add(snippet);
            }
        }
        snippets.retainAll(dblist);
    }
}
