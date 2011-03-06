package ws.palladian.daterecognition.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import edu.umass.cs.mallet.projects.seg_plus_coref.coreference.PublisherPipe;

import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.HTTPDate;
import ws.palladian.daterecognition.dates.URLDate;
import ws.palladian.daterecognition.searchengine.DBExport;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.daterecognition.technique.HTTPDateGetter;
import ws.palladian.daterecognition.technique.HttpDateRater;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.daterecognition.technique.TechniqueDateGetter;
import ws.palladian.daterecognition.technique.TechniqueDateRater;
import ws.palladian.daterecognition.technique.URLDateGetter;
import ws.palladian.daterecognition.technique.UrlDateRater;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.helper.date.DateComparator;
import ws.palladian.helper.date.DateHelper;

public class UrlEvaluator {

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TechniqueDateGetter<URLDate> dg = new URLDateGetter();
		TechniqueDateRater<URLDate> dr = new UrlDateRater(PageDateType.publish);
		
		String file = "data/evaluation/daterecognition/datasets/urldataset.txt";

		//evaluate("pub0",DBExport.PUB_DATE, dg, dr, file);
		//evaluate("mod0",DBExport.MOD_DATE, dg, dr, file);
		
	//	EvaluationHelper.calculateOutput(0, EvaluationHelper.URLEVAL);
		
		//Integer[] list = {50, 100, 150, 200, 250, 300, 350};
		//EvaluationHelper.calculateConfidenceInterval(EvaluationHelper.URLEVAL, "mod0", list);
		
		/*
		System.out.println(EvaluationHelper.count("pub0", EvaluationHelper.URLEVAL, 350, DataSetHandler.TP));
		
		System.out.println("pub:");
		double ci = EvaluationHelper.calculateCI(EvaluationHelper.URLEVAL, "pub0", DataSetHandler.TP, 150, false);
		System.out.println("CI: " + ci);
		System.out.println("Sample Size: " + EvaluationHelper.calculateSampleSize(ci));
		System.out.println("mod:");
		ci = EvaluationHelper.calculateCI(EvaluationHelper.URLEVAL, "mod0", DataSetHandler.TP, 150, false);
		System.out.println("CI: " + ci);
		System.out.println("Sample Size: " + EvaluationHelper.calculateSampleSize(ci));
		*/
		
		//compareUrlDateFoundDate(PageDateType.publish,"data/evaluation/dateextraction/urldataset.txt");
		
		/*
		String in1 = "data/evaluation/dateextraction/dataset.txt";
		String in2= "data/evaluation/dateextraction/urldataset_old.txt";
		String out= "data/evaluation/dateextraction/urldataset.txt";
		mergeUrlsets(in1, in2, out);
		*/
		System.out.println("pub");
		System.out.println("RF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.URLEVAL, DataSetHandler.RF));
		System.out.println("RNF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.URLEVAL, DataSetHandler.RNF));
		System.out.println("WF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.URLEVAL, DataSetHandler.WF));
		System.out.println("WNF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.URLEVAL, DataSetHandler.WNF));
		System.out.println("FF: " + EvaluationHelper.count(file, "pub0", EvaluationHelper.URLEVAL, DataSetHandler.FF));
				
		System.out.println("mod");
		System.out.println("RF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.URLEVAL, DataSetHandler.RF));
		System.out.println("RNF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.URLEVAL, DataSetHandler.RNF));
		System.out.println("WF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.URLEVAL, DataSetHandler.WF));
		System.out.println("WNF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.URLEVAL, DataSetHandler.WNF));
		System.out.println("FF: " + EvaluationHelper.count(file, "mod0", EvaluationHelper.URLEVAL, DataSetHandler.FF));
		
		
	}
	
	private static <T> void  evaluate(String round,int pub_mod, TechniqueDateGetter<T> dg, TechniqueDateRater<T> dr, String file){
		
		Evaluator.evaluate(EvaluationHelper.URLEVAL, round, pub_mod, dg, dr, file);
		
	}
	
	private static void countUrlsWithDate(String file){
		HashMap<String, DBExport> set =  EvaluationHelper.readFile(file);
		URLDateGetter dg = new URLDateGetter();
		int count =0;
		for(Entry<String, DBExport> e : set.entrySet()){
			if(dg.getFirstDate(e.getKey()) != null){
				System.out.println(e.getKey());
				count++;
			}
		}
		System.out.println(count);
	}
	
	private static void compareUrlDateFoundDate(PageDateType pub_mod, String file){
		
		HashMap<String, DBExport> set =  EvaluationHelper.readFile(file);
		URLDateGetter dg = new URLDateGetter();
		DateComparator dc = new DateComparator();
		int countAll = 0;
		int countTP = 0;
		int countFN = 0;
		for(Entry<String, DBExport> e : set.entrySet()){
			ExtractedDate urlDate = dg.getFirstDate(e.getKey());
			if(urlDate != null){
				ExtractedDate foundDate;
				if(pub_mod == PageDateType.publish){
					foundDate = DateGetterHelper.findDate(e.getValue().get(DBExport.PUB_DATE));
				}else{
					foundDate = DateGetterHelper.findDate(e.getValue().get(DBExport.MOD_DATE));
				}
				if(foundDate != null){
					int compare = dc.compare(urlDate, foundDate, dc.getCompareDepth(urlDate, foundDate));
					if(compare == 0){
						countTP++;
					}else{
						countFN++;
						System.out.println(e.getKey());
						System.out.println("urlDate: " + urlDate.getNormalizedDateString() + " foundDate: " + foundDate.getNormalizedDateString());
					}
				}
				
				countAll++;
			}
		}
		System.out.println("countAll: " + countAll + " countTP: " + countTP + " countFN: " + countFN);
	}
	

	private static void mergeUrlsets(String in1, String in2, String out){
		
		HashMap<String, DBExport> set1 = EvaluationHelper.readFile(in1);
		HashMap<String, DBExport> set2 = EvaluationHelper.readFile(in2);
		
		HashMap<String, DBExport> merged = new HashMap<String, DBExport>();
		merged.putAll(set1);
		merged.putAll(set2);
		String separator = DataSetHandler.separator;
		File file = new File(out);
		try{
			FileWriter outw = new FileWriter(file, false);
			BufferedWriter bw = new BufferedWriter(outw);
			URLDateGetter dg = new URLDateGetter();
			bw.write("url *;_;* path *;_;* pub_date *;_;* pub_sureness *;_;* mod_date *;_;* mod_sureness *;_;* google_date *;_;* hakia_date *;_;* ask_date *;_;* header_last_mod *;_;* header_date *;_;* down_date");
			for(Entry<String, DBExport>e : merged.entrySet()){
				if(dg.getFirstDate(e.getKey()) != null){
					String write = e.getValue().getUrl() + separator
					+ e.getValue().getFilePath() + separator
					+ e.getValue().getPubDate() + separator
					+ String.valueOf(e.getValue().isPubSureness()) + separator
					+ e.getValue().getModDate() + separator
					+ String.valueOf(e.getValue().isModSureness()) + separator
					+ e.getValue().getGoogleDate() + separator
					+ e.getValue().getHakiaDate() + separator
					+ e.getValue().getAskDate() + separator
					+ e.getValue().getLastModDate() + separator
					+ e.getValue().getDateDate() + separator
					+ e.getValue().getActDate();
					bw.write(write + "\n");
				
					System.out.println(write);
				}
			}	
			bw.close();
			outw.close();
		} catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	
	
}
