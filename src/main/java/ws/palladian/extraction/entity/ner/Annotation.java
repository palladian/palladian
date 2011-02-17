package ws.palladian.extraction.entity.ner;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.page.ClassificationDocument;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.Preprocessor;
import ws.palladian.helper.StringHelper;

/**
 * An annotation made by a {@link NamedEntityRecognizer} when tagging a text.
 * 
 * @author David Urbansky
 * 
 */
public class Annotation extends UniversalInstance {

    private static final long serialVersionUID = 6235371698078169268L;

    /** The start index of the annotation in the annotated text. */
    private int offset = -1;

    /** The length of the annotation. */
    private int length = -1;

    /** The annotated entity. */
    private String entity;

    /** The left context of the annotation */
    private String leftContext = "";

    /** The right context of the annotation */
    private String rightContext = "";

    public Annotation(Annotation annotation) {
        super(null);
        offset = annotation.getOffset();
        length = annotation.getLength();
        entity = annotation.getEntity();
        assignedCategoryEntries = annotation.getTags();
    }

    public Annotation(Annotation annotation, Instances<UniversalInstance> instances) {
        super(instances);
        offset = annotation.getOffset();
        length = annotation.getLength();
        entity = annotation.getEntity();
        assignedCategoryEntries = annotation.getTags();
    }

    public Annotation(int offset, String entityName, String tagName) {
        super(null);
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        assignedCategoryEntries.add(new CategoryEntry(assignedCategoryEntries, new Category(tagName), 1));
    }

    public Annotation(int offset, String entityName, String tagName, Instances<UniversalInstance> instances) {
        super(instances);
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        assignedCategoryEntries.add(new CategoryEntry(assignedCategoryEntries, new Category(tagName), 1));
    }

    public Annotation(int offset, String entityName, CategoryEntries tags) {
        super(null);
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        this.assignedCategoryEntries = tags;
    }

    public Annotation(int offset, String entityName, CategoryEntries tags, Instances<UniversalInstance> instances) {
        super(instances);
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        this.assignedCategoryEntries = tags;
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

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public CategoryEntries getTags() {
        return getAssignedCategoryEntries();
    }

    public void setTags(CategoryEntries tags) {
        this.assignedCategoryEntries = tags;
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
        builder.append(entity);
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

        String entityName = getEntity().toLowerCase();
        int length = entityName.length();

        for (Annotation annotation : annotations) {
            if (annotation.getLength() < length) {
                int index = entityName.indexOf(" " + annotation.getEntity().toLowerCase() + " ");
                if (index > -1) {
                    Annotation wrappedAnnotation = new Annotation(getOffset() + index + 1, annotation.getEntity(),
                            annotation.getMostLikelyTagName());
                    unwrappedAnnotations.add(wrappedAnnotation);
                }
            }
        }

        return unwrappedAnnotations;
    }

    public Annotations unwrapAnnotations(DictionaryClassifier classifier, Preprocessor preprocessor) {
        Annotations unwrappedAnnotations = new Annotations();

        if (getEntity().indexOf(" ") == -1) {
            return unwrappedAnnotations;
        }

        String[] words = getEntity().split(" ");
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
                List<Integer> indexList = StringHelper.getOccurrenceIndices(getEntity(), " ");
                int offsetPlus = 0;
                if (i > 0) {
                    offsetPlus = indexList.get(i - 1) + 1;
                }
                lastAnnotation = new Annotation(getOffset() + offsetPlus, words[i],
                        tags[i]);
                unwrappedAnnotations.add(lastAnnotation);
            } else {
                // update last annotation
                lastAnnotation.setEntity(lastAnnotation.getEntity() + " " + words[i]);
                lastAnnotation.setLength(lastAnnotation.getEntity().length());
            }
        }

        return unwrappedAnnotations;
    }

    /**
     * Get the following features for each annotation:<br>
     * Numeric features<br>
     * <ol>
     * <li>#Words: The number of words of the annotation.</li>
     * <li>#Chars: The number of characters of the annotation.</li>
     * <li>#Digits: The number of digits of the annotation.</li>
     * <li>#UpperCaseChars: The number of upper case chars of the annotation.</li>
     * </ol>
     * 
     * Nominal features<br>
     * <ol>
     * <li>Whether the annotation is at the start of a sentence (yes/no)</li>
     * <li>Whether the annotation is in quotes (yes/no)</li>
     * <li>Whether the annotation is all uppercase (yes/no)</li>
     * </ol>
     */
    public void createFeatures() {
        
        String entity = getEntity();
        String leftContext = getLeftContext().trim();
        String rightContext = getRightContext().trim();

        // // get the numeric features
        List<Double> numericFeatures = new ArrayList<Double>();
        
        // get the number of words
        double numberOfWords = entity.split(" ").length;
        numericFeatures.add(numberOfWords);

        // get the number of chars
        double numberOfChars = entity.length();
        numericFeatures.add(numberOfChars);
        
        // get the number of digits
        double numberOfDigits = StringHelper.countOccurences(entity, "[0-9]", true);
        numericFeatures.add(numberOfDigits);
        
        // get the number of uppercase chars
        double numberOfUppercaseChars = StringHelper.countOccurences(entity, "[A-Z]", false);
        numericFeatures.add(numberOfUppercaseChars);

        // // get the nominal features
        List<String> nominalFeatures = new ArrayList<String>();
        
        // is the entity at the start of a sentence? check if there is a period in the immediate left context
        boolean startOfSentence = leftContext.endsWith(".");
        nominalFeatures.add(String.valueOf(startOfSentence));

        // is the entity in quotes? ",',´
        boolean inQuotes = false;
        if (leftContext.endsWith("\"") && rightContext.startsWith("\"") || leftContext.endsWith("'")
                && rightContext.startsWith("'") || leftContext.endsWith("´") && rightContext.startsWith("´")) {
            inQuotes = true;
        }
        nominalFeatures.add(String.valueOf(inQuotes));

        // whether the entity is uppercase only
        boolean completelyUppercase = StringHelper.isCompletelyUppercase(entity);
        nominalFeatures.add(String.valueOf(completelyUppercase));

        setNumericFeatures(numericFeatures);
        setNominalFeatures(nominalFeatures);        
    }

}
