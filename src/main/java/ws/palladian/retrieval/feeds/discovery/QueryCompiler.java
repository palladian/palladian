package ws.palladian.retrieval.feeds.discovery;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.LineAction;

public class QueryCompiler {

    public static void main(String[] args) {
        String dmozCatergoriesFile = "/Users/pk/Desktop/categories.txt";
        List<String> queries = readQueriesFromDmoz(dmozCatergoriesFile);
        FileHelper.writeToFile("/Users/pk/Desktop/finalQueries.txt", queries);
    }

    /**
     * Load query terms from DMOZ dataset.
     * http://rdf.dmoz.org/rdf/categories.txt
     * 
     * @param dmozCatergoriesFile
     * @return
     */
    private static List<String> readQueriesFromDmoz(String dmozCatergoriesFile) {

        final Bag<String> items = new HashBag<String>();

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                line = line.replace("_", " ");

                // ignore items with "World" as a root
                if (line.startsWith("World")) {
                    return;
                }

                String[] split = line.split("/");
                for (String item : split) {

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
            if (numOccur > 2) {
                result.add(item);
            }
        }

        // System.out.println("# items " + result.size());
        return result;

    }


}
