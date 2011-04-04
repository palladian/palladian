package ws.palladian.daterecognition.searchengine;

import java.awt.event.KeyListener;
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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.ExtractedDateHelper;
import ws.palladian.daterecognition.KeyWords;
import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.MetaDate;
import ws.palladian.daterecognition.evaluation.EvaluationHelper;
import ws.palladian.helper.date.ContentDateComparator;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.helper.date.DateComparator;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.retrieval.DocumentRetriever;


public class DataSetHandler{
	
	private static final String DRIVER = "jdbc:mysql://localhost/";
	private static final String DB = "dateset";
	
	public static Connection cn = null;
	public static Statement st = null;
	public static ResultSet rs = null;
	
	private static final String HREF="<a href=\"";
	private static final String ENDHREF = ">";
	
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
	 * Available Found Wrong. <br>
	 * ED = 1 & FD = 1 & ED != Fd 
	 */
	public static final int AFW = -2;
	/**
	 * -1 <br>
	 * Available Not Found. <br>
	 * ED = 1 & FD = 0
	 */
	public static final int ANF = -1;
	/**
	 * 0 <br>
	 * Absent Wrong Detected. <br>
	 * ED = 0 & FD = 1
	 */
	public static final int AWD = 0;
	/**
	 * 1 <br>
	 * Absent Right Detected. <br>
	 * ED = 0 & FD = 0
	 */
	public static final int ARD = 1;
	/**
	 * 2 <br>
	 * Available Found Right. <br>
	 * ED = 1 & FD = 1 & ED == FD
	 */
	public static final int AFR = 2;
	
	public static final String SEPARATOR = " *;_;* "; 
	
	public static void main(String[] args){
		/*
		String path = "D:/_Uni/_semester16/dataset/";	
		String name = "urlSet06.htm";
		saveAllUrls(path, name);
		*/
		//writeSearchEngineDates(SEARCH_ASK);
		
		//AskDateGetter adg = new AskDateGetter();
		//adg.getAskDate("http://bettingchoice.co.uk/2010-world-cup-group-a-predictions-and-betting-tips_20033");
		
		//HakiaDateGetter hdg = new HakiaDateGetter();
		//hdg.getHakiaDate("http://www.upi.com/Science_News/Resource-Wars/2010/05/26/Gulf-Keystone-to-raise-funds-for-Iraq-work/UPI-50891274879566/");
		
		//downloadUrls("data/webpages/daterecognition/");
		
		
		//String path = "data/evaluation/daterecognition/datasets/headdataset.txt";
		//createSearchDatesAndDownload("headdateset", path);
		//setDownloadTo(path, "urlset", 1);
		
		/*
		String path = "D:/_Uni/_semester16/dataset/";	
		String in = "urlSet03.htm";
		String out = "emptyDateUrlSet03.htm";
		createUrlSetWithoutDate(path + in, path + out);
		*/
		
		//addCloumn(EvaluationHelper.CONTENTEVAL, "pub5", "int", "-10");
		//addCloumn(EvaluationHelper.CONTENTEVAL, "mod5", "int", "-10");
		
		//checkWebpagesFile("data/evaluation/daterecognition/datasets/dataset_old.txt","data/evaluation/daterecognition/datasets/dataset.txt","urlset");
		//exportContentFactor();
		
		
		//googleCheck("data/evaluation/daterecognition/datasets/dataset.txt");
	}
	
	
	
