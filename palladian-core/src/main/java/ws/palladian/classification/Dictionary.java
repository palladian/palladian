package ws.palladian.classification;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.classification.persistence.DictionaryDbIndexH2;
import ws.palladian.classification.persistence.DictionaryDbIndexMySql;
import ws.palladian.classification.persistence.DictionaryFileIndex;
import ws.palladian.classification.persistence.DictionaryIndex;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.TreeNode;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;

/**
 * A dictionary holds a list of words with their probabilities/scores of belonging to certain categories. Word Category1
 * ... CategoryN test 0.1 0.3 ...
 * 
 * @author David Urbansky
 */
public class Dictionary extends HashMap<String, CategoryEntries> implements Serializable {

    private static final long serialVersionUID = 3309493348334861440L;

    private int numberOfDocuments = 0;
    private String name = "dictionary";

    private String indexPath = "data/models/";

    private Categories categories = new Categories();

    private WordCorrelationMatrix wcm = new WordCorrelationMatrix();

    /** The hierarchy of categories (for hierarchical classification). */
    public TreeNode hierarchyRootNode = new TreeNode("root");

    /** A dictionary index saved on disk, that is more scalable than holding the dictionary in memory. */
    private transient DictionaryIndex dictionaryIndex = null;

    private boolean caseSensitive = false;

    // ////////////////// index types ////////////////////

    /** save dictionary in a database all in one table */
    public static final int DB_INDEX_FAST = 1;

    /** save dictionary in a database, normalized in three tables (slower than using one table) */
    public static final int DB_INDEX_NORMALIZED = 2;

    /** save dictionary on disk in Lucene index */
    public static final int LUCENE_INDEX = 3;

    /** the chosen index type */
    private int indexType = DB_INDEX_FAST;

    // ////////////////// database options ////////////////////
    /** Use client server mysql database. */
    public static final int DB_MYSQL = 1;

    /** Use embedded h2 database. */
    public static final int DB_H2 = 2;

    /** If database is used for index, specify which one. */
    private int databaseType = DB_H2;

    /** Decide whether to use memory or index saved on disk, if a dictionary is loaded from disk it must use the index. */
    private boolean useIndex = false;

    /**
     * There is an algorithm that makes it unnecessary to read from index before updating and n-gram, which speeds up
     * the process.
     */
    private boolean readFromIndexForUpdate = true;

    /** Which class type (one category, hierarchical, or tags). */
    private int classType = ClassificationTypeSetting.SINGLE;

    public Dictionary(String name) {
        super();
        this.name = name;
    }

    public Dictionary(String name, int classType) {
        super();
        this.name = name;
        this.classType = classType;
    }

    public Dictionary(String name, int classType, int indexType, int databaseType) {
        this.name = name;
        this.classType = classType;
        this.indexType = indexType;
        this.databaseType = databaseType;
    }

    /**
     * Open or create an index. Either in database or on a Lucene index on disk. The index is then ready to be read or
     * written.
     * 
     * @param classType The class type distinguishes certain indexes. There can be several indexes with the same name
     *            but only with different class types.
     */
    public void useIndex() {

        if (dictionaryIndex == null) {

            if (indexType == DB_INDEX_FAST || indexType == DB_INDEX_NORMALIZED) {

                if (databaseType == DB_MYSQL) {
                    dictionaryIndex = new DictionaryDbIndexMySql(getName(), "root", "", getIndexPath());
                    if (indexType == DB_INDEX_FAST) {
                        ((DictionaryDbIndexMySql) dictionaryIndex).setFastMode(true);
                    } else {
                        ((DictionaryDbIndexMySql) dictionaryIndex).setFastMode(false);
                    }
                } else {
                    dictionaryIndex = new DictionaryDbIndexH2(getName(), "root", "", getIndexPath());
                    if (indexType == DB_INDEX_FAST) {
                        ((DictionaryDbIndexH2) dictionaryIndex).setFastMode(true);
                    } else {
                        ((DictionaryDbIndexH2) dictionaryIndex).setFastMode(false);
                    }
                }

            } else if (indexType == LUCENE_INDEX) {
                dictionaryIndex = new DictionaryFileIndex(getName());
            } else {
                Logger.getRootLogger().error(
                        "no dictionary index could be found for the dictionary " + getName() + " with the index type "
                                + indexType);
            }

        }

        // dictionaryIndex.setCategories(categories);
        dictionaryIndex.setDictionary(this);
        dictionaryIndex.openWriter();
        dictionaryIndex.openReader();
        this.useIndex = true;
    }

