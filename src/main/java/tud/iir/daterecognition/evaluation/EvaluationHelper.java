package tud.iir.daterecognition.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Map.Entry;

import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.ExtractedDateHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.searchengine.DBExport;
import tud.iir.daterecognition.searchengine.DataSetHandler;
import tud.iir.helper.ArrayHelper;
import tud.iir.helper.DateComparator;

public class EvaluationHelper {

	private static File file = new File("data/evaluation/dateextraction/dataset.txt");
	private static String separator = DataSetHandler.separator;
	
	public static final String CONTENTEVAL = "contenteval";
	public static final String HTTPEVAL = "httpeval";
	public static final String URLEVAL = "urleval";
	public static final String STRUCTEVAL = "structeval";
	public static final String HEADEVAL = "headeval";
	
	public static HashMap<String, DBExport> readFile(){
		return readFile(-1);
	}
	public static HashMap<String, DBExport> readFile(int maxEntries){
		return readFile(maxEntries, false);
	}
	public static HashMap<String, DBExport> readFile(int entries, boolean random){
		int maxEntries;
		if(random && entries > 0){
			maxEntries = -1;
		}else{
			maxEntries = entries;
		}
		HashMap<String, DBExport> set = new HashMap<String, DBExport>();
		FileReader fr;
		try {
			fr = new FileReader(file);
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
						}else if(parts[i].equals("down_date")){
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
		ExtractedDate realDate = DateGetterHelper.findDate(dbExport.get(compareDate));
		if(realDate == null){
			if(foundDate == null){
				returnValue = DataSetHandler.TN;
			}else{
				returnValue = DataSetHandler.FP;
			}
		}else{
			if(foundDate == null){
				returnValue = DataSetHandler.FN;
			}else{
				DateComparator dc = new DateComparator();
				if (dc.compare(realDate, (ExtractedDate) foundDate, dc.getCompareDepth(realDate, (ExtractedDate) foundDate)) == 0){
					returnValue = DataSetHandler.TP;
				}else{
					returnValue = DataSetHandler.FN;
					//returnValue = -3;
				}
			}
		}
		return returnValue;
	}
	/**
	 * Compares a date, found by in header, with an date, found by hand. 
	 * @param foundDate Date found by date-getter and date-rater.
	 * @param header Use static field header_last or header_date of {@link DBExport}.
	 * @param dbExport Export of database holding publish and modified date.  
	 * @param compareDate Use static field mod_date or pub_date of {@link DBExport}.
	 * @return -2 false negative; -1 false positive; 0 true negative; 1 true positive
	 */
	public static int compareDate(DBExport headDate, int header, DBExport dbExport, int compareDate){
		int returnValue;
		ExtractedDate realDate = DateGetterHelper.findDate(dbExport.get(compareDate));
		ExtractedDate headerDate = DateGetterHelper.findDate(headDate.get(header));
		if(realDate == null){
			if(headerDate == null){
				returnValue = DataSetHandler.TN;
			}else{
				returnValue = DataSetHandler.FP;
			}
		}else{
			if(headerDate == null){
				returnValue = DataSetHandler.FN;
			}else{
				DateComparator dc = new DateComparator();
				if (dc.compare(realDate, headerDate, dc.getCompareDepth(realDate, headerDate)) == 0){
					returnValue = DataSetHandler.TP;
				}else{
					returnValue = DataSetHandler.FN;
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
		ArrayList<String> urls = ArrayHelper.toArrayList(readFile(numberUrls, random));
		HashMap<String, Integer> valuedUrls = DataSetHandler.getClassification(table, round, urls); 
		return count(valuedUrls, classifire);
	}
	public static double count(String round, String table, int numberUrls, int classifire){
		return count(round, table, numberUrls, classifire, false);
	}
	
	public static double calculateP(String round, String table, ArrayList<String> urls){
		HashMap<String, Integer> valuedUrls = DataSetHandler.getClassification(table, round, urls); 
    	double tp = count(valuedUrls, DataSetHandler.TP);
    	double fp = count(valuedUrls, DataSetHandler.FP);
    	return (tp / (tp + fp));
    	
    }
	public static double calculateR(String round, String table, ArrayList<String> urls){
		HashMap<String, Integer> valuedUrls = DataSetHandler.getClassification(table, round, urls);
		double tp = count(valuedUrls, DataSetHandler.TP);
    	double fn = count(valuedUrls, DataSetHandler.FN);
    	
    	return (tp / (tp+fn));
    }
	public static double calculateF1(String round, String table,  ArrayList<String> urls){
		double p = calculateP(round, table, urls);
		double r = calculateR(round, table, urls);
		return ((2*p*r) / (p+r));
	}
	
	private static void calculateOutput(String round, String table, ArrayList<String> urls){
		double p = Math.round((calculateP( round, table, urls)*100000.0))/1000.0;
		double r = Math.round((calculateR( round, table, urls)*100000.0))/1000.0;
		double f = Math.round((calculateF1( round, table, urls)*100000.0))/1000.0;
		
		System.out.print(round + ":   ");
		System.out.print("p: " + p);
		System.out.print(" - r: " + r);
		System.out.print(" - f: " + f);
		System.out.println();

	}
	
	
	public static void calculateOutput(int round, String table){
		calculateOutput(round, table, 50);
		calculateOutput(round, table, 100);
		calculateOutput(round, table, 150);
		calculateOutput(round, table, 200);
		calculateOutput(round, table, 250);
		calculateOutput(round, table, 300);
		calculateOutput(round, table, 350);
	}
	
	public static void calculateOutput(String round1, String round2, String table){
		ArrayList<String> urls = ArrayHelper.toArrayList(readFile());
		System.out.println("sample size: " + urls.size());
		calculateOutput(round1, table, urls);
		calculateOutput(round2, table, urls);
	}
	
	public static void calculateOutput(String[] round, String table){
		ArrayList<String> urls = ArrayHelper.toArrayList(readFile());
		System.out.println("sample size: " + urls.size());
		for(int i=0; i<round.length; i++){
			calculateOutput(round[i], table, urls);
		}
	}
	
	public static void calculateOutput(String round, String table){
		ArrayList<String> urls = ArrayHelper.toArrayList(readFile());
		System.out.println("sample size: " + urls.size());
		calculateOutput(round, table, urls);
	}
	
	/**
	 * 
	 * @param round
	 * @param table
	 * @param numberUrls
	 */
	public static void calculateOutput(int round, String table, int numberUrls){
	
		ArrayList<String> urls = ArrayHelper.toArrayList(readFile(numberUrls));
		System.out.println("sample size: " + urls.size());
		calculateOutput("pub" + round, table, urls);
		calculateOutput("mod" + round, table, urls);
		
	}
	public static double calculateSampleSize(double ci){
		return Math.pow((1.96/2.0), 2) / Math.pow(ci, 2);
	}
	
	
	public static double calculateCI(String table, String round, int classifire, int sampleSize, boolean random){
		double countClasifire = count(round, table, sampleSize, classifire, random);
		double percentage = countClasifire / (double)sampleSize;
		double z = 1.96;
		double determinate = (Math.pow(z, 2) * percentage * (1 - percentage)) / (double)sampleSize;
		double ci = Math.sqrt(determinate);
		
		return ci;
		
	}
}
