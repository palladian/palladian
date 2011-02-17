package ws.palladian.daterecognition.searchengine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import ws.palladian.daterecognition.dates.ExtractedDate;

public class AskDataSet {

	private static HashMap<String, String> getAskDates(ArrayList<String> urls){
		HashMap<String, String> returnMap = new HashMap<String, String>();
		
		AskDateGetter adg = new AskDateGetter();
		
		String url;
		for(int i=0; i<urls.size(); i++){
			url=urls.get(i);
			ExtractedDate date = adg.getAskDate(url);
			if(date != null){
				returnMap.put(url, date.getNormalizedDate(false));
			}else{
				returnMap.put(url, "");
			}
				
			System.out.println(returnMap.get(url) + " " + url);
		}
		
		return returnMap;
		
	}
	
	public static void writeAskDates(){
		ArrayList<String> urls = DataSetHandler.loadURLsFromDB("urlset");
		HashMap<String, String> urlMap = getAskDates(urls);
		String sqlQuery;
		
		DataSetHandler.openConnection();
		try {
			System.out.println(urlMap.size());
			for(Entry<String, String> e : urlMap.entrySet()){
				
				sqlQuery ="INSERT INTO askdates (url, date) VALUES ('"+ e.getKey().replaceAll("'", "''") + "', '" + e.getValue().replaceAll("'", "''") + "') ON DUPLICATE KEY UPDATE date='" + e.getValue().replaceAll("'", "''") + "'";
				System.out.println(sqlQuery);
				DataSetHandler.st.executeUpdate(sqlQuery);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataSetHandler.closeConnection();
	}
}
