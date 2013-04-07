package ws.palladian.extraction.entity;

import java.util.List;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.Annotated;

/**
 * An annotation made by a {@link NamedEntityRecognizer} when tagging a text.
 * 
 * @author David Urbansky
 * 
 */
public class Annotation implements Annotated {

    public static final int WINDOW_SIZE = 40;

    /** The category of the instance, null if not classified. */
    private CategoryEntriesMap tags = new CategoryEntriesMap();

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

    private List<String> subTypes = null;

    public Annotation(int offset, String entityName, String tagName) {
        this.offset = offset;
        this.length = entityName.length();
        entity = entityName;
        tags.set(tagName, 1);
    }

    public String getLeftContext() {
        return leftContext;
    }

    public String[] getLeftContexts() {

        String[] contexts = new String[3];
        contexts[0] = "";
        contexts[1] = "";
        contexts[2] = "";

        String leftContext = getLeftContext();
        String[] words = leftContext.split(" ");
        int wordNumber = 1;
        for (int i = words.length - 1; i >= 0; i--) {

            String token = words[i];
            /*
             * if (DateHelper.containsDate(token)) {
             * token = "DATE";
             * } else
             */if (StringHelper.isNumber(token) || StringHelper.isNumericExpression(token)) {
                token = "NUM";
            }

            if (wordNumber == 1) {
                contexts[0] = token;
                contexts[1] = token;
                contexts[2] = token;
            }

            if (wordNumber == 2) {
                contexts[1] = token + " " + contexts[1];
                contexts[2] = token + " " + contexts[2];
            }

            if (wordNumber == 3) {
                contexts[2] = token + " " + contexts[2];
                break;
            }

            wordNumber++;
        }

        if (words.length < 3) {
            contexts[2] = "";
        }
        if (words.length < 2) {
            contexts[1] = "";
        }

        return contexts;
    }

    public String[] getRightContexts() {

        String[] contexts = new String[3];
        contexts[0] = "";
        contexts[1] = "";
        contexts[2] = "";

        String rightContext = getRightContext();
        String[] words = rightContext.split(" ");
        int wordNumber = 1;
        for (String word : words) {

            String token = word;
            /*
             * if (DateHelper.containsDate(token)) {
             * token = "DATE";
             * } else
             */if (StringHelper.isNumber(token) || StringHelper.isNumericExpression(token)) {
                token = "NUM";
            }

            if (wordNumber == 1) {
                contexts[0] = token;
                contexts[1] = token;
                contexts[2] = token;
            }

            if (wordNumber == 2) {
                contexts[1] = contexts[1] + " " + token;
                contexts[2] = contexts[2] + " " + token;
            }

            if (wordNumber == 3) {
                contexts[2] = contexts[2] + " " + token;
                break;
            }

            wordNumber++;
        }

        if (words.length < 3) {
            contexts[2] = "";
        }
        if (words.length < 2) {
            contexts[1] = "";
        }

        return contexts;
    }

    public int getLength() {
        return length;
    }

    public String getRightContext() {
        return rightContext;
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

//    public boolean matches(Annotation annotation) {
//        return getStartPosition() == annotation.getStartPosition() && getLength() == annotation.getLength();
//    }

//    public boolean overlaps(Annotation annotation) {
//        return getStartPosition() <= annotation.getStartPosition() && getEndPosition() >= annotation.getStartPosition()
//                || getStartPosition() <= annotation.getEndPosition()
//                && getEndPosition() >= annotation.getStartPosition();
//    }

//    public boolean sameTag(Annotation annotation) {
//        return getTag().equalsIgnoreCase(annotation.getTag());
//    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void setLeftContext(String leftContext) {
        this.leftContext = leftContext;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setRightContext(String rightContext) {
        this.rightContext = rightContext;
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

}