    public void closeIndexWriter() {
        if (dictionaryIndex != null) {
            dictionaryIndex.close();
            dictionaryIndex.openReader();
        }
    }

    public void closeIndex() {
        if (dictionaryIndex != null) {
            dictionaryIndex.close();
        }
    }

    public void emptyIndex() {
        if (dictionaryIndex != null) {
            dictionaryIndex.empty();
        } else {
            Logger.getRootLogger().warn("could not empty index because it was not yet initialized");
        }
    }

    public void useMemory() {
        this.useIndex = false;
    }

    public boolean isUseIndex() {
        return useIndex;
    }

    /**
     * In hierarchical classification mode, the root category is the main category. For evaluation purposes we need to
     * tell the dictionary which categories are
     * main categories.
     * 
     * @param categories Categories of which some are main categories.
     */
    public void setMainCategories(Categories categories) {
        for (Category c : categories) {
            Category dictionaryCategory = getCategories().getCategoryByName(c.getName());
            if (dictionaryCategory != null) {
                dictionaryCategory.setClassType(c.getClassType());
                if (c.isMainCategory()) {
                    dictionaryCategory.setMainCategory(true);
                }
            }
        }
    }

    public CategoryEntries updateWord(String word, Category category, double value) {
        return updateWord(word, category.getName(), value);
    }

    public CategoryEntries updateWord(String word, String categoryName, double value) {

        if (!isCaseSensitive()) {
            word = word.toLowerCase();
        }

        Category category = categories.getCategoryByName(categoryName);
        if (category == null) {
            category = new Category(categoryName);
            categories.add(category);
        }

        if (useIndex) {

            CategoryEntries categoryEntries = new CategoryEntries();

            if (isReadFromIndexForUpdate()) {
                categoryEntries = dictionaryIndex.read(word);
            }

            if (categoryEntries.size() > 0) {

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

                // dictionaryIndex.update(word, categoryEntries);
                dictionaryIndex.update(word, ce);

                return categoryEntries;
            } else {

                categoryEntries = new CategoryEntries();

                CategoryEntry categoryEntry = new CategoryEntry(categoryEntries, category, value);
                categoryEntries.add(categoryEntry);

                // a new word was added to the category so we need to increase
                // the frequency for the category
                category.increaseFrequency();
                category.increaseTotalTermWeight(categoryEntry.getAbsoluteRelevance());

                // dictionaryIndex.write(word, categoryEntries);
                dictionaryIndex.write(word, categoryEntry);

                return categoryEntries;
            }

        } else {

            if (containsKey(word)) {

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

                super.put(word, categoryEntries);

                return categoryEntries;
            }

        }

    }

    /**
     * Update the word correlation matrix.
     * 
     * @param terms A set of terms that co-occurred
     */
    public void updateWCM(Term[] terms) {
        for (int i = 0; i < terms.length - 1; i++) {
            wcm.updatePair(terms[i], terms[i + 1]);
        }
    }

    /**
     * Get the best matching category for a given word.
     * 
     * @param word The word to be looked up.
     * @param minimumScore The minimum score required to return the category.
     * @return The category name that the word is most likely associated with.
     */
    public CategoryEntry getMostLikelyCategoryEntry(String word, double minimumScore) {

        if (!isCaseSensitive()) {
            word = word.toLowerCase();
        }

        CategoryEntries categoryEntries = this.get(word);
        if (categoryEntries == null) {
            return null;
        }

        CategoryEntry bestMatchingCategoryEntry = null;

        for (CategoryEntry categoryEntry : categoryEntries) {
            if (bestMatchingCategoryEntry == null) {
                bestMatchingCategoryEntry = categoryEntry;
            } else if (categoryEntry.getRelevance() > bestMatchingCategoryEntry.getRelevance()) {
                bestMatchingCategoryEntry = categoryEntry;
            }
        }

        if (bestMatchingCategoryEntry.getRelevance() >= minimumScore) {
            return bestMatchingCategoryEntry;
        }

        return null;
    }

