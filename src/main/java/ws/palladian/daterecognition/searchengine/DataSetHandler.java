package ws.palladian.daterecognition.searchengine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hp.hpl.jena.ontology.OntDocumentManager.ReadFailureHandler;

import ws.palladian.daterecognition.ExtractedDateHelper;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.HTTPDate;
import ws.palladian.daterecognition.evaluation.EvaluationHelper;
import ws.palladian.web.Crawler;


public class DataSetHandler{
	
	private static final String DRIVER = "jdbc:mysql://localhost/";
	private static final String DB = "dateset";
	
	public static Connection cn = null;
	public static Statement st = null;
	public static ResultSet rs = null;
	
	private static String HREF="<a href=\"";
	private static String ENDHREF = ">";
	
	private static final int SEARCH_GOOGLE = 1;
	private static final int SEARCH_HAKIA = 2;
	private static final int SEARCH_ASK = 3;
	
	/**
	 * Extracted Date (Datum der Webseite): ED
	 * Gefundenes Datum: FD
	 * 
	 */
	/**
	 * -2 <br>
	 * Wrong Found. <br>
	 * ED = 1 & FD = 1 & ED != Fd 
	 */
	public static final int WF = -2;
	/**
	 * -1 <br>
	 * Wrong not found. <br>
	 * ED = 1 & FD = 0
	 */
	public static final int WNF = -1;
	/**
	 * 0 <br>
	 * False Found. <br>
	 * ED = 0 & FD = 1
	 */
	public static final int FF = 0;
	/**
	 * 1 <br>
	 * Right not Found. <br>
	 * ED = 0 & FD = 0
	 */
	public static final int RNF = 1;
	/**
	 * 2 <br>
	 * Right Found. <br>
	 * ED = 1 & FD = 1 & ED == FD
	 */
	public static final int RF = 2;
	
	public static final String separator = " *;_;* "; 
	
	public static void main(String[] args){
		//String path = "D:/_Uni/_semester16/dataset/";	
		//String name = "urlSet06.htm";
		//saveAllUrls(path, name);
		
		//writeSearchEngineDates(SEARCH_ASK);
		
		//AskDateGetter adg = new AskDateGetter();
		//adg.getAskDate("http://bettingchoice.co.uk/2010-world-cup-group-a-predictions-and-betting-tips_20033");
		
		//HakiaDateGetter hdg = new HakiaDateGetter();
		//hdg.getHakiaDate("http://www.upi.com/Science_News/Resource-Wars/2010/05/26/Gulf-Keystone-to-raise-funds-for-Iraq-work/UPI-50891274879566/");
		
		//downloadUrls("data/webpages/daterecognition/");
		
		
		//String path = "data/evaluation/dateextraction/dataset.txt";
		//createSearchDatesAndDownload("urlset", path);
		//setDownloadTo(path, "urlset", 1);
		
		/*
		String path = "D:/_Uni/_semester16/dataset/";	
		String in = "urlSet03.htm";
		String out = "emptyDateUrlSet03.htm";
		createUrlSetWithoutDate(path + in, path + out);
		*/
		
		//addCloumn(EvaluationHelper.HEADEVAL, "pub1", "int", "-10");
		//addCloumn(EvaluationHelper.HEADEVAL, "mod1", "int", "-10");
		
	}
	
