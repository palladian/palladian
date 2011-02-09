package tud.iir.extraction.entity.ner;

import java.util.List;

import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.page.ClassificationDocument;
import tud.iir.classification.page.DictionaryClassifier;
import tud.iir.classification.page.Preprocessor;
import tud.iir.helper.StringHelper;

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

    /** The left context of the annotation */
    private String leftContext = "";

    /** The right context of the annotation */
    private String rightContext = "";

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
                || getOffset() <= annotation.getEndIndex() && getEndIndex() >= annotation.getOffset()) {
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

    public String getLeftContext() {
        return leftContext;
    }

    public void setLeftContext(String leftContext) {
        this.leftContext = leftContext;
    }

    public String getRightContext() {
        return rightContext;
    }

    public void setRightContext(String rightContext) {
        this.rightContext = rightContext;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotation [offset=");
        builder.append(offset);
        builder.append(", length=");
        builder.append(length);
        builder.append(", entity=");
        builder.append(entity.getName());
        builder.append(", tag=");
        builder.append(getMostLikelyTagName());
        builder.append("]");
        return builder.toString();
    }

    /**
     * Try to find which of the given annotation are part of this entity. For example: "New York City and Dresden"
     * contains two entities that might be in the given annotation set. If so, we return the found annotations.
     * 
     * @param annotations The annotations we are searching for in this entity.
     * @return A set of annotations found in this annotation.
     */
    public Annotations unwrapAnnotations(Annotations annotations) {
        Annotations unwrappedAnnotations = new Annotations();

        String entityName = getEntity().getName().toLowerCase();
        int length = entityName.length();

        for (Annotation annotation : annotations) {
            if (annotation.getLength() < length) {
                int index = entityName.indexOf(" " + annotation.getEntity().getName().toLowerCase() + " ");
                if (index > -1) {
                    Annotation wrappedAnnotation = new Annotation(getOffset() + index + 1, annotation.getEntity()
                            .getName(), annotation.getMostLikelyTagName());
                    unwrappedAnnotations.add(wrappedAnnotation);
                }
            }
        }

        return unwrappedAnnotations;
    }

    public Annotations unwrapAnnotations(DictionaryClassifier classifier, Preprocessor preprocessor) {
        Annotations unwrappedAnnotations = new Annotations();

        if (getEntity().getName().indexOf(" ") == -1) {
            return unwrappedAnnotations;
        }

        String[] words = getEntity().getName().split(" ");
        String[] tags = new String[words.length];

        // classify each word
        for (int i = 0; i < words.length; i++) {

            ClassificationDocument document = preprocessor.preProcessDocument(words[i]);
            classifier.classify(document, false);
            tags[i] = document.getMainCategoryEntry().getCategory().getName();

        }


        // create annotations
        Annotation lastAnnotation = new Annotation(0, "", "");
        for (int i = 0; i < words.length; i++) {
            String tag = tags[i];

            if (!tag.equalsIgnoreCase(lastAnnotation.getMostLikelyTagName())) {
                List<Integer> indexList = StringHelper.getOccurrenceIndices(getEntity().getName(), " ");
                int offsetPlus = 0;
                if (i > 0) {
                    offsetPlus = indexList.get(i - 1) + 1;
                }
                lastAnnotation = new Annotation(getOffset() + offsetPlus, words[i],
                        tags[i]);
                unwrappedAnnotations.add(lastAnnotation);
            } else {
                // update last annotation
                lastAnnotation.getEntity().setName(lastAnnotation.getEntity().getName() + " " + words[i]);
                lastAnnotation.setLength(lastAnnotation.getEntity().getName().length());
            }
        }

        return unwrappedAnnotations;
    }

}
