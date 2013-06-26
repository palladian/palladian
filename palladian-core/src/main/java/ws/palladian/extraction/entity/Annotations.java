package ws.palladian.extraction.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.Annotated;

/**
 * <p>
 * A list of {@link Annotated}s.
 * </p>
 * 
 * @author David Urbansky
 */
public class Annotations<T extends Annotated> extends ArrayList<T> implements List<T> {

    private static final long serialVersionUID = 1L;

    public Annotations() {
    }

    public Annotations(Collection<? extends T> c) {
        super(c);
    }

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
        for (Annotated annotation : this) {
            output.append(annotation.getStartPosition()).append(";");
            output.append(annotation.getValue().length()).append(";");
            output.append(annotation.getEndPosition()).append(";");
            output.append(annotation.getValue()).append(";");
            output.append(annotation.getTag()).append("\n");
        }
        return output.toString();
    }

    public void removeNested() {
        sort();
        Iterator<T> iterator = iterator();
        int lastEndIndex = 0;
        while (iterator.hasNext()) {
            T annotation = iterator.next();
            // ignore nested annotations
            if (annotation.getStartPosition() < lastEndIndex) {
                iterator.remove();
                continue;
            }
            lastEndIndex = annotation.getEndPosition();
        }
    }

    /**
     * <p>
     * The order of annotations is important. Annotations are sorted by their offsets in ascending order.
     * </p>
     */
    public void sort() {
        Collections.sort(this);
    }

    @Override
    public boolean add(T e) {
        for (Annotated a : this) {
            if (a.getStartPosition() == e.getStartPosition()) {
                return false;
            }
        }
        return super.add(e);
    }

}
