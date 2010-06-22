package tud.iir.temp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;

public class TrainingDataSeparation {

	private static final Logger logger = Logger.getLogger(TrainingDataSeparation.class);
	
	
	public TrainingDataSeparation(){
		
	}
	
	/**
	 * separates the training set by trainingDataPercentage, 
	 * randomly chosen or first part training, second testing   
	 * @param trainingDataPercentage Percentage of file which should 
	 * be used for training, range [0, 100]. The remainder of the file
	 * can be used for testing. 
	 * @param random Specifies whether lines should be picked randomly 
	 * or not. If false, the first lines are used for training 
	 */
	public void seperateFile(int trainingDataPercentage, boolean random){
		if(trainingDataPercentage < 0  || trainingDataPercentage > 100)
		{
			logger.error("trainingDataPercentage out of range [0, 100]: "+ trainingDataPercentage);
			logger.error("File not separated! ");
			return;
		}
		StringBuilder trainingSB = new StringBuilder();
		StringBuilder testingSB = new StringBuilder();
		String originalTrainingData = "data/temp/dataRewrittenCombined_completeSet.csv";
		int trainingDataLines = 0; 
		int lineCounter = 0;
		
		// calculate number of lines of training and test data sets
		lineCounter = FileHelper.getNumberOfLines(originalTrainingData);
		trainingDataLines = Math.round((float)lineCounter*(float)trainingDataPercentage/100);

				
		// 'reassign' lines, 
		// part 1: output if (random) <random, lineNumber> else <lineNumber, lineNumber> 
		TreeMap<Double, Integer> randomLinesMap = new TreeMap<Double, Integer>();
		for (int line = 1; line <= lineCounter; line++){
			double key = 0;
			if(random){
				do { 
					key = Math.random();
				} while(randomLinesMap.containsKey(key));
			}
			else key = line;
			randomLinesMap.put(key, line);			
		}
		
		//part 2
		// output <lineNumber, typeOfSet> with typeOfSet=0? training : test 
		TreeMap<Integer, Integer> alignedLinesMap = new TreeMap<Integer, Integer>();
		Set<Double> randomLinesSet = randomLinesMap.keySet();
		Iterator<Double> randomLinesIter = randomLinesSet.iterator();		
		int j = 0;
		while(randomLinesIter.hasNext()) {
			Double key = randomLinesIter.next();
			int line = randomLinesMap.get(key);
			if (j < trainingDataLines) {
				j++;
				alignedLinesMap.put(line, 0);
			}
			else{
				alignedLinesMap.put(line, 1);
			}
		}

		// separate training and test set 
		Set<Integer> alignedLinesSet = alignedLinesMap.keySet();
		Iterator<Integer> alignedLinesIter = alignedLinesSet.iterator();
		try {
			FileReader orgDataFR = new FileReader(originalTrainingData);
			BufferedReader orgDataBR = new BufferedReader(orgDataFR);
			String orgDataLine = "";		
						
			do {
				orgDataLine = orgDataBR.readLine();
				if (orgDataLine == null || !alignedLinesIter.hasNext())
					break;
				int currentLine = alignedLinesIter.next();
				if(alignedLinesMap.get(currentLine) == 0)
					trainingSB.append(orgDataLine).append("\n");
				else
					testingSB.append(orgDataLine).append("\n");
			} while (orgDataLine != null && alignedLinesIter.hasNext());
						
			orgDataFR.close();
			orgDataBR.close();
			
		} catch (FileNotFoundException e) {
			logger.error(originalTrainingData + e.getMessage());
		} catch (IOException e) {
			logger.error(originalTrainingData + e.getMessage());
		} catch (OutOfMemoryError e) {
			logger.error(originalTrainingData + e.getMessage());
		}				
		
		// write data to files
		FileHelper.writeToFile("data/temp/dataRewrittenCombined_Training.csv", trainingSB);
		FileHelper.writeToFile("data/temp/dataRewrittenCombined_Testing.csv", testingSB);
		System.out.println("Data separated into training and test set, split at " + trainingDataPercentage + "%, random: " + random);
	}
		
}
