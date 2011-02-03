package tud.iir.daterecognition.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.dates.StructureDate;
import tud.iir.daterecognition.searchengine.DBExport;
import tud.iir.daterecognition.searchengine.DataSetHandler;
import tud.iir.daterecognition.technique.ContentDateGetter;
import tud.iir.daterecognition.technique.ContentDateRater;
import tud.iir.daterecognition.technique.PageDateType;
import tud.iir.daterecognition.technique.StructureDateGetter;
import tud.iir.daterecognition.technique.StructureDateRater;
import tud.iir.daterecognition.technique.TechniqueDateGetter;
import tud.iir.daterecognition.technique.TechniqueDateRater;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
import tud.iir.web.Crawler;

public class StructureEvaluator {

	private static StructureDateGetter sdg = new StructureDateGetter();
	private static StructureDateRater sdr = new StructureDateRater(PageDateType.publish);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TechniqueDateGetter<StructureDate> dg = new StructureDateGetter();
		TechniqueDateRater<StructureDate> pub_dr = new StructureDateRater(PageDateType.publish);
		TechniqueDateRater<StructureDate> mod_dr = new StructureDateRater(PageDateType.last_modified);
		
		String file = "data/evaluation/daterecognition/datasets/dataset.txt";
		evaluate("pub0", DBExport.PUB_DATE, dg, pub_dr,file);
		evaluate("mod0", DBExport.MOD_DATE, dg, mod_dr,file);
		
		System.out.println("pub");
		System.out.println("RF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.STRUCTEVAL, DataSetHandler.RF));
		System.out.println("RNF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.STRUCTEVAL, DataSetHandler.RNF));
		System.out.println("WF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.STRUCTEVAL, DataSetHandler.WF));
		System.out.println("WNF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.STRUCTEVAL, DataSetHandler.WNF));
		System.out.println("FF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.STRUCTEVAL, DataSetHandler.FF));
				
		System.out.println("mod");
		System.out.println("RF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.STRUCTEVAL, DataSetHandler.RF));
		System.out.println("RNF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.STRUCTEVAL, DataSetHandler.RNF));
		System.out.println("WF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.STRUCTEVAL, DataSetHandler.WF));
		System.out.println("WNF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.STRUCTEVAL, DataSetHandler.WNF));
		System.out.println("FF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.STRUCTEVAL, DataSetHandler.FF));
		
	}

	public static void evaluate(String round,int pub_mod, TechniqueDateGetter<StructureDate> dg, TechniqueDateRater<StructureDate> dr, String file){
		Evaluator.evaluate(EvaluationHelper.STRUCTEVAL, round, pub_mod, dg, dr, file);
		/*
		int truePositiv = 0;
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
			StructureDate bestDate = null;
			
			System.out.println(url);
				System.out.print("get dates... ");
			sdg.setDocument(crawler.getWebDocument(url));
			ArrayList<StructureDate> structDates = sdg.getDates();
			structDates = DateArrayHelper.filter(structDates, DateArrayHelper.FILTER_FULL_DATE);
			structDates = DateArrayHelper.filter(structDates, DateArrayHelper.FILTER_IS_IN_RANGE);
			if(structDates.size() > 0){
				System.out.print("rate...");
				HashMap<StructureDate, Double> structDateMap = sdr.rate(structDates);
				double rate = DateArrayHelper.getHighestRate(structDateMap);
				structDates = DateArrayHelper.getRatedDates(structDateMap, rate);
				if(structDates.size() > 1){
					DateComparator dc = new DateComparator();
					if(pub_mod == DBExport.PUB_DATE){
						bestDate = dc.getOldestDate(structDates);
					}else{
						bestDate = dc.getYoungestDate(structDates);
					}
				}else{
					bestDate = structDates.get(0);
				}
			
			}
				
			if(bestDate != null){
				structDateString = bestDate.getNormalizedDate(true);
			}
				System.out.println("compare...");
			compare = EvaluationHelper.compareDate(bestDate, e.getValue(), pub_mod);
				System.out.print(compare + " structDate: " + structDateString + " - " + pub_mod + ": " + e.getValue().get(pub_mod));
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
			DataSetHandler.writeInDB(EvaluationHelper.STRUCTEVAL,e.getValue().get(DBExport.URL), compare, round);
			counter++;
			System.out.println();
			System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv);
			System.out.println("---------------------------------------------------------------------");
		}
		System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv);
		*/
	}	
}
