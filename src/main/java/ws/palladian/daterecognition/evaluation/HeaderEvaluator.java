package ws.palladian.daterecognition.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.HTTPDate;
import ws.palladian.daterecognition.dates.HeadDate;
import ws.palladian.daterecognition.searchengine.DBExport;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.daterecognition.technique.HeadDateGetter;
import ws.palladian.daterecognition.technique.HeadDateRater;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.daterecognition.technique.TechniqueDateGetter;
import ws.palladian.daterecognition.technique.TechniqueDateRater;
import ws.palladian.web.Crawler;

public class HeaderEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//DataSetHandler.addCloumn(EvaluationHelper.HEADEVAL, "mod2", "INT", "-10");
		
		TechniqueDateGetter<HeadDate> dg = new HeadDateGetter();
		TechniqueDateRater<HeadDate> dr = new HeadDateRater(PageDateType.publish);
		
		//TestHeadDateRater testDR = new TestHeadDateRater(PageDateType.publish);
		
		String file = "data/evaluation/daterecognition/datasets/headdataset.txt";
		evaluate("pub1", DBExport.PUB_DATE, dg, dr, file);
		evaluate("mod1", DBExport.MOD_DATE, dg, dr, file);
		
		
		/*
		String in1 = "data/evaluation/daterecognition/datasets/urldataset.txt";
		String in2 = "data/evaluation/daterecognition/datasets/headdataset3.txt";
		mergeUrlsets(in1, in2, file);
		*/
		
		//HashMap<Byte, Integer[]> parameter = new HashMap<Byte, Integer[]>();
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
			if(dates.size() != 0){
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
					if(!dates.isEmpty()){
						System.out.println("+");
						headSet.add(line);
					}
				}
				
				lineindex++;
			}
			br.close();
			fr.close();
		}catch(IOException e){
			
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
			String separator = DataSetHandler.SEPARATOR;
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
