package tud.iir.helper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.news.Feed;
import tud.iir.news.FeedDatabase;
import tud.iir.news.evaluation.FeedReaderEvaluator;

/**
 * Dump class to test various algorithms.
 * 
 * @author David Urbansky
 * 
 */
public class Temp {


    public static void createTrainingData() {

        List<Feed> feeds = FeedDatabase.getInstance().getFeeds();

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(new File("data/temp/learningData.txt"));

            int mod = 100 / 10;

            int feedCounter = 0;
            for (Feed feed : feeds) {

                feedCounter++;

                // skip some feeds if we want to take a sample only
                if ((feedCounter + 1) % mod != 0) {
                    continue;
                }

                String safeFeedName = feed.getId()
                + "_"
                + StringHelper.makeSafeName(
                        feed.getFeedUrl().replaceFirst("http://www.", "").replaceFirst("www.", ""), 30);

                String fileName = FeedReaderEvaluator.findHistoryFile(safeFeedName);

                List<String> items = FileHelper.readFileToArray(fileName);

                if (items.size() < 6) {
                    continue;
                }

                List<Long> intervals = new ArrayList<Long>();

                // make list of intervals
                for (int i = items.size() - 2; i >= 0; i--) {

                    String[] item1 = items.get(i).split(";");
                    String[] item2 = items.get(i + 1).split(";");

                    long interval = (Long.valueOf(item1[0]) - Long.valueOf(item2[0])) / DateHelper.MINUTE_MS;

                    intervals.add(interval);
                }


                // move a 6 item long window through the file, 5 variables + 1 outcome
                for (int i = 0; i < intervals.size() - 6; i++) {

                    fileWriter.write(String.valueOf(intervals.get(i)));
                    fileWriter.write(";");
                    fileWriter.write(String.valueOf(intervals.get(i + 1)));
                    fileWriter.write(";");
                    fileWriter.write(String.valueOf(intervals.get(i + 2)));
                    fileWriter.write(";");
                    fileWriter.write(String.valueOf(intervals.get(i + 3)));
                    fileWriter.write(";");
                    fileWriter.write(String.valueOf(intervals.get(i + 4)));
                    fileWriter.write(";");
                    fileWriter.write(String.valueOf(intervals.get(i + 5)));
                    fileWriter.write(";\n");
                    fileWriter.flush();
                }

            }

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }

    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // Temp.createTrainingData();

        // deserialize model
        // Classifier cls = (Classifier) weka.core.SerializationHelper.read("data/temp/weka.model");
        //
        // Instance i = new Instance(5);
        // i.setValue(0, 2712);
        // i.setValue(1, 1757);
        // i.setValue(2, 4099);
        // i.setValue(3, 2913);
        // i.setValue(4, 19941);
        // double d = cls.classifyInstance(i);
        // System.out.println(d);
    }

}
