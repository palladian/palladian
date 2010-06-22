package tud.iir.reporting;

/**
 * A Report is a list of measures and calculations. measures: totalEntities, totalCorrectEntities, entityPrecision, correctEntityPerMinute, totalFacts,
 * totalCorrectFacts, factPrecision, correctFactsPerMinute, avgFactsPerEntity, avgCorrectFactsPerEntity, factF1 define "correct" as having a corroboration over
 * "minEntityCorroboration" and "minFactCorroboration" as defined in the filter class
 * 
 * @author David Urbansky
 */
public class Report {

    /**
     * Values will be accessed through variables. ...ForView functions are normalized and made to print for view (in reports)
     */
    public double totalEntities = 0.0;
    public double totalCorrectEntities = 0.0;
    public double entityPrecision = 0.0;
    public double correctEntitiesPerMinute = 0.0;
    public double totalFacts = 0.0;
    public double totalCorrectFacts = 0.0;
    public double factPrecision = 0.0;
    public double correctFactsPerMinute = 0.0;
    public double avgFactsPerEntity = 0.0;
    public double avgCorrectFactsPerEntity = 0.0;
    public double factF1 = 0.0;

    private double round(double value) {
        return Math.round(100 * value) / (double) 100;
    }

    public String getTotalEntitiesForView() {
        return String.valueOf(round(totalEntities));
    }

    public String getTotalCorrectEntitiesForView() {
        return String.valueOf(round(totalCorrectEntities));
    }

    public String getEntityPrecisionForView() {
        return String.valueOf(round(entityPrecision * 100)) + "%";
    }

    public String getCorrectEntitiesPerMinuteForView() {
        return String.valueOf(round(correctEntitiesPerMinute));
    }

    public String getTotalFactsForView() {
        return String.valueOf(round(totalFacts));
    }

    public String getTotalCorrectFactsForView() {
        return String.valueOf(round(totalCorrectFacts));
    }

    public String getFactPrecisionForView() {
        return String.valueOf(round(factPrecision * 100)) + "%";
    }

    public String getCorrectFactsPerMinuteForView() {
        return String.valueOf(round(correctFactsPerMinute));
    }

    public String getAvgFactsPerEntityForView() {
        return String.valueOf(round(avgFactsPerEntity));
    }

    public String getAvgCorrectFactsPerEntityForView() {
        return String.valueOf(round(avgCorrectFactsPerEntity));
    }

    public String getFactF1ForView() {
        return String.valueOf(round(factF1 * 100)) + "%";
    }

    /**
     * For saving purposes return all report values as a list.
     */
    public String toList() {
        StringBuilder list = new StringBuilder();

        list.append(totalEntities).append(";total entities\n");
        list.append(totalCorrectEntities).append(";total correct entities\n");
        list.append(totalFacts).append(";total facts\n");
        list.append(totalCorrectFacts).append(";total correct facts\n");
        list.append(entityPrecision).append(";entity precision\n");
        list.append(factPrecision).append(";fact precision\n");
        list.append(correctEntitiesPerMinute).append(";correct entities per minute\n");
        list.append(correctFactsPerMinute).append(";correct facts per minute\n");
        list.append(avgFactsPerEntity).append(";average facts per entity\n");
        list.append(avgCorrectFactsPerEntity).append(";average correct facts per entity\n");
        list.append(factF1).append(";fact F1\n");

        System.out.println(list.toString());

        return list.toString();
    }
}