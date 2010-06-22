package tud.iir.reporting;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.knowledge.Concept;

/**
 * A ReportSet holds reports (with the measures) for several domains.
 * 
 * @author David Urbansky
 */
public class ReportSet extends HashMap<Concept, Report> {

    private static final long serialVersionUID = -1453115451931847056L;

    private int runtime = 0; // runtime of the extraction in seconds

    public ReportSet(int runtime) {
        super();
        this.setRuntime(runtime);
    }

    public double getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    /**
     * Get number of all extracted entities for all domains.
     * 
     * @return Number of all extracted entities for all domains.
     */
    public double getTotalEntities() {
        double entities = 0.0;
        Iterator<Map.Entry<Concept, Report>> domainIterator = this.entrySet().iterator();
        while (domainIterator.hasNext()) {
            Map.Entry<Concept, Report> entry = domainIterator.next();
            entities += entry.getValue().totalEntities;
        }
        return entities;
    }

    public String getTotalEntitiesForView() {
        return String.valueOf(this.getTotalEntities());
    }

    /**
     * Get number of all correct extracted entities for all domains.
     * 
     * @return Number of all correct extracted entities for all domains.
     */
    public double getTotalCorrectEntities() {
        double totalCorrectEntities = 0.0;
        Iterator<Map.Entry<Concept, Report>> domainIterator = this.entrySet().iterator();
        while (domainIterator.hasNext()) {
            Map.Entry<Concept, Report> entry = domainIterator.next();
            totalCorrectEntities += entry.getValue().totalCorrectEntities;
        }
        return totalCorrectEntities;
    }

    public String getTotalCorrectEntitiesForView() {
        return String.valueOf(this.getTotalCorrectEntities());
    }

    /**
     * Get number of all extracted facts for all domains.
     * 
     * @return Number of all extracted facts for all domains.
     */
    public double getTotalFacts() {
        double totalFacts = 0.0;
        Iterator<Map.Entry<Concept, Report>> domainIterator = this.entrySet().iterator();
        while (domainIterator.hasNext()) {
            Map.Entry<Concept, Report> entry = domainIterator.next();
            totalFacts += entry.getValue().totalFacts;
        }
        return totalFacts;
    }

    public String getTotalFactsForView() {
        return String.valueOf(this.getTotalFacts());
    }

    /**
     * Get number of all correct extracted facts for all domains.
     * 
     * @return Number of all correct extracted facts for all domains.
     */
    public double getTotalCorrectFacts() {
        double totalCorrectFacts = 0.0;
        Iterator<Map.Entry<Concept, Report>> domainIterator = this.entrySet().iterator();
        while (domainIterator.hasNext()) {
            Map.Entry<Concept, Report> entry = domainIterator.next();
            totalCorrectFacts += entry.getValue().totalCorrectFacts;
        }
        return totalCorrectFacts;
    }

    public String getTotalCorrectFactsForView() {
        return String.valueOf(this.getTotalCorrectFacts());
    }

    /**
     * Get precision for all extracted entities and domains.
     * 
     * @return Precision for all extracted entities and domains.
     */
    public double getTotalEntityPrecision() {
        if (this.getTotalEntities() == 0.0)
            return 0.0;
        double totalEntityPrecision = this.getTotalCorrectEntities() / this.getTotalEntities();
        return totalEntityPrecision;
    }

    public String getTotalEntityPrecisionForView() {
        return String.valueOf(Math.round(10000 * this.getTotalEntityPrecision()) / 100) + "%";
    }

    /**
     * Get precision for all extracted facts and domains.
     * 
     * @return Precision for all extracted facts and domains.
     */
    public double getTotalFactPrecision() {
        if (this.getTotalFacts() == 0.0)
            return 0.0;
        double totalFactPrecision = this.getTotalCorrectFacts() / this.getTotalFacts();
        return totalFactPrecision;
    }

    public String getTotalFactPrecisionForView() {
        return String.valueOf(Math.round(10000 * this.getTotalFactPrecision()) / 100) + "%";
    }

    /**
     * Get extracted correct entities per minute for all domains.
     * 
     * @return Extracted correct entities per minute for all domains.
     */
    public double getCorrectEntitiesPerMinute() {
        double correctEntitiesPerMinute = this.getTotalCorrectEntities() / this.getRuntime();
        correctEntitiesPerMinute *= 60;
        return correctEntitiesPerMinute;
    }

    public String getCorrectEntitiesPerMinuteForView() {
        return String.valueOf(Math.round(100 * this.getCorrectEntitiesPerMinute()) / 100);
    }

