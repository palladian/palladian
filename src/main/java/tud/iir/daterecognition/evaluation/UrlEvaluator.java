package tud.iir.daterecognition.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.dates.HTTPDate;
import tud.iir.daterecognition.dates.URLDate;
import tud.iir.daterecognition.searchengine.DBExport;
import tud.iir.daterecognition.searchengine.DataSetHandler;
import tud.iir.daterecognition.technique.HTTPDateGetter;
import tud.iir.daterecognition.technique.HttpDateRater;
import tud.iir.daterecognition.technique.PageDateType;
import tud.iir.daterecognition.technique.TechniqueDateGetter;
import tud.iir.daterecognition.technique.TechniqueDateRater;
import tud.iir.daterecognition.technique.URLDateGetter;
import tud.iir.daterecognition.technique.UrlDateRater;
import tud.iir.helper.DateArrayHelper;

public class UrlEvaluator {

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TechniqueDateGetter<URLDate> dg = new URLDateGetter();
		TechniqueDateRater<URLDate> dr = new UrlDateRater(PageDateType.publish);
		

		//evaluate("pub0",DBExport.PUB_DATE, dg, dr);
		//evaluate("mod0",DBExport.MOD_DATE, dg, dr);
		
	//	EvaluationHelper.calculateOutput(0, EvaluationHelper.URLEVAL);
		
		//Integer[] list = {50, 100, 150, 200, 250, 300, 350};
		//EvaluationHelper.calculateConfidenceInterval(EvaluationHelper.URLEVAL, "mod0", list);
		
		System.out.println("pub:");
		double ci = EvaluationHelper.calculateCI(EvaluationHelper.URLEVAL, "pub0", DataSetHandler.TP, 150, false);
		System.out.println("CI: " + ci);
		System.out.println("Sample Size: " + EvaluationHelper.calculateSampleSize(ci));
		System.out.println("mod:");
		ci = EvaluationHelper.calculateCI(EvaluationHelper.URLEVAL, "mod0", DataSetHandler.TP, 150, false);
		System.out.println("CI: " + ci);
		System.out.println("Sample Size: " + EvaluationHelper.calculateSampleSize(ci));
	}
	
	private static <T> void  evaluate(String round,int pub_mod, TechniqueDateGetter<T> dg, TechniqueDateRater<T> dr){
		
		Evaluator.evaluate(EvaluationHelper.URLEVAL, round, pub_mod, dg, dr);
		
		/*int truePositiv = 0;
		int trueNegative = 0;
		int falsePositv = 0;
		int falseNegativ = 0;
		int counter=0;
		int compare;
		HashMap<String, DBExport> set = EvaluationHelper.readFile();
		
		for(Entry<String, DBExport> e : set.entrySet()){
			String url = e.getValue().get(DBExport.URL);
			String urlDateString = "";
			System.out.println(url);
				System.out.print("get dates... ");
			dg.setUrl(url);
			ArrayList<URLDate> urlDates = dg.getDates();
				System.out.print("rate...");
			HashMap<URLDate, Double> urlDateMap = dr.rate(urlDates);
			URLDate urlDate = DateArrayHelper.getFirstElement(urlDateMap);
			if(urlDate != null){
				urlDateString = urlDate.getNormalizedDate(true);
			}
				System.out.println("compare...");
			compare = EvaluationHelper.compareDate(urlDate, e.getValue(), pub_mod);
				System.out.print(compare + " urlDate: " + urlDateString + " - " + pub_mod + ": " + e.getValue().get(pub_mod));
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
			DataSetHandler.writeInDB(EvaluationHelper.URLEVAL,url, compare, round);
			counter++;
			System.out.println();
			System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv);
			System.out.println("---------------------------------------------------------------------");
		}
		System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv);
		*/
	}

	
	
	
	
}
