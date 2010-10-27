package tud.iir.news.evaluation;

import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.classification.page.ClassifierManager;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;

public class ChartCreator {

    private static final Logger LOGGER = Logger.getLogger(ChartCreator.class);
    
    private final String feedSizeHistogrammFilePath = "data/evaluation/feedSizeHistogrammData.csv";
    private final String feedAgeFilePath = "data/evaluation/feedAgeData.csv";
    
    private void createFeedSizeHistogrammFile(){
        EvaluationDatabase ed = EvaluationDatabase.getInstance();
        List<EvaluationFeedPoll> polls = ed.getFeedSizes();
        int[] feedSizeDistribution = new int[21];
        
        for (EvaluationFeedPoll poll : polls) {
//            int feedID = poll.getFeedID();
//            float pollSize = poll.getSizeOfPoll();
            int i =  new Double(Math.floor(poll.getSizeOfPoll()/10/1024)).intValue() ;
            i = (i > 20)? 20 : i;
            feedSizeDistribution[i]++;             
        }        
        
        StringBuilder feedSizeDistributionSB = new StringBuilder();
        feedSizeDistributionSB.append("Feed size in KB;all feeds;\n");
        int size = 0;
        String size2 = "";
        for (int number : feedSizeDistribution) {
            size +=10;
            size2 = (size > 200)? "more" : String.valueOf(size);
            feedSizeDistributionSB.append(size2).append(";").append(number).append(";\n");                        
        }
        
        FileHelper.writeToFile(feedSizeHistogrammFilePath, feedSizeDistributionSB);
        LOGGER.info("feedSizeHistogrammFile *hopefully* :) written to: " + feedSizeHistogrammFilePath);
    }
    
    
    private void createFeedAgeFile(){
        EvaluationDatabase ed = EvaluationDatabase.getInstance();
        List<EvaluationItemIntervalItem> polls = ed.getAverageUpdateIntervals();
        int[] feedAgeDistribution = new int[34];
        
        for (EvaluationItemIntervalItem intervalItem : polls) {
            int averageUpdateIntervalHours = new Double(Math.floor(intervalItem.getAverageUpdateInterval()/3600000)).intValue();
            int i = -1;
            if(averageUpdateIntervalHours <= 24) i = averageUpdateIntervalHours;
            else if(averageUpdateIntervalHours <= 24*2) i = 24;  //2 days
            else if(averageUpdateIntervalHours <= 24*3) i = 25;  //3 days
            else if(averageUpdateIntervalHours <= 24*4) i = 26;  //4 days
            else if(averageUpdateIntervalHours <= 24*5) i = 27; //5 days
            else if(averageUpdateIntervalHours <= 24*6) i = 28; //6 days
            else if(averageUpdateIntervalHours <= 24*7) i = 29; //7 days
            else if(averageUpdateIntervalHours <= 24*7*2) i = 30; //2 weeks
            else if(averageUpdateIntervalHours <= 24*7*3) i = 31; //3 weeks
            else if(averageUpdateIntervalHours <= 24*7*4) i = 32; //4 weeks
            else i = 33; //more
            feedAgeDistribution[i]++;             
        }        

        StringBuilder feedSizeDistributionSB = new StringBuilder();
        feedSizeDistributionSB.append("Feed age;all feeds;\n");
        int i = 0;
        String[] caption = {"1 hour","2 hours","3 hours","4 hours","5 hours","6 hours","7 hours","8 hours","9 hours","10 hours","11 hours","12 hours","13 hours","14 hours","15 hours","16 hours","17 hours","18 hours","19 hours","20 hours","21 hours","22 hours","23 hours","24 hours","2 days","3 days","4 days","5 days","6 days","7 days","2 weeks","3 weeks","4 weeks","more"};
        
        for (int number : feedAgeDistribution) {
            feedSizeDistributionSB.append(caption[i]).append(";").append(number).append(";\n");
            i++;
        }
        
        FileHelper.writeToFile(feedAgeFilePath, feedSizeDistributionSB);
        LOGGER.info("feedAgeFile *hopefully* :) written to: " + feedAgeFilePath);       
    }
    
    
    private void createTimeliness2File(){
        
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

//	    cc.createFeedSizeHistogrammFile();
	    cc.createFeedAgeFile();
		
		
//		FeedDatabase fd = FeedDatabase.getInstance();             
//      List<Feed> feeds = fd.getFeeds();
//      for (Feed feed : feeds) {           
//          System.out.println(feed.getFeedUrl());          
//      }

	}

}