	private static void googleCheck(String dataSet){
		HashMap<String, DBExport> map = EvaluationHelper.readFile(dataSet);
		DateComparator dc = new DateComparator();
		int cntGoogle = 0;
		int cntGoogleRightPub = 0;
		int cntGoogleRightMod = 0;
		int cntAll = 0;
		int cntPubMod = 0;
		int googleButNoDate = 0;
		for(Entry<String, DBExport>e : map.entrySet()){
			ExtractedDate pubDate = DateGetterHelper.findDate(e.getValue().getPubDate());
			ExtractedDate modDate = DateGetterHelper.findDate(e.getValue().getModDate());
			ExtractedDate googleDate = DateGetterHelper.findDate(e.getValue().getGoogleDate());
			if(googleDate != null && pubDate != null){
				if(dc.compare(pubDate, googleDate, DateComparator.STOP_DAY) == 0){
					cntGoogleRightPub++;
				}
			} 
			
			if(googleDate != null && modDate != null){
				if(dc.compare(modDate, googleDate, DateComparator.STOP_DAY) == 0){
					cntGoogleRightMod++;
				}
			}
			if(googleDate != null){
				cntGoogle++;
			}
			if(pubDate != null || modDate != null){
				cntPubMod++;
			}
			if(googleDate != null && pubDate == null && modDate == null){
				googleButNoDate++;
			}
		cntAll++;
			
		}
		System.out.println("cnt all: " + cntAll);
		System.out.println("cnt google: " + cntGoogle);
		System.out.println("cnt google right: " + cntGoogleRightPub);
		System.out.println("cnt google right: " + cntGoogleRightMod);
		System.out.println("cnt pub or mod: " + cntPubMod);
		System.out.println("Google but no pub/mod date: " + googleButNoDate);
	}
	public static void checkWebpagesFile(String dataSet,String newDataSet, String table){
		HashMap<String, DBExport> map = EvaluationHelper.readFile(dataSet);
		HashMap<String, DBExport> newMap = new HashMap<String, DBExport>();
		
		for(Entry<String, DBExport> e : map.entrySet()){
			try{
			File file = new File(e.getValue().getFilePath());
			FileReader fr = new FileReader(file);
			newMap.put(e.getKey(), e.getValue());
			fr.close();
			}
			catch (Exception ex) {
				resetDownloadTo(e.getValue().getUrl(), table);
			}
		}
		File file = new File(newDataSet);
		try{
			FileWriter out = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(out);
			
			for(Entry<String, DBExport>e : newMap.entrySet()){
				String write =e.getValue().getUrl() + SEPARATOR
					+ e.getValue().getFilePath() + SEPARATOR
					+ e.getValue().getPubDate() + SEPARATOR
					+ String.valueOf(e.getValue().isPubSureness()) + SEPARATOR
					+ e.getValue().getModDate() + SEPARATOR
					+ String.valueOf(e.getValue().isModSureness()) + SEPARATOR
					+ e.getValue().getGoogleDate() + SEPARATOR
					+ e.getValue().getHakiaDate() + SEPARATOR
					+ e.getValue().getAskDate() + SEPARATOR
					+ e.getValue().getLastModDate() + SEPARATOR
					+ e.getValue().getDateDate() + SEPARATOR
					+ e.getValue().getActDate();
					bw.write(write + "\n");
				}
			
			bw.close();
			out.close();
		} catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
		
		
	
	
	/**
	 * Takes URL out of "urlset", tries to download it and, if downloaded search for google, hakia and ask date. <br>
	 * Writes results in to file.
	 * @param path
	 */
	public static void createSearchDatesAndDownload(String table, String path){
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
					String write = set.get(i).getUrl() + SEPARATOR
					+ set.get(i).getFilePath() + SEPARATOR
					+ set.get(i).getPubDate() + SEPARATOR
					+ String.valueOf(set.get(i).isPubSureness()) + SEPARATOR
					+ set.get(i).getModDate() + SEPARATOR
					+ String.valueOf(set.get(i).isModSureness()) + SEPARATOR
					+ set.get(i).getGoogleDate() + SEPARATOR
					+ set.get(i).getHakiaDate() + SEPARATOR
					+ set.get(i).getAskDate() + SEPARATOR
					+ set.get(i).getLastModDate() + SEPARATOR
					+ set.get(i).getDateDate() + SEPARATOR
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
			br.close();
			fr.close();
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
		DocumentRetriever crawler = new DocumentRetriever();
		
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
		
		DocumentRetriever crawler = new DocumentRetriever();
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
		DocumentRetriever crawler = new DocumentRetriever();
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
			
			br.close();
			fr.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return urls;
	}
	public static void setDownloadTo(String file, String table, int downloaded){
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
	private static void resetDownloadTo(String entry, String table){
		
		
		openConnection();
		
			String sqlQuery ="UPDATE " + table + " SET downloaded=" + 0 + " WHERE url='" + entry.toLowerCase() + "'";
			try{
				System.out.println(sqlQuery);
				st.execute(sqlQuery);
				
			}catch (Exception ex) {
				ex.printStackTrace();
			
			}
		
		closeConnection();
		
	}
	
	public static void writeDateFactors(ArrayList<ContentDate> dates, String url, String doc, String contentTable, boolean useWeight){
		
		HashMap<String, DBExport> map = EvaluationHelper.readFile();
		ExtractedDate pubDate =DateGetterHelper.findDate(map.get(url).getPubDate());
		ExtractedDate modDate =DateGetterHelper.findDate(map.get(url).getModDate());
		DateComparator dc = new DateComparator();
		
		LinkedList<ContentDate> posOrder = new LinkedList<ContentDate>();
		LinkedList<ContentDate> ageOrder = new LinkedList<ContentDate>();
		for(int i=0; i< dates.size(); i++){
			if(dates.get(i).get(ContentDate.DATEPOS_IN_DOC) != -1){
				posOrder.add(dates.get(i));
			}
			ageOrder.add(dates.get(i));
		}
		
		Collections.sort(posOrder, new ContentDateComparator());
		Collections.sort(ageOrder, new DateComparator());
		
		openConnection();
		for(int i=0; i< dates.size(); i++){
			ContentDate date = dates.get(i);
			String pubString = " ";
			String modString = " ";
			int pub = 0;
			if (pubDate != null){
				pub = (dc.compare(date, pubDate, DateComparator.STOP_DAY) == 0) ?  1 : 0;
				pubString = pubDate.getNormalizedDateString();
			}
			int mod = 0;
			if(modDate != null){
				mod = (dc.compare(date, modDate, DateComparator.STOP_DAY) == 0) ?  1 : 0;
				modString = modDate.getNormalizedDateString();
			}
			
			double relSizeDB =  Math.round((1.0/(double)dates.size())*1000.0) / 1000.0;
			double relSize =  useWeight ? relSizeDB : 1;
			
			double year = ((date.get(ExtractedDate.YEAR) == -1) ? 0 : 1) * relSize;
			double month = ((date.get(ExtractedDate.MONTH) == -1) ? 0 : 1) * relSize;
			double day = ((date.get(ExtractedDate.DAY) == -1) ? 0 : 1) * relSize;
			double hour = ((date.get(ExtractedDate.HOUR) == -1) ? 0 : 1) * relSize;
			double minute = ((date.get(ExtractedDate.MINUTE )== -1) ? 0 : 1) * relSize;
			double second = ((date.get(ExtractedDate.SECOND) == -1) ? 0 : 1) * relSize;
			
			double docPos = (date.get(ContentDate.DATEPOS_IN_DOC)) * relSize;
			double relDocPos = (Math.round(((double)docPos/(double)doc.length())*1000.0)/1000.0) * relSize;
			
			double datePosOrder = Math.round(
						((double)(posOrder.indexOf(date) + 1.0) /(double)posOrder.size())
						*1000.0
					) / 1000.0;
			int datePosOrderAbsl = posOrder.indexOf(date);
			double dateAgeOrder = Math.round(
						((double)(ageOrder.indexOf(date) + 1.0)/(double)dates.size())
						*1000.0
					)/1000.0;
			int dateAgeOrdAbsl = ageOrder.indexOf(date);
			
			
			
			int distPosBeforeDate = -1;
			int distPosAfterDate = -1;
			long distAgeBeforeDate = -1;
			long distAgeAfterDate = -1;
			
			if(datePosOrderAbsl > 0){
				distPosBeforeDate = date.get(ContentDate.DATEPOS_IN_DOC) - posOrder.get(datePosOrderAbsl -1).get(ContentDate.DATEPOS_IN_DOC);
			}
			if(datePosOrderAbsl < posOrder.size() -1){
				distPosAfterDate = posOrder.get(datePosOrderAbsl +1).get(ContentDate.DATEPOS_IN_DOC) - date.get(ContentDate.DATEPOS_IN_DOC);
			}
			if(dateAgeOrdAbsl > 0){
				distAgeBeforeDate = Math.round(dc.getDifference(date, ageOrder.get(dateAgeOrdAbsl - 1), DateComparator.MEASURE_HOUR));
			}
			if(dateAgeOrdAbsl < ageOrder.size() -1){
				distAgeBeforeDate = Math.round(dc.getDifference(date, ageOrder.get(dateAgeOrdAbsl + 1), DateComparator.MEASURE_HOUR));
			}
			
			int keyClass = 0;
			int keyLoc = 0;
			double keyDiff = 0;
			
			
			double hasStructDate = ((date.hasStrucutreDate()) ? 1 : 0) * relSize;
			double inMetaDates = ((date.isInMetaDates()) ? 1 : 0) * relSize;
			double inUrl = ((date.isInUrl()) ? 1 : 0) * relSize;
			double relCntSame = Math.round(( (double)(DateArrayHelper.countDates(date, dates, DateComparator.STOP_DAY) + 1) / (double)dates.size() ) * 1000.0) / 1000.0;
			
			double isKeyClass1 = 0;
			double isKeyClass2 = 0;
			double isKeyClass3 = 0;
			
			double keyLoc201 = 0;
			double keyLoc202 = 0;
			
			String keyword = date.getKeyword();
			if(keyword != null){
				keyClass = KeyWords.getKeywordPriority(keyword);
				switch(keyClass){
				case 1:
					isKeyClass1 = 1 *relSize;
					break;
				case 2:
					isKeyClass2 = 1 *relSize;
					break;
				case 3:
					isKeyClass3 = 1 *relSize;
					break;
				}
				
				
				
				int tempKeyLoc = date.get(ContentDate.KEYWORDLOCATION);
				if(tempKeyLoc == 201){
					keyLoc = 1;
					keyLoc201 = 1 * relSize;
				}else{
					keyLoc = 2;
					keyLoc202 = 1 * relSize;
				}
				keyDiff = date.get(ContentDate.DISTANCE_DATE_KEYWORD);
				if(keyDiff >= 30 || keyDiff == -1){
					keyDiff = 0.0;
				}else{
					keyDiff = (1 - Math.round((keyDiff / 30.0)*1000.0)/1000.0) *relSize;
				}
			}
			
			double simpleTag = (HTMLHelper.isSimpleElement(date.getNode()) ? 1 : 0) * relSize;
			double hTag = (HTMLHelper.isHeadlineTag(date.getNode()) ? 1 : 0) * relSize;
			String tagName = date.getTag();
			
			String format = date.getFormat();
			int exactness = date.getExactness();
						
			String sqlQuery = 
					"INSERT INTO "+ contentTable +
						"(url, date, pubDate, modDate, " +
						"pub, `mod`, year, month, day, hour, minute, second, " +
						"relDocPos, ordDocPos, ordAgePos, keyClass, keyLoc, " +
						"keyDiff, simpleTag, hTag, tagName," +
						"hasStructureDate, inMetaDates, inUrl," +
						"relCntSame, relSize," +
						"distPosBefore, distPosAfter, distAgeBefore, distAgeAfter," +
						"format, keyword, excatness," +
						"keyLoc201, keyLoc202," +
						"isKeyClass1, isKeyClass2 , isKeyClass3) " +
					"VALUES ('"+ 
						url + "','" + date.getNormalizedDateString() + "','" + pubString  + "','" + modString + "','" + 
						pub + "','" + mod + "', '" + year + "','" + month + "','" + day + "','" + hour + "','" + minute + "','" +  second+ "'," + 
						relDocPos + "," + datePosOrder + "," + dateAgeOrder + ",'" + keyClass + "','" + keyLoc + "'," +
						keyDiff + ",'" + simpleTag + "','" +hTag + "','" + tagName + "','" +
						hasStructDate + "','" +	inMetaDates + "','" + inUrl + "'," +
						relCntSame  + "," + relSizeDB + "," +
						distPosBeforeDate + "," + distPosAfterDate + "," + distAgeBeforeDate + "," + distAgeAfterDate + ",'" + 
						format + "','" + keyword + "'," + exactness + "," +
						keyLoc201 + ", " + keyLoc202 + ", " +
						isKeyClass1 + ", " + isKeyClass2 + ", " + isKeyClass3 + ")";
			//System.out.println(sqlQuery);
			try {
				st.executeUpdate(sqlQuery);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				System.out.println(sqlQuery);
				e.printStackTrace();
			}
		}
		closeConnection();
	}
	
	public static void exportContentFactor(){
		openConnection();
		
		int cnt = 0;

		int cntYear = 0;
		int cntMonth = 0;
		int cntDay = 0;
		int cntHour = 0;
		int cntMinute = 0;
		int cntSecond = 0;
		
		int cntNoKey = 0;
		int cntKeyClass1 = 0;
		int cntKeyClass2 = 0;
		int cntKeyClass3 = 0;
		
		int posDoc = 0;
		int relPosdoc = 0;
		int ordPos = 0;
		int ordAge = 0;
		
		int cntKeyText = 0;
		int cntkeyAttr = 0;
		
		int cntSmplTag = 0;
		int cnthTag = 0; 
		
		String sqlString ="Select * From contentfactor Where `mod` = 1";
		
		try {
			rs = st.executeQuery(sqlString);

			while(rs.next() ) {
				cnt++;
				if(rs.getInt("year") == 0){
					cntYear++;
				}
				if(rs.getInt("month") == 0){
					cntMonth++;
				}
				if(rs.getInt("day") == 0){
					cntDay++;
				}
				if(rs.getInt("hour") == 0){
					cntHour++;
				}
				if(rs.getInt("minute") == 0){
					cntMinute++;
				}
				if(rs.getInt("second") == 0){
					cntSecond++;
				}
				if(rs.getInt("keyClass") == -1){
					cntNoKey++;
				}else if(rs.getInt("keyClass") == 1){
					cntKeyClass1++;
				}else if(rs.getInt("keyClass") == 2){
					cntKeyClass2++;
				}else if(rs.getInt("keyClass") == 3){
					cntKeyClass3++;
				}
				posDoc += rs.getInt("docPos");
				relPosdoc += rs.getInt("relDocPos");
				ordPos += rs.getInt("ordDocPos");
				ordAge += rs.getInt("ordAgePos");
				
				if(rs.getInt("keyLoc") == 202){
					cntKeyText++;
				}else if(rs.getInt("keyLoc") == 201){
					cntkeyAttr++;
				}
				if(rs.getInt("simpleTag") == 1){
					cntSmplTag++;
				}
				if(rs.getInt("hTag") == 1){
					cnthTag++;
				}
				
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Count: " + cnt);
		System.out.println("Year: " + cntYear);
		System.out.println("Month: " + cntMonth);
		System.out.println("Day: " + cntDay);
		System.out.println("Hour: " + cntHour);
		System.out.println("Minute: " + cntMinute);
		System.out.println("Second: " + cntSecond);
		
		System.out.println("No Key: " + cntNoKey);
		System.out.println("KeyClass1: " + cntKeyClass1);
		System.out.println("KeyClass2: " + cntKeyClass2);
		System.out.println("KeyClass3: " + cntKeyClass3);
		
		System.out.println("Avg DocPos: " + (double)posDoc/(double)cnt);
		System.out.println("Avg relDocPos: " + (double)relPosdoc/(double)cnt);
		System.out.println("Avg ordDocPos: " + (double)ordPos/(double)cnt);
		System.out.println("Avg ordAgePos: " + (double)ordAge/(double)cnt);
		
		System.out.println("keyLoc Text: " + cntKeyText);
		System.out.println("keyLoc Attr: " + cntkeyAttr);
		System.out.println("simple Tag: " + cntSmplTag);
		System.out.println("h-Tag: " + cnthTag);
		
		
		closeConnection();
	}
	
	public static void clearTable(String table, String file){
		HashMap<String, DBExport> dbMap = EvaluationHelper.readFile(file);
		openConnection();
		for(Entry<String, DBExport> e : dbMap.entrySet()){
			String url = e.getKey();
			String sqlQuery = "UPDATE " + table + " SET downloaded = 2 WHERE url='" + url + "'";
			try {
				st.execute(sqlQuery);
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		ArrayList<String> deleteUrls = new ArrayList<String>();
		try {
			String sqlQuery = "SELECT url FROM " + table + " WHERE downloaded=" + 1;
			rs = st.executeQuery(sqlQuery);
			while(rs.next()){
				String url = rs.getString("url");
				deleteUrls.add(url);
			}
			for(int i = 0; i<deleteUrls.size(); i++){
				sqlQuery = "DELETE FROM " + table + " WHERE url = '" + deleteUrls.get(i) + "'";
				st.execute(sqlQuery);
			}
			sqlQuery = "UPDATE " + table + " SET downloaded = 1";
			st.execute(sqlQuery);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		closeConnection();
	}
	
}

