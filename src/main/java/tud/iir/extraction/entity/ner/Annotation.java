package tud.iir.extraction.entity.ner;

import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.knowledge.Entity;

/**
 * An annotation made by a {@link NamedEntityRecognizer} when tagging a text.
 * 
 * @author David Urbansky
 * 
 */
public class Annotation {

    /** The start index of the annotation in the annotated text. */
    private int offset = -1;

    /** The length of the annotation. */
    private int length = -1;

    /** The annotated entity. */
    private Entity entity;

    /** The assigned tags for the entity. */
    private CategoryEntries tags = new CategoryEntries();

    public Annotation(Annotation annotation) {
        offset = annotation.getOffset();
        length = annotation.getLength();
        entity = annotation.getEntity();
        tags = annotation.getTags();
    }

    public Annotation(int offset, String entityName, String tagName) {
        this.offset = offset;
        this.length = entityName.length();
        entity = new Entity(entityName);
        tags.add(new CategoryEntry(tags, new Category(tagName), 1));
    }

    public Annotation(int offset, String entityName, CategoryEntries tags) {
        this.offset = offset;
        this.length = entityName.length();
        entity = new Entity(entityName);
        this.tags = tags;
    }

    public boolean matches(Annotation annotation) {
        if (getOffset() == annotation.getOffset() && getLength() == annotation.getLength()) {
            return true;
        }
        return false;
    }

    public boolean overlaps(Annotation annotation) {
        if (getOffset() <= annotation.getOffset() && getEndIndex() >= annotation.getOffset()
                || getOffset() <= annotation.getEndIndex() && getEndIndex() >= annotation.getEndIndex()) {
            return true;
        }
        return false;
    }

    public boolean sameTag(Annotation annotation) {
        if (getMostLikelyTag().getCategory().getName()
                .equalsIgnoreCase(annotation.getMostLikelyTag().getCategory().getName())) {
            return true;
        }
        return false;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getEndIndex() {
        return getOffset() + getLength();
    }

    public Entity getEntity() {
        return entity;
    }
    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public CategoryEntries getTags() {
        return tags;
    }

    public void setTags(CategoryEntries tags) {
        this.tags = tags;
    }

    public CategoryEntry getMostLikelyTag() {
        return getTags().getMostLikelyCategoryEntry();
    }

    public String getMostLikelyTagName() {
        return getTags().getMostLikelyCategoryEntry().getCategory().getName();
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
        builder.append(getMostLikelyTagName());
        builder.append("]");
        return builder.toString();
    }

}
