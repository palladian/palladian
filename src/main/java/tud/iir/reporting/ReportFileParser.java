package tud.iir.reporting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * The ReportFileParser reads report files and builds data structures from the values that can be used to assemble reports that look back in time.
 * 
 * @author David Urbansky
 * 
 */
public class ReportFileParser {

    /**
     * Read "totalReport" files and extract data about entity and fact extractions (quantities). Automatically take the correct reporting folder.
     * 
     * @return
     */
    public static ArrayList<ArrayList<Double[]>> getExtractionQuantities() {

        // series, data, 0:time,1:value
        ArrayList<ArrayList<Double[]>> quantities = new ArrayList<ArrayList<Double[]>>();

        // collect total entity and total fact extraction
        ArrayList<Double[]> entityExtraction = new ArrayList<Double[]>();
        ArrayList<Double[]> factExtraction = new ArrayList<Double[]>();

        // get all files from the matching reporting folder
        File f = new File(Reporter.getReportFolderPath());

        File files[] = f.listFiles();

        int time = 0;
        for (int i = 0; i < files.length; i++) {

            // process only files
            if (!files[i].isFile())
                continue;

            // process only text files
            if (files[i].getName().indexOf("totalReport.txt") == -1)
                continue;

            String line = "";
            double value;
            int index;
            Double[] data = new Double[2];

            // entity extractions
            line = ReportFileParser.getLineFromFile(files[i], 1);
            // System.out.println("entity extraction "+line);
            index = line.indexOf(";");
            if (index > -1) {
                value = Double.valueOf(line.substring(0, index));
                data[0] = (double) time;
                data[1] = value;
                // System.out.println(data[0]+" _ "+data[1]);
                entityExtraction.add(data);
            }

            // entity extractions
            data = new Double[2];
            line = ReportFileParser.getLineFromFile(files[i], 3);
            // System.out.println("fact extraction "+line);
            index = line.indexOf(";");
            if (index > -1) {
                value = Double.valueOf(line.substring(0, index));
                data[0] = (double) time;
                data[1] = value;
                // System.out.println(data[0]+" _ "+data[1]);
                factExtraction.add(data);
            }

            ++time;
        }

        quantities.add(entityExtraction);
        quantities.add(factExtraction);

        return quantities;
    }

    /**
     * Read "totalReport" files and extract data about entity and fact precisions and fact F1 (qualities). Automatically take the correct reporting folder.
     * 
     * @return An array of values.
     */
    public static ArrayList<ArrayList<Double[]>> getExtractionQualities() {

        // series, data, 0:time,1:value
        ArrayList<ArrayList<Double[]>> qualities = new ArrayList<ArrayList<Double[]>>();

        // collect total entity and total fact extraction
        ArrayList<Double[]> entityPrecision = new ArrayList<Double[]>();
        ArrayList<Double[]> factPrecision = new ArrayList<Double[]>();
        ArrayList<Double[]> factF1 = new ArrayList<Double[]>();

        // get all files from the matching reporting folder
        File f = new File(Reporter.getReportFolderPath());

        File files[] = f.listFiles();

        int time = 0;
        for (int i = 0; i < files.length; i++) {

            // process only files
            if (!files[i].isFile())
                continue;

            // process only text files
            if (files[i].getName().indexOf("totalReport.txt") == -1)
                continue;

            String line = "";
            double value;
            int index;
            Double[] data = new Double[2];

            // entity precision
            line = ReportFileParser.getLineFromFile(files[i], 5);
            index = line.indexOf(";");
            if (index > -1) {
                value = Double.valueOf(line.substring(0, index));
                data[0] = (double) time;
                data[1] = 100 * value;
                entityPrecision.add(data);
            }

            // fact precision
            data = new Double[2];
            line = ReportFileParser.getLineFromFile(files[i], 6);
            index = line.indexOf(";");
            if (index > -1) {
                value = Double.valueOf(line.substring(0, index));
                data[0] = (double) time;
                data[1] = 100 * value;
                factPrecision.add(data);
            }

            // fact F1
            line = ReportFileParser.getLineFromFile(files[i], 11);
            index = line.indexOf(";");
            data = new Double[2];
            if (index > -1) {
                value = Double.valueOf(line.substring(0, index));
                data[0] = (double) time;
                data[1] = 100 * value;
                factF1.add(data);
            }

            ++time;
        }

        qualities.add(entityPrecision);
        qualities.add(factPrecision);
        qualities.add(factF1);

        return qualities;
    }

    private static String getLineFromFile(File file, int lineNumber) {
        String resultLine = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            int lineCount = 1;
            String line = "";
            do {
                line = br.readLine();
                if (lineCount == lineNumber && line != null) {
                    resultLine = line;
                    break;
                }
                ++lineCount;
            } while (line != null);

            br.close();

        } catch (FileNotFoundException e) {
            Logger.getRootLogger().error(file.getAbsolutePath() + " " + lineNumber, e);
        } catch (IOException e) {
            Logger.getRootLogger().error(file.getAbsolutePath() + " " + lineNumber, e);
        }

        return resultLine;
    }
}