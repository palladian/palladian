package ws.palladian.classification.text;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * A dictionary holds a list of words with their probabilities/scores of belonging to certain categories. Word Category1
 * ... CategoryN test 0.1 0.3 ...
 * 
 * @author David Urbansky
 */
public class Dictionary implements Serializable {
    
    // XXX merge with DictionaryModel

    private static final long serialVersionUID = 3309493348334861440L;

    private int numberOfDocuments = 0;

    private Categories categories = new Categories();
    
    private boolean caseSensitive = false;

    private final Map<String, CategoryEntries> termCategoryEntries = CollectionHelper.newHashMap();

    public CategoryEntries updateWord(String word, Category category, double value) {
        return updateWord(word, category.getName(), value);
    }

    public CategoryEntries updateWord(String word, String categoryName, double value) {

        if (!caseSensitive) {
            word = word.toLowerCase();
        }

        Category category = categories.getCategoryByName(categoryName);
        if (category == null) {
            category = new Category(categoryName);
            categories.add(category);
        }

        if (termCategoryEntries.containsKey(word)) {

            CategoryEntries categoryEntries = get(word);

            CategoryEntry ce = categoryEntries.getCategoryEntry(categoryName);

            if (ce == null) {
                ce = new CategoryEntry(categoryEntries, category, value);
                categoryEntries.add(ce);

                // the word is new for that category so we need to increase
                // the frequency for the category
                category.increaseFrequency();
                category.increaseTotalTermWeight(ce.getAbsoluteRelevance());
            } else {
                ce.addAbsoluteRelevance(value);
                category.increaseTotalTermWeight(value);
            }

            return categoryEntries;
        } else {

            CategoryEntries categoryEntries = new CategoryEntries();

            CategoryEntry categoryEntry = new CategoryEntry(categoryEntries, category, value);
            categoryEntries.add(categoryEntry);

            // a new word was added to the category so we need to increase
            // the frequency for the category
            category.increaseFrequency();
            category.increaseTotalTermWeight(categoryEntry.getAbsoluteRelevance());

            termCategoryEntries.put(word, categoryEntries);

            return categoryEntries;
        }

    }

    public String toCsv() {
        StringBuilder dictionaryString = new StringBuilder("");

        // add some meta information
        dictionaryString.append("Files processed,").append(numberOfDocuments).append("\n");
        dictionaryString.append("Words,").append(termCategoryEntries.entrySet().size()).append("\n").append("\n");

        // create the file head
        dictionaryString.append("Term,");
        Iterator<Category> ic = categories.iterator();
        while (ic.hasNext()) {
            dictionaryString.append(ic.next().getName()).append(",");
        }
        dictionaryString.append("\n");

        Logger.getRootLogger().debug("word count " + termCategoryEntries.entrySet().size());

        // one word per line with term frequencies per category
        for (Map.Entry<String, CategoryEntries> term : termCategoryEntries.entrySet()) {

            dictionaryString.append(term.getKey()).append(",");

            // get word frequency for each category and current term
            for (Category category : categories) {
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

    public void calculateCategoryPriors() {
        categories.calculatePriors();
    }

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

    public Categories getCategories() {
        categories.calculatePriors();
        return categories;
    }

    @Override
    public String toString() {
        StringBuilder dictionaryString = new StringBuilder();

        dictionaryString.append("Words,");
        for (Category category : categories) {
            dictionaryString.append(category.getName()).append("(").append(category.getPrior()).append(")").append(",");
        }
        dictionaryString.append("\n");

        for (Map.Entry<String, CategoryEntries> term : termCategoryEntries.entrySet()) {

            dictionaryString.append(term.getKey()).append(",");

            // get word frequency for each category and current term
            for (Category category : categories) {
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

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public int size() {
        return termCategoryEntries.size();
    }

    public Map<String, CategoryEntries> getCategoryEntries() {
        return termCategoryEntries;
    }

}