    public CategoryEntry getMostLikelyCategoryEntry(String[] words, double minimumScore) {

        LinkedHashMap<String, Double> bestFitCategoryEntries = new LinkedHashMap<String, Double>();
        LinkedHashMap<String, CategoryEntry> bestFitCategoryEntries2 = new LinkedHashMap<String, CategoryEntry>();
        for (String word : words) {
            CategoryEntry categoryEntry = getMostLikelyCategoryEntry(word, minimumScore);
            if (categoryEntry == null) {
                continue;
            }

            if (bestFitCategoryEntries.containsKey(categoryEntry.getCategory().getName())) {
                bestFitCategoryEntries.put(
                        categoryEntry.getCategory().getName(),
                        bestFitCategoryEntries.get(categoryEntry.getCategory().getName())
                        + categoryEntry.getRelevance());
            } else {
                bestFitCategoryEntries.put(categoryEntry.getCategory().getName(), categoryEntry.getRelevance());
                bestFitCategoryEntries2.put(categoryEntry.getCategory().getName(), categoryEntry);
            }
        }

        bestFitCategoryEntries = CollectionHelper.sortByValue(bestFitCategoryEntries);

        // Category c = new
        // Category(bestFitCategoryEntries.entrySet().iterator().next().getKey());
        // c.setRelevance(bestFitCategoryEntries.entrySet().iterator().next().getValue());

        return bestFitCategoryEntries2.get(bestFitCategoryEntries.entrySet().iterator().next().getKey());
    }

    public CategoryEntries getCategoryEntries(String word, double minimumScore) {

        if (!isCaseSensitive()) {
            word = word.toLowerCase();
        }

        CategoryEntries categoryEntries = this.get(word);
        if (categoryEntries == null) {
            return null;
        }

        for (CategoryEntry categoryEntry : categoryEntries) {
            if (categoryEntry.getRelevance() < minimumScore) {
                categoryEntries.remove(categoryEntry);
            }
        }

        return categoryEntries;
    }

    /**
     * Get a list of categories that can be associated with the list of words.
     * 
     * @param words A list of words.
     * @return categories The categories the words belong to.
     */
    public CategoryEntries getCategoryEntries(String[] words) {

        CategoryEntries categoryEntries = new CategoryEntries();

        for (String word : words) {

            CategoryEntries wordCategoryEntries = getCategoryEntries(word, 0.0);
            if (wordCategoryEntries == null) {
                continue;
            }

            for (CategoryEntry ce : wordCategoryEntries) {
                CategoryEntry ce1 = categoryEntries.getCategoryEntry(ce.getCategory());
                if (ce1 != null) {
                    ce1.addAbsoluteRelevance(ce.getAbsoluteRelevance());
                }
            }

        }

        return categoryEntries;
    }

    public int getNumberOfDocuments() {
        return numberOfDocuments;
    }

    public void setNumberOfDocuments(int numberOfDocuments) {
        this.numberOfDocuments = numberOfDocuments;
    }

    public void increaseNumberOfDocuments() {
        this.numberOfDocuments++;
    }

