package ws.palladian.extraction.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.extraction.entity.evaluation.EvaluationAnnotation;
import ws.palladian.helper.io.FileHelper;

/**
 * A list of {@link Annotation}s.
 * 
 * @author David Urbansky
 * 
 */
public class Annotations extends ArrayList<Annotation> {

    private static final long serialVersionUID = -628839540653937643L;

    /**
     * Save the annotation list to a file.
     * 
     * @param outputFilePath The path where the annotation list should be saved to.
     */
    public void save(String outputFilePath) {

        sort();

        StringBuilder output = new StringBuilder();

        for (Annotation annotation : this) {

            output.append(annotation.getOffset()).append(";");
            output.append(annotation.getLength()).append(";");
            output.append(annotation.getEndIndex()).append(";");
            output.append(annotation.getEntity()).append(";");
            output.append(annotation.getMostLikelyTagName()).append("\n");

        }

        FileHelper.writeToFile(outputFilePath, output);

    }

    public void removeNestedAnnotations() {
        Annotations removedNested = new Annotations();

        sort();

        int lastEndIndex = 0;
        for (Annotation annotation : this) {

            // ignore nested annotations
            if (annotation.getOffset() < lastEndIndex) {
                continue;
            }

            removedNested.add(annotation);
            lastEndIndex = annotation.getEndIndex();
        }

        clear();
        this.addAll(removedNested);
    }

    /**
     * The order of annotations is important. Annotations are sorted by their offsets in ascending order.
     */
    public void sort() {
        Comparator<Annotation> c = new Comparator<Annotation>() {

            @Override
            public int compare(Annotation a1, Annotation a2) {
                return a1.getOffset() - a2.getOffset();
            }
        };

        Collections.sort(this, c);
    }

    public void sortByLength() {
        Comparator<Annotation> c = new Comparator<Annotation>() {

            @Override
            public int compare(Annotation a1, Annotation a2) {
                return a2.getLength() - a1.getLength();
            }
        };

        Collections.sort(this, c);
    }

    public void transformToEvaluationAnnotations() {

        Annotations evaluationAnnotations = new Annotations();

        for (Annotation annotation : this) {
            evaluationAnnotations.add(new EvaluationAnnotation(annotation));
        }

        clear();
        this.addAll(evaluationAnnotations);
    }

    @Override
    public boolean add(Annotation e) {
        for (Annotation a : this) {
            if (a.getOffset() == e.getOffset()) {
                return false;
            }
        }
        return super.add(e);
    }

    public void instanceCategoryToClassified() {
        for (Annotation annotation : this) {
            CategoryEntriesMap ces = new CategoryEntriesMap();
            ces.set(annotation.getTargetClass(), 1);
            annotation.setTags(ces);
        }
    }
}