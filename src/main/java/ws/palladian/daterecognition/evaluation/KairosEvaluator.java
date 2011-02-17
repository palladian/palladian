package ws.palladian.daterecognition.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import ws.palladian.daterecognition.DateConverter;
import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.WebPageDateEvaluator;
import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.HTTPDate;
import ws.palladian.daterecognition.searchengine.DBExport;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.daterecognition.technique.ContentDateGetter;
import ws.palladian.daterecognition.technique.ContentDateRater;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.daterecognition.technique.TechniqueDateGetter;
import ws.palladian.daterecognition.technique.TechniqueDateRater;
import ws.palladian.daterecognition.technique.URLDateGetter;
import ws.palladian.daterecognition.technique.testtechniques.WebPageDateEvaluatorTest;
import ws.palladian.helper.DateArrayHelper;
import ws.palladian.helper.DateHelper;
import ws.palladian.web.Crawler;

public class KairosEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TechniqueDateGetter<ContentDate> dg = new ContentDateGetter();
		TechniqueDateRater<ContentDate> pub_dr = new ContentDateRater(PageDateType.publish);
		TechniqueDateRater<ContentDate> mod_dr = new ContentDateRater(PageDateType.last_modified);
		
		String file = "data/evaluation/daterecognition/datasets/dataset.txt";
		evaluate(EvaluationHelper.KAIROSEVAL, "pub0",PageDateType.publish, dg, pub_dr, file);
		evaluate(EvaluationHelper.KAIROSEVAL, "mod0",PageDateType.last_modified, dg, mod_dr, file);
		
		
		
		//EvaluationHelper.calculateOutput(0, EvaluationHelper.CONTENTEVAL);
		System.out.println("pub");
		System.out.println("RF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.KAIROSEVAL, DataSetHandler.RF));
		System.out.println("RNF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.KAIROSEVAL, DataSetHandler.RNF));
		System.out.println("WF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.KAIROSEVAL, DataSetHandler.WF));
		System.out.println("WNF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.KAIROSEVAL, DataSetHandler.WNF));
		System.out.println("FF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.KAIROSEVAL, DataSetHandler.FF));
				
		System.out.println("mod");
		System.out.println("RF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.KAIROSEVAL, DataSetHandler.RF));
		System.out.println("RNF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.KAIROSEVAL, DataSetHandler.RNF));
		System.out.println("WF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.KAIROSEVAL, DataSetHandler.WF));
		System.out.println("WNF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.KAIROSEVAL, DataSetHandler.WNF));
		System.out.println("FF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.KAIROSEVAL, DataSetHandler.FF));
		
	}
	
	public static <T> void evaluate(String table, String round,PageDateType pub_mod, TechniqueDateGetter<ContentDate> dg, TechniqueDateRater<ContentDate> dr, String file){
		int rnf = 0;
		int ff= 0;
		int wnf= 0;
		int rf= 0;
		int wf = 0;
		int counter=0;
		int compare;
		
		HashMap<String, DBExport> set = EvaluationHelper.readFile(file);
		Crawler crawler = new Crawler();
		
		for(Entry<String, DBExport> e : set.entrySet()){
			String url =e.getValue().get(DBExport.URL);
			String path = e.getValue().get(DBExport.PATH);
			Document document = crawler.getWebDocument(path);
			System.out.println(url);
			
			WebPageDateEvaluatorTest wp = new WebPageDateEvaluatorTest();
			wp.setUrl(url);
			wp.setDocument(document);
			wp.setPubMod(pub_mod);
			wp.setHttpDates(getHttpDates(e.getValue()));
			wp.setActualDate(getDownloadedDate(e.getValue()));
			wp.evaluate();
			T bestDate = (T) wp.getBestRatedDate();
			
			String bestDateString ="";
			
			System.out.print("get dates... ");
			bestDateString = ((ExtractedDate) bestDate).getNormalizedDate(true);
				
			System.out.println("compare...");
			int pub_mod_int;
			if(PageDateType.publish == pub_mod){
				pub_mod_int = DBExport.PUB_DATE;
			}else{
				pub_mod_int = DBExport.MOD_DATE;
			}
			compare = EvaluationHelper.compareDate(bestDate, e.getValue(),pub_mod_int);
			ExtractedDate date;
			String dbExportDateString;
			if(pub_mod_int == DBExport.PUB_DATE){
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
				case DataSetHandler.WF:
					wf++;
					break;
				case DataSetHandler.WNF:
					wnf++;
					break;
				case DataSetHandler.FF:
					ff++;
					break;
				case DataSetHandler.RNF:
					rnf++;
					break;
				case DataSetHandler.RF:
					rf++;
					break;
					
			}
			
			DataSetHandler.writeInDB(table, e.getValue().getUrl(), compare, round);
			counter++;
			
			System.out.println();
			System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
			System.out.println("---------------------------------------------------------------------");
		}
		System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
		
	}
	
	private static ArrayList<HTTPDate> getHttpDates(DBExport dbExport){
		ArrayList<HTTPDate> dates = new ArrayList<HTTPDate>();
		String headerDate = dbExport.get(DBExport.HEADER_DATE);
		String headerLastMod = dbExport.get(DBExport.HEADER_LAST);
		
		ExtractedDate headerExtrDate = DateGetterHelper.findDate(headerDate);
		HTTPDate headerHttpDate = DateConverter.convert(headerExtrDate, DateConverter.TECH_HTTP_HEADER);
		if(headerHttpDate != null){
			headerHttpDate.setKeyword("date");
		}
		ExtractedDate headerExtrLastMod = DateGetterHelper.findDate(headerLastMod);
		HTTPDate headerHttpLastMod = DateConverter.convert(headerExtrLastMod, DateConverter.TECH_HTTP_HEADER);
		if(headerHttpLastMod != null){
			headerHttpLastMod.setKeyword("last-modified");
		}
		dates.add(headerHttpDate);
		dates.add(headerHttpLastMod);
		
		
		return dates;
		
	}
	
	private static ExtractedDate getDownloadedDate(DBExport dbExport){
		return DateGetterHelper.findDate(dbExport.get(DBExport.ACTUAL_DATE));
	}
}
