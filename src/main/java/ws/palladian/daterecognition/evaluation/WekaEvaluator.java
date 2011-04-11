package ws.palladian.daterecognition.evaluation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;

import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.helper.date.DateComparator;

public class WekaEvaluator {
	private String db;
	private void setDB(String db){
		this.db = db;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WekaEvaluator we = new WekaEvaluator();
		PageDateType pageDateType = PageDateType.publish;
		//PageDateType pageDateType = PageDateType.last_modified;
		
		String factorTable = "contentfactor3";
		String table = "regression"; 
		
		for(int i= 1; i< 550; i++){
			table = "wekaout" + 1;
			we.setDB("wekaout");
			try{
				we.evaluationOut(pageDateType, table, factorTable);
			}catch (SQLException e) {
				// TODO: handle exception
			}
		}
		
	}

	
	private void evaluationOut(PageDateType pageDateType, String table, String factorTable) throws SQLException{
		
		HashMap<Integer, Double> wekaMap = importWeka(pageDateType, table);
		HashMap<Integer, String> urlMap = importUrl(wekaMap, factorTable);
		HashMap<String, Integer> bestUrlId = findBestValue(wekaMap, urlMap, factorTable, pageDateType);
		double limit;
		System.out.println(" - ARD - AWD - ANF - AFR - AFW");
		for(int i=0; i<=100; i++){
			limit = ((double)i) / 100.0;
			HashMap<String, Integer> importIdDate = importIdDate(bestUrlId, wekaMap, pageDateType, factorTable, limit);
			
			int ard = 0;
			int awd = 0;
			int anf = 0;
			int afr = 0;
			int afw = 0;
			for(Entry<String, Integer> entry : importIdDate.entrySet()){
				switch(entry.getValue()){
				case DataSetHandler.ARD : 
					ard++;
					break;
				case DataSetHandler.AWD :
					awd++;
					break;
				case DataSetHandler.ANF :
					anf++;
					break;
				case DataSetHandler.AFR :
					afr++;
					break;
				case DataSetHandler.AFW :
					afw++;
					break;
				}
			}
			int accuracy = (int) Math.round(((double)(ard + afr) / (double)(ard + awd + anf + afr +afw)) * 1000.0);
			int dedection = (int) Math.round(((double)ard / (double)(ard + awd)) * 1000.0);
			
			System.out.print(limit + " - ");
			System.out.print(ard + " - ");
			System.out.print(awd + " - ");
			System.out.print(anf + " - ");
			System.out.print(afr + " - ");
			System.out.println(afw);
			//System.out.print(accuracy + " - ");
			//System.out.println(dedection);
		}

	}
	
