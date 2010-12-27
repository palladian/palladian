package tud.iir.daterecognition.searchengine;

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
import java.util.List;
import java.util.Map;

import tud.iir.daterecognition.ExtractedDateHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.web.Crawler;


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
		
		//createSearchDatesAndDownload("data/evaluation/dateextraction/dataset.txt");
	}
	
	
	private static void createSearchDatesAndDownload(String path){
		ArrayList<DBExport> set = loadURLsFromUrlset(1);
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
				setUrlDownloaded(url);
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
	
	private static void setUrlDownloaded(String url){
		openConnection();
		
		String sqlQuery;
		try {
			sqlQuery ="UPDATE urlset SET downloaded=1 WHERE url='" + url + "'";
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
				urls.add(rs.getString("url"));
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
		String path = "data/webpages/daterecognition/";
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
	
}