	/**
	 * Takes URL out of "urlset", tries to download it and, if downloaded search for google, hakia and ask date. <br>
	 * Writes results in to file.
	 * @param path
	 */
	private static void createSearchDatesAndDownload(String table, String path){
		ArrayList<DBExport> set = loadURLsFromUrlset(0, table);
		GoogleDateGetter gdg = new GoogleDateGetter();
		HakiaDateGetter hdg = new HakiaDateGetter();
		AskDateGetter adg = new AskDateGetter();
		
		
		for(int i=0; i<set.size(); i++){
			DBExport dbExport = set.get(i);
			String url = dbExport.getUrl();
			ExtractedDate tempDate;
			
			System.out.println(url);
			System.out.println( dbExport.getPubDate() + " - " + dbExport.getModDate());
			
			System.out.print("google: ");
			tempDate = gdg.getGoogleDate(url);
			if(tempDate != null){
				dbExport.setGoogleDate(tempDate.getNormalizedDate(true));
			}
			System.out.println(dbExport.getGoogleDate());
			
			System.out.print("hakia: ");
			tempDate = hdg.getHakiaDate(url);
			if(tempDate != null){
				dbExport.setHakiaDate(tempDate.getNormalizedDate(true));
			}
			System.out.println(dbExport.getHakiaDate());
			
			System.out.print("ask: ");
			tempDate = adg.getAskDate(url);
			if(tempDate != null){
				dbExport.setAskDate(tempDate.getNormalizedDate(true));
			}
			System.out.println(dbExport.getAskDate());
			
			Map<String, List<String>> headers = getHeader(url);
			List<String> date = headers.get("Date");
			if(date != null){
				System.out.print("header date: ");
				dbExport.setDateDate(date.get(0));
				System.out.println(dbExport.getDateDate());
			}
			date = headers.get("Last-Modified");
			if(date != null){
				System.out.print("header last modified: ");
				dbExport.setLastModDate(date.get(0));
				System.out.println(dbExport.getLastModDate());
			}
			
			dbExport.setActDate(ExtractedDateHelper.createActualDate().getNormalizedDate(true));
			
			System.out.print("downloaded: ");
			String filepath = downloadUrl(url);
			if(!filepath.equalsIgnoreCase("false")){
				dbExport.setFilePath(filepath);
				setUrlDownloaded(table, url);
				dbExport.setDownloaded(true);
				System.out.print("true");
			}
			System.out.println();
			System.out.println("____________________________________");
		}
		
		File file = new File(path);
		try{
			FileWriter out = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(out);
			
			for(int i=0; i<set.size(); i++){
				if(set.get(i).isDownloaded()){
					String write = set.get(i).getUrl() + separator
					+ set.get(i).getFilePath() + separator
					+ set.get(i).getPubDate() + separator
					+ String.valueOf(set.get(i).isPubSureness()) + separator
					+ set.get(i).getModDate() + separator
					+ String.valueOf(set.get(i).isModSureness()) + separator
					+ set.get(i).getGoogleDate() + separator
					+ set.get(i).getHakiaDate() + separator
					+ set.get(i).getAskDate() + separator
					+ set.get(i).getLastModDate() + separator
					+ set.get(i).getDateDate() + separator
					+ set.get(i).getActDate();
					bw.write(write + "\n");
				}
			}	
			bw.close();
			out.close();
		} catch(Exception e){
			e.printStackTrace();
		}
		
		
	}
	
