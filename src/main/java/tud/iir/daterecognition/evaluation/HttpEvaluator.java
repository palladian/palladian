package tud.iir.daterecognition.evaluation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.ExtractedDateHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HTTPDate;
import tud.iir.daterecognition.searchengine.DataSetHandler;
import tud.iir.daterecognition.technique.HTTPDateGetter;
import tud.iir.helper.DateComparator;
import tud.iir.helper.DateHelper;

public class HttpEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		evaluateLastModified();

	}
	
	private static void evaluateLastModified(){
		HashMap<String, String[]> evalMap = new HashMap<String, String[]>();
		
		ArrayList<String> urls = DataSetHandler.loadURLsFromDB("allurls");
		HTTPDateGetter dg = new HTTPDateGetter();
		Iterator<String> it = urls.iterator();
		int index=0;
		while(it.hasNext()){
			String url;
			ExtractedDate[] dates = new ExtractedDate[3];
			url = it.next();
			dg.setUrl(url);
			
			Iterator<HTTPDate> dateIterator = dg.getDates().iterator();
			while(dateIterator.hasNext()){
				HTTPDate date = dateIterator.next();
				if(date.getKeyword().equalsIgnoreCase("last-modified")){
					dates[0]=date;
					
				}else if(date.getKeyword().equalsIgnoreCase("date")){
					dates[1]=date;
					
				}
			}
			
			ExtractedDate currentDate = ExtractedDateHelper.createActualDate();
			dates[2] = currentDate;
			DateComparator dc = new DateComparator();
			ExtractedDate moddate = dates[0];
			if(dates[0] == null){
				moddate = dates[1];
			}
			String[] dbEntry = new String[4];
			dbEntry[0] = url;
			if(moddate != null){
				dbEntry[1] = moddate.getNormalizedDate(true);
				dbEntry[2] = currentDate.getNormalizedDate(true);
				dbEntry[3] = String.valueOf(Math.round(dc.getDifference(moddate, currentDate, DateComparator.MEASURE_MIN)));
			}else{
				dbEntry[1] = "";
				dbEntry[2] = "";
				dbEntry[3] = "";
			}
			System.out.println(url);
			System.out.println(dbEntry[1] + " " + dbEntry[2] + "  -> " + dbEntry[3]);
			System.out.println(index++);
			evalMap.put(url, dbEntry);
		}
		
		DataSetHandler.openConnection();
		
		String sqlQuery;
		try {
			for(Entry<String, String[]>e: evalMap.entrySet()){
				String values = "'" + e.getValue()[0].replaceAll("'", "''") + "','" + e.getValue()[1] + "','" + e.getValue()[2] + "','" + e.getValue()[3] + "'";
				String update = "lastmod_date='" + e.getValue()[1] + "', call_date='" + e.getValue()[2] + "', difference='" + e.getValue()[3] + "'";
				sqlQuery ="INSERT INTO httpevaluation (url, lastmod_date, call_date, difference) VALUES (" + values + ") ON DUPLICATE KEY UPDATE " + update;
				DataSetHandler.st.executeUpdate(sqlQuery);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		DataSetHandler.closeConnection();
		
	}

}
