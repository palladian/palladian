package ws.palladian.extraction.entity;

import java.util.List;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.processing.features.Annotated;

/**
 * An annotation made by a {@link NamedEntityRecognizer} when tagging a text.
 * 
 * @author David Urbansky
 * 
 */
public class NerAnnotation implements Annotated {

    // XXX this should inherit from Annotation, but it has this stupid setters.

    /** The category of the instance, null if not classified. */
    private CategoryEntriesMap tags = new CategoryEntriesMap();

    /** The start index of the annotation in the annotated text. */
    private int offset;

    /** The length of the annotation. */
    private int length;

    /** The annotated entity. */
    private String entity;

    private List<String> subTypes = null;

    public NerAnnotation(int offset, String entityName, String tagName) {
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        tags.set(tagName, 1);
    }

    public CategoryEntries getTags() {
        return tags;
    }

    public void addSubTypes(List<String> subTypes) {
        if (this.subTypes == null) {
            this.subTypes = subTypes;
        } else {
            this.subTypes.addAll(subTypes);
        }
    }

    public List<String> getSubTypes() {
        return subTypes;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setTags(CategoryEntries tags) {
        this.tags = new CategoryEntriesMap(tags);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotation [offset=");
        builder.append(offset);
        builder.append(", length=");
        builder.append(length);
        builder.append(", entity=");
        builder.append(entity);
        builder.append(", tag=");
        builder.append(getTag());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int getStartPosition() {
        return offset;
    }

    @Override
    public int getEndPosition() {
        return offset + length;
    }

    @Override
    public String getValue() {
        return entity;
    }

    @Override
    public String getTag() {
        return getTags().getMostLikelyCategory();
    }

    @Override
    public int compareTo(Annotated other) {
        return this.getStartPosition() - other.getStartPosition();
    }

    @Override
    // FIXME this needs to go in parent
    public boolean overlaps(Annotated annotated) {
        return NerHelper.overlaps(this, annotated);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entity == null) ? 0 : entity.hashCode());
        result = prime * result + length;
        result = prime * result + offset;
        result = prime * result + ((tags == null) ? 0 : tags.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NerAnnotation other = (NerAnnotation)obj;
        if (entity == null) {
            if (other.entity != null)
                return false;
        } else if (!entity.equals(other.entity))
            return false;
        if (length != other.length)
            return false;
        if (offset != other.offset)
            return false;
        if (tags == null) {
            if (other.tags != null)
                return false;
        } else if (!tags.equals(other.tags))
            return false;
        return true;
    }

}
