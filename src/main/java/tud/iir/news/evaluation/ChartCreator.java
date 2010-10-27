package tud.iir.news.evaluation;

import java.util.List;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;

public class ChartCreator {

    private final String feedSizeHistogrammFilePath = "data/evaluation/feedSizeHistogrammData.csv";
    private final String feedAgeFilePath = "data/evaluation/feedAgeData.csv";
    
    private void createFeedSizeHistogrammFile(){
        EvaluationDatabase ed = EvaluationDatabase.getInstance();
        List<EvaluationFeedPoll> polls = ed.getFeedSizes();
        int[] feedSizeDistribution = new int[21];
        
        for (EvaluationFeedPoll poll : polls) {
            int i =  new Double(Math.floor(poll.getSizeOfPoll()/5/1024)).intValue() ;
            i = (i > 20)? 20 : i;
            feedSizeDistribution[i]++;             
        }        
        
        StringBuilder feedSizeDistributionSB = new StringBuilder();
        feedSizeDistributionSB.append("Feed size in KB;all feeds;\n");
        int size = 0;
        String size2 = "";
        for (int number : feedSizeDistribution) {
            size +=5;
            size2 = (size > 100)? "more" : String.valueOf(size);
            feedSizeDistributionSB.append(size2).append(";").append(number).append(";\n");                        
        }
        
        FileHelper.writeToFile(feedSizeHistogrammFilePath, feedSizeDistributionSB);
    }
    
    
    private void createFeedAgeFile(){
        
    }
    
    
    
    
    
    
    /**
     * simply testing the connection and EvaluationFeedPoll class.
     * */
    private void firstTest(){
        EvaluationDatabase ed = EvaluationDatabase.getInstance();
        List<EvaluationFeedPoll> polls = ed.getFeedPolls();
        for (EvaluationFeedPoll poll : polls) {
            System.out.println(poll);
        }
    }
        
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    
	    ChartCreator cc = new ChartCreator();
//	    cc.firstTest();
	    cc.createFeedSizeHistogrammFile();
		
		
//		FeedDatabase fd = FeedDatabase.getInstance();             
//      List<Feed> feeds = fd.getFeeds();
//      for (Feed feed : feeds) {           
//          System.out.println(feed.getFeedUrl());          
//      }

	}

}
