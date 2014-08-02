package ws.palladian.classification.text;

import java.util.Iterator;

import ws.palladian.classification.AbstractCategoryEntries;
import ws.palladian.core.Category;

public class LinkedCategoryEntries extends AbstractCategoryEntries {

    @Override
    public Iterator<Category> iterator() {
        // TODO Auto-generated method stub
        return null;
    }
    
    private static final class LinkedCategoryCount {
        private final String categoryName;
        private int count;
        private LinkedCategoryCount nextCategory;

        private LinkedCategoryCount(String name, int count) {
            this.categoryName = name;
            this.count = count;
        }

    }

}
