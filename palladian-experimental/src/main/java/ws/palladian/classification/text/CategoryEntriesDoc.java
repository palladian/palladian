package ws.palladian.classification.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;

import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.collection.AbstractIterator2;

/**
 * <p>
 * This class serves as adapter between a {@link CategoryEntries} instance and {@link Iterable} {@link IndexableField}s
 * which are given to the Lucene {@link IndexWriter}.
 * 
 * @author Philipp Katz
 */
class CategoryEntriesDoc implements Iterable<IndexableField> {

    /**
     * {@link FieldType} which keeps the term vector (necessary for the term documents, where the counts represent the
     * categories).
     */
    private static final FieldType TERM_VECTOR_TYPE = createTermVectorType();

    private static FieldType createTermVectorType() {
        FieldType fieldType = new FieldType();
        fieldType.setIndexed(true);
        fieldType.setStored(true);
        fieldType.setStoreTermVectors(true);
        fieldType.freeze();
        return fieldType;
    }

    private final String fieldName;

    private final CategoryEntries categoryEntries;

    private final List<IndexableField> additionalFields;

    CategoryEntriesDoc(String fieldName, CategoryEntries categoryEntries, IndexableField... additionalFields) {
        Validate.notEmpty(fieldName, "fieldName must not be empty");
        Validate.notNull(categoryEntries, "categoryEntries must not be null");
        this.fieldName = fieldName;
        this.categoryEntries = categoryEntries;
        this.additionalFields = additionalFields != null ? Arrays.asList(additionalFields) : Collections
                .<IndexableField> emptyList();
    }

    @Override
    public Iterator<IndexableField> iterator() {
        return new AbstractIterator2<IndexableField>() {

            private final Iterator<IndexableField> additionalFieldsIterator = additionalFields.iterator();

            private final Iterator<Category> categoryIterator = categoryEntries.iterator();

            private Category currentCategory;

            private Field currentField;

            private int currentCount;

            @Override
            protected IndexableField getNext() {
                if (additionalFieldsIterator.hasNext()) {
                    return additionalFieldsIterator.next();
                }
                if (currentCategory == null) {
                    if (categoryIterator.hasNext()) {
                        currentCategory = categoryIterator.next();
                        currentCount = 0;
                        currentField = new Field(fieldName, currentCategory.getName(), TERM_VECTOR_TYPE);
                    } else {
                        return finished();
                    }
                }
                if (++currentCount >= currentCategory.getCount()) {
                    currentCategory = null;
                }
                return currentField;
            }
        };
    }

}
