package ws.palladian.classification.text.evaluation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ws.palladian.helper.io.FileHelper;
/**
 * This class separates a given training set into a training set and a evaluation set. 
 * 
 * @author Sandro Reichert
 *
 */
public class TrainingDataSeparation {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TrainingDataSeparation.class);
	
    /**
     * Separates a given training set by trainingDataPercentage into two files, containing
     * training and testing data. The separation can be done by randomly chosen lines or
     * the first part is used for training and the second part for testing.<br />
     * <br />
     * 
     * Example:<br />
     * 1) fileToSeparate contains 10 lines, trainingDataPercentage = 40 and randomlyChooseLines
     * is false, than lines 1-4 are written to trainingDataFileToWrite and lines 5-10 are
     * written to testingDataFileToWrite.<br />
     * 2) fileToSeparate contains 10 lines, trainingDataPercentage = 40 and randomlyChooseLines
     * is true, than 4 randomly chosen lines are written to trainingDataFileToWrite and
     * the remaining lines are written to testingDataFileToWrite.
     * 
     * @param fileToSeparate Path to the file to be separated.
     * @param trainingDataFileToWrite Path to the file the training data will be written to.
     * @param testingDataFileToWrite Path to the file the testing data will be written to.
     * @param trainingDataPercentage Percentage of file which should
     *            be used for training, range [0, 100]. The remainder of the file
     *            can be used for testing.
     * @param randomlyChooseLines Specifies whether lines should be picked randomly
     *            or not. If false, the first lines are used for training.
     * @throws IllegalArgumentException if trainingDataPercentage is out of range [0, 100].
     * @throws FileNotFoundException if fileToSeparate can not be found.
     * @throws IOException if fileToSeparate can not be accessed.
     */
	public void separateFile(String fileToSeparate,
			String trainingDataFileToWrite,
			String testingDataFileToWrite,
            double trainingDataPercentage, boolean randomlyChooseLines) throws FileNotFoundException, IOException{

        // FileHelper.delete(trainingDataFileToWrite);
        // FileHelper.delete(testingDataFileToWrite);

        if (trainingDataPercentage < 0 || trainingDataPercentage > 100) {
        	throw new IllegalArgumentException("trainingDataPercentage out of range [0, 100]: " + trainingDataPercentage + "File not separated! ");			
		}
		StringBuilder trainingSB = new StringBuilder();
		StringBuilder testingSB = new StringBuilder();
		int trainingDataLines = 0; 
		int lineCounter = 0;
		
		// calculate number of lines of training and test data sets
		lineCounter = FileHelper.getNumberOfLines(fileToSeparate);
		trainingDataLines = Math.round(lineCounter*(float)trainingDataPercentage/100);

				
		// 'reassign' lines, 
		// part 1: output if (random) <random, lineNumber> else <lineNumber, lineNumber> 
		TreeMap<Double, Integer> randomLinesMap = new TreeMap<Double, Integer>();
		for (int line = 1; line <= lineCounter; line++){
			double key = 0;
			if(randomlyChooseLines){
				do { 
					key = Math.random();
				} while(randomLinesMap.containsKey(key));
			} else {
                key = line;
            }
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
		
		FileReader orgDataFR = new FileReader(fileToSeparate);
		BufferedReader orgDataBR = new BufferedReader(orgDataFR);
		String orgDataLine = "";		
					
		do {
			orgDataLine = orgDataBR.readLine();
			if (orgDataLine == null || !alignedLinesIter.hasNext()) {
                break;
            }
			int currentLine = alignedLinesIter.next();
			if(alignedLinesMap.get(currentLine) == 0) {
                trainingSB.append(orgDataLine).append("\n");
            } else {
                testingSB.append(orgDataLine).append("\n");
            }
		} while (orgDataLine != null && alignedLinesIter.hasNext());
					
		orgDataFR.close();
		orgDataBR.close();						
		
		// write data to files
		FileHelper.writeToFile(trainingDataFileToWrite, trainingSB);
		FileHelper.writeToFile(testingDataFileToWrite, testingSB);
        LOGGER.debug("Data separated into training and test set, split at " + trainingDataPercentage + "%, random: "
                + randomlyChooseLines);
	}
		
}
