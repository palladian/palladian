package tud.iir.news.evaluation;

import java.util.List;

import tud.iir.news.Feed;
import tud.iir.news.FeedDatabase;

public class ChartCreator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FeedDatabase fd = FeedDatabase.getInstance();
		EvaluationDatabase ed = EvaluationDatabase.getInstance();
		
		
//		List<Feed> feeds = fd.getFeeds();
//		for (Feed feed : feeds) {			
//			System.out.println(feed.getFeedUrl());			
//		}

		
		List<EvaluationDataObject> polls = ed.getFeedPolls();
		for (EvaluationDataObject poll : polls) {
		    System.out.println(poll);
		}
	}

}
