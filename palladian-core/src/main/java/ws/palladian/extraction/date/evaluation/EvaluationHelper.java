package ws.palladian.extraction.date.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.dates.DateExactness;
import ws.palladian.helper.date.dates.ExtractedDate;

public class EvaluationHelper {

	private static File file = new File("data/evaluation/daterecognition/datasets/dataset.txt");
	// private static String separator = EvaluationHelper.SEPARATOR;
	
	public static final String SEPARATOR = " *;_;* ";
	/**
	 * 2 <br>
	 * Available Found Right. <br>
	 * ED = 1 & FD = 1 & ED == FD
	 */
	public static final int AFR = 2;
	/**
	 * 1 <br>
	 * Absent Right Detected. <br>
	 * ED = 0 & FD = 0
	 */
	public static final int ARD = 1;
	/**
	 * 0 <br>
	 * Absent Wrong Detected. <br>
	 * ED = 0 & FD = 1
	 */
	public static final int AWD = 0;
	/**
	 * Extracted Date (Datum der Webseite): ED
	 * Gefundenes Datum: FD
	 * 
	 */
	/**
	 * -2 <br>
	 * Available Found Wrong. <br>
	 * ED = 1 & FD = 1 & ED != Fd 
	 */
	/**
	 * -1 <br>
	 * Available Not Found. <br>
	 * ED = 1 & FD = 0
	 */
	public static final int ANF = -1;
	public static final int AFW = -2;
	
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
	
	

	public static void setFile(File file) {
		EvaluationHelper.file = file;
	}

	public File getFile() {
		return file;
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
				returnValue = EvaluationHelper.ARD;
			}else{
				returnValue = EvaluationHelper.AWD;
			}
		}else{
			if(foundDate == null){
				returnValue = EvaluationHelper.ANF;
			}else{
			    DateExactness compareDepth = DateExactness.getCommonExactness(ed, (ExtractedDate) foundDate);
				DateComparator dc = new DateComparator(DateExactness.getCommonExactness(compareDepth, DateExactness.DAY));
				if (dc.compare(ed, (ExtractedDate) foundDate) == 0){
					returnValue = EvaluationHelper.AFR;
				}else{
					returnValue = EvaluationHelper.AFW;
				}
			}
		}
		return returnValue;
	}
	
	
	
}
