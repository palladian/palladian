package ws.palladian.extraction.location.evaluation;

import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.CORRECT;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR1;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR2;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR4;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractorUtils.LocationDocument;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * Evaluation result for toponym disambiguation.
 * </p>
 * 
 * @author Philipp Katz
 */
class GeoEvaluationResult {

    /** ignore other types than CITY and POI because they are too broad. */
    private static final List<String> CONSIDERED_TYPES = Arrays.asList("CITY", "POI");

    private static final class EvaluationItem implements Comparable<EvaluationItem> {

        public EvaluationItem(String file, Annotation annotation, ResultType resultType, GeoCoordinate goldCoordinate,
                GeoCoordinate taggedCoordinate) {
            this.file = file;
            this.annotation = annotation;
            this.resultType = resultType;
            this.goldCoord = goldCoordinate;
            this.taggedCoord = taggedCoordinate;
        }

        String file;
        Annotation annotation;
        ResultType resultType;
        GeoCoordinate goldCoord;
        GeoCoordinate taggedCoord;

        @Override
        public int compareTo(EvaluationItem other) {
            int result = this.file.compareTo(other.file);
            if (result != 0) {
                return result;
            }
            return Integer.valueOf(this.annotation.getStartPosition()).compareTo(other.annotation.getStartPosition());
        }

        public Double getDistance() {
            if (goldCoord == null || goldCoord.getLatitude() == null || goldCoord.getLongitude() == null) {
                return null;
            }
            if (taggedCoord == null || taggedCoord.getLatitude() == null || taggedCoord.getLongitude() == null) {
                return null;
            }
            return GeoUtils.getDistance(goldCoord, taggedCoord);
        }

        public String toCsvLine() {
            StringBuilder builder = new StringBuilder();
            builder.append(file).append(';');
            builder.append(annotation.getStartPosition()).append(';');
            builder.append(annotation.getTag()).append(';');
            builder.append(annotation.getValue()).append(';');
            builder.append(resultType).append(';');
            builder.append(goldCoord != null ? goldCoord.getLatitude() : "").append(';');
            builder.append(goldCoord != null ? goldCoord.getLongitude() : "").append(';');
            builder.append(taggedCoord != null ? taggedCoord.getLatitude() : "").append(';');
            builder.append(taggedCoord != null ? taggedCoord.getLongitude() : "").append(';');
            Double distance = getDistance();
            builder.append(distance != null ? getDistance() : "");
            return builder.toString();
        }
    }

    private final List<EvaluationItem> completeEvaluationList = CollectionHelper.newArrayList();

    private int correct = 0;

    private int retrieved = 0;

    private int relevant = 0;

    private final String extractorName;

    private final String datasetPath;

    public GeoEvaluationResult(String extractorName, String datasetPath) {
        this.extractorName = extractorName;
        this.datasetPath = datasetPath;
    }

    private void add(List<EvaluationItem> evaluationList) {

        for (EvaluationItem item : evaluationList) {

            if (!CONSIDERED_TYPES.contains(item.annotation.getTag())) {
                continue;
            }

            // when gold standard has no location, ignore
            if (item.goldCoord == null) {
                continue;
            }

            Double distance = item.getDistance();
            if (distance != null && distance < 100) {
                correct++;
            }
            if (Arrays.asList(CORRECT, ERROR4, ERROR1).contains(item.resultType)) {
                retrieved++;
            }
            if (Arrays.asList(CORRECT, ERROR4, ERROR2).contains(item.resultType)) {
                relevant++;
            }
        }

        completeEvaluationList.addAll(evaluationList);
    }

    public void addResultFromDocument(LocationDocument document, List<LocationAnnotation> result) {
        List<EvaluationItem> evaluationList = CollectionHelper.newArrayList();
        Set<Annotation> taggedAnnotations = CollectionHelper.newHashSet();
        String fileName = document.getFileName();

        // evaluate
        for (LocationAnnotation assignedAnnotation : result) {

            boolean taggedOverlap = false;
            int counter = 0;

            for (LocationAnnotation goldAnnotation : document.getAnnotations()) {
                counter++;

                GeoCoordinate goldCoordinate = goldAnnotation.getLocation();

                if (assignedAnnotation.congruent(goldAnnotation)) {
                    // same start and end
                    taggedAnnotations.add(goldAnnotation);
                    evaluationList.add(new EvaluationItem(fileName, goldAnnotation, CORRECT, goldCoordinate,
                            assignedAnnotation.getLocation()));
                    break;
                } else if (assignedAnnotation.overlaps(goldAnnotation)) {
                    // overlap
                    taggedOverlap = true;
                    taggedAnnotations.add(goldAnnotation);
                    evaluationList.add(new EvaluationItem(fileName, goldAnnotation, ERROR4, goldCoordinate,
                            assignedAnnotation.getLocation()));
                } else if (assignedAnnotation.getStartPosition() < goldAnnotation.getEndPosition()
                        || counter == document.getAnnotations().size()) {
                    if (!taggedOverlap) {
                        // false alarm
                        evaluationList.add(new EvaluationItem(fileName, assignedAnnotation, ERROR1, null,
                                assignedAnnotation.getLocation()));
                    }
                    break;
                } else {
                    continue;
                }
            }
        }

        // check which gold standard annotations have not been found by the NER (error2)
        for (LocationAnnotation goldAnnotation : document.getAnnotations()) {
            if (!taggedAnnotations.contains(goldAnnotation)) {
                GeoCoordinate goldCooardinate = goldAnnotation.getLocation();
                evaluationList.add(new EvaluationItem(fileName, goldAnnotation, ERROR2, goldCooardinate, null));
            }
        }

        Collections.sort(evaluationList);
        add(evaluationList);
    }

    public int getCorrect() {
        return correct;
    }

    public int getRetrieved() {
        return retrieved;
    }

    public int getRelevant() {
        return relevant;
    }

    public double getPrecision() {
        return (float)correct / retrieved;
    }

    public double getRecall() {
        return (float)correct / relevant;
    }

    public double getF1() {
        return 2 * getPrecision() * getRecall() / (getPrecision() + getRecall());
    }

    public void writeDetailedReport(File fileName) {
        StringBuilder evaluationDetails = new StringBuilder();

        evaluationDetails.append("# Result for:").append(extractorName).append('\n');
        evaluationDetails.append("# Using dataset:").append(datasetPath).append('\n');

        evaluationDetails.append("#\n");
        evaluationDetails.append("#\n");

        evaluationDetails.append("# num correct: " + getCorrect()).append('\n');
        evaluationDetails.append("# num retrieved: " + getRetrieved()).append('\n');
        evaluationDetails.append("# relevant: " + getRelevant()).append('\n');

        evaluationDetails.append("# precision: " + getPrecision()).append('\n');
        evaluationDetails.append("# recall: " + getRecall()).append('\n');
        evaluationDetails.append("# f1: " + getF1()).append('\n');
        evaluationDetails.append("#\n");
        evaluationDetails.append("#\n");

        evaluationDetails
                .append("file;offset;type;value;annotationResult;goldLat;goldLng;taggedLat;taggedLng;distance\n");
        for (EvaluationItem item : completeEvaluationList) {
            evaluationDetails.append(item.toCsvLine()).append('\n');
        }

        FileHelper.writeToFile(fileName.getPath(), evaluationDetails.toString());

    }

    public String getSummary() {
        StringBuilder result = new StringBuilder();
        result.append("Precision-Geo: ").append(getPrecision()).append('\n');
        result.append("Recall-Geo: ").append(getRecall()).append('\n');
        result.append("F1-Geo: ").append(getF1());
        return result.toString();
    }

}
