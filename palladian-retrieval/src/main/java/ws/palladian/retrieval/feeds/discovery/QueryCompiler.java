package ws.palladian.retrieval.feeds.discovery;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

public class QueryCompiler {

    public static void main(String[] args) {
        String dmozCatergoriesFile = "/home/pk/Desktop/categories.txt";
        List<String> queries = readQueriesFromDmoz(dmozCatergoriesFile, -1, 2);
        System.out.println(queries);
        FileHelper.writeToFile("/home/pk/Desktop/newsseecrQueries_2011-08-04.txt", queries);
    }

    /**
     * Load query terms from DMOZ dataset.
     * http://rdf.dmoz.org/rdf/categories.txt
     * 
     * @param dmozCatergoriesFile
     * @return
     */
    public static List<String> readQueriesFromDmoz(String dmozCatergoriesFile) {
        return readQueriesFromDmoz(dmozCatergoriesFile, -1, Integer.MAX_VALUE);
    }

    /**
     * Load query terms from DMOZ dataset.
     * http://rdf.dmoz.org/rdf/categories.txt
     * 
     * @param dmozCatergoriesFile
     * @param minOccurence minimum occurence count of a query term in the list to be included to the result
     * @param maxDepth maximum depth in the category tree to consider
     * @return
     */
    public static List<String> readQueriesFromDmoz(String dmozCatergoriesFile, int minOccurence, final int maxDepth) {

        final Bag<String> items = new HashBag<String>();

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                line = line.replace("_", " ");

                // for the second iteration, we do not ignore "World" any longer
                
                // ignore items with "World" as a root
                // if (line.startsWith("World")) {
                // return;
                // }

                String[] split = line.split("/");
                for (int i = 0; i < Math.min(maxDepth, split.length); i++) {

                    String item = split[i];
                    
                    // ignore items of length 1
                    if (item.length() > 1) {
                        items.add(item);
                    }
                }

                if (lineNumber % 10000 == 0) {
                    System.out.println(lineNumber);
                }
            }
        };

        FileHelper.performActionOnEveryLine(dmozCatergoriesFile, la);

        List<String> result = new ArrayList<String>();

        for (String item : items.uniqueSet()) {
            // remove those, which only occur once
            int numOccur = items.getCount(item);
            if (numOccur > minOccurence) {
                result.add(item);
            }
        }

        System.out.println("# items " + result.size());
        return result;

    }

}