	private static void setUrlDownloaded(String table, String url){
		openConnection();
		
		String sqlQuery;
		try {
			sqlQuery ="UPDATE "+ table + " SET downloaded=1 WHERE url='" + url + "'";
			st.executeUpdate(sqlQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		closeConnection();
		
	}
	
	private static void writeSearchEngineDates(int searchEngine){
		switch(searchEngine){
			case SEARCH_ASK: 
				AskDataSet.writeAskDates();
				break;
			case SEARCH_GOOGLE:
				GoogleDataSet.writeGoogleDates();
				break;
			case SEARCH_HAKIA: 
				HakiaDataSet.writeHakiaDates();
				break;
		}
	}
	
	
	private static void saveAllUrls(String path, String name){
		File file = new File(path + name);
		ArrayList<String> urls = new ArrayList<String>();
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line;
			int i=1;
			while((line=br.readLine())!=null){
				if(i<3){
					i++;
					continue;
				}
				int begin = line.indexOf(HREF) + HREF.length();
				int end = line.indexOf(ENDHREF);
				urls.add(line.substring(begin, end-1).replaceAll("'", "''"));
				System.out.println(line.substring(begin, end-1).replaceAll("'", "''"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		openConnection();
		
		String sqlQuery;
		try {
			Object[] urlArray = urls.toArray();
			for(int i=0; i< urlArray.length; i++){
				sqlQuery ="INSERT INTO allurls (url) VALUES ('"+ urlArray[i].toString() + "') ON DUPLICATE KEY UPDATE url='" + urlArray[i].toString() + "'";
				st.executeUpdate(sqlQuery);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		closeConnection();
		
		
		
	}
	
	public static void openConnection(){
		try {
			cn = DriverManager.getConnection(DRIVER + DB, "root", "");
			st = cn.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}	

	public static void closeConnection(){
		try {
			if(rs != null){
				rs.close();
			}
			st.close();
			cn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<String> loadURLsFromDB(String db){
		ArrayList<String> urls = new ArrayList<String>();
		String sqlQuery ="select url from " + db;
		
		openConnection();
		try {
			rs = DataSetHandler.st.executeQuery(sqlQuery);
			while( rs.next() ) {
				urls.add((rs.getString("url")).toLowerCase());
	        }
		} catch (SQLException e) {
			e.printStackTrace();
		}
		closeConnection();
		
		return urls;
	}
	
	public static ArrayList<DBExport> loadURLsFromUrlset(int downloaded){
		ArrayList<DBExport> set= new ArrayList<DBExport>();
		String sqlQuery ="SELECT * FROM `urlset` WHERE `downloaded`=" + downloaded;
		
		openConnection();
		try {
			rs = DataSetHandler.st.executeQuery(sqlQuery);
			while( DataSetHandler.rs.next() ) {
				String url = rs.getString("url");
				String pubDate = rs.getString("pub_date");
				String modDate = rs.getString("mod_date");
				
				boolean pubSureness = false;
				switch(Integer.valueOf(rs.getString("pub_sure"))){
					case 0:
						pubSureness = false;
						break;
					case 1:
						pubSureness = true;
						break;
				}
				boolean modSureness = false;
				switch(Integer.valueOf(rs.getString("mod_sure"))){
				case 0:
					modSureness = false;
					break;
				case 1:
					modSureness = true;
					break;
				}
				set.add(new DBExport(url, pubDate, modDate, pubSureness, modSureness));
	        }
		} catch (SQLException e) {
			e.printStackTrace();
		}
		closeConnection();
		
		return set;
	}
	public static ArrayList<DBExport> loadURLsFromUrlset(int downloaded, String table){
		ArrayList<DBExport> set= new ArrayList<DBExport>();
		String sqlQuery ="SELECT * FROM `"+ table + "` WHERE `downloaded`=" + downloaded;
		System.out.println(sqlQuery);
		openConnection();
		try {
			rs = DataSetHandler.st.executeQuery(sqlQuery);
			while( DataSetHandler.rs.next() ) {
				String url = rs.getString("url");
				String pubDate = rs.getString("pub_date");
				String modDate = rs.getString("mod_date");
				
				boolean pubSureness = false;
				switch(Integer.valueOf(rs.getString("pub_sure"))){
					case 0:
						pubSureness = false;
						break;
					case 1:
						pubSureness = true;
						break;
				}
				boolean modSureness = false;
				switch(Integer.valueOf(rs.getString("mod_sure"))){
				case 0:
					modSureness = false;
					break;
				case 1:
					modSureness = true;
					break;
				}
				set.add(new DBExport(url, pubDate, modDate, pubSureness, modSureness));
	        }
		} catch (SQLException e) {
			e.printStackTrace();
		}
		closeConnection();
		
		return set;
	}
	
	private static void downloadUrls(String path){
		Crawler crawler = new Crawler();
		
		ArrayList<String> urls = loadURLsFromDB("urlset");
		openConnection();
		try{
			for(int i=0; i<urls.size();i++){
				String file = "webpage_" + String.valueOf(GregorianCalendar.getInstance().getTimeInMillis()) + ".html";
				String fullPath =  path + file;
				boolean success = crawler.downloadAndSave(urls.get(i),fullPath);
				if(success){
					String sqlQuery ="INSERT INTO localpath (url, path) VALUES ('"+ urls.get(i) + "', '" + fullPath +"') ON DUPLICATE KEY UPDATE path='" + fullPath + "'";
					st.executeUpdate(sqlQuery);
				}
				
			}
		}catch (Exception e){
			
		}
		closeConnection();
		
	}
	private static String downloadUrl(String url){
		
		Crawler crawler = new Crawler();
		String path = "data/evaluation/daterecognition/webpages/";
		String file = "webpage_" + String.valueOf(GregorianCalendar.getInstance().getTimeInMillis()) + ".html";
		String storage = path + file;
		boolean success = crawler.downloadAndSave(url, storage);
		if(!success){
			storage = "false";
		}
		return storage;
	}
	
	private static Map<String, List<String>> getHeader(String url){
		Crawler crawler = new Crawler();
		return crawler.getHeaders(url);
	}
	

	
	
	public static HashMap<String, Integer> getClassification(String table, String round, ArrayList<String>urls){
		HashMap<String, Integer> classificationMap = new HashMap<String, Integer>();
		String url;
		openConnection();
		for(int i=0; i<urls.size(); i++){
			url = urls.get(i);
			try {
				String sqlQuery ="SELECT * FROM " + table + " WHERE url='" + url + "'";
				rs = DataSetHandler.st.executeQuery(sqlQuery);
				while(DataSetHandler.rs.next() ) {
					classificationMap.put(url, rs.getInt(round));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		closeConnection();
		return classificationMap;
	}
	
	public static void writeInDB(String table, String url, int compare, String round){
    	DataSetHandler.openConnection();
    	String sqlQuery ="INSERT INTO " + table + " (url, " + round + ") VALUES ('"+ url + "','" + compare + "') ON DUPLICATE KEY UPDATE " + round + "='" + compare + "'";
    	try {
			DataSetHandler.st.executeUpdate(sqlQuery);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	DataSetHandler.closeConnection();
    }
	
	public static void addCloumn(String table, String ColumnName, String type, String defaultVal){
		String defaultString = "";
		if(defaultVal!=null && !defaultVal.equalsIgnoreCase("")){
			defaultString = "DEFAULT '" + defaultVal + "'";
		}
		String sqlQuery = "ALTER TABLE  " + table + " ADD  " + ColumnName + " " + type + " NOT NULL " + defaultString;
		openConnection();
		try {
			System.out.println(sqlQuery);
			st.execute(sqlQuery);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		closeConnection();
	}
	
	private static void createUrlSetWithoutDate(String in, String out){
		ArrayList<String> set = loadURLsFromDB("urlset");
		HashMap<String, Boolean> urlSet = new HashMap<String, Boolean>();
		ArrayList<String> tempUrls = readUrlsetFile(in);
		ArrayList<String> finalUrlset = new ArrayList<String>();
		for(int i=0; i<tempUrls.size(); i++){
			urlSet.put(tempUrls.get(i), true);
		}
		for(int i=0; i<set.size(); i++){
			urlSet.put(set.get(i), false);
		}
		for(Entry<String, Boolean> e: urlSet.entrySet()){
			if(e.getValue()){
				finalUrlset.add("<a href=\"" + e.getKey() + "\">" + e.getKey() + "</a><br>");
			}
		}
		writeInFile(out, finalUrlset, false);
		
	}
	
	public static void writeInFile(String file, ArrayList<String> set, boolean appendFile){
		try{
			File in = new File(file);
			FileWriter fw = new FileWriter(in,appendFile);
			BufferedWriter bw = new BufferedWriter(fw);
			for(int i=0; i<set.size(); i++){
				bw.write(set.get(i));
			}
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private static ArrayList<String> readUrlsetFile(String file){
		ArrayList<String> urls = new ArrayList<String>(); 
		try{
			
			File in= new File(file);
			FileReader fr = new FileReader(in);
			BufferedReader br = new BufferedReader(fr);
			String line;
			int lineindex=0;
			while((line=br.readLine())!=null){
				if(lineindex>1){
					int indexStart = line.indexOf('>');
					int indexEnd = line.indexOf("</a>");
					String url = line.substring(indexStart + 1, indexEnd);
					urls.add(url.toLowerCase());
				}
				lineindex++;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return urls;
	}
	private static void setDownloadTo(String file, String table, int downloaded){
		HashMap<String, DBExport> readySet = EvaluationHelper.readFile(file);
		
		openConnection();
		for(Entry<String, DBExport> e: readySet.entrySet()){
			String sqlQuery ="UPDATE " + table + " SET downloaded=" + downloaded + " WHERE url='" + e.getKey().toLowerCase() + "'";
			try{
				System.out.println(sqlQuery);
				st.execute(sqlQuery);
				
			}catch (Exception ex) {
				ex.printStackTrace();
				break;
			}
		}
		closeConnection();
		
	}
}