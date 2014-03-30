package ws.palladian.classification.text;

import java.io.PrintStream;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Category;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Function;

public abstract class AbstractDictionaryModel implements DictionaryModel {

    private static final long serialVersionUID = 1L;

    @Override
    public Set<String> getCategories() {
        return CollectionHelper.convertSet(getPriors(), new Function<Category,String>() {
            @Override
            public String compute(Category input) {
                return input.getName();
            }
        });
    }

    @Override
    public void toCsv(PrintStream printStream) {
        Validate.notNull(printStream, "printStream must not be null");
        printStream.print("Term,");
        printStream.print(StringUtils.join(getPriors(), ","));
        printStream.print('\n');
        Set<String> categories = getCategories();
        for (TermCategoryEntries entries : this) {
            printStream.print(entries.getTerm());
            printStream.print(',');
            boolean first = true;
            for (String category : categories) {
                double probability = entries.getProbability(category);
                if (!first) {
                    printStream.print(',');
                } else {
                    first = false;
                }
                printStream.print(probability);
            }
            printStream.print('\n');
        }
        printStream.flush();
    }

}
