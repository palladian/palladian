package tud.iir.daterecognition.searchengine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.daterecognition.DateGetter;
import tud.iir.daterecognition.dates.ExtractedDate;

public class HakiaDataSet {
	
	
	
	private static HashMap<String, String> getHakiaDates(ArrayList<String> urlMap){
		HashMap<String, String> returnMap = new HashMap<String, String>();
		
		HakiaDateGetter hdg = new HakiaDateGetter();
		
		String url;
		for(int i=0; i<urlMap.size(); i++){
			url=urlMap.get(i);
			ExtractedDate date = hdg.getHakiaDate(url);
			if(date != null){
				returnMap.put(url, date.getNormalizedDate(false));
			}else{
				returnMap.put(url, "");
			}
				
			System.out.println(returnMap.get(url) + " " + url);
		}
		
		return returnMap;
		
	}
	
	public static void writeHakiaDates(){
		ArrayList<String> urls = DataSetHandler.loadURLsFromDB("urlset");
		HashMap<String, String> urlMap = getHakiaDates(urls);
		String sqlQuery;
		DataSetHandler.openConnection();
		
		try {
			System.out.println(urlMap.size());
			for(Entry<String, String> e : urlMap.entrySet()){
				sqlQuery ="INSERT INTO hakiadates (url, date) VALUES ('"+ e.getKey().replaceAll("'", "''") + "', '" + e.getValue().replaceAll("'", "''") + "') ON DUPLICATE KEY UPDATE date='" + e.getValue().replaceAll("'", "''") + "'";
				DataSetHandler.st.executeUpdate(sqlQuery);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataSetHandler.closeConnection();
	}

}
