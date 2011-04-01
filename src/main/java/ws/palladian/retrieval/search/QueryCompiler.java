package ws.palladian.retrieval.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.LineAction;
import edu.umass.cs.mallet.base.util.Random;

public class QueryCompiler {

    public static void main(String[] args) {

        String dmozCatergoriesFile = "/home/pk/Desktop/categories.txt";

        List<String> queries = readQueriesFromDmoz(dmozCatergoriesFile);
        List<String> combinedQueries = combineQueries(queries, 100000);
        FileHelper.writeToFile("/home/pk/Desktop/finalQueries.txt", combinedQueries);

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

    public static List<String> combineQueries(List<String> queries, int targetCount) {

        int availableQueries = queries.size();

        Set<String> combinedQueries = new HashSet<String>();
        Random random = new Random();

        if (availableQueries > targetCount) {

            // we have more queries than we want, create a random subset of specified size
            Collections.shuffle(queries);
            combinedQueries.addAll(queries.subList(0, targetCount));

        } else {

            // add all supplied queries to result
            combinedQueries.addAll(queries);

            // create combined queries
            // for (int i = 0; i < targetCount - availableQueries; i++) {
            while (combinedQueries.size() < targetCount) {

                // get a query term randomly by combining two single queries
                String queryTerm1 = queries.get(random.nextInt(queries.size()));
                String queryTerm2 = queries.get(random.nextInt(queries.size()));
                combinedQueries.add(queryTerm1 + " " + queryTerm2);
            }
            
        }

        // return random shuffled result
        ArrayList<String> result = new ArrayList<String>(combinedQueries);
        Collections.shuffle(result);
        return result;

    }

}
