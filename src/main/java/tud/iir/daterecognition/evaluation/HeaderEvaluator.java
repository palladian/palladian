package tud.iir.daterecognition.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HTTPDate;
import tud.iir.daterecognition.dates.HeadDate;
import tud.iir.daterecognition.dates.StructureDate;
import tud.iir.daterecognition.dates.URLDate;
import tud.iir.daterecognition.searchengine.DBExport;
import tud.iir.daterecognition.searchengine.DataSetHandler;
import tud.iir.daterecognition.technique.HTTPDateGetter;
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
		
		String file = "data/evaluation/daterecognition/datasets/headdataset.txt";
		evaluate("pub1", DBExport.PUB_DATE, dg, dr, file);
		evaluate("mod1", DBExport.MOD_DATE, dg, dr, file);
		
		
		/*
		String in1 = "data/evaluation/daterecognition/datasets/urldataset.txt";
		String in2 = "data/evaluation/daterecognition/datasets/headdataset3.txt";
		mergeUrlsets(in1, in2, file);
		*/
		
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
		
		
		/*
		String in = "D:/_Uni/_semester16/dataset/urlSet07.htm";
		String out = "D:/_Uni/_semester16/dataset/HeadUrlSet.htm";
		//countHeadURls(in, out, (HeadDateGetter)dg);
		createHeadUrlList(in, out, (HeadDateGetter)dg);
		*/
		
		String pub = "pub1";
		System.out.println(pub);
		System.out.println("RF: " + EvaluationHelper.count(file, pub, EvaluationHelper.HEADEVAL, DataSetHandler.RF));
		System.out.println("RNF: " + EvaluationHelper.count(file, pub, EvaluationHelper.HEADEVAL, DataSetHandler.RNF));
		System.out.println("WF: " + EvaluationHelper.count(file, pub, EvaluationHelper.HEADEVAL, DataSetHandler.WF));
		System.out.println("WNF: " + EvaluationHelper.count(file, pub, EvaluationHelper.HEADEVAL, DataSetHandler.WNF));
		System.out.println("FF: " + EvaluationHelper.count(file, pub, EvaluationHelper.HEADEVAL, DataSetHandler.FF));
				
		String mod = "mod1";
		System.out.println(mod);
		System.out.println("RF: " + EvaluationHelper.count(file, mod, EvaluationHelper.HEADEVAL, DataSetHandler.RF));
		System.out.println("RNF: " + EvaluationHelper.count(file, mod, EvaluationHelper.HEADEVAL, DataSetHandler.RNF));
		System.out.println("WF: " + EvaluationHelper.count(file, mod, EvaluationHelper.HEADEVAL, DataSetHandler.WF));
		System.out.println("WNF: " + EvaluationHelper.count(file, mod, EvaluationHelper.HEADEVAL, DataSetHandler.WNF));
		System.out.println("FF: " + EvaluationHelper.count(file, mod, EvaluationHelper.HEADEVAL, DataSetHandler.FF));
		
	}

	private static <T> void evaluate(String round,int pub_mod, TechniqueDateGetter<T> dg, TechniqueDateRater<T> dr, String file){
		Evaluator.evaluate(EvaluationHelper.HEADEVAL, round, pub_mod, dg, dr, file);
		
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

	
	private static void countHeadURls(String in, String out, HeadDateGetter dg){
		HashMap<String, DBExport> set = EvaluationHelper.readFile(in);
		ArrayList<DBExport> headSet = new ArrayList<DBExport>();
		Crawler c = new Crawler();
		int index=0;
		for(Entry<String, DBExport> e : set.entrySet()){
			System.out.println(index + ": " + e.getKey());
			dg.setDocument(c.getWebDocument(e.getValue().get(DBExport.PATH)));
			ArrayList<HeadDate> dates = dg.getDates();
			if(dates.size() > 0){
				headSet.add(e.getValue());
			}
			index++;
		}
		
		try{
			File file = new File(out);
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("url *;_;* path *;_;* pub_date *;_;* pub_sureness *;_;* mod_date *;_;* mod_sureness *;_;* google_date *;_;* hakia_date *;_;* ask_date *;_;* header_last_mod *;_;* header_date *;_;* down_date \n");
			for(int i=0; i< headSet.size(); i++){
				bw.write(headSet.get(i).toString() + "\n");
			}
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private static void createHeadUrlList(String in, String out, HeadDateGetter dg){
		ArrayList<String> headSet = new ArrayList<String>();
		try{
			
			File file = new File(in);
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line;
			Crawler c = new Crawler();
			int lineindex=0;
			while((line=br.readLine())!=null){
				if(lineindex>1){
					int indexStart = line.indexOf('>');
					int indexEnd = line.indexOf("</a>");
					String url = line.substring(indexStart + 1, indexEnd);
					
					System.out.println(lineindex + ": " + url);
					dg.setDocument(c.getWebDocument(url));
					ArrayList<HTTPDate> dates = dg.getDates();
					if(dates.size() > 0){
						System.out.println("+");
						headSet.add(line);
					}
				}
				
				lineindex++;
			}
			br.close();
			fr.close();
		}catch(Exception e){
			
		}
		
		try{
			File file = new File(out);
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(int i=0; i<headSet.size(); i++){
				bw.write(headSet.get(i)+ "\n");
			}
			
			bw.close();
			fw.close();
		}catch(Exception e){
			
		}
	}
	
	private static void mergeUrlsets(String in1, String in2, String out){
			
			HashMap<String, DBExport> set1 = EvaluationHelper.readFile(in1);
			HashMap<String, DBExport> set2 = EvaluationHelper.readFile(in2);
			System.out.println(set1.size());
			System.out.println(set2.size());
			HashMap<String, DBExport> merged = new HashMap<String, DBExport>();
			merged.putAll(set1);
			merged.putAll(set2);
			String separator = DataSetHandler.separator;
			File file = new File(out);
			try{
				FileWriter outw = new FileWriter(file, false);
				BufferedWriter bw = new BufferedWriter(outw);
				bw.write("url *;_;* path *;_;* pub_date *;_;* pub_sureness *;_;* mod_date *;_;* mod_sureness *;_;* google_date *;_;* hakia_date *;_;* ask_date *;_;* header_last_mod *;_;* header_date *;_;* down_date \n");
				Crawler c = new Crawler();
				for(Entry<String, DBExport>e : merged.entrySet()){
					HeadDateGetter dg = new HeadDateGetter();
					
					dg.setDocument(c.getWebDocument(e.getValue().get(DBExport.PATH)));
					ArrayList<ExtractedDate> headDates = dg.getDates();
					System.out.println(headDates.size());					
					if(headDates.size()>0){
					
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
