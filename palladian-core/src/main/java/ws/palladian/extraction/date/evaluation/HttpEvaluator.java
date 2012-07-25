package ws.palladian.extraction.date.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.getter.HttpDateGetter;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.extraction.date.rater.HttpDateRater;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.dates.MetaDate;


public class HttpEvaluator {

	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//evaluateLastModified();
		
		//createHttpUrlList("", "");
		
		HttpDateGetter dg = new HttpDateGetter();
		HttpDateRater dr = new HttpDateRater(PageDateType.publish);
		
		String file = "data/evaluation/daterecognition/datasets/httpdataset.txt";
		evaluate(DBExport.PUB_DATE, dg, dr,file);
		evaluate(DBExport.MOD_DATE, dg, dr,file);
	}

	private static void evaluate(int pub_mod, HttpDateGetter dg, HttpDateRater dr, String file){
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
			
			List<MetaDate> dates = new ArrayList<MetaDate>();
			ExtractedDate dateDate = DateParser.findDate(e.getValue().get(DBExport.HEADER_DATE));
			// MetaDate tempDate = DateConverter.convert(dateDate, DateType.MetaDate);
			MetaDate tempDate = new MetaDate(dateDate);
			if(tempDate != null){
				dates.add(tempDate);
			}
			ExtractedDate lastDate = DateParser.findDate(e.getValue().get(DBExport.HEADER_LAST));
			//tempDate = DateConverter.convert(lastDate, DateType.MetaDate);
			tempDate = new MetaDate(lastDate);
			if(tempDate != null){
				dates.add(tempDate);
			}
			
			System.out.print("rate...");
			
			ExtractedDate downloadedDate = DateParser.findDate(e.getValue().get(DBExport.ACTUAL_DATE));
			HashMap<MetaDate, Double> dateArray = dr.evaluateHTTPDate(dates, downloadedDate);
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
				case EvaluationHelper.AFW:
					wf++;
					break;
				case EvaluationHelper.ANF:
					wnf++;
					break;
				case EvaluationHelper.AWD:
					ff++;
					break;
				case EvaluationHelper.ARD:
					rnf++;
					break;
				case EvaluationHelper.AFR:
					rf++;
					break;
					
			}
			counter++;
			System.out.println();
			System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
			System.out.println("---------------------------------------------------------------------");
		}
		System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
		
	}
	

	
	
}
