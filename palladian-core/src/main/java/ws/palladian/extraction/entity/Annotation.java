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
public class Annotation implements Annotated {

    /** The category of the instance, null if not classified. */
    private CategoryEntriesMap tags = new CategoryEntriesMap();

    /** The start index of the annotation in the annotated text. */
    private int offset = -1;

    /** The length of the annotation. */
    private int length = -1;

    /** The annotated entity. */
    private String entity;

    private List<String> subTypes = null;

    public Annotation(int offset, String entityName, String tagName) {
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
    public int getIndex() {
        // TODO Auto-generated method stub
        return 0;
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

}
