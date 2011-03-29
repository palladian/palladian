package ws.palladian.daterecognition.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import ws.palladian.daterecognition.DateConverter;
import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.ExtractedDateHelper;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.HTTPDate;
import ws.palladian.daterecognition.searchengine.DBExport;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.daterecognition.technique.HTTPDateGetter;
import ws.palladian.daterecognition.technique.HttpDateRater;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.helper.date.DateComparator;


public class HttpEvaluator {

	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//evaluateLastModified();
		
		//createHttpUrlList("", "");
		
		HTTPDateGetter dg = new HTTPDateGetter();
		HttpDateRater dr = new HttpDateRater(PageDateType.publish);
		
		String file = "data/evaluation/daterecognition/datasets/httpdataset.txt";
		evaluate("pub1",DBExport.PUB_DATE, dg, dr,file);
		evaluate("mod1",DBExport.MOD_DATE, dg, dr,file);
		
		/*
		String in1 = "data/evaluation/daterecognition/datasets/dataset.txt";
		String in2 = "data/evaluation/daterecognition/datasets/httpdataset_old.txt";
		mergeUrlsets(in1, in2, file);
		*/
		
		String pub = "pub0";
		System.out.println(pub);
		System.out.println("RF: " + EvaluationHelper.count(file, pub, EvaluationHelper.HTTPEVAL, DataSetHandler.AFR));
		System.out.println("RNF: " + EvaluationHelper.count(file, pub, EvaluationHelper.HTTPEVAL, DataSetHandler.ARD));
		System.out.println("WF: " + EvaluationHelper.count(file, pub, EvaluationHelper.HTTPEVAL, DataSetHandler.AFW));
		System.out.println("WNF: " + EvaluationHelper.count(file, pub, EvaluationHelper.HTTPEVAL, DataSetHandler.ANF));
		System.out.println("FF: " + EvaluationHelper.count(file, pub, EvaluationHelper.HTTPEVAL, DataSetHandler.AWD));
				
		String mod = "mod0";
		System.out.println(mod);
		System.out.println("RF: " + EvaluationHelper.count(file, mod, EvaluationHelper.HTTPEVAL, DataSetHandler.AFR));
		System.out.println("RNF: " + EvaluationHelper.count(file, mod, EvaluationHelper.HTTPEVAL, DataSetHandler.ARD));
		System.out.println("WF: " + EvaluationHelper.count(file, mod, EvaluationHelper.HTTPEVAL, DataSetHandler.AFW));
		System.out.println("WNF: " + EvaluationHelper.count(file, mod, EvaluationHelper.HTTPEVAL, DataSetHandler.ANF));
		System.out.println("FF: " + EvaluationHelper.count(file, mod, EvaluationHelper.HTTPEVAL, DataSetHandler.AWD));
		
	}

	private static void evaluate(String round,int pub_mod, HTTPDateGetter dg, HttpDateRater dr, String file){
		int rnf = 0;
		int ff= 0;
		int wnf= 0;
		int rf= 0;
		int wf = 0;
		int counter=0;
		int compare;
		HashMap<String, DBExport> set = EvaluationHelper.readFile(file);
		
		for(Entry<String, DBExport> e : set.entrySet()){
			String tempDateString = "";
			
			System.out.println(e.getValue().getUrl());
			System.out.print("get dates... ");
			
			ArrayList<HTTPDate> dates = new ArrayList<HTTPDate>();
			ExtractedDate dateDate = DateGetterHelper.findDate(e.getValue().get(DBExport.HEADER_DATE));
			HTTPDate tempDate = DateConverter.convert(dateDate, DateConverter.TECH_HTTP_HEADER);
			if(tempDate != null){
				dates.add(tempDate);
			}
			ExtractedDate lastDate = DateGetterHelper.findDate(e.getValue().get(DBExport.HEADER_LAST));
			tempDate = DateConverter.convert(lastDate, DateConverter.TECH_HTTP_HEADER);
			if(tempDate != null){
				dates.add(tempDate);
			}
			
			System.out.print("rate...");
			
			ExtractedDate downloadedDate = DateGetterHelper.findDate(e.getValue().get(DBExport.ACTUAL_DATE));
			HashMap<HTTPDate, Double> dateArray = dr.evaluateHTTPDate(dates, downloadedDate);
			double rate = DateArrayHelper.getHighestRate(dateArray);
			dates = DateArrayHelper.getRatedDates(dateArray, rate);
			if(dates.size()>0 && dates.get(0) != null){
				tempDate = dates.get(0);
				tempDateString = tempDate.getNormalizedDate(true);
				
			}else{
				tempDate = null;
			}
			
			System.out.println("compare...");

			if(rate == 0){
				tempDate = null;
			}
			compare = EvaluationHelper.compareDate(tempDate, e.getValue(), pub_mod);
			
			System.out.print(compare + " httpDate:" + tempDateString + " - " + pub_mod + ":" + e.getValue().get(pub_mod));
			switch(compare){
				case DataSetHandler.AFW:
					wf++;
					break;
				case DataSetHandler.ANF:
					wnf++;
					break;
				case DataSetHandler.AWD:
					ff++;
					break;
				case DataSetHandler.ARD:
					rnf++;
					break;
				case DataSetHandler.AFR:
					rf++;
					break;
					
			}
			DataSetHandler.writeInDB(EvaluationHelper.HTTPEVAL, e.getValue().getUrl(), compare, round);
			counter++;
			System.out.println();
			System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
			System.out.println("---------------------------------------------------------------------");
		}
		System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
		
	}
	
	/**
	 * Gets for all headers from urls out of DB "allurls" the last-modified-tag. compares it to actual date.
	 */
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
	
