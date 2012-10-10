package ws.palladian.classification.text;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Map;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;

/**
 * A dictionary holds a list of words with their probabilities/scores of belonging to certain categories. Word Category1
 * ... CategoryN test 0.1 0.3 ...
 * 
 * @author David Urbansky
 */
public class Dictionary implements Serializable {
    
    // XXX merge with DictionaryModel

    private static final long serialVersionUID = 3309493348334861440L;

//    private final Categories categories = new Categories();
    private final CountMap<String> categories = CountMap.create();
    
    private final boolean caseSensitive;

    private final Map<String, CategoryEntries> termCategoryEntries = CollectionHelper.newHashMap();
    
    public Dictionary() {
        this.caseSensitive = false;
    }
    
    public Dictionary(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public void updateWord(String word, String categoryName, double value) {

        if (!caseSensitive) {
            word = word.toLowerCase();
        }

//        Category category = categories.getCategoryByName(categoryName);
//        if (category == null) {
//            category = new Category(categoryName);
//            categories.add(category);
//        }
        categories.add(categoryName);

        if (termCategoryEntries.containsKey(word)) {

            CategoryEntries categoryEntries = get(word);

            CategoryEntry ce = categoryEntries.getCategoryEntry(categoryName);

            if (ce == null) {
                ce = new CategoryEntry(categoryEntries, new Category(categoryName), value);
                categoryEntries.add(ce);

                // the word is new for that category so we need to increase
                // the frequency for the category
//                category.increaseFrequency();
            } else {
                ce.addAbsoluteRelevance(value);
            }
        } else {

            CategoryEntries categoryEntries = new CategoryEntries();

            CategoryEntry categoryEntry = new CategoryEntry(categoryEntries, new Category(categoryName), value);
            categoryEntries.add(categoryEntry);

            // a new word was added to the category so we need to increase
            // the frequency for the category
//            category.increaseFrequency();

            termCategoryEntries.put(word, categoryEntries);
        }

    }

    public void toCsv(PrintStream printStream) {
        // StringBuilder dictionaryString = new StringBuilder("");

        // add some meta information
        printStream.print("Words," + termCategoryEntries.entrySet().size() + "\n\n");

        // create the file head
        printStream.print("Term,");
        for (String category : categories.uniqueItems()) {
            printStream.print(category + ",");
        }
        printStream.print("\n");

        // one word per line with term frequencies per category
        for (Map.Entry<String, CategoryEntries> term : termCategoryEntries.entrySet()) {

            printStream.print(term.getKey());
            printStream.print(",");

            // get word frequency for each category and current term
            for (String category : categories.uniqueItems()) {
                CategoryEntry ce = term.getValue().getCategoryEntry(category);
                if (ce == null) {
                    printStream.print("0.0,");
                } else {
                    printStream.print(ce.getRelevance() + ",");
                }
            }
            printStream.print("\n");
        }
        
        printStream.flush();
    }

//    public void calculateCategoryPriors() {
//        categories.calculatePriors();
//    }

    /**
     * Get a list of category entries for the given term.
     * 
     * @param term A term might be a word or any other sequence of characters.
     * @return A list of category entries.
     */
    public CategoryEntries get(String term) {

        CategoryEntries categoryEntries = null;

        categoryEntries = termCategoryEntries.get(term);

        if (categoryEntries == null) {
            categoryEntries = new CategoryEntries();
        }

        return categoryEntries;
    }

//    public Categories getCategories() {
//        categories.calculatePriors();
//        return categories;
//    }
    
    public CountMap<String> getCategories() {
        return categories;
    }

    @Override
    public String toString() {
        StringBuilder dictionaryString = new StringBuilder();

        dictionaryString.append("Words,");
        for (String category : categories) {
            dictionaryString.append(category).append("(").append(categories.get(category)).append(")").append(",");
        }
        dictionaryString.append("\n");

        for (Map.Entry<String, CategoryEntries> term : termCategoryEntries.entrySet()) {

            dictionaryString.append(term.getKey()).append(",");

            // get word frequency for each category and current term
            for (String category : categories) {
                CategoryEntry ce = term.getValue().getCategoryEntry(category);
                if (ce == null) {
                    dictionaryString.append("0.0,");
                } else {
                    dictionaryString.append(ce.getRelevance()).append(",");
                }
            }
            dictionaryString.append("\n");
        }

        return dictionaryString.toString();
    }

    public Map<String, CategoryEntries> getCategoryEntries() {
        return termCategoryEntries;
    }

}