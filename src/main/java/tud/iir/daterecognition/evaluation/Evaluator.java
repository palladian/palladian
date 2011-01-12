package tud.iir.daterecognition.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HeadDate;
import tud.iir.daterecognition.dates.StructureDate;
import tud.iir.daterecognition.searchengine.DBExport;
import tud.iir.daterecognition.searchengine.DataSetHandler;
import tud.iir.daterecognition.technique.TechniqueDateGetter;
import tud.iir.daterecognition.technique.TechniqueDateRater;
import tud.iir.helper.DateArrayHelper;
import tud.iir.web.Crawler;

public abstract class Evaluator {

	
	private ExtractedDate actualDate;
	
	public static <T> void evaluate(String table, String round,int pub_mod, TechniqueDateGetter<T> dg, TechniqueDateRater<T> dr){
		int truePositiv = 0;
		int trueNegative = 0;
		int falsePositv = 0;
		int falseNegativ = 0;
		int ff = 0;
		int counter=0;
		int compare;
		
		HashMap<String, DBExport> set = EvaluationHelper.readFile();
		Crawler crawler = new Crawler();
		
		for(Entry<String, DBExport> e : set.entrySet()){
			
			T bestDate = null;
			String bestDateString ="";
			String url =e.getValue().get(DBExport.URL);
			System.out.println(url);
			if(table.equalsIgnoreCase(EvaluationHelper.CONTENTEVAL) || table.equalsIgnoreCase(EvaluationHelper.STRUCTEVAL) || table.equalsIgnoreCase(EvaluationHelper.HEADEVAL)){
				String path = e.getValue().get(DBExport.PATH);
				//System.out.println(path);
				dg.setDocument(crawler.getWebDocument(path));
			}else{
				
				dg.setUrl(url);
			}
			
			System.out.print("get dates... ");
				
			ArrayList<T> list = dg.getDates();
			list = DateArrayHelper.removeNull(list);
			if(list.size() > 0){
				ArrayList<T> filteredDates = DateArrayHelper.filter(list, DateArrayHelper.FILTER_FULL_DATE);
				filteredDates = DateArrayHelper.filter(filteredDates, DateArrayHelper.FILTER_IS_IN_RANGE);
				
				if(filteredDates.size()>0){
						
					System.out.print("rate dates... ");
					dr.rate(filteredDates);
					System.out.print("best date... ");
					bestDate = dr.getBestDate();
					
					bestDateString = ((ExtractedDate) bestDate).getNormalizedDate(true);
				}
			}
			System.out.println("compare...");
			
			compare = EvaluationHelper.compareDate(bestDate, e.getValue(),pub_mod);
			ExtractedDate date;
			String dbExportDateString;
			if(pub_mod == DBExport.PUB_DATE){
				date = DateGetterHelper.findDate(e.getValue().getPubDate());
				dbExportDateString =" - pubDate:" ;
			}else{
				date = DateGetterHelper.findDate(e.getValue().getModDate());
				dbExportDateString =" - modDate:" ;
			}
			
			if(date!=null){
				dbExportDateString +=  date.getNormalizedDateString();
			}
			System.out.print(compare + " bestDate:" + bestDateString + dbExportDateString);
			switch(compare){
				case -3:
					ff++;
					break;
				case -2:
					falseNegativ++;
					break;
				case -1:
					falsePositv++;
					break;
				case 0:
					trueNegative++;
					break;
				case 1:
					truePositiv++;
					break;
					
			}
			
			DataSetHandler.writeInDB(table, e.getValue().getUrl(), compare, round);
			counter++;
			
			System.out.println();
			System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv + " ff: " + ff);
			System.out.println("---------------------------------------------------------------------");
		}
		System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv + " ff: " + ff);
	}
	
}