	private HashMap<Integer, Double> importWeka(PageDateType pageDateType, String table) throws SQLException{
		HashMap<Integer, Double> wekaMap = new HashMap<Integer, Double>();
		String yesType = pageDateType.equals(PageDateType.publish) ? "yesPub" : "yesMod";
		DataSetHandler.setDB(this.db);
		DataSetHandler.openConnection();
		String sqlQuery ="SELECT * FROM " + table;
		
		
			ResultSet rs = DataSetHandler.st.executeQuery(sqlQuery);
			while( rs.next() ) {
				int id = rs.getInt("id");
				double yesValue = rs.getDouble(yesType);
				wekaMap.put(id, yesValue);
	        
		
			}
		DataSetHandler.closeConnection();
		return wekaMap;
	}
	
	
	private HashMap<Integer, String> importUrl(HashMap<Integer, Double> wekaMap, String factorTable){
		HashMap<Integer, String> urlMap = new HashMap<Integer, String>();
		
		DataSetHandler.openConnection();
		
		
		try{
			for(Entry<Integer, Double> entry : wekaMap.entrySet()){
				String sqlQuery = "SELECT * FROM " + factorTable + " WHERE id=" + entry.getKey();
				//System.out.println(sqlQuery);
				ResultSet rs = DataSetHandler.st.executeQuery(sqlQuery);
				rs.first();
				String url = rs.getString("url");
				urlMap.put(entry.getKey(), url);
			}
		}catch (SQLException e) {
			e.printStackTrace();
		}
		
		DataSetHandler.closeConnection();
		return urlMap;
	}
	
	
	private HashMap<String, Integer> findBestValue(HashMap<Integer, Double> wekaMap, HashMap<Integer, String> urlMap, String factorTable, PageDateType pageDateType){
		HashMap<String, Integer> bestUrlId = new HashMap<String, Integer>();
		
		for(Entry<Integer, String> entry : urlMap.entrySet()){
			String url = entry.getValue();
			int newId = entry.getKey();
			double newValue = wekaMap.get(newId);
			Integer oldId = bestUrlId.get(url);
			if(oldId == null || wekaMap.get(oldId) <= newValue){
				if(oldId != null && wekaMap.get(oldId) == newValue){
					DataSetHandler.openConnection();
					String sqlQueryOld = "SELECT * FROM " + factorTable + " WHERE id=" + oldId;
					String sqlQueryNew = "SELECT * FROM " + factorTable + " WHERE id=" + oldId;
					ResultSet rs;
					try {
						rs = DataSetHandler.st.executeQuery(sqlQueryOld);
						rs.first();
						String oldDate = rs.getString("date");
						
						rs = DataSetHandler.st.executeQuery(sqlQueryNew);
						rs.first();
						String newDate = rs.getString("date");
						DateComparator dc = new DateComparator();
						
						int comp = dc.compare(DateGetterHelper.findDate(oldDate), DateGetterHelper.findDate(newDate), DateComparator.STOP_DAY);
						if(comp > 0 && pageDateType.equals(PageDateType.publish)){
							bestUrlId.put(url, newId);
							System.out.println(oldDate + " - " + newDate);
						} else if(comp == -1 && pageDateType.equals(PageDateType.last_modified)){
							bestUrlId.put(url, newId);
							System.out.println(oldDate + " - " + newDate);
						}
						
						
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					DataSetHandler.closeConnection();
				}else{
					bestUrlId.put(url, newId);
				}
			}
		}
		return bestUrlId;
	}
	
	private HashMap<String, Integer> importIdDate(HashMap<String, Integer> bestUrlId, HashMap<Integer, Double> wekaMap, PageDateType pageDateType, String factorTable, double limit){
		HashMap<String, Integer> urlIdDate = new HashMap<String, Integer>();
		DataSetHandler.openConnection();
		String dateType = pageDateType.equals(PageDateType.publish) ? "pubDate" : "modDate";
		DateComparator dc = new DateComparator();
		try{
			for(Entry<String, Integer> entry : bestUrlId.entrySet()){
				String sqlQuery = "SELECT * FROM " + factorTable + " WHERE id=" + entry.getValue();
				ResultSet rs = DataSetHandler.st.executeQuery(sqlQuery);
				rs.first();
				String pageDateString = rs.getString(dateType);
				String wekaDateString = rs.getString("date");

				ExtractedDate wekaDate = null;
				if(wekaMap.get(entry.getValue()) >= limit){
					wekaDate = DateGetterHelper.findDate(wekaDateString);
				}
				ExtractedDate pageDate = DateGetterHelper.findDate(pageDateString);
				
				Integer result;
				if(pageDate == null){
					if(wekaDate == null){
						result = DataSetHandler.ARD;
					}else{
						result = DataSetHandler.AWD;
					}
				}else{
					if(wekaDate == null){
						result = DataSetHandler.ANF;
					}else{
						//System.out.println(pageDate.getNormalizedDateString() + " - " + wekaDate.getNormalizedDateString());
						int compare = dc.compare(pageDate, wekaDate, dc.getCompareDepth(pageDate, wekaDate));
						if(compare == 0){
							result = DataSetHandler.AFR;
						}else{
							result = DataSetHandler.AFW;
						}
					}
				}
				urlIdDate.put(entry.getKey(), result);
				
			}
		}catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		DataSetHandler.closeConnection();
		return urlIdDate;
	}
}