    /**
     * Get extracted correct facts per minute for all domains.
     * 
     * @return Extracted correct facts per minute for all domains.
     */
    public double getCorrectFactsPerMinute() {
        double correctFactsPerMinute = this.getTotalCorrectFacts() / this.getRuntime();
        correctFactsPerMinute *= 60;
        return correctFactsPerMinute;
    }

    public String getCorrectFactsPerMinuteForView() {
        return String.valueOf(Math.round(100 * this.getCorrectFactsPerMinute()) / 100);
    }

    /**
     * Get avg. precision for all extracted facts, entities and domains.
     * 
     * @return Average precision for all extracted facts, entities and domains.
     */
    public double getAvgFactsPerEntity() {
        if (this.getTotalEntities() == 0.0)
            return 0.0;
        double avgFactsPerEntity = this.getTotalFacts() / this.getTotalEntities();
        return avgFactsPerEntity;
    }

    public String getAvgFactsPerEntityForView() {
        return String.valueOf(Math.round(100 * this.getAvgFactsPerEntity()) / 100);
    }

    /**
     * Get avg. precision for all extracted correct facts, entities and domains.
     * 
     * @return Average precision for all extracted correct facts, entities and domains.
     */
    public double getAvgCorrectFactsPerEntity() {
        if (this.getTotalCorrectEntities() == 0.0)
            return 0.0;
        double avgCorrectFactsPerEntity = this.getTotalCorrectFacts() / this.getTotalCorrectEntities();
        return avgCorrectFactsPerEntity;
    }

    public String getAvgCorrectFactsPerEntityForView() {
        return String.valueOf(Math.round(100 * this.getAvgCorrectFactsPerEntity()) / 100);
    }

    /**
     * Get avg. f1 for all extracted facts and domains.
     * 
     * @return Average f1 for all extracted facts and domains.
     */
    public double getFactF1() {
        if (this.getTotalFactPrecision() == 0.0 && this.getAvgCorrectFactsPerEntity() == 0.0)
            return 0.0;
        double f1 = (2 * this.getTotalFactPrecision() * this.getAvgCorrectFactsPerEntity())
                / (this.getTotalFactPrecision() + this.getAvgCorrectFactsPerEntity());
        return f1;
    }

    public String getFactF1ForView() {
        return String.valueOf(Math.round(10000 * this.getFactF1()) / 100) + "%";
    }

    /**
     * Save the complete ReportSet in a txt file.
     */
    public void saveCompleteReportSet() {
        try {

            FileWriter fileWriter = new FileWriter(Reporter.getReportFolderPath() + DateHelper.getCurrentDatetime() + "_completeReportSet.txt");

            Iterator<Map.Entry<Concept, Report>> domainIterator = this.entrySet().iterator();
            while (domainIterator.hasNext()) {
                Map.Entry<Concept, Report> entry = domainIterator.next();
                fileWriter.write("#" + entry.getKey().getName() + "\n"); // # for comment
                fileWriter.write(entry.getValue().toList());
                fileWriter.write("-\n"); // sign for end of report
                fileWriter.write("\n");
                fileWriter.flush();
            }

            fileWriter.close();
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    /**
     * Save only total values (that have been calculated from all domains) in a txt file.
     */
    public void saveTotalOnly() {
        try {

            FileWriter fileWriter = new FileWriter(Reporter.getReportFolderPath() + DateHelper.getCurrentDatetime() + "_totalReport.txt");

            fileWriter.write(this.getTotalEntities() + ";total entities\n");
            fileWriter.write(this.getTotalCorrectEntities() + ";total correct entities\n");
            fileWriter.write(this.getTotalFacts() + ";total facts\n");
            fileWriter.write(this.getTotalCorrectFacts() + ";total correct facts\n");
            fileWriter.write(this.getTotalEntityPrecision() + ";total entity precision\n");
            fileWriter.write(this.getTotalFactPrecision() + ";total fact precision\n");
            fileWriter.write(this.getCorrectEntitiesPerMinute() + ";correct entities per minute\n");
            fileWriter.write(this.getCorrectFactsPerMinute() + ";correct facts per minute\n");
            fileWriter.write(this.getAvgFactsPerEntity() + ";average facts per entity\n");
            fileWriter.write(this.getAvgCorrectFactsPerEntity() + ";average correct facts per entity\n");
            fileWriter.write(this.getFactF1() + ";fact F1\n");

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }
}