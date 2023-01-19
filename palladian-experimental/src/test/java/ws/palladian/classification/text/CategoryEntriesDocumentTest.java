package ws.palladian.classification.text;

import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.junit.Test;
import ws.palladian.helper.collection.CollectionHelper;

import static org.junit.Assert.assertEquals;
import static ws.palladian.classification.text.LuceneDictionaryModel.FIELD_TERM;

public class CategoryEntriesDocumentTest {

    @Test
    public void testCategoryEntriesDocument() {
        CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
        builder.set("category1", 10);
        builder.set("category2", 5);
        builder.set("category3", 8);
        CategoryEntriesDoc document = new CategoryEntriesDoc(LuceneDictionaryModel.FIELD_TERM_CAT, builder.create(), new StringField(FIELD_TERM, "term", Store.YES));
        assertEquals(24, CollectionHelper.count(document.iterator()));
        assertEquals(24, CollectionHelper.count(document.iterator()));
    }

}
