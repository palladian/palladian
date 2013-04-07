package ws.palladian.extraction.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ws.palladian.helper.io.FileHelper;

/**
 * A list of {@link Annotation}s.
 * 
 * @author David Urbansky
 * 
 */
public class Annotations extends ArrayList<Annotation> {

    public static final Comparator<Annotation> ANNOTATION_COMPARATOR = new Comparator<Annotation>() {
        @Override
        public int compare(Annotation a1, Annotation a2) {
            return a1.getStartPosition() - a2.getStartPosition();
        }
    };

    public static void removeNestedAnnotations(List<Annotation> annotations) {
        Annotations removedNested = new Annotations();
        Collections.sort(annotations, ANNOTATION_COMPARATOR);

        int lastEndIndex = 0;
        for (Annotation annotation : annotations) {

            // ignore nested annotations
            if (annotation.getStartPosition() < lastEndIndex) {
                continue;
            }

            removedNested.add(annotation);
            lastEndIndex = annotation.getEndPosition();
        }

        annotations.clear();
        annotations.addAll(removedNested);
    }

    private static final long serialVersionUID = -628839540653937643L;

    /**
     * Save the annotation list to a file.
     * 
     * @param outputFilePath The path where the annotation list should be saved to.
     */
    public void save(String outputFilePath) {

        String output = toString();

        FileHelper.writeToFile(outputFilePath, output);

    }

    @Override
    public String toString() {
        sort();
        StringBuilder output = new StringBuilder();
        for (Annotation annotation : this) {
            output.append(annotation.getStartPosition()).append(";");
            output.append(annotation.getLength()).append(";");
            output.append(annotation.getEndPosition()).append(";");
            output.append(annotation.getValue()).append(";");
            output.append(annotation.getTag()).append("\n");
        }
        return output.toString();
    }

    public void removeNestedAnnotations() {
        Annotations removedNested = new Annotations();

        sort();

        int lastEndIndex = 0;
        for (Annotation annotation : this) {

            // ignore nested annotations
            if (annotation.getStartPosition() < lastEndIndex) {
                continue;
            }

            removedNested.add(annotation);
            lastEndIndex = annotation.getEndPosition();
        }

        clear();
        this.addAll(removedNested);
    }

    /**
     * The order of annotations is important. Annotations are sorted by their offsets in ascending order.
     */
    public void sort() {
        Collections.sort(this, ANNOTATION_COMPARATOR);
    }

    @Override
    public boolean add(Annotation e) {
        for (Annotation a : this) {
            if (a.getStartPosition() == e.getStartPosition()) {
                return false;
            }
        }
        return super.add(e);
    }

}
