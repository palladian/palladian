package ws.palladian.daterecognition.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Map.Entry;

import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.searchengine.DBExport;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.helper.collection.ArrayHelper;
import ws.palladian.helper.date.DateComparator;

public class EvaluationHelper {

	private static File file = new File("data/evaluation/daterecognition/datasets/dataset.txt");
	private static String separator = DataSetHandler.SEPARATOR;
	
	public static final String CONTENTEVAL = "contenteval";
	public static final String HTTPEVAL = "httpeval";
	public static final String URLEVAL = "urleval";
	public static final String STRUCTEVAL = "structeval";
	public static final String HEADEVAL = "headeval";
	public static final String KAIROSEVAL = "kairoseval";
	
	public static HashMap<String, DBExport> readFile(String file){
		return readFile(-1, false, file);
	}
	public static HashMap<String, DBExport> readFile(){
		return readFile(-1);
	}
	public static HashMap<String, DBExport> readFile(int maxEntries){
		return readFile(maxEntries, false, null);
	}
	public static HashMap<String, DBExport> readFile(int entries, boolean random, String dataset){
		File readfile;
		if(dataset == null || dataset.equalsIgnoreCase("")){
			readfile = file;
		}else{
			readfile = new File(dataset);
		}
		int maxEntries;
		if(random && entries > 0){
			maxEntries = -1;
		}else{
			maxEntries = entries;
		}
		HashMap<String, DBExport> set = new HashMap<String, DBExport>();
		FileReader fr;
		try {
			fr = new FileReader(readfile);
			BufferedReader br = new BufferedReader(fr);
			String line;
			int lineIndex=0;
			int urlIndex = 0, pathIndex = 0, pubDateIndex = 0, pubSurenessIndex = 0, 
				modDateIndex = 0, modsurenessIndex = 0, googleIndex = 0, 
				hakiaIndex = 0, askIndex = 0, headerLastModIndex = 0, 
				headerDateIndex = 0, downIndex = 0;
			String[] parts;
			while((line=br.readLine())!=null && (lineIndex <= maxEntries || maxEntries == -1)){
				parts = line.split(" \\*;_;\\* ");
				if(lineIndex==0){
					for(int i=0; i<parts.length; i++){
						if(parts[i].equals("url")){
							urlIndex = i;
						}else if(parts[i].equals("path")){
							pathIndex = i;
						}else if(parts[i].equals("pub_date")){
							pubDateIndex = i;
						}else if(parts[i].equals("pub_sureness")){
							pubSurenessIndex = i;
						}else if(parts[i].equals("mod_date")){
							modDateIndex = i;
						}else if(parts[i].equals("mod_sureness")){
							modsurenessIndex = i;
						}else if(parts[i].equals("google_date")){
							googleIndex = i;
						}else if(parts[i].equals("hakia_date")){
							hakiaIndex = i;
						}else if(parts[i].equals("ask_date")){
							askIndex = i;
						}else if(parts[i].equals("header_last_mod")){
							headerLastModIndex = i;
						}else if(parts[i].equals("header_date")){
							headerDateIndex = i;
						}else if(parts[i].substring(0, 9).equals("down_date")){
							downIndex = i;
						}
						
					}
				}else{
					for(int i=0; i<parts.length; i++){
						set.put(parts[urlIndex], new DBExport(parts[urlIndex], parts[pathIndex], 
								parts[pubDateIndex],parts[modDateIndex],
								Boolean.valueOf(parts[pubSurenessIndex]),Boolean.valueOf(parts[modsurenessIndex]), 
								parts[googleIndex], parts[hakiaIndex], parts[askIndex], 
								parts[headerLastModIndex], parts[headerDateIndex], parts[downIndex]));
					}
				}
				lineIndex++;
			}
			
			if(random){
				HashMap<String, DBExport> temp = new HashMap<String, DBExport>();
				Random ran = new Random();
				while(temp.size() < entries && temp.size() < set.size()){
					int ranInt = ran.nextInt(set.size());
					int index = 0;
					Entry<String, DBExport> tempEntry = null;
					for(Entry<String, DBExport>e : set.entrySet()){
						tempEntry = e;
						if(ranInt == index){
							break;
						}
						index++;
					}
					if(tempEntry != null){
						temp.put(tempEntry.getKey(), tempEntry.getValue());
					}
				}
				set = temp;
			}
			
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		return set;
	}
	/**
	 * Compares a date, found by date-getter and date-rater, with an date, found by hand. 
	 * @param <T>
	 * @param foundDate Date found by date-getter and date-rater.
	 * @param dbExport Export of database holding publish and modified date.  
	 * @param compareDate Use static field mod_date or pub_date of {@link DBExport}.
	 * @return -2 false negative; -1 false positive; 0 true negative; 1 true positive
	 */
	public static <T> int compareDate(T foundDate, DBExport dbExport, int compareDate){
		int returnValue;
		ExtractedDate ed = DateGetterHelper.findDate(dbExport.get(compareDate));
		if(ed == null){
			if(foundDate == null){
				returnValue = DataSetHandler.RNF;
			}else{
				returnValue = DataSetHandler.FF;
			}
		}else{
			if(foundDate == null){
				returnValue = DataSetHandler.WNF;
			}else{
				DateComparator dc = new DateComparator();
				if (dc.compare(ed, (ExtractedDate) foundDate, Math.min(dc.getCompareDepth(ed, (ExtractedDate) foundDate),dc.STOP_DAY)) == 0){
					returnValue = DataSetHandler.RF;
				}else{
					returnValue = DataSetHandler.WF;
				}
			}
		}
		return returnValue;
	}
	

	public void setFile(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}
	
	private static double count(HashMap<String, Integer> urls, int classifire){
		double count=0;
		for(Entry<String, Integer> e : urls.entrySet()){
			if(e.getValue() == classifire){
				count++;
			}
		}
		return count;
	}
	
	
	public static double count(String round, String table, int numberUrls, int classifire, boolean random){
		return count(null, round, table, numberUrls, classifire, random);
	}
	public static double count(String file, String round, String table, int numberUrls, int classifire, boolean random){
		ArrayList<String> urls = ArrayHelper.toArrayList(readFile(numberUrls, random, file));
		HashMap<String, Integer> valuedUrls = DataSetHandler.getClassification(table, round, urls); 
		return count(valuedUrls, classifire);
	}
	public static double count(String round, String table, int numberUrls, int classifire){
		return count(round, table, numberUrls, classifire, false);
	}
	public static double count(String file, String round, String table, int numberUrls, int classifire){
		return count(file, round, table, numberUrls, classifire, false);
	}
	public static double count(String file, String round, String table, int classifire){
		return count(file, round, table, -1, classifire, false);
	}
	/**
	 * Calculates exactness. <br>
	 * Exactness is defined by relation of correct answers to all answers. <br>
	 * Correct answer includes found the right date and found if no date exists. <br>
	 * (RNF+ RF) / all  
	 * @param round
	 * @param table
	 * @param urls
	 * @return
	 */
	public static double calculateExactness(String round, String table, ArrayList<String> urls){
		HashMap<String, Integer> valuedUrls = DataSetHandler.getClassification(table, round, urls); 
    	double tp = count(valuedUrls, DataSetHandler.RNF) + count(valuedUrls, DataSetHandler.RF);
    	double all = (double)urls.size();
    	return (tp / all);
    	
    }
	/**
	 * Calculates exactness of missing dates. <br>
	 * Is defined by relation of correct not found to all missing dates. <br>
	 * RNF / (RNF + FF) 
	 * @param round
	 * @param table
	 * @param urls
	 * @return
	 */
	public static double calculateMissingExactness(String round, String table, ArrayList<String> urls){
		HashMap<String, Integer> valuedUrls = DataSetHandler.getClassification(table, round, urls);
		double rnf = count(valuedUrls, DataSetHandler.RNF);
    	double ff = count(valuedUrls, DataSetHandler.FF);
    	
    	return (rnf / (rnf+ff));
    }
	
	/**
	 * Calculates sample size for Technique. <br> 
	 * @param round e.g. "mod0", "pub0", "mod1" ...
	 * @param table Name of DB-table.
	 * @param file fitting file for table.
	 * @return
	 */
	public static double calculateSampleSize(String round, String table,String file){
		double zSqr = 1.96 * 1.96;
		double ciSqr = 0.05 * 0.05;
		ArrayList<String> urls = ArrayHelper.toArrayList(readFile(-1, false, file));
		double p = calculateExactness(round, table, urls);
		double n = ((zSqr * p * (1-p))/ciSqr)+1.0;
		return n;
	}
	
	
}
