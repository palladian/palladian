package ws.palladian.daterecognition.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.searchengine.DBExport;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.daterecognition.technique.TechniqueDateGetter;
import ws.palladian.daterecognition.technique.TechniqueDateRater;
import ws.palladian.daterecognition.technique.URLDateGetter;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.web.Crawler;

public abstract class Evaluator {

	
	private ExtractedDate actualDate;
	
	public static <T> void evaluate(String table, String round,int pub_mod, TechniqueDateGetter<T> dg, TechniqueDateRater<T> dr, String file){
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
			dg.reset();
			T bestDate = null;
			String bestDateString ="";
			String url =e.getValue().get(DBExport.URL);
			dg.setUrl(url);
			//System.out.println(url);
			if(table.equalsIgnoreCase(EvaluationHelper.CONTENTEVAL) || table.equalsIgnoreCase(EvaluationHelper.STRUCTEVAL) || table.equalsIgnoreCase(EvaluationHelper.HEADEVAL)){
				String path = e.getValue().get(DBExport.PATH);
				//System.out.println(path);
				dg.setDocument(crawler.getWebDocument(path));
			}else{
				
				dg.setUrl(url);
			}
			
			System.out.print("get dates... ");
			StopWatch timer = new StopWatch();
			ArrayList<T> list = dg.getDates();
			timer.stop();
			timer.getElapsedTimeString(true);
			list = DateArrayHelper.removeNull(list);
			
			if(list.size() > 0){
				
				ArrayList<T> filteredDates = DateArrayHelper.filter(list, DateArrayHelper.FILTER_FULL_DATE);
				filteredDates = DateArrayHelper.filter(filteredDates, DateArrayHelper.FILTER_IS_IN_RANGE);
				
				if(dg instanceof URLDateGetter){
					filteredDates = DateArrayHelper.filter(list, DateArrayHelper.FILTER_IS_IN_RANGE);
				}
				
				
				if(filteredDates.size()>0){
						
					//System.out.print("rate dates... ");
					dr.rate(filteredDates);
					//System.out.print("best date... ");
					bestDate = dr.getBestDate();
					if(bestDate != null){
						bestDateString = ((ExtractedDate) bestDate).getNormalizedDate(true);
					}
				}
			}
			//System.out.println("compare...");
			
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
			
			//System.out.print(compare + " bestDate:" + bestDateString + dbExportDateString);
			
			switch(compare){
				case DataSetHandler.AFW:
					wf++;
					System.out.println(url);
					System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
					//System.out.println("-------------------------------------------------------");
					break;
				case DataSetHandler.ANF:
					System.out.println(url);
					System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
					//System.out.println("-------------------------------------------------------");
					wnf++;
					break;
				case DataSetHandler.AWD:
					System.out.println(url);
					System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
					//System.out.println("-------------------------------------------------------");
					ff++;
					break;
				case DataSetHandler.ARD:
					rnf++;
					break;
				case DataSetHandler.AFR:
					rf++;
					break;
					
			}
			
			DataSetHandler.writeInDB(table, e.getValue().getUrl(), compare, round);
			counter++;
			
			//System.out.println();
			System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
			System.out.println("---------------------------------------------------------------------");
			
		}
		//System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
	}
	
}
