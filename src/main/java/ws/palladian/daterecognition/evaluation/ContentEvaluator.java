package ws.palladian.daterecognition.evaluation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.dates.MetaDate;
import ws.palladian.daterecognition.dates.URLDate;
import ws.palladian.daterecognition.searchengine.DBExport;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.daterecognition.technique.ContentDateGetter;
import ws.palladian.daterecognition.technique.ContentDateRater;
import ws.palladian.daterecognition.technique.ContentDateRater_old;
import ws.palladian.daterecognition.technique.MetaDateGetter;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.daterecognition.technique.TechniqueDateGetter;
import ws.palladian.daterecognition.technique.TechniqueDateRater;
import ws.palladian.daterecognition.technique.URLDateGetter;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.ContentDateComparator;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.helper.date.DateComparator;
import ws.palladian.retrieval.DocumentRetriever;

public class ContentEvaluator {

	

	
	public static void main(String[] args){
		
		TechniqueDateGetter<ContentDate> dg = new ContentDateGetter();
		TechniqueDateRater<ContentDate> pub_dr = new ContentDateRater_old(PageDateType.publish);
		TechniqueDateRater<ContentDate> mod_dr = new ContentDateRater_old(PageDateType.last_modified);
		
		String file = "data/evaluation/daterecognition/datasets/dataset.txt";
		String pub = "pub6";
		String mod = "mod6";
//		evaluate(pub, DBExport.PUB_DATE, dg, pub_dr, file);
//		evaluate(mod, DBExport.MOD_DATE, dg, mod_dr, file);
		
		
		
		//EvaluationHelper.calculateOutput(0, EvaluationHelper.CONTENTEVAL);
		
		System.out.println(pub);
		System.out.println("AFR: " + EvaluationHelper.count(file, pub, EvaluationHelper.CONTENTEVAL, DataSetHandler.AFR));
		System.out.println("ADR: " + EvaluationHelper.count(file, pub, EvaluationHelper.CONTENTEVAL, DataSetHandler.ARD));
		System.out.println("AFW: " + EvaluationHelper.count(file, pub, EvaluationHelper.CONTENTEVAL, DataSetHandler.AFW));
		System.out.println("ANF: " + EvaluationHelper.count(file, pub, EvaluationHelper.CONTENTEVAL, DataSetHandler.ANF));
		System.out.println("ADW: " + EvaluationHelper.count(file, pub, EvaluationHelper.CONTENTEVAL, DataSetHandler.AWD));
				
		
		System.out.println(mod);
		System.out.println("AFR: " + EvaluationHelper.count(file, mod, EvaluationHelper.CONTENTEVAL, DataSetHandler.AFR));
		System.out.println("ADR: " + EvaluationHelper.count(file, mod, EvaluationHelper.CONTENTEVAL, DataSetHandler.ARD));
		System.out.println("AFW: " + EvaluationHelper.count(file, mod, EvaluationHelper.CONTENTEVAL, DataSetHandler.AFW));
		System.out.println("ANF: " + EvaluationHelper.count(file, mod, EvaluationHelper.CONTENTEVAL, DataSetHandler.ANF));
		System.out.println("ADW: " + EvaluationHelper.count(file, mod, EvaluationHelper.CONTENTEVAL, DataSetHandler.AWD));
		
		String contentTable = "contentfactorFinal";
		boolean useWeight = false;
		evluateFacotors((ContentDateGetter) dg, new MetaDateGetter(), new URLDateGetter(), file, contentTable, useWeight);
		
	}
	
	public static void evaluate(String round,int pub_mod, TechniqueDateGetter<ContentDate> dg, TechniqueDateRater<ContentDate> dr, String file){
		Evaluator.evaluate(EvaluationHelper.CONTENTEVAL, round, pub_mod, dg, dr,file);
	}
	
	private static void evluateFacotors(ContentDateGetter cdg,MetaDateGetter mdg, URLDateGetter udg, String file, String contentTable, boolean useWeight){
		HashMap<String, DBExport> map = EvaluationHelper.readFile(file);
		StopWatch allTimer = new StopWatch();
		int i=0;
		
		DataSetHandler.openConnection();
		String sqlString ="Select * From " + contentTable;
		
		HashMap<String, Boolean> alreadyAnalysed = new HashMap<String, Boolean>();
		
		try{
			ResultSet rs = DataSetHandler.st.executeQuery(sqlString);
			while(rs.next()){
				alreadyAnalysed.put(rs.getString("url"),true);
			}
		}catch (SQLException e){
			
		}

		DataSetHandler.closeConnection();
		
		
		for(Entry<String, DBExport>e : map.entrySet()){
			
			
			StopWatch timer = new StopWatch();
			DocumentRetriever crawler = new DocumentRetriever();
			Document document = crawler.getWebDocument(e.getValue().get(DBExport.PATH));
			String url = e.getValue().get(DBExport.URL);
			if(alreadyAnalysed.get(url) != null){
				continue;
			}
			
			System.out.println(url);
			
			cdg.reset();
			
			cdg.setDocument(document);
			mdg.setDocument(document);
			mdg.setUrl(url);
			udg.setUrl(url);
			
			ArrayList<ContentDate> contDates = cdg.getDates();
			
			contDates = DateArrayHelper.filter(contDates, DateArrayHelper.FILTER_IS_IN_RANGE);
			contDates = DateArrayHelper.filter(contDates, DateArrayHelper.FILTER_FULL_DATE);
			
			ArrayList<MetaDate> metaDates = DateArrayHelper.removeNull(mdg.getDates());
			ArrayList<URLDate> urlDates = DateArrayHelper.removeNull(udg.getDates());
			
			for(ContentDate date : contDates){
				if(metaDates.size() > 0 && DateArrayHelper.countDates(date, metaDates, DateComparator.STOP_DAY) > 0){
					date.setInMetaDates(true);
				}
				if(urlDates.size() > 0 && DateArrayHelper.countDates(date, urlDates, DateComparator.STOP_DAY) > 0){
					date.setInUrl(true);
				}
			}
			DataSetHandler.writeDateFactors(contDates, url, cdg.getDoc(), contentTable, useWeight);
			timer.stop();
			System.out.print(i++ + ": ");
			timer.getElapsedTimeString(true);
			allTimer.getElapsedTimeString(true);
			
		}
		allTimer.stop();
		allTimer.getElapsedTimeString(true);
		
	}
	
	private static HashMap<ContentDate, Double> guessRate(HashMap<ContentDate, Double> dates) {
        HashMap<ContentDate, Double> result = dates;
        if (result.size() > 0) {
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
