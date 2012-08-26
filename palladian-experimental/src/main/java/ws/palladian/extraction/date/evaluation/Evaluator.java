package ws.palladian.extraction.date.evaluation;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.extraction.date.getter.TechniqueDateGetter;
import ws.palladian.extraction.date.getter.UrlDateGetter;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.extraction.date.rater.TechniqueDateRater;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.retrieval.DocumentRetriever;

public abstract class Evaluator {
	
	public static <T extends ExtractedDate> void evaluate(int pub_mod, TechniqueDateGetter<T> dg, TechniqueDateRater<T> dr, String file){
		int rnf = 0;
		int ff= 0;
		int wnf= 0;
		int rf= 0;
		int wf = 0;
		int counter=0;
		int compare;
		
		Map<String, DBExport> set = EvaluationHelper.readFile(file);
		DocumentRetriever crawler = new DocumentRetriever();
		
		for(Entry<String, DBExport> e : set.entrySet()){
			dg.reset();
			T bestDate = null;
			String bestDateString ="";
			String url =e.getValue().get(DBExport.URL);
			dg.setUrl(url);
			String path = e.getValue().get(DBExport.PATH);
			dg.setDocument(crawler.getWebDocument(path));
			
			dg.setUrl(url);
			
			System.out.print("get dates... ");
			StopWatch timer = new StopWatch();
			List<T> list = dg.getDates();
			timer.stop();
			timer.getElapsedTimeString(true);
			CollectionHelper.removeNulls(list);
			
			if(list.size() > 0){
				
				List<T> filteredDates = DateArrayHelper.filterFullDate(list);
				filteredDates = DateArrayHelper.filterByRange(filteredDates);
				
				if(dg instanceof UrlDateGetter){
					filteredDates = DateArrayHelper.filterByRange(list);
				}
				
				
				if(filteredDates.size()>0){
						
					//System.out.print("rate dates... ");
					dr.rate(filteredDates);
					//System.out.print("best date... ");
					bestDate = dr.getBestDate();
					if(bestDate != null){
						bestDateString = bestDate.getNormalizedDateString(true);
					}
				}
			}
			//System.out.println("compare...");
			
			compare = EvaluationHelper.compareDate(bestDate, e.getValue(),pub_mod);
			ExtractedDate date;
			String dbExportDateString;
			if(pub_mod == DBExport.PUB_DATE){
				date = DateParser.findDate(e.getValue().getPubDate());
				dbExportDateString =" - pubDate:" ;
			}else{
				date = DateParser.findDate(e.getValue().getModDate());
				dbExportDateString =" - modDate:" ;
			}
			
			if(date!=null){
				dbExportDateString +=  date.getNormalizedDateString();
			}
			
			//System.out.print(compare + " bestDate:" + bestDateString + dbExportDateString);
			
			switch(compare){
				case EvaluationHelper.AFW:
					wf++;
					System.out.println(url);
					System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
					//System.out.println("-------------------------------------------------------");
					break;
				case EvaluationHelper.ANF:
					System.out.println(url);
					System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
					//System.out.println("-------------------------------------------------------");
					wnf++;
					break;
				case EvaluationHelper.AWD:
					System.out.println(url);
					System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
					//System.out.println("-------------------------------------------------------");
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
			
			//System.out.println();
			System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
			System.out.println("---------------------------------------------------------------------");
			
		}
		//System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
	}
	
}
