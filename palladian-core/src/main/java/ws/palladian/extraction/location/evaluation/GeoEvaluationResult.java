package ws.palladian.extraction.location.evaluation;

import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.CORRECT;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR1;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR2;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR4;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.FatStats;

/**
 * <p>
 * Evaluation result for toponym disambiguation.
 * </p>
 * 
 * @author Philipp Katz
 */
public class GeoEvaluationResult {

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
            if (goldCoord == null) {
                return null;
            }
            if (taggedCoord == null) {
                return null;
            }
            return goldCoord.distance(taggedCoord);
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

    private final List<EvaluationItem> completeEvaluationList = new ArrayList<>();

    private int correct = 0;

    private int retrieved = 0;

    private int relevant = 0;

    private final String extractorName;

    private final String datasetName;

    private final FatStats errorDistanceStats = new FatStats();

    public GeoEvaluationResult(String extractorName, String datasetName) {
        this.extractorName = extractorName;
        this.datasetName = datasetName;
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
        addResultFromDocument(document.getFileName(), document.getAnnotations(), result);
    }
    
    public void addResultFromDocument(String fileName, List<LocationAnnotation> gold, List<LocationAnnotation> result) {
        List<EvaluationItem> evaluationList = new ArrayList<>();
        Set<Annotation> taggedAnnotations = new HashSet<>();

        // different approach: loop through the gold annotations,
        // get the resolved annotations, and check the accuracy

//        FatStats distanceStats = new FatStats();

        for (LocationAnnotation goldAnnotation : gold) {
//            System.out.println("*****");
//            System.out.println("gold: " + goldAnnotation);

            boolean resolved = false;

            for (LocationAnnotation assignedLocation : result) {
                if (goldAnnotation.overlaps(assignedLocation) || assignedLocation.overlaps(goldAnnotation)) {

//                    System.out.println("assigned: " + assignedLocation);

                    GeoCoordinate goldCoordinate = goldAnnotation.getLocation().getCoordinate();
                    if (goldCoordinate == null) {
//                        System.out.println("gold has no coordinate " + goldAnnotation);
                        continue;
                    }
                    GeoCoordinate assignedCoordinate = assignedLocation.getLocation().getCoords().orElse(GeoCoordinate.NULL);
                    errorDistanceStats.add(goldCoordinate.distance(assignedCoordinate));
                    resolved = true;

                    break; // continue with next gold annotation
                }

            }

//            if (!resolved) {
//                // gold annotation was not annotated, consider error with maximum distance
////                System.out.println("not assigned");
//                errorDistanceStats.add(GeoUtils.EARTH_MAX_DISTANCE_KM);
//            }

//            System.out.println("*****");
        }

//        System.out.println(distanceStats);

        // evaluate
        for (LocationAnnotation assignedAnnotation : result) {

            boolean taggedOverlap = false;
            int counter = 0;

            for (LocationAnnotation goldAnnotation : gold) {
                counter++;

                GeoCoordinate goldCoordinate = goldAnnotation.getLocation().getCoordinate();

                if (assignedAnnotation.congruent(goldAnnotation)) {
                    // same start and end
                    taggedAnnotations.add(goldAnnotation);
                    evaluationList.add(new EvaluationItem(fileName, goldAnnotation, CORRECT, goldCoordinate,
                            assignedAnnotation.getLocation().getCoordinate()));
                    break;
                } else if (assignedAnnotation.overlaps(goldAnnotation)) {
                    // overlap
                    taggedOverlap = true;
                    taggedAnnotations.add(goldAnnotation);
                    evaluationList.add(new EvaluationItem(fileName, goldAnnotation, ERROR4, goldCoordinate,
                            assignedAnnotation.getLocation().getCoordinate()));
                } else if (assignedAnnotation.getStartPosition() < goldAnnotation.getEndPosition()
                        || counter == gold.size()) {
                    if (!taggedOverlap) {
                        // false alarm
                        evaluationList.add(new EvaluationItem(fileName, assignedAnnotation, ERROR1, null,
                                assignedAnnotation.getLocation().getCoordinate()));
                    }
                    break;
                } else {
                    continue;
                }
            }
        }

        // check which gold standard annotations have not been found by the NER (error2)
        for (LocationAnnotation goldAnnotation : gold) {
            if (!taggedAnnotations.contains(goldAnnotation)) {
                GeoCoordinate goldCooardinate = goldAnnotation.getLocation().getCoordinate();
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

    public FatStats getErrorDistanceStats() {
        return errorDistanceStats;
    }

    public void writeDetailedReport(File fileName) {
        StringBuilder evaluationDetails = new StringBuilder();

        evaluationDetails.append("# Result for:").append(extractorName).append('\n');
        evaluationDetails.append("# Using dataset:").append(datasetName).append('\n');

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

        result.append("\n\nDistances:\n").append(errorDistanceStats);

        return result.toString();
    }

}
