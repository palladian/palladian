package ws.palladian.extraction.entity;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.Annotated;

public class ContextAnnotation implements Annotated {

    /** The category of the instance, null if not classified. */
    private CategoryEntriesMap tags = new CategoryEntriesMap();

    /** The start index of the annotation in the annotated text. */
    private int offset;

    /** The annotated entity. */
    private String entity;

    /** The left context of the annotation */
    private final String leftContext;

    /** The right context of the annotation */
    private final String rightContext;

    public ContextAnnotation(int offset, String entityName, String tagName, String leftContext, String rightContext) {
        this.offset = offset;
        this.entity = entityName;
        tags.set(tagName, 1);
        this.leftContext = leftContext;
        this.rightContext = rightContext;
    }

    public ContextAnnotation(int offset, String entityName, String tagName) {
        this(offset, entityName, tagName, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    public ContextAnnotation(Annotated annotated) {
        this(annotated.getStartPosition(), annotated.getValue(), annotated.getTag(), StringUtils.EMPTY,
                StringUtils.EMPTY);
    }

    public CategoryEntries getTags() {
        return tags;
    }

    public void setEntity(String entity) {
        this.entity = entity;
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
        return offset + entity.length();
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

    public String getLeftContext() {
        return leftContext;
    }

    public String getRightContext() {
        return rightContext;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entity == null) ? 0 : entity.hashCode());
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
        ContextAnnotation other = (ContextAnnotation)obj;
        if (entity == null) {
            if (other.entity != null)
                return false;
        } else if (!entity.equals(other.entity))
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
