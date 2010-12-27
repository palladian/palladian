package tud.iir.daterecognition.searchengine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.h2.result.ResultExternal;

import opennlp.tools.util.HashList;
import tud.iir.daterecognition.dates.ExtractedDate;

public class GoogleDataSet {

	
	
	private static HashMap<String, String> getGoogleDates(ArrayList<String> urls){
		HashMap<String, String> returnMap = new HashMap<String, String>();
		
		GoogleDateGetter gd = new GoogleDateGetter();
		
		String url;
		for(int i=0; i<urls.size(); i++){
			url=urls.get(i);
			ExtractedDate date = gd.getGoogleDate(url);
			if(date != null){
				returnMap.put(url, date.getNormalizedDate(false));
			}else{
				returnMap.put(url, "");
			}
				
			System.out.println(returnMap.get(url) + " " + url);
		}
		
		return returnMap;
		
	}
	
	public static void writeGoogleDates(){
		ArrayList<String> urls = DataSetHandler.loadURLsFromDB("urlset");
		HashMap<String, String> urlMap = getGoogleDates(urls);
		String sqlQuery;
		
		DataSetHandler.openConnection();
		try {
			System.out.println(urlMap.size());
			for(Entry<String, String> e : urlMap.entrySet()){
				sqlQuery ="INSERT INTO googledates (url, date) VALUES ('"+ e.getKey().replaceAll("'", "''") + "', '" + e.getValue().replaceAll("'", "''") + "') ON DUPLICATE KEY UPDATE date='" + e.getValue().replaceAll("'", "''") + "'";
				DataSetHandler.st.executeUpdate(sqlQuery);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataSetHandler.closeConnection();
	}
}
