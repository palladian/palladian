package tud.iir.daterecognition.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.daterecognition.dates.HeadDate;
import tud.iir.daterecognition.dates.StructureDate;
import tud.iir.daterecognition.dates.URLDate;
import tud.iir.daterecognition.searchengine.DBExport;
import tud.iir.daterecognition.searchengine.DataSetHandler;
import tud.iir.daterecognition.technique.HeadDateGetter;
import tud.iir.daterecognition.technique.HeadDateRater;
import tud.iir.daterecognition.technique.PageDateType;
import tud.iir.daterecognition.technique.StructureDateGetter;
import tud.iir.daterecognition.technique.StructureDateRater;
import tud.iir.daterecognition.technique.TechniqueDateGetter;
import tud.iir.daterecognition.technique.TechniqueDateRater;
import tud.iir.daterecognition.technique.testtechniques.TestHeadDateRater;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
import tud.iir.web.Crawler;

public class HeaderEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//DataSetHandler.addCloumn(EvaluationHelper.HEADEVAL, "mod2", "INT", "-10");
		
		TechniqueDateGetter<HeadDate> dg = new HeadDateGetter();
		TechniqueDateRater<HeadDate> dr = new HeadDateRater(PageDateType.publish);
		
		TestHeadDateRater testDR = new TestHeadDateRater(PageDateType.publish);
		
		//evaluate("pub1", DBExport.PUB_DATE, dg, dr);
		//evaluate("mod0", DBExport.MOD_DATE, dg, dr);
		
		
		HashMap<Byte, Integer[]> parameter = new HashMap<Byte, Integer[]>();
		//parameter.put(testDR.MOD_DATE_PARAMETER, null);
		//Integer[] measure = {DateComparator.MEASURE_MIN};
		//parameter.put(testDR.MEASURE_PARAMETER, measure);
		/*parameter.put(testDR.YOUNGEST_PARAMERT, null);
		testDR.setParameter(parameter);
		evaluate("mod2", DBExport.MOD_DATE, dg, testDR);
		String[] round = {"mod0", "mod1", "mod2"};
		EvaluationHelper.calculateOutput(round, EvaluationHelper.HEADEVAL);
		*/
		System.out.println("pub:");
		double ci = EvaluationHelper.calculateCI(EvaluationHelper.HEADEVAL, "pub0", DataSetHandler.TP, 350, false);
		System.out.println("CI: " + ci);
		System.out.println("Sample Size: " + EvaluationHelper.calculateSampleSize(ci));
		System.out.println("mod:");
		ci = EvaluationHelper.calculateCI(EvaluationHelper.HEADEVAL, "mod0", DataSetHandler.TP, 350, true);
		System.out.println("CI: " + ci);
		System.out.println("Sample Size: " + EvaluationHelper.calculateSampleSize(ci));
	}

	private static <T> void evaluate(String round,int pub_mod, TechniqueDateGetter<T> dg, TechniqueDateRater<T> dr){
		Evaluator.evaluate(EvaluationHelper.HEADEVAL, round, pub_mod, dg, dr);
		
		/*int truePositiv = 0;
		int trueNegative = 0;
		int falsePositv = 0;
		int falseNegativ = 0;
		int counter=0;
		int compare;
		HashMap<String, DBExport> set = EvaluationHelper.readFile(maxURLs);
		Crawler crawler = new Crawler();
		
			
		for(Entry<String, DBExport> e : set.entrySet()){
			//URL is local path!
			String url = e.getValue().getFilePath();
			String structDateString = "";
			HeadDate bestDate = null;
			
			System.out.println(url);
				System.out.print("get dates... ");
			hdg.setDocument(crawler.getWebDocument(url));
			ArrayList<HeadDate> headDates = hdg.getDates();
			headDates = DateArrayHelper.filter(headDates, DateArrayHelper.FILTER_FULL_DATE);
			headDates = DateArrayHelper.filter(headDates, DateArrayHelper.FILTER_IS_IN_RANGE);
			if(headDates.size() > 0){
				System.out.print("rate...");
				HashMap<HeadDate, Double> headDateMap = hdr.rate(headDates);
				double rate = DateArrayHelper.getHighestRate(headDateMap);
				headDates = DateArrayHelper.getRatedDates(headDateMap, rate);
				if(headDates.size() > 1){
					DateComparator dc = new DateComparator();
					if(pub_mod == DBExport.PUB_DATE){
						bestDate = dc.getOldestDate(headDates);
					}else{
						bestDate = dc.getYoungestDate(headDates);
					}
				}else{
					bestDate = headDates.get(0);
				}
			
			}
				
			if(bestDate != null){
				structDateString = bestDate.getNormalizedDate(true);
			}
				System.out.println("compare...");
			compare = EvaluationHelper.compareDate(bestDate, e.getValue(), pub_mod);
				System.out.print(compare + " headDate: " + structDateString + " - " + pub_mod + ": " + e.getValue().get(pub_mod));
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
			DataSetHandler.writeInDB(EvaluationHelper.HEADEVAL,e.getValue().get(DBExport.URL), compare, round);
			counter++;
			System.out.println();
			System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv);
			System.out.println("---------------------------------------------------------------------");
		}
		System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv);
		*/
	}	

}
