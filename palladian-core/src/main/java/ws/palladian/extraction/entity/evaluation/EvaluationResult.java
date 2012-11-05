package ws.palladian.extraction.entity.evaluation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * In NER there are 5 possible errors that can influence evaluation:<br>
 * <ol>
 * <li>ERROR 1: tagged something that should not have been tagged</li>
 * <li>ERROR 2: missed an entity</li>
 * <li>ERROR 3: correct boundaries but wrong tag</li>
 * <li>ERROR 4: correctly tagged an entity but either too much or too little (wrong boundaries)</li>
 * <li>ERROR 5: wrong boundaries and wrong tag</li>
 * </ol>
 * For more information see <a
 * href="http://nlp.cs.nyu.edu/sekine/papers/li07.pdf">http://nlp.cs.nyu.edu/sekine/papers/li07.pdf</a> page 14.
 * </p>
 * 
 * <p>
 * We can evaluate using two approaches:<br>
 * <ol>
 * <li>Exact match ({@link EvaluationResult.EXACT_MATCH}), that is, only if boundary and tag are assigned correctly, the
 * assignment is true positive. Error types are not taken into account, all errors are equally wrong.</li>
 * <li>MUC ({@link EvaluationResult.MUC}), takes error types into account. 1 point for correct tag (regardless of
 * boundaries), 1 point for correct text (regardless of tag). Totally correct (correct boundaries and correct tag) = 2
 * points</li>
 * </ol>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class EvaluationResult {

    public EvaluationResult(Map<String, CountMap<String>> assignments, Annotations goldStandardAnnotations,
            Map<String, Annotations> errorAnnotations) {
        this.assignments = assignments;
        this.goldStandardAnnotations = goldStandardAnnotations;
        this.errorAnnotations = errorAnnotations;
    }

    /** The annotations from the gold standard. */
    private Annotations goldStandardAnnotations;

    /** All error annotations by error class. */
    private Map<String, Annotations> errorAnnotations;

    /**
     * <p>
     * This map holds the number of errors for each type, the number of correct extractions, and the number of possible
     * extractions for each class.
     * </p>
     * <p>
     * Exact match example (error 3 to 5 are 0 in this simple example):<br>
     * class |error1|error2|correct|possible<br>
     * phone | 2 | 8 | 5 | 35<br>
     * person| 3 | 1 | 1 | 5
     * </p>
     * <p>
     * The resulting precision would be:<br>
     * precision(phone) = 5 / 15 ~ 33%<br>
     * precision(person) = 1 / 5 = 20%<br>
     * precision = 6 / 20 = 30%<br>
     * tag averaged precision = 33+20 / 2 = 26.5%<br>
     * <br>
     * 
     * The resulting recall would be:<br>
     * recall(phone) = 5 / 35 ~ 14%<br>
     * recall(person) = 5 / 5 = 100%<br>
     * recall = 10 / 40 = 25%<br>
     * tag averaged recall = (14+100) / 2 = 57%
     * </p>
     * 
     * <p>
     * MUC example (for error3 and error4 1 point each, correct = 2 points):<br>
     * class |error1|error2|error3|error4|error5|correct|possible<br>
     * phone | 2 | 8 | 2 | 4 | 5 | 3 | 35<br>
     * person| 3 | 1 | 1 | 5 | 4 | 8 | 22
     * </p>
     * <p>
     * The resulting precision would be:<br>
     * COR = error3 + error4 + 2*correct<br>
     * ACT = 2 * (error1 + error3 + error4 + error5 + correct)<br>
     * precision(tag) = COR / ACT<br>
     * precision(phone) = (2 + 4 + 2*3) / (2 * (2 + 2 + 4 + 5 + 3)) = 12 / 32 ~ 37.5%<br>
     * precision(person) = (1 + 5 + 2*8) / (2 * (3 + 1 + 5 + 4 + 8)) = 22 / 42 ~ 52%<br>
     * precision = (12 + 22) / (32 + 42 ) = 34 / 48 ~ 46%<br>
     * tag averaged precision = 37.5+52 / 2 = 44.75%<br>
     * <br>
     * 
     * The resulting recall would be:<br>
     * POS = 2*possible<br>
     * recall(tag) = COR / POS<br>
     * recall(phone) = 12 / 70 ~ 17%<br>
     * recall(person) = 22 / 44 = 50%<br>
     * recall = 34 / 114 = 29.8%<br>
     * tag averaged recall = (17+50) / 2 = 33.5%
     * </p>
     */
    private Map<String, CountMap<String>> assignments = new HashMap<String, CountMap<String>>();

    /** The exact match evaluation mode. */
    public static final int EXACT_MATCH = 0;

    /** The MUC evaluation mode. */
    public static final int MUC = 1;

    /** A marker that marks special fields. */
    public static final String SPECIAL_MARKER = "#";

    /** Tagged something that should not have been tagged. */
    public static final String ERROR1 = SPECIAL_MARKER + "error1" + SPECIAL_MARKER;

    /** Completely missed to tag an entity. */
    public static final String ERROR2 = SPECIAL_MARKER + "error2" + SPECIAL_MARKER;

    /** Exact same content but wrong tag. */
    public static final String ERROR3 = SPECIAL_MARKER + "error3" + SPECIAL_MARKER;

    /** Overlapping content and correct tag. */
    public static final String ERROR4 = SPECIAL_MARKER + "error4" + SPECIAL_MARKER;

    /** Overlapping content but wrong tag. */
    public static final String ERROR5 = SPECIAL_MARKER + "error5" + SPECIAL_MARKER;

    /** Exact same content and correct tag. */
    public static final String CORRECT = SPECIAL_MARKER + "correct" + SPECIAL_MARKER;

    /** Number of possible annotations. */
    public static final String POSSIBLE = SPECIAL_MARKER + "possible" + SPECIAL_MARKER;

    public double getPrecisionFor(String tagName, int type) {
        double precision = -1;

        CountMap<String> cm = assignments.get(tagName);

        if (cm == null) {
            return precision;
        }

        int correctAssignments = 0;
        int totalAssignments = 0;

        if (type == EXACT_MATCH) {

            correctAssignments = cm.getCount(CORRECT);
            totalAssignments = cm.getCount(ERROR1) + cm.getCount(ERROR3) + cm.getCount(ERROR4) + cm.getCount(ERROR5)
            + correctAssignments;

        } else if (type == MUC) {

            correctAssignments = cm.getCount(ERROR3) + cm.getCount(ERROR4) + 2 * cm.getCount(CORRECT);
            totalAssignments = 2 * (cm.getCount(ERROR1) + cm.getCount(ERROR3) + cm.getCount(ERROR4) + cm.getCount(ERROR5) + cm.getCount(CORRECT));

        }

        if (totalAssignments == 0) {
            return precision;
        }

        precision = (double) correctAssignments / (double) totalAssignments;

        return precision;
    }

    public double getRecallFor(String tagName, int type) {
        double recall = -1;

        CountMap<String> cm = assignments.get(tagName);

        if (cm == null) {
            return recall;
        }

        int correctAssignments = 0;
        int possibleAssignments = 0;

        if (type == EXACT_MATCH) {

            correctAssignments = cm.getCount(CORRECT);
            possibleAssignments = cm.getCount(POSSIBLE);

        } else if (type == MUC) {

            correctAssignments = cm.getCount(ERROR3) + cm.getCount(ERROR4) + 2 * cm.getCount(CORRECT);
            possibleAssignments = 2 * cm.getCount(POSSIBLE);

        }

        if (possibleAssignments == 0) {
            return recall;
        }

        recall = (double) correctAssignments / (double) possibleAssignments;

        return recall;
    }

    public double getF1For(String tagName, int type) {
        double f1 = -1;

        double precision = 0;
        double recall = 0;

        precision = getPrecisionFor(tagName, type);
        recall = getRecallFor(tagName, type);

        if (precision == 0 || recall == 0) {
            return 0.0;
        }

        if (precision == -1 || recall == -1) {
            return f1;
        }

        f1 = 2 * precision * recall / (precision + recall);

        return f1;
    }

    public double getTagAveragedPrecision(int type) {

        double totalPrecision = 0;

        // count number of tags with not undefined precisions (precision > -1)
        double totalPrecisionsSet = 0;

        for (Entry<String, CountMap<String>> tagEntry : assignments.entrySet()) {
            double tagPrecision = getPrecisionFor(tagEntry.getKey(), type);
            if (tagPrecision > -1) {
                totalPrecision += tagPrecision;
                totalPrecisionsSet++;
            }
        }

        double tagAveragedPrecision = totalPrecision / totalPrecisionsSet;
        return tagAveragedPrecision;
    }

    public double getTagAveragedRecall(int type) {

        double totalRecall = 0;

        // count number of tags with not undefined recall (recall > -1)
        double totalRecallsSet = 0;

        for (Entry<String, CountMap<String>> tagEntry : assignments.entrySet()) {
            double tagRecall = getRecallFor(tagEntry.getKey(), type);
            if (tagRecall > -1) {
                totalRecall += tagRecall;
                totalRecallsSet++;
            }
        }

        double tagAveragedRecall = totalRecall / totalRecallsSet;
        return tagAveragedRecall;
    }

    public double getTagAveragedF1(int type) {

        double f1 = -1;

        double precision = getTagAveragedPrecision(type);
        double recall = getTagAveragedRecall(type);

        if (precision == 0 || recall == 0) {
            return 0.0;
        } else if (precision < 0 || recall < 0) {
            return f1;
        }

        f1 = 2 * precision * recall / (precision + recall);

        return f1;
    }

    public double getPrecision(int type) {
        double precision = 0;

        int correctAssignments = 0;
        int totalAssignments = 0;

        for (Entry<String, CountMap<String>> tagEntry : assignments.entrySet()) {

            CountMap<String> cm = tagEntry.getValue();

            if (cm == null) {
                continue;
            }

            if (type == EXACT_MATCH) {

                correctAssignments += cm.getCount(CORRECT);
                totalAssignments += cm.getCount(ERROR1) + cm.getCount(ERROR3) + cm.getCount(ERROR4) + cm.getCount(ERROR5) + cm.getCount(CORRECT);

            } else if (type == MUC) {

                correctAssignments += cm.getCount(ERROR3) + cm.getCount(ERROR4) + 2 * cm.getCount(CORRECT);
                totalAssignments += 2 * (cm.getCount(ERROR1) + cm.getCount(ERROR3) + cm.getCount(ERROR4) + cm.getCount(ERROR5) + cm
                        .getCount(CORRECT));

            }

        }

        precision = (double) correctAssignments / (double) totalAssignments;

        return precision;
    }

    public double getRecall(int type) {
        double recall = 0;

        int correctAssignments = 0;
        int possibleAssignments = 0;

        for (Entry<String, CountMap<String>> tagEntry : assignments.entrySet()) {

            CountMap<String> cm = tagEntry.getValue();

            if (cm == null) {
                continue;
            }

            if (type == EXACT_MATCH) {

                correctAssignments += cm.getCount(CORRECT);
                possibleAssignments += cm.getCount(POSSIBLE);

            } else if (type == MUC) {

                correctAssignments += cm.getCount(ERROR3) + cm.getCount(ERROR4) + 2 * cm.getCount(CORRECT);
                possibleAssignments += 2 * cm.getCount(POSSIBLE);

            }

        }

        recall = (double) correctAssignments / (double) possibleAssignments;

        return recall;
    }

    public double getF1(int type) {
        double f1 = -1;

        double precision = 0;
        double recall = 0;

        precision = getPrecision(type);
        recall = getRecall(type);

        if (precision == 0 || recall == 0) {
            return 0.0;
        } else if (precision < 0 || recall < 0) {
            return f1;
        }

        f1 = 2 * precision * recall / (precision + recall);

        return f1;
    }

    public Map<String, CountMap<String>> getAssignments() {
        return assignments;
    }

    public void setAssignments(Map<String, CountMap<String>> assignments) {
        this.assignments = assignments;
    }

    @Override
    public String toString() {
        // StringBuilder builder = new StringBuilder();
        // builder.append("EvaluationResult [precision exact=");
        // builder.append(getPrecision(EXACT_MATCH));
        // builder.append(", precision MUC=");
        // builder.append(getPrecision(MUC));
        // builder.append(", recall exact=");
        // builder.append(getRecall(EXACT_MATCH));
        // builder.append(", recall MUC=");
        // builder.append(getRecall(MUC));
        // builder.append(", F1 exact=");
        // builder.append(getF1(EXACT_MATCH));
        // builder.append(", F1 MUC=");
        // builder.append(getF1(MUC));
        // builder.append("]");
        // return builder.toString();
        return NamedEntityRecognizer.printEvaluationDetails(this).toString();
    }

    public String getExactMatchResultsReadable() {
        StringBuilder builder = new StringBuilder();
        builder.append("precision exact: ");
        builder.append(MathHelper.round(100 * getPrecision(EXACT_MATCH), 2)).append("%");
        builder.append(", recall exact: ");
        builder.append(MathHelper.round(100 * getRecall(EXACT_MATCH), 2)).append("%");
        builder.append(", F1 exact: ");
        builder.append(MathHelper.round(100 * getF1(EXACT_MATCH), 2)).append("%");
        return builder.toString();
    }

    public String getMUCResultsReadable() {
        StringBuilder builder = new StringBuilder();
        builder.append("precision MUC: ");
        builder.append(MathHelper.round(100 * getPrecision(MUC), 2)).append("%");
        builder.append(", recall MUC: ");
        builder.append(MathHelper.round(100 * getRecall(MUC), 2)).append("%");
        builder.append(", F1 MUC: ");
        builder.append(MathHelper.round(100 * getF1(MUC), 2)).append("%");
        return builder.toString();
    }

    public void setGoldStandardAnnotations(Annotations goldStandardAnnotations) {
        this.goldStandardAnnotations = goldStandardAnnotations;
    }

    public Annotations getGoldStandardAnnotations() {
        return goldStandardAnnotations;
    }

    public void setErrorAnnotations(Map<String, Annotations> errorAnnotations) {
        this.errorAnnotations = errorAnnotations;
    }

    public Map<String, Annotations> getErrorAnnotations() {
        return errorAnnotations;
    }

}