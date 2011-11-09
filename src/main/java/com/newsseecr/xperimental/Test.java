//package com.newsseecr.xperimental;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.commons.math.stat.descriptive.rank.Median;
//import org.tartarus.snowball.SnowballStemmer;
//import org.tartarus.snowball.ext.englishStemmer;
//
//import ws.palladian.classification.page.Stopwords;
//import ws.palladian.helper.StopWatch;
//import ws.palladian.persistence.DatabaseManagerFactory;
//import ws.palladian.retrieval.feeds.Feed;
//import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
//
//import com.representqueens.lingua.en.Fathom;
//import com.representqueens.lingua.en.Fathom.Stats;
//import com.representqueens.lingua.en.Readability;
//import com.sun.syndication.io.SyndFeedInput;
//
//public class Test {
//
//    public static void main(String[] args) {
//
//        SyndFeedInput feedInput = new SyndFeedInput();
//
//        // this preserves the "raw" feed data and gives direct access to RSS/Atom specific elements see
//        // http://wiki.java.net/bin/view/Javawsxml/PreservingWireFeeds
//        // feedInput.setPreserveWireFeed(true);
//        // System.exit(0);
//
//        String text2 = "This is some text (with parenthesis). Let's see, if Fathom will determine the number of sentences correctly!!";
//        Stats stats2 = Fathom.analyze(text2);
//        System.out.println(stats2.getNumSentences());
//        System.out.println(stats2.getNumWords());
//
//        System.exit(0);
//
//        Map<String, List<Integer>> x = new HashMap<String, List<Integer>>();
//        x.put("x", Arrays.asList(1 ,2, 3));
//        System.out.println(x);
//        System.exit(0);
//
//        StopWatch sw = new StopWatch();
//        for (int i = 0; i < 1000000; i++) {
//            // Throwable t = new Throwable();
//            // System.out.println(t.getStackTrace()[0]);
//        }
//        System.out.println(sw.getElapsedTimeString());
//        // with stack trace : 1s:90ms
//        // without stack trace : 12 ms
//
//        System.exit(0);
//
//        Stopwords stopwords2 = new Stopwords();
//        System.out.println(stopwords2);
//
//        System.exit(0);
//
//        stem("hack");
//        stem("hacks");
//        stem("hacking");
//        // stem("programs");
//        System.exit(0);
//
//        String text = "the \n quick \n brown \n\n fox.";
//        //String text = "";
//        if (text.isEmpty()) {
//            text = " ";
//        }
//
//        int newLinesCount = text.length() - text.replaceAll("\n", "").length();
//        int blankLinesCount = (text.length() - text.replaceAll("\n\n", "").length()) / 2;
//        float newLinesPercent = 100 / (float) text.length() * newLinesCount;
//
//        System.out.println("nl:"+newLinesCount);
//        System.out.println("bl:"+blankLinesCount);
//        System.out.println("nl%:"+newLinesPercent);
//
//        System.exit(0);
//
//        Median median = new Median();
//        double result = median.evaluate(new double[]{1,1,1,1,1,2,1000});
//        System.out.println(result);
//        System.exit(0);
//
//        //String text = "the quick brown fox \njumps over the lazy dog a nice dog... Yes, it is.";
//        // String text =
//        // "Just for comparison, I went and priced out a MB E550, it starts out at the same price as the Model S (57k) and with the majority of options surpasses more than 20k in addition to the base price (4matic, panaroma sunroof, Navigation, 18 inch wheels, keyless ignition, review camera etc).";
//
//        Stats stats = Fathom.analyze(text);
//        System.out.println(stats.getNumSentences());
//
//        System.out.println(stats.getNumTextLines());
//
//        System.out.println(Readability.wordsPerSentence(stats));
//
//        System.exit(0);
//
//        double[] test = new double[4];
//        test[0]++;
//        System.out.println(test[0]);
//
//        System.exit(0);
//
//        String[] tagsArray = new String[7];
//
//        int numReRanking = tagsArray.length * (tagsArray.length - 1) / 2;
//
//        System.out.println(numReRanking);
//        System.exit(0);
//
//        FeedDatabase fd = DatabaseManagerFactory.create(FeedDatabase.class);
//        Feed feed = fd.getFeedByID(1);
//        System.out.println(feed);
//
//        Stopwords stopwords = new Stopwords(Stopwords.Predefined.EN);
//        System.out.println(stopwords);
//
//    }
//
//    private static void stem(String string) {
//        SnowballStemmer stemmer = new englishStemmer();
//        stemmer.setCurrent(string);
//        stemmer.stem();
//        System.out.println(stemmer.getCurrent());
//    }
//
//}
