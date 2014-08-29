package ws.palladian.extraction.location;

import ws.palladian.core.Annotation;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.ImmutableAnnotation;

public final class ClassifiedAnnotation extends ImmutableAnnotation {

    private final CategoryEntries categoryEntries;

    public ClassifiedAnnotation(Annotation annotation, CategoryEntries categoryEntries) {
        super(annotation.getStartPosition(), annotation.getValue(), categoryEntries.getMostLikelyCategory());
        this.categoryEntries = categoryEntries;
    }

    public CategoryEntries getCategoryEntries() {
        return categoryEntries;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getValue());
        builder.append(" (");
        builder.append(getStartPosition()).append("-").append(getEndPosition()).append(",");
        builder.append(getTag()).append(",");
        builder.append(categoryEntries).append(")");
        return builder.toString();
    }

}
