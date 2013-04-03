package ws.palladian.extraction.entity.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.math.ConfusionMatrix;
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
 * <li>Exact match ({@link EvaluationMode.EXACT_MATCH}), that is, only if boundary and tag are assigned correctly, the
 * assignment is true positive. Error types are not taken into account, all errors are equally wrong.</li>
 * <li>MUC ({@link EvaluationMode.MUC}), takes error types into account. 1 point for correct tag (regardless of
 * boundaries), 1 point for correct text (regardless of tag). Totally correct (correct boundaries and correct tag) = 2
 * points</li>
 * </ol>
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class EvaluationResult {

    public EvaluationResult(Map<String, CountMap<ResultType>> assignments, Annotations goldStandardAnnotations,
            Map<ResultType, Annotations> errorAnnotations, ConfusionMatrix confusionMatrix) {
        this.assignments = assignments;
        this.goldStandardAnnotations = goldStandardAnnotations;
        this.errorAnnotations = errorAnnotations;
        this.confusionMatrix = confusionMatrix;
    }

    /** The annotations from the gold standard. */
    private final Annotations goldStandardAnnotations;

    /** All error annotations by error class. */
    private final Map<ResultType, Annotations> errorAnnotations;

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
    private final Map<String, CountMap<ResultType>> assignments;

    private final ConfusionMatrix confusionMatrix;

    public  enum EvaluationMode {
        /** The exact match evaluation mode. */
        EXACT_MATCH,
        /** The MUC evaluation mode. */
        MUC
    }

    public  enum ResultType {
        /** Tagged something that should not have been tagged. */
        ERROR1("ERROR1: Tagged something that should not have been tagged, false positive - bad for precision"),
        /** Completely missed to tag an entity. */
        ERROR2("ERROR2: Completely missed an annotation, false negative - bad for recall"),
        /** Exact same content but wrong tag. */
        ERROR3("ERROR3: Incorrect annotation type but correct boundaries - bad for precision and recall"),
        /** Overlapping content and correct tag. */
        ERROR4("ERROR4: Correct annotation type but incorrect boundaries - bad for precision and recall"),
        /** Overlapping content but wrong tag. */
        ERROR5("ERROR5: Incorrect annotation type and incorrect boundaries - bad for precision and recall"),
        /** Exact same content and correct tag. */
        CORRECT("CORRECT: Tag and boundaries correct"),
        /** Number of possible annotations. */
        POSSIBLE(null);

        private final String description;

        ResultType(String description) {
            this.description = description;
        }

        /**
         * <p>
         * Get an explanation about the result type.
         * </p>
         * 
         * @return
         */
        public String getDescription() {
            return description;
        }
    }

    /** A marker that marks special fields. */
    public static final String SPECIAL_MARKER = "#";

    public double getPrecisionFor(String tagName, EvaluationMode type) {
        double precision = -1;

        CountMap<ResultType> cm = assignments.get(tagName);

        if (cm == null) {
            return precision;
        }

        int correctAssignments = 0;
        int totalAssignments = 0;

        if (type == EvaluationMode.EXACT_MATCH) {

            correctAssignments = cm.getCount(ResultType.CORRECT);
            totalAssignments = cm.getCount(ResultType.ERROR1) + cm.getCount(ResultType.ERROR3)
                    + cm.getCount(ResultType.ERROR4) + cm.getCount(ResultType.ERROR5) + correctAssignments;

        } else if (type == EvaluationMode.MUC) {

            correctAssignments = cm.getCount(ResultType.ERROR3) + cm.getCount(ResultType.ERROR4) + 2
                    * cm.getCount(ResultType.CORRECT);
            totalAssignments = 2 * (cm.getCount(ResultType.ERROR1) + cm.getCount(ResultType.ERROR3)
                    + cm.getCount(ResultType.ERROR4) + cm.getCount(ResultType.ERROR5) + cm.getCount(ResultType.CORRECT));

        }

        if (totalAssignments == 0) {
            return precision;
        }

        precision = (double)correctAssignments / (double)totalAssignments;

        return precision;
    }

    public double getRecallFor(String tagName, EvaluationMode type) {
        double recall = -1;

        CountMap<ResultType> cm = assignments.get(tagName);

        if (cm == null) {
            return recall;
        }

        int correctAssignments = 0;
        int possibleAssignments = 0;

        if (type == EvaluationMode.EXACT_MATCH) {

            correctAssignments = cm.getCount(ResultType.CORRECT);
            possibleAssignments = cm.getCount(ResultType.POSSIBLE);

        } else if (type == EvaluationMode.MUC) {

            correctAssignments = cm.getCount(ResultType.ERROR3) + cm.getCount(ResultType.ERROR4) + 2
                    * cm.getCount(ResultType.CORRECT);
            possibleAssignments = 2 * cm.getCount(ResultType.POSSIBLE);

        }

        if (possibleAssignments == 0) {
            return recall;
        }

        recall = (double)correctAssignments / (double)possibleAssignments;

        return recall;
    }

    public double getF1For(String tagName, EvaluationMode type) {
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

    public double getTagAveragedPrecision(EvaluationMode type) {

        double totalPrecision = 0;

        // count number of tags with not undefined precisions (precision > -1)
        double totalPrecisionsSet = 0;

        for (Entry<String, CountMap<ResultType>> tagEntry : assignments.entrySet()) {
            double tagPrecision = getPrecisionFor(tagEntry.getKey(), type);
            if (tagPrecision > -1) {
                totalPrecision += tagPrecision;
                totalPrecisionsSet++;
            }
        }

        double tagAveragedPrecision = totalPrecision / totalPrecisionsSet;
        return tagAveragedPrecision;
    }

    public double getTagAveragedRecall(EvaluationMode type) {

        double totalRecall = 0;

        // count number of tags with not undefined recall (recall > -1)
        double totalRecallsSet = 0;

        for (Entry<String, CountMap<ResultType>> tagEntry : assignments.entrySet()) {
            double tagRecall = getRecallFor(tagEntry.getKey(), type);
            if (tagRecall > -1) {
                totalRecall += tagRecall;
                totalRecallsSet++;
            }
        }

        double tagAveragedRecall = totalRecall / totalRecallsSet;
        return tagAveragedRecall;
    }

    public double getTagAveragedF1(EvaluationMode type) {

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

    public double getPrecision(EvaluationMode type) {
        double precision = 0;

        int correctAssignments = 0;
        int totalAssignments = 0;

        for (Entry<String, CountMap<ResultType>> tagEntry : assignments.entrySet()) {

            CountMap<ResultType> cm = tagEntry.getValue();

            if (cm == null) {
                continue;
            }

            if (type == EvaluationMode.EXACT_MATCH) {

                correctAssignments += cm.getCount(ResultType.CORRECT);
                totalAssignments += cm.getCount(ResultType.ERROR1) + cm.getCount(ResultType.ERROR3)
                        + cm.getCount(ResultType.ERROR4) + cm.getCount(ResultType.ERROR5)
                        + cm.getCount(ResultType.CORRECT);

            } else if (type == EvaluationMode.MUC) {

                correctAssignments += cm.getCount(ResultType.ERROR3) + cm.getCount(ResultType.ERROR4) + 2
                        * cm.getCount(ResultType.CORRECT);
                totalAssignments += 2 * (cm.getCount(ResultType.ERROR1) + cm.getCount(ResultType.ERROR3)
                        + cm.getCount(ResultType.ERROR4) + cm.getCount(ResultType.ERROR5) + cm
                        .getCount(ResultType.CORRECT));

            }

        }

        precision = (double)correctAssignments / (double)totalAssignments;

        return precision;
    }

    public double getRecall(EvaluationMode type) {
        double recall = 0;

        int correctAssignments = 0;
        int possibleAssignments = 0;

        for (Entry<String, CountMap<ResultType>> tagEntry : assignments.entrySet()) {

            CountMap<ResultType> cm = tagEntry.getValue();

            if (cm == null) {
                continue;
            }

            if (type == EvaluationMode.EXACT_MATCH) {

                correctAssignments += cm.getCount(ResultType.CORRECT);
                possibleAssignments += cm.getCount(ResultType.POSSIBLE);

            } else if (type == EvaluationMode.MUC) {

                correctAssignments += cm.getCount(ResultType.ERROR3) + cm.getCount(ResultType.ERROR4) + 2
                        * cm.getCount(ResultType.CORRECT);
                possibleAssignments += 2 * cm.getCount(ResultType.POSSIBLE);

            }

        }

        recall = (double)correctAssignments / (double)possibleAssignments;

        return recall;
    }

    public double getF1(EvaluationMode type) {
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getExactMatchResultsReadable());
        builder.append(", ");
        builder.append(getMUCResultsReadable());
        return builder.toString();
    }

    public String getExactMatchResultsReadable() {
        StringBuilder builder = new StringBuilder();
        builder.append("precision exact: ");
        builder.append(MathHelper.round(100 * getPrecision(EvaluationMode.EXACT_MATCH), 2)).append("%");
        builder.append(", recall exact: ");
        builder.append(MathHelper.round(100 * getRecall(EvaluationMode.EXACT_MATCH), 2)).append("%");
        builder.append(", F1 exact: ");
        builder.append(MathHelper.round(100 * getF1(EvaluationMode.EXACT_MATCH), 2)).append("%");
        return builder.toString();
    }

    public String getMUCResultsReadable() {
        StringBuilder builder = new StringBuilder();
        builder.append("precision MUC: ");
        builder.append(MathHelper.round(100 * getPrecision(EvaluationMode.MUC), 2)).append("%");
        builder.append(", recall MUC: ");
        builder.append(MathHelper.round(100 * getRecall(EvaluationMode.MUC), 2)).append("%");
        builder.append(", F1 MUC: ");
        builder.append(MathHelper.round(100 * getF1(EvaluationMode.MUC), 2)).append("%");
        return builder.toString();
    }

    public String getEvaluationDetails() {

        // write evaluation results to file
        StringBuilder results = new StringBuilder();

        results.append("Number of distinct tags:; ").append(assignments.size()).append("\n");
        results.append("Total annotations in test set:; ").append(getGoldStandardAnnotations().size()).append("\n");
        results.append("Confusion Matrix:\n");

        results.append("predicted\\real;");

        // order of tag names for matrix
        List<String> tagOrder = new ArrayList<String>();
        for (String tagName : assignments.keySet()) {
            tagOrder.add(tagName);
            results.append(tagName).append(";");
        }
        // add "OTHER" in case of ERROR1
        tagOrder.add(EvaluationResult.SPECIAL_MARKER + "OTHER" + EvaluationResult.SPECIAL_MARKER);
        results.append(EvaluationResult.SPECIAL_MARKER + "OTHER" + EvaluationResult.SPECIAL_MARKER).append(";");

        results.append("#total number;Exact Match Precision;Exact Match Recall;Exact Match F1;MUC Precision;MUC Recall;MUC F1\n");

        int totalTagAssignments = 0;
        for (Entry<String, CountMap<ResultType>> tagEntry : assignments.entrySet()) {

            String predictedTageName = tagEntry.getKey();
            int totalNumber = 0;

            results.append(tagEntry.getKey()).append(";");

            // write frequencies of confusion matrix
            for (String tagName : tagOrder) {
                int confusionCount = confusionMatrix.getConfusions(tagName, predictedTageName);
                results.append(confusionCount).append(";");
                totalNumber += confusionCount;
            }

            // total number of real tags in test set
            results.append(totalNumber).append(";");
            totalTagAssignments += totalNumber;

            // precision, recall, and F1 for exact match
            results.append(getPrecisionFor(tagEntry.getKey(), EvaluationMode.EXACT_MATCH)).append(";");
            results.append(getRecallFor(tagEntry.getKey(), EvaluationMode.EXACT_MATCH)).append(";");
            results.append(getF1For(tagEntry.getKey(), EvaluationMode.EXACT_MATCH)).append(";");

            // precision, recall, and F1 for MUC score
            results.append(getPrecisionFor(tagEntry.getKey(), EvaluationMode.MUC)).append(";");
            results.append(getRecallFor(tagEntry.getKey(), EvaluationMode.MUC)).append(";");
            results.append(getF1For(tagEntry.getKey(), EvaluationMode.MUC)).append("\n");

        }

        // write last line with averages over all tags
        results.append("ALL TAGS;");
        for (String tagName : tagOrder) {
            int totalAssignments = confusionMatrix.getRealDocuments(tagName);
            results.append(totalAssignments).append(";");
        }

        // total assignments
        results.append(totalTagAssignments).append(";");

        // precision, recall, and F1 for exact match
        results.append("tag averaged:")
                .append(MathHelper.round(getTagAveragedPrecision(EvaluationMode.EXACT_MATCH), 4)).append(", overall:");
        results.append(MathHelper.round(getPrecision(EvaluationMode.EXACT_MATCH), 4)).append(";");
        results.append("tag averaged:").append(MathHelper.round(getTagAveragedRecall(EvaluationMode.EXACT_MATCH), 4))
                .append(", overall:");
        results.append(MathHelper.round(getRecall(EvaluationMode.EXACT_MATCH), 4)).append(";");
        results.append("tag averaged:").append(MathHelper.round(getTagAveragedF1(EvaluationMode.EXACT_MATCH), 4))
                .append(", overall:");
        results.append(MathHelper.round(getF1(EvaluationMode.EXACT_MATCH), 4)).append(";");

        // precision, recall, and F1 for MUC score
        results.append("tag averaged:").append(MathHelper.round(getTagAveragedPrecision(EvaluationMode.MUC), 4))
                .append(", overall:");
        results.append(MathHelper.round(getPrecision(EvaluationMode.MUC), 4)).append(";");
        results.append("tag averaged:").append(MathHelper.round(getTagAveragedRecall(EvaluationMode.MUC), 4))
                .append(", overall:");
        results.append(MathHelper.round(getRecall(EvaluationMode.MUC), 4)).append(";");
        results.append("tag averaged:").append(MathHelper.round(getTagAveragedF1(EvaluationMode.MUC), 4))
                .append(", overall:");
        results.append(MathHelper.round(getF1(EvaluationMode.MUC), 4)).append("\n");

        Map<ResultType, String> resultTypes = new TreeMap<ResultType, String>();
        resultTypes.put(ResultType.ERROR1, "ERROR 1: Completely Incorrect Annotations");
        resultTypes.put(ResultType.ERROR2, "ERROR 2: Missed Annotations");
        resultTypes.put(ResultType.ERROR3, "ERROR 3: Correct Boundaries, Wrong Tag");
        resultTypes.put(ResultType.ERROR4, "ERROR 4: Wrong Boundaries, Correct Tag");
        resultTypes.put(ResultType.ERROR5, "ERROR 5: Wrong Boundaries, Wrong Tag");

        results.append("\n\n");
        results.append("CORRECT:");
        results.append(" : ").append(errorAnnotations.get(ResultType.CORRECT).size()).append("\n");
        for (Entry<ResultType, String> errorTypeEntry : resultTypes.entrySet()) {
            results.append(errorTypeEntry.getValue());
            results.append(" : ").append(errorAnnotations.get(errorTypeEntry.getKey()).size()).append("\n");
        }

        for (Entry<ResultType, String> errorTypeEntry : resultTypes.entrySet()) {
            results.append("\n\n");
            results.append(errorTypeEntry.getValue());
            results.append(" (total: ").append(errorAnnotations.get(errorTypeEntry.getKey()).size()).append("):\n\n");

            CountMap<String> cm = getAnnotationCountForTag(errorAnnotations.get(errorTypeEntry.getKey()));
            for (String item : cm) {
                results.append(item).append(":; ").append(cm.getCount(item)).append("\n");
            }
            results.append("\n");
            for (Annotation annotation : errorAnnotations.get(errorTypeEntry.getKey())) {
                results.append("  ").append(annotation).append("\n");
            }
        }

        return results.toString();
    }

    private static CountMap<String> getAnnotationCountForTag(Annotations annotations) {
        CountMap<String> cm = CountMap.create();
        for (Annotation annotation : annotations) {
            if (annotation instanceof EvaluationAnnotation) {
                cm.add(annotation.getTargetClass());
            } else {
                cm.add(annotation.getMostLikelyTagName());
            }
        }
        return cm;
    }

    public Annotations getGoldStandardAnnotations() {
        return goldStandardAnnotations;
    }

    public Map<ResultType, Annotations> getErrorAnnotations() {
        return errorAnnotations;
    }

}