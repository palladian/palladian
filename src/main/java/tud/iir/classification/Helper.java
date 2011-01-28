package tud.iir.classification;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;
import tud.iir.persistence.DatabaseManager;

/**
 * All methods of this class help importing and exporting data between the database and CSV files.
 * 
 * @author David Urbansky
 */
public class Helper {

    /**
     * Import hand chosen training and testing data for entity assessment. The file contains several concepts with classified entities for training and testing.
     */
    public void importEntityAssessmentData() {

        long t1 = System.currentTimeMillis();

        // read csv data in the format of concept id, positive data, negative data
        LineAction la = new LineAction() {

            List<Integer> conceptIDs = new ArrayList<Integer>();
            DatabaseManager dbm = new DatabaseManager();

            @Override
            public void performAction(String line, int lineNumber) {

                String[] parts = line.split("\t");

                if (lineNumber == 1) {
                    for (int i = 0; i < parts.length; i++) {
                        conceptIDs.add(Integer.valueOf(parts[i]));
                    }
                } else {
                    int classValue = 0;

                    // positive instances
                    if (lineNumber < 202) {
                        classValue = 1;

                        // negative instances
                    } else if (lineNumber >= 502 && lineNumber < 702) {
                        classValue = 0;

                        // consider only 200 positive and negative instances
                    } else {
                        return;
                    }

                    for (int i = 0; i < parts.length; i++) {
                        // dbm.addAssessmentInstance(conceptIDs.get(i), Integer.valueOf(parts[i]), classValue);
                    }
                }

            }

        };

        FileHelper.performActionOnEveryLine("data/reports/entityAssessmentForImport.csv", la);

        Logger.getRootLogger().info("importet assessment instances in " + DateHelper.getRuntime(t1) + "s");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        new Helper().importEntityAssessmentData();
    }

}