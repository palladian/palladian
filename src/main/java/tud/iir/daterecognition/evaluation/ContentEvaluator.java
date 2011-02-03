package tud.iir.daterecognition.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.searchengine.DBExport;
import tud.iir.daterecognition.searchengine.DataSetHandler;
import tud.iir.daterecognition.technique.ContentDateGetter;
import tud.iir.daterecognition.technique.ContentDateRater;
import tud.iir.daterecognition.technique.PageDateType;
import tud.iir.daterecognition.technique.TechniqueDateGetter;
import tud.iir.daterecognition.technique.TechniqueDateRater;
import tud.iir.helper.ContentDateComparator;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
import tud.iir.web.Crawler;

public class ContentEvaluator {

	

	
	public static void main(String[] args){
		
		TechniqueDateGetter<ContentDate> dg = new ContentDateGetter();
		TechniqueDateRater<ContentDate> pub_dr = new ContentDateRater(PageDateType.publish);
		TechniqueDateRater<ContentDate> mod_dr = new ContentDateRater(PageDateType.last_modified);
		
		String file = "data/evaluation/daterecognition/datasets/dataset.txt";
		evaluate("pub0",DBExport.PUB_DATE, dg, pub_dr, file);
		evaluate("mod0",DBExport.MOD_DATE, dg, mod_dr, file);
		
		
		
		//EvaluationHelper.calculateOutput(0, EvaluationHelper.CONTENTEVAL);
		System.out.println("pub");
		System.out.println("RF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.CONTENTEVAL, DataSetHandler.RF));
		System.out.println("RNF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.CONTENTEVAL, DataSetHandler.RNF));
		System.out.println("WF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.CONTENTEVAL, DataSetHandler.WF));
		System.out.println("WNF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.CONTENTEVAL, DataSetHandler.WNF));
		System.out.println("FF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.CONTENTEVAL, DataSetHandler.FF));
				
		System.out.println("mod");
		System.out.println("RF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.CONTENTEVAL, DataSetHandler.RF));
		System.out.println("RNF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.CONTENTEVAL, DataSetHandler.RNF));
		System.out.println("WF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.CONTENTEVAL, DataSetHandler.WF));
		System.out.println("WNF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.CONTENTEVAL, DataSetHandler.WNF));
		System.out.println("FF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.CONTENTEVAL, DataSetHandler.FF));
		
	}
	
	public static void evaluate(String round,int pub_mod, TechniqueDateGetter<ContentDate> dg, TechniqueDateRater<ContentDate> dr, String file){
		Evaluator.evaluate(EvaluationHelper.CONTENTEVAL, round, pub_mod, dg, dr,file);
		
		/*int truePositiv = 0;
		int trueNegative = 0;
		int falsePositv = 0;
		int falseNegativ = 0;
		int counter=0;
		int compare;
		
		HashMap<String, DBExport> set = EvaluationHelper.readFile();
		Crawler crawler = new Crawler();
		
		for(Entry<String, DBExport> e : set.entrySet()){
			ContentDate bestDate = null;
			String bestDateString ="";
			cdg.setDocument(crawler.getWebDocument(e.getValue().getFilePath()));
			
			System.out.println(e.getValue().getFilePath());
			System.out.print("get dates... ");
				
			ArrayList<ContentDate> list = cdg.getDates();
			ArrayList<ContentDate> filteredDates = DateArrayHelper.filter(list, DateArrayHelper.FILTER_FULL_DATE);
			filteredDates = DateArrayHelper.filter(filteredDates, DateArrayHelper.FILTER_IS_IN_RANGE);
			
			if(filteredDates.size()>0){
					
				System.out.print("rate dates... ");
				
				HashMap<ContentDate, Double> map = cdr.rate(filteredDates);
				double highestRate = DateArrayHelper.getHighestRate(map);
				System.out.print(highestRate + " ");
				HashMap<ContentDate, Double> allBestDates = DateArrayHelper.getRatedDatesMap(map, highestRate);
					
				System.out.print("best date... ");
				
				if(allBestDates.size()>1){
					allBestDates = guessRate(allBestDates);
				}
				
				highestRate = DateArrayHelper.getHighestRate(allBestDates);
				allBestDates = DateArrayHelper.getRatedDatesMap(allBestDates, highestRate);
				bestDate = DateArrayHelper.getFirstElement(allBestDates);	
				bestDateString = bestDate.getNormalizedDate(true);
			}
			
			System.out.println("compare...");
			
			compare = EvaluationHelper.compareDate(bestDate, e.getValue(),pub_mod);
			System.out.print(compare + " bestDate:" + bestDateString + " - pubDate:" + e.getValue().getPubDate());
			switch(compare){
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
			
			DataSetHandler.writeInDB(EvaluationHelper.CONTENTEVAL, e.getValue().getUrl(), compare, round);
			counter++;
			
			System.out.println();
			System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv);
			System.out.println("---------------------------------------------------------------------");
		}
		System.out.println("all: " + counter + " FN: " + falseNegativ + " FP: " + falsePositv + " TN: " + trueNegative + " TP: " + truePositiv);
		*/
	}
	
	private static HashMap<ContentDate, Double> guessRate(HashMap<ContentDate, Double> dates) {
        HashMap<ContentDate, Double> result = dates;
        if (result.size() > 0 && result != null) {
            ArrayList<ContentDate> orderAge = DateArrayHelper.hashMapToArrayList(dates);
            ArrayList<ContentDate> orderPosInDoc = orderAge;

            DateComparator dc = new DateComparator();
            Collections.sort(orderAge, dc);
            Collections.sort(orderPosInDoc, new ContentDateComparator());

            double factorAge;
            double factorPos;
            double factorRate;
            double newRate;
            double oldRate;

            int ageSize = orderAge.size();
            int maxPos = orderPosInDoc.get(orderPosInDoc.size() - 1).get(ContentDate.DATEPOS_IN_DOC);
            int counter = 0;

            ContentDate temp = orderAge.get(0);
            ContentDate actDate;

            for (int i = 0; i < ageSize; i++) {
                actDate = orderAge.get(i);
                if (dc.compare(temp, actDate) != 0) {
                    temp = orderAge.get(i);
                    counter++;
                }
                factorAge = ((ageSize - counter) * 1.0) / (ageSize * 1.0);
                factorPos = 1 - ((actDate.get(ContentDate.DATEPOS_IN_DOC) * 1.0) / (maxPos * 1.0));
                factorPos += 0.01;
                factorRate = factorAge * factorPos;
                oldRate = dates.get(actDate);
                newRate = oldRate + (1 - oldRate) * factorRate;
                result.put(actDate, Math.round(newRate * 10000) / 10000.0);
            }
        }
        normalizeRate(result);
        return result;
    }
	
	 /**
     * If some rates are greater then one, use this method to normalize them.
     * 
     * @param <T>
     * @param dates
     */
    private static <T> void normalizeRate(HashMap<T, Double> dates) {
        double highestRate = DateArrayHelper.getHighestRate(dates);
        if (highestRate > 1.0) {
            for (Entry<T, Double> e : dates.entrySet()) {
                dates.put(e.getKey(), Math.round((e.getValue() / highestRate) * 10000) / 10000.0);
            }
        }

    }
}
