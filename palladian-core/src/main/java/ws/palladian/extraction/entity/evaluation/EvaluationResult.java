package ws.palladian.extraction.entity.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.Annotation;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * In NER there are five possible errors that can influence evaluation:
 * <ul>
 * <li>ERROR 1: tagged something that should not have been tagged</li>
 * <li>ERROR 2: missed an entity</li>
 * <li>ERROR 3: correct boundaries but wrong tag</li>
 * <li>ERROR 4: correctly tagged an entity but either too much or too little (wrong boundaries)</li>
 * <li>ERROR 5: wrong boundaries and wrong tag</li>
 * </ul>
 * For more information see <a
 * href="http://nlp.cs.nyu.edu/sekine/papers/li07.pdf">http://nlp.cs.nyu.edu/sekine/papers/li07.pdf</a> page 14.
 * </p>
 * 
 * <p>
 * We can evaluate using two approaches:
 * <ol>
 * <li>Exact match ({@link EvaluationMode#EXACT_MATCH}), that is, only if boundary and tag are assigned correctly, the
 * assignment is true positive. Error types are not taken into account, all errors are equally wrong.</li>
 * <li>MUC ({@link EvaluationMode#MUC}), takes error types into account. 1 point for correct tag (regardless of
 * boundaries), 1 point for correct text (regardless of tag). Totally correct (correct boundaries and correct tag) = 2
 * points</li>
 * </ol>
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class EvaluationResult {

    public enum EvaluationMode {
        /** Evaluate recognition, ignore annotation type. */
        RECOGNITION,
        /** The exact match evaluation mode. */
        EXACT_MATCH,
        /** The MUC evaluation mode. */
        MUC
    }

    public enum ResultType {
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
        CORRECT("CORRECT: Tag and boundaries correct");

        private final String description;

        ResultType(String description) {
            this.description = description;
        }

        /**
         * <p>
         * Get an explanation about the result type.
         * </p>
         * 
         * @return Explanation for result type.
         */
        public String getDescription() {
            return description;
        }
    }

    /** A marker that marks special fields. */
    private static final String SPECIAL_MARKER = "#";

    /** Marker which is used in case of {@link ResultType#ERROR1}. */
    private static final String OTHER_MARKER = SPECIAL_MARKER + "OTHER" + SPECIAL_MARKER;

    /** Keep {@link Annotation}s indexed by {@link ResultType}. */
    private final MultiMap<ResultType, Annotation> resultAnnotations;

    /** Keep counts of actual tag assignments. */
    private final Bag<String> actualAssignments;

    /** Keep counts of tag assignments from gold standard. */
    private final Bag<String> possibleAssignments;

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
    private final Map<String, Bag<ResultType>> assignments;

    private final ConfusionMatrix confusionMatrix;

    /**
     * <p>
     * Create a new {@link EvaluationResult} based on the given gold standard.
     * </p>
     * 
     * @param goldStandard The gold standard, not <code>null</code>.
     */
    public EvaluationResult(List<? extends Annotation> goldStandard) {
        Validate.notNull(goldStandard, "goldStandard must not be null");
        this.assignments = LazyMap.create(new Bag.BagFactory<ResultType>());
        this.resultAnnotations = DefaultMultiMap.createWithList();
        this.confusionMatrix = new ConfusionMatrix();
        this.actualAssignments = Bag.create();
        this.possibleAssignments = Bag.create();
        for (Annotation annotation : goldStandard) {
            possibleAssignments.add(annotation.getTag());
        }
    }

    public double getPrecisionFor(String tagName, EvaluationMode type) {
        int actualAssignments = getActualAssignments(tagName);
        if (actualAssignments == 0) {
            return -1;
        }
        int correctAssignments = 0;
        if (type == EvaluationMode.EXACT_MATCH) {
            correctAssignments = getResultTypeCount(tagName, ResultType.CORRECT);
        } else if (type == EvaluationMode.MUC) {
            correctAssignments = getWeightedMuc(tagName);
            actualAssignments *= 2;
        } else if (type == EvaluationMode.RECOGNITION) {
            correctAssignments = getResultTypeCount(tagName, ResultType.CORRECT)
                    + getResultTypeCount(tagName, ResultType.ERROR3);
        }
        return (double)correctAssignments / actualAssignments;
    }

    public double getRecallFor(String tagName, EvaluationMode type) {
        int possibleAssignments = getPossibleAssignments(tagName);
        if (possibleAssignments == 0) {
            return -1;
        }
        int correctAssignments = 0;
        if (type == EvaluationMode.EXACT_MATCH) {
            correctAssignments = getResultTypeCount(tagName, ResultType.CORRECT);
        } else if (type == EvaluationMode.MUC) {
            correctAssignments = getWeightedMuc(tagName);
            possibleAssignments *= 2;
        } else if (type == EvaluationMode.RECOGNITION) {
            correctAssignments = getResultTypeCount(tagName, ResultType.CORRECT)
                    + getResultTypeCount(tagName, ResultType.ERROR3);
        }
        return (double)correctAssignments / possibleAssignments;
    }

    /** Get correct assignments weighted by MUC scheme. */
    private int getWeightedMuc(String tagName) {
        return getResultTypeCount(tagName, ResultType.ERROR3) + getResultTypeCount(tagName, ResultType.ERROR4) + 2
                * getResultTypeCount(tagName, ResultType.CORRECT);
    }

    public double getF1For(String tagName, EvaluationMode type) {
        double precision = getPrecisionFor(tagName, type);
        double recall = getRecallFor(tagName, type);
        if (precision == 0 || recall == 0) {
            return 0.0;
        }
        if (precision == -1 || recall == -1) {
            return -1;
        }
        return 2 * precision * recall / (precision + recall);
    }

    public double getTagAveragedPrecision(EvaluationMode type) {
        double totalPrecision = 0;

        // count number of tags with not undefined precisions (precision > -1)
        double totalPrecisionsSet = 0;

        for (String tagName : assignments.keySet()) {
            double tagPrecision = getPrecisionFor(tagName, type);
            if (tagPrecision > -1) {
                totalPrecision += tagPrecision;
                totalPrecisionsSet++;
            }
        }
        return totalPrecision / totalPrecisionsSet;
    }

    public double getTagAveragedRecall(EvaluationMode type) {
        double totalRecall = 0;

        // count number of tags with not undefined recall (recall > -1)
        double totalRecallsSet = 0;

        for (String tagName : assignments.keySet()) {
            double tagRecall = getRecallFor(tagName, type);
            if (tagRecall > -1) {
                totalRecall += tagRecall;
                totalRecallsSet++;
            }
        }
        return totalRecall / totalRecallsSet;
    }

    public double getTagAveragedF1(EvaluationMode type) {
        double precision = getTagAveragedPrecision(type);
        double recall = getTagAveragedRecall(type);
        if (precision == 0 || recall == 0) {
            return 0.0;
        }
        if (precision < 0 || recall < 0) {
            return -1;
        }
        return 2 * precision * recall / (precision + recall);
    }

    public double getPrecision(EvaluationMode type) {
        int sumCorrect = 0;
        int sumTotal = getActualAssignments();
        if (type == EvaluationMode.MUC) {
            sumTotal *= 2;
        }
        for (String tagName : assignments.keySet()) {
            if (type == EvaluationMode.EXACT_MATCH) {
                sumCorrect += getResultTypeCount(tagName, ResultType.CORRECT);
            } else if (type == EvaluationMode.MUC) {
                sumCorrect += getWeightedMuc(tagName);
            } else if (type == EvaluationMode.RECOGNITION) {
                sumCorrect += getResultTypeCount(tagName, ResultType.CORRECT)
                        + getResultTypeCount(tagName, ResultType.ERROR3);
            }
        }
        return (double)sumCorrect / sumTotal;
    }

    public double getRecall(EvaluationMode type) {
        int sumCorrect = 0;
        int sumPossible = getPossibleAssignments();
        if (type == EvaluationMode.MUC) {
            sumPossible *= 2;
        }
        for (String tagName : assignments.keySet()) {
            if (type == EvaluationMode.EXACT_MATCH) {
                sumCorrect += getResultTypeCount(tagName, ResultType.CORRECT);
            } else if (type == EvaluationMode.MUC) {
                sumCorrect += getWeightedMuc(tagName);
            } else if (type == EvaluationMode.RECOGNITION) {
                sumCorrect += getResultTypeCount(tagName, ResultType.CORRECT)
                        + getResultTypeCount(tagName, ResultType.ERROR3);
            }
        }
        return (double)sumCorrect / sumPossible;
    }

    public double getF1(EvaluationMode type) {
        double precision = getPrecision(type);
        double recall = getRecall(type);
        if (precision == 0 || recall == 0) {
            return 0.0;
        }
        if (precision < 0 || recall < 0) {
            return -1;
        }
        return 2 * precision * recall / (precision + recall);
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
        results.append("Total annotations in test set:; ").append(getPossibleAssignments()).append("\n");
        results.append("Confusion Matrix:\n");

        results.append("predicted\\real;");

        // order of tag names for matrix
        List<String> tagOrder = new ArrayList<String>();
        for (String tagName : assignments.keySet()) {
            tagOrder.add(tagName);
            results.append(tagName).append(";");
        }
        // add "OTHER" in case of ERROR1
        tagOrder.add(OTHER_MARKER);
        results.append(OTHER_MARKER).append(";");

        results.append("#total number;Exact Match Precision;Exact Match Recall;Exact Match F1;MUC Precision;MUC Recall;MUC F1\n");

        int totalTagAssignments = 0;
        for (String predictedTag : assignments.keySet()) {

            int totalNumber = 0;

            results.append(predictedTag).append(";");

            // write frequencies of confusion matrix
            for (String tagName : tagOrder) {
                int confusionCount = confusionMatrix.getConfusions(tagName, predictedTag);
                results.append(confusionCount).append(";");
                totalNumber += confusionCount;
            }

            // total number of real tags in test set
            results.append(totalNumber).append(";");
            totalTagAssignments += totalNumber;

            // precision, recall, and F1 for exact match
            results.append(getPrecisionFor(predictedTag, EvaluationMode.EXACT_MATCH)).append(";");
            results.append(getRecallFor(predictedTag, EvaluationMode.EXACT_MATCH)).append(";");
            results.append(getF1For(predictedTag, EvaluationMode.EXACT_MATCH)).append(";");

            // precision, recall, and F1 for MUC score
            results.append(getPrecisionFor(predictedTag, EvaluationMode.MUC)).append(";");
            results.append(getRecallFor(predictedTag, EvaluationMode.MUC)).append(";");
            results.append(getF1For(predictedTag, EvaluationMode.MUC)).append("\n");

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

        results.append("\n\n");
        results.append(ResultType.CORRECT.getDescription()).append(" : ");
        results.append(getResultTypeCount(ResultType.CORRECT)).append('\n');
        for (ResultType resultType : ResultType.values()) {
            if (resultType == ResultType.CORRECT) {
                continue;
            }
            results.append(resultType.getDescription()).append(" : ");
            results.append(getResultTypeCount(resultType)).append('\n');
        }

        for (ResultType resultType : ResultType.values()) {
            if (resultType == ResultType.CORRECT) {
                continue; // skip correct annotations
            }
            results.append("\n\n");
            results.append(resultType.getDescription());
            results.append(" (total: ").append(getResultTypeCount(resultType)).append("):\n\n");

            Bag<String> cm = getAnnotationCount(resultType);
            for (String item : cm) {
                results.append(item).append(":; ").append(cm.count(item)).append("\n");
            }
            results.append("\n");
            if (getResultTypeCount(resultType) > 0) {
                for (Annotation annotation : resultAnnotations.get(resultType)) {
                    results.append("  ").append(annotation).append("\n");
                }
            }
        }

        return results.toString();
    }

    private Bag<String> getAnnotationCount(ResultType resultType) {
        Bag<String> counts = Bag.create();
        for (Annotation annotation : getAnnotations(resultType)) {
            counts.add(annotation.getTag());
        }
        return counts;
    }

    int getResultTypeCount(ResultType resultType) {
        Collection<Annotation> annotations = resultAnnotations.get(resultType);
        return annotations != null ? annotations.size() : 0;
    }

    int getActualAssignments() {
        return actualAssignments.size();
    }

    int getActualAssignments(String tagName) {
        return actualAssignments.count(tagName);
    }

    int getPossibleAssignments() {
        return possibleAssignments.size();
    }

    int getPossibleAssignments(String tagName) {
        return possibleAssignments.count(tagName);
    }

    int getResultTypeCount(String tagName, ResultType resultType) {
        return assignments.get(tagName).count(resultType);
    }

    public Collection<Annotation> getAnnotations(ResultType resultType) {
        Collection<Annotation> annotations = resultAnnotations.get(resultType);
        return annotations != null ? Collections.unmodifiableCollection(annotations) : Collections
                .<Annotation> emptyList();
    }

    /**
     * <p>
     * Add data to this evaluation result, consisting of a {@link ResultType}, the real annotation from the gold
     * standard, and the assigned {@link Annotation} from an NER.
     * </p>
     * 
     * @param resultType The type of the result, not <code>null</code>.
     * @param realAnnotation The real annotation from the gold standard, or <code>null</code> in case of
     *            {@link ResultType#ERROR1} (something was tagged, which is not in the gold standard).
     * @param nerAnnotation The annotation assigned by the NER, or <code>null</code> in case of
     *            {@link ResultType#ERROR2} (something from the gold standard was not tagged at all).
     */
    public void add(ResultType resultType, Annotation realAnnotation, Annotation nerAnnotation) {
        Validate.notNull(resultType, "resultType must not be null");

        switch (resultType) {
            case CORRECT:
            case ERROR3:
            case ERROR4:
            case ERROR5:
                actualAssignments.add(nerAnnotation.getTag());
                resultAnnotations.add(resultType, nerAnnotation);
                assignments.get(realAnnotation.getTag()).add(resultType);
                confusionMatrix.add(realAnnotation.getTag(), nerAnnotation.getTag());
                break;
            case ERROR1:
                actualAssignments.add(nerAnnotation.getTag());
                resultAnnotations.add(resultType, nerAnnotation);
                assignments.get(nerAnnotation.getTag()).add(resultType);
                confusionMatrix.add(OTHER_MARKER, nerAnnotation.getTag());
                break;
            case ERROR2:
                resultAnnotations.add(resultType, realAnnotation);
                assignments.get(realAnnotation.getTag()).add(resultType);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    public void merge(EvaluationResult result) {
        this.resultAnnotations.addAll(result.resultAnnotations);
        this.actualAssignments.addAll(result.actualAssignments);
        this.possibleAssignments.addAll(result.possibleAssignments);

        // merge assignments
        for (String assignment : result.assignments.keySet()) {
            Bag<ResultType> counts = result.assignments.get(assignment);
            this.assignments.get(assignment).addAll(counts);
        }
        // merge confusion matrix
        Set<String> categories = result.confusionMatrix.getCategories();
        for (String realCategory : categories) {
            for (String predictedCategory : categories) {
                int count = result.confusionMatrix.getConfusions(realCategory, predictedCategory);
                this.confusionMatrix.add(realCategory, predictedCategory, count);
            }
        }
    }

}
