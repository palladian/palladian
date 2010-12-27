package tud.iir.daterecognition.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.ExtractedDateHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.searchengine.DBExport;
import tud.iir.daterecognition.searchengine.DataSetHandler;
import tud.iir.helper.DateComparator;

public class EvaluationHelper {

	private static File file = new File("data/evaluation/dateextraction/dataset.txt");
	private static String separator = DataSetHandler.separator;
	
	public static HashMap<String, DBExport> readFile(){
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
			while((line=br.readLine())!=null){
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
					lineIndex++;
				}else{
					for(int i=0; i<parts.length; i++){
						set.put(parts[urlIndex], new DBExport(parts[urlIndex], parts[pathIndex], 
								parts[pubDateIndex],parts[modDateIndex],
								Boolean.valueOf(parts[pubSurenessIndex]),Boolean.valueOf(parts[modsurenessIndex]), 
								parts[googleIndex], parts[hakiaIndex], parts[askIndex], 
								parts[headerLastModIndex], parts[headerDateIndex], parts[downIndex]));
					}
				}
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
	 * @param foundDate Date found by date-getter and date-rater.
	 * @param dbExport Export of database holding publish and modified date.  
	 * @param compareDate Use static field mod_date or pub_date of {@link DBExport}.
	 * @return -2 false negative; -1 false positive; 0 true negative; 1 true positive
	 */
	public static int compareDate(ExtractedDate foundDate, DBExport dbExport, int compareDate){
		int returnValue;
		ExtractedDate realDate = DateGetterHelper.findDate(dbExport.get(compareDate));
		if(realDate == null){
			if(foundDate == null){
				returnValue = 0;
			}else{
				returnValue = -1;
			}
		}else{
			if(foundDate == null){
				returnValue = -2;
			}else{
				DateComparator dc = new DateComparator();
				if (dc.compare(realDate, foundDate, dc.getCompareDepth(realDate, foundDate)) == 0){
					returnValue = 1;
				}else{
					returnValue = -2;
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
	
}