    public String toCsv() {
        StringBuilder dictionaryString = new StringBuilder("");

        // add some meta information
        dictionaryString.append("Files processed,").append(getNumberOfDocuments()).append("\n");
        dictionaryString.append("Words,").append(entrySet().size()).append("\n").append("\n");

        // create the file head
        dictionaryString.append("Term,");
        Iterator<Category> ic = categories.iterator();
        while (ic.hasNext()) {
            dictionaryString.append(ic.next().getName()).append(",");
        }
        dictionaryString.append("\n");

        Logger.getRootLogger().debug("word count " + entrySet().size());

        // one word per line with term frequencies per category
        for (Map.Entry<String, CategoryEntries> term : entrySet()) {

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

    /**
     * Save the constructed context map to a csv file.
     */
    public void saveAsCSV() {

        if (categories == null) {
            Logger.getRootLogger().error("no categories assigned");
            return;
        }

        String dictionaryString = toCsv();

        Logger.getRootLogger().debug("save dictionary...");
        FileHelper.writeToFile(
                "data/temp/" + DateHelper.getCurrentDatetime("yyyy-MM-dd_HH-mm-ss") + getName() + ".csv",
                dictionaryString);
    }

    public void calculateCategoryPriors() {
        categories.calculatePriors();
    }

    /**
     * Write the complete dictionary to an index.
     * 
     * @param indexPath The path of the index.
     */
    public void index(boolean deleteIndexFirst) {
        calculateCategoryPriors();
        useIndex();

        if (deleteIndexFirst) {
            emptyIndex();
        }

        // HashSet<String> usedString = new HashSet<String>();

        int c = 0;
        for (Map.Entry<String, CategoryEntries> dictionaryEntry : entrySet()) {
            // Logger.getRootLogger().debug("write: "+dictionaryEntry.getKey() +
            // " : " + dictionaryEntry.getValue());
            // if (!usedString.add(dictionaryEntry.getKey().getText())) {
            // System.out.println("wait " + dictionaryEntry.getKey());
            // }

            dictionaryIndex.write(dictionaryEntry.getKey(), dictionaryEntry.getValue());
            if (c % 4000 == 0) {
                double percent = MathHelper.round(100.0 * c / entrySet().size(), 2);
                Logger.getRootLogger().info("saving dictionary process: " + percent + "%");
            }
            c++;
        }

        dictionaryIndex.close();
    }

    /**
     * Serialize the dictionary but without the actual entries. They can be retrieved from the index.
     * 
     * @param indexPath
     * @param indexFirst
     */
    public void serialize(String indexPath, boolean indexFirst, boolean deleteIndexFirst) {
        if (indexFirst) {
            index(deleteIndexFirst);
        }
        clear();
        FileHelper.serialize(this, indexPath);
    }

    /**
     * Get a list of category entries for the given term.
     * 
     * @param term A term might be a word or any other sequence of characters.
     * @return A list of category entries.
     */
    public CategoryEntries get(Term term) {

        CategoryEntries categoryEntries = null;

        if (useIndex) {
            categoryEntries = dictionaryIndex.read(term.getText());
        } else {
            categoryEntries = super.get(term);
        }

        if (categoryEntries == null) {
            categoryEntries = new CategoryEntries();
        }

        return categoryEntries;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Categories getCategories() {
        categories.calculatePriors();
        return categories;
    }

    public void setCategories(Categories categories) {
        this.categories = categories;
    }

    public boolean isReadFromIndexForUpdate() {
        return readFromIndexForUpdate;
    }

    public void setReadFromIndexForUpdate(boolean readFromIndexForUpdate) {
        this.readFromIndexForUpdate = readFromIndexForUpdate;
    }

    @Override
    public String toString() {
        StringBuilder dictionaryString = new StringBuilder();

        dictionaryString.append("Words,");
        for (Category category : categories) {
            dictionaryString.append(category.getName()).append("(").append(category.getPrior()).append(")").append(",");
        }
        dictionaryString.append("\n");

        for (Map.Entry<String, CategoryEntries> term : entrySet()) {

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

    public void setClassType(int classType) {
        this.classType = classType;
    }

    public int getClassType() {
        return classType;
    }

    public void setIndexType(int indexType) {
        this.indexType = indexType;
    }

    public int getIndexType() {
        return indexType;
    }

    public String getIndexPath() {
        return indexPath;
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = FileHelper.addTrailingSlash(indexPath);
    }

    public void setDatabaseType(int databaseType) {
        this.databaseType = databaseType;
    }

    public int getDatabaseType() {
        return databaseType;
    }

    public void setWcm(WordCorrelationMatrix wcm) {
        this.wcm = wcm;
    }

    public WordCorrelationMatrix getWcm() {
        return wcm;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

}