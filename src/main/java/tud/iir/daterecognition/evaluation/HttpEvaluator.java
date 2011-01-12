package tud.iir.daterecognition.evaluation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import tud.iir.daterecognition.DateConverter;
import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.ExtractedDateHelper;
import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HTTPDate;
import tud.iir.daterecognition.searchengine.DBExport;
import tud.iir.daterecognition.searchengine.DataSetHandler;
import tud.iir.daterecognition.technique.ContentDateGetter;
import tud.iir.daterecognition.technique.ContentDateRater;
import tud.iir.daterecognition.technique.HTTPDateGetter;
import tud.iir.daterecognition.technique.HttpDateRater;
import tud.iir.daterecognition.technique.PageDateType;
import tud.iir.daterecognition.technique.TechniqueDateGetter;
import tud.iir.daterecognition.technique.TechniqueDateRater;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;


public class HttpEvaluator {

	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//evaluateLastModified();
		
		HTTPDateGetter dg = new HTTPDateGetter();
		HttpDateRater dr = new HttpDateRater(PageDateType.publish);
		
		
		//evaluate("pub0",DBExport.PUB_DATE, dg, dr);
		//evaluate("mod0",DBExport.MOD_DATE, dg, dr);
		System.out.println(EvaluationHelper.count("pub0", EvaluationHelper.HTTPEVAL, 200, DataSetHandler.TP));
		System.out.println(EvaluationHelper.count("mod0", EvaluationHelper.HTTPEVAL, 200, DataSetHandler.TP));
		//EvaluationHelper.calculateOutput(0, EvaluationHelper.HTTPEVAL, 350);
		//Integer[] list = {50, 100, 150, 200, 250, 300, 350};
		//EvaluationHelper.calculateConfidenceInterval(EvaluationHelper.HTTPEVAL, "pub0", list);
		System.out.println("pub:");
		double ci = EvaluationHelper.calculateCI(EvaluationHelper.HTTPEVAL, "pub0", DataSetHandler.TP, 150, false);
		System.out.println("CI: " + ci);
		System.out.println("Sample Size: " + EvaluationHelper.calculateSampleSize(ci));
		System.out.println("mod:");
		ci = EvaluationHelper.calculateCI(EvaluationHelper.HTTPEVAL, "mod0", DataSetHandler.TP, 150, false);
		System.out.println("CI: " + ci);
		System.out.println("Sample Size: " + EvaluationHelper.calculateSampleSize(ci));
		

		
	}

	private static void evaluate(String round,int pub_mod, HTTPDateGetter dg, HttpDateRater dr){
		int truePositiv = 0;
		int trueNegative = 0;
		int falsePositv = 0;
		int falseNegativ = 0;
		int counter=0;
		int compare;
		HashMap<String, DBExport> set = EvaluationHelper.readFile();
		
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
			
			compare = EvaluationHelper.compareDate(tempDate, e.getValue(), pub_mod);
			System.out.print(compare + " httpDate:" + tempDateString + " - " + pub_mod + ":" + e.getValue().get(pub_mod));
			switch(compare){
				case DataSetHandler.FN:
					falseNegativ++;
					break;
				case DataSetHandler.FP:
					falsePositv++;
					break;
				case DataSetHandler.TN:
					trueNegative++;
					break;
				case DataSetHandler.TP:
					truePositiv++;
					break;
					
			}
			DataSetHandler.writeInDB(EvaluationHelper.HTTPEVAL, e.getValue().getUrl(), compare, round);
			counter++;
			System.out.println();
			System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv);
			System.out.println("---------------------------------------------------------------------");
		}
		System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv);
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
}