private static void mergeUrlsets(String in1, String in2, String out){
		
		HashMap<String, DBExport> set1 = EvaluationHelper.readFile(in1);
		HashMap<String, DBExport> set2 = EvaluationHelper.readFile(in2);
		
		HashMap<String, DBExport> merged = new HashMap<String, DBExport>();
		merged.putAll(set1);
		merged.putAll(set2);
		String separator = DataSetHandler.SEPARATOR;
		File file = new File(out);
		try{
			FileWriter outw = new FileWriter(file, false);
			BufferedWriter bw = new BufferedWriter(outw);
			bw.write("url *;_;* path *;_;* pub_date *;_;* pub_sureness *;_;* mod_date *;_;* mod_sureness *;_;* google_date *;_;* hakia_date *;_;* ask_date *;_;* header_last_mod *;_;* header_date *;_;* down_date \n");
			for(Entry<String, DBExport>e : merged.entrySet()){
				ExtractedDate header_last_mod = DateGetterHelper.findDate(e.getValue().get(DBExport.HEADER_LAST));
				ExtractedDate downloaded = getDownloadedDate(e.getValue());
				DateComparator dc = new DateComparator();
				
				
				boolean pub_sure = Boolean.valueOf(e.getValue().get(DBExport.PUB_SURE));
				boolean mod_sure = Boolean.valueOf(e.getValue().get(DBExport.MOD_SURE));
				if(header_last_mod != null && !(!pub_sure && mod_sure)  && hasPubOrModDate(e.getValue())){
					int compare = dc.compare(downloaded, header_last_mod, DateComparator.MEASURE_DAY);
					if( compare != 0){
						String write = e.getValue().getUrl() + separator
						+ e.getValue().getFilePath() + separator
						+ e.getValue().getPubDate() + separator
						+ String.valueOf(e.getValue().isPubSureness()) + separator
						+ e.getValue().getModDate() + separator
						+ String.valueOf(e.getValue().isModSureness()) + separator
						+ e.getValue().getGoogleDate() + separator
						+ e.getValue().getHakiaDate() + separator
						+ e.getValue().getAskDate() + separator
						+ e.getValue().getLastModDate() + separator
						+ e.getValue().getDateDate() + separator
						+ e.getValue().getActDate();
						bw.write(write + "\n");
					
						System.out.println(write);
					}
					
				}
			}	
			bw.close();
			outw.close();
		} catch(Exception e){
			e.printStackTrace();
		}
		
	}
	

	private static void createHttpUrlList(String in, String out){
		HashMap<String, String> set = new HashMap<String, String>();
		try{
			
			File file = new File(in);
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line;
			
			HTTPDateGetter dg = new HTTPDateGetter();
			int lineindex=0;
			while((line=br.readLine())!=null){
				if(lineindex>1){
					int indexStart = line.indexOf('>');
					int indexEnd = line.indexOf("</a>");
					String url = line.substring(indexStart + 1, indexEnd);
					
					System.out.println(url);
					dg.setUrl(url);
					ArrayList<HTTPDate> dates = dg.getDates();
					if(dates.size() > 0){
						boolean last = false;
						for(int i=0; i<dates.size(); i++){
							if(dates.get(i).getKeyword().equalsIgnoreCase("last-modified")){
								System.out.println("!!!!!!!!!!!!!!!!!!!!");
								set.put(line, null);
							}
						}
					}
				}
				
				lineindex++;
			}
			br.close();
			fr.close();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try{
			File file = new File(out);
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(Entry<String, String> e: set.entrySet()){
				bw.write(e.getKey() + "\n");
			}
			
			bw.close();
			fw.close();
		}catch(Exception e){
			
		}
	}
	
	private static ExtractedDate getDownloadedDate(DBExport dbExport){
		return DateGetterHelper.findDate(dbExport.get(DBExport.ACTUAL_DATE));
	}
	
	private static boolean hasPubOrModDate(DBExport dbExport){
		ExtractedDate pubDate = DateGetterHelper.findDate(dbExport.get(DBExport.PUB_DATE));
		ExtractedDate modDate = DateGetterHelper.findDate(dbExport.get(DBExport.MOD_DATE));
		return (pubDate != null || modDate != null);
	}
}
