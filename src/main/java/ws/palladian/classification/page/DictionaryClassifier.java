package ws.palladian.classification.page;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Dictionary;
import ws.palladian.classification.Term;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.WordCorrelation;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.html.TreeNode;

/**
 * This classifier builds a weighed term look up table for the categories to classify new documents.
 * 
 * @author David Urbansky
 */
public class DictionaryClassifier extends TextClassifier {

    /** The serial version id. */
    private static final long serialVersionUID = 5705851277677296181L;

    /** The context map: matrix of terms x categories with weights in cells. */
    protected Dictionary dictionary = null;

    /** The path to the dictionary, whether where it should be or has been serialized. */
    private String dictionaryPath = null;

    /** Whether to train incremental, that is much slower but scales well. */
    private boolean incrementalTraining = false;

    public DictionaryClassifier() {
        ClassifierManager.log("DictionaryClassifier created");
        setName("DictionaryClassifier");
        dictionary = new Dictionary(getDictionaryName(), ClassificationTypeSetting.SINGLE);
    }

    public DictionaryClassifier(String name, String dictionaryPath) {
        ClassifierManager.log("DictionaryClassifier created");
        setName(name);

        dictionary = new Dictionary(getDictionaryName(), ClassificationTypeSetting.SINGLE);
        dictionary.setIndexPath(dictionaryPath);

        setDictionaryPath(dictionaryPath);
    }

    public void init() {
        dictionary.setName(getName());
    }

    public void useIndex() {
        dictionary.useIndex();
    }

    public void useMemory() {
        dictionary.useMemory();
    }

    /**
     * Create the dictionary consisting of stemmed words.
     */
    protected void buildDictionary(int classType) {
        if (dictionary == null) {
            dictionary = new Dictionary(getName(), classType);
        } else {
            dictionary.setClassType(classType);
        }

        for (TextInstance document : getTrainingDocuments()) {
            addToDictionary(document, classType);
        }

        // dictionary.setCategories(categories);

        ClassifierManager.log("dictionary built");
    }

    public void addToDictionary(TextInstance trainingDocument, int classType) {

        long t1 = System.currentTimeMillis();

        for (Map.Entry<Term, Double> entry : trainingDocument.getWeightedTerms().entrySet()) {

            // get the first category only
            if (classType == ClassificationTypeSetting.SINGLE) {
                dictionary.updateWord(entry.getKey(), trainingDocument.getFirstRealCategory().getName(), entry
                        .getValue());
            }

            // get all categories for hierarchical and tag mode
            else {
                for (Category realCategory : trainingDocument.getRealCategories()) {
                    // System.out.println("update " + entry.getKey() + " " + realCategory.getName());
                    dictionary.updateWord(entry.getKey(), realCategory.getName(), entry.getValue());
                }
            }
        }
        dictionary.increaseNumberOfDocuments();

        // co-occurrence: update the category correlation matrix of the dictionary (works but takes more time and space)
        if (classificationTypeSetting.getClassificationType() == ClassificationTypeSetting.TAG
                && classificationTypeSetting.getClassificationTypeTagSetting().isUseCooccurrence()) {

            Term[] terms;
            if (trainingDocument.getRealCategories().size() == 1) {
                terms = new Term[2];
                terms[0] = new Term(trainingDocument.getRealCategories().get(0).getName());
                terms[1] = new Term(trainingDocument.getRealCategories().get(0).getName());
            } else {
                terms = new Term[trainingDocument.getRealCategories().size()];
                int c = 0;
                for (Category realCategory : trainingDocument.getRealCategories()) {
                    terms[c++] = new Term(realCategory.getName());
                }
            }

            dictionary.updateWCM(terms);
        }

        // empty the training documents term map to save memory
        trainingDocument.getWeightedTerms().clear();

        LOGGER.debug("added to dictionary in " + DateHelper.getRuntime(t1) + " ("
                + DateHelper.getRuntime(initTime) + "), " + dictionary.getNumberOfDocuments() + " documents processed");
    }

    /**
     * Use this save method only if you created the DictionaryClassifier with a path.
     */
    public void save() {
        save(getDictionaryPath());
    }

    @Override
    public void save(String path) {
        setDictionaryPath(path);

        if (!isIncrementalTraining()) {
            saveDictionary(path, true, true);
        } else {
            saveDictionary(path, false, false);
            getDictionary().calculateCategoryPriors();
        }

        FileHelper.serialize(this, path + getName() + ".gz");
    }

    public void save(String path, boolean indexFirst, boolean deleteIndexFirst) {
        setDictionaryPath(path);
        saveDictionary(path, indexFirst, deleteIndexFirst);
        FileHelper.serialize(this, path + getName() + ".gz");
    }

    /**
     * Serialize the dictionary. All category information and parameters will be saved in the .ser file. The actual
     * dictionary will be stored in the dictionary index.
     * 
     * @param classType The class type for the dictionary to distinguish the name.
     */
    private void saveDictionary(String path, boolean indexFirst, boolean deleteIndexFirst) {

        if (path.length() > 0 && !path.endsWith("/")) {
            path += "/";
        }

        LOGGER.info("saving the dictionary");
        dictionary.serialize(path + getDictionaryName() + ".gz", indexFirst, deleteIndexFirst);
        // dictionary.index("data/models/dictionaryIndex_"+getName()+"_"+classType);

        LOGGER.info("saved model at " + path + getDictionaryName() + ".gz");
        // dictionary.saveAsCSV();

        // we now have to use the index for classification because the in-memory dictionary is empty
        dictionary.useIndex();
    }

    public static DictionaryClassifier load(String classifierPath) {

        LOGGER.info("deserialzing classifier from " + classifierPath);

        DictionaryClassifier classifier;

        classifier = (DictionaryClassifier) FileHelper.deserialize(classifierPath);
        classifier.reset();

        if ((classifier).getDictionary().isUseIndex()) {
            (classifier).useIndex();
        }

        LOGGER.info("loading dictionary");
        classifier.loadDictionary();

        return classifier;
    }

    public void loadDictionary() {
        loadDictionary(0);
    }

    /**
     * Load the dictionary into memory, or activate it when several have been loaded.
     */
    public void loadDictionary(int classType) {

        if (dictionaryPath == null) {
            return;
        }

        if (dictionary == null) {
            String modelFilePath = getDictionaryPath() + getDictionaryName() + ".gz";
            dictionary = (Dictionary) FileHelper.deserialize(modelFilePath);

            // all serialized dictionaries must use the index since their dictionaries are not serialized
            dictionary.useIndex();
        }

        if (dictionary == null) {
            return;
        }
        categories = dictionary.getCategories();
    }

    /**
     * Load all dictionaries into memory.
     */
    public void loadAllDictionaries() {
        loadDictionary(ClassificationTypeSetting.SINGLE);
        loadDictionary(ClassificationTypeSetting.HIERARCHICAL);
        loadDictionary(ClassificationTypeSetting.TAG);
    }

    public void train(UniversalInstance instance) {
        addTrainingInstance(instance);
        trainWithInstance(instance);
    }

    private void trainWithInstance(UniversalInstance instance) {

        Categories documentCategories = new Categories();

        Category knownCategory = categories.getCategoryByName(instance.getInstanceCategory().getName());
        if (knownCategory == null) {
            knownCategory = new Category(instance.getInstanceCategory().getName());
            categories.add(knownCategory);
        }

        documentCategories.add(knownCategory);

        TextInstance trainingDocument = preprocessor.preProcessDocument(instance.getTextFeature());
        // ClassificationDocument trainingDocument = preprocessor.preProcessDocument(annotation.getLeftContext() +
        // " "+ annotation.getEntity().getName() + " " + annotation.getRightContext());
        trainingDocument.setDocumentType(TextInstance.TRAINING);
        trainingDocument.setRealCategories(documentCategories);
        // getTrainingDocuments().add(trainingDocument);

        addToDictionary(trainingDocument, getClassificationType());
    }

    public void train() {

        categories = new Categories();

        // set the feature settings
        // FeatureSetting fs = new FeatureSetting();
        // fs.setMinNGramLength(2);
        // fs.setMaxNGramLength(9);
        // dictionaryClassifier.setFeatureSetting(fs);

        getDictionary().setDatabaseType(Dictionary.DB_H2);

        for (UniversalInstance instance : getTrainingInstances()) {

            trainWithInstance(instance);

        }

    }

    // protected abstract double calculateRelevance(Category category, Map.Entry<String, Double> categoryEntry,
    // Map.Entry<String, Double> weightedTerm);
    protected double calculateRelevance(CategoryEntry categoryEntry, Map.Entry<String, Double> map) {
        return 0.0;
    }

    // weightedTerm);

    @Override
    public TextInstance classify(TextInstance document, Set<String> possibleClasses) {
        return classify(document, false, possibleClasses);
    }
    public TextInstance classify(TextInstance document, boolean loadDictionary) {
        return classify(document, loadDictionary, null);
    }

    public TextInstance classify(TextInstance document, boolean loadDictionary,
            Set<String> possibleClasses) {

        int classType = getClassificationType();

        if (document == null) {
            return new TextInstance();
        }

        long t1 = System.currentTimeMillis();

        if (loadDictionary) {
            loadDictionary(classType);
        } else {
            categories = dictionary.getCategories();
        }

        // make a look up in the context map for every single term
        CategoryEntries bestFitList = new CategoryEntries();

        // initialize all categories with 0
        // for (Category category : categories) {
        // Category c = new Category(category.getName());
        // c.setClassType(classType);
        // category.setClassType(classType);
        // c.calculatePrior(dictionary.getNumberOfDocuments());
        // bestFitList.add(c);
        // }

        // create one category entry for every category with relevance 0
        for (Category category : categories) {
            if (possibleClasses != null && !possibleClasses.contains(category.getName())) {
                continue;
            }
            CategoryEntry c = new CategoryEntry(bestFitList, category, 0);
            bestFitList.add(c);
        }

        // count the number of categories that are somehow relevant for the current document
        // Map<String, Integer> relevantCategories = new HashMap<String, Integer>();

        // iterate through all weighted terms in the document
        for (Map.Entry<Term, Double> weightedTerm : document.getWeightedTerms().entrySet()) {

            CategoryEntries dictionaryCategoryEntries = dictionary.get(weightedTerm.getKey());

            if (!dictionaryCategoryEntries.isEmpty()) {

                /**
                 * XXX Attention: The following loop will create *loads* of CategoryEntry instances, filling up the
                 * memory in no time. We only need this for Bayes + La Place; which is commented out below. Elsewise
                 * this code is not neccessary. It will slow down the classification process significantly and consume
                 * great amounts of memory! -- Philipp, 2010-07-21.
                 * 
                 * Performance comparison (1000 Entries, Tagging, Bigrams):
                 * 4:38 Minutes, 309 MB vs. 0:18 Minutes, 72 MB
                 * 
                 */
                // // add empty category entries for categories that did not match the term
                // for (CategoryEntry ce : bestFitList) {
                // if (!dictionaryCategoryEntries.hasEntryWithCategory(ce.getCategory())) {
                // dictionaryCategoryEntries.add(new CategoryEntry(dictionaryCategoryEntries, ce.getCategory(), 0));
                // } else {
                // Integer relevanceCount0 = relevantCategories.get(ce.getCategory().getName());
                // if (relevanceCount0 == null) {
                // relevantCategories.put(ce.getCategory().getName(), 1);
                // } else {
                // int relevanceCount = relevantCategories.get(ce.getCategory().getName());
                // relevanceCount++;
                // relevantCategories.put(ce.getCategory().getName(), relevanceCount);
                // }
                // }
                // }

                // iterate through all categories in the dictionary for the weighted term
                for (CategoryEntry categoryEntry : dictionaryCategoryEntries) {
                    String categoryName = categoryEntry.getCategory().getName();
                    CategoryEntry c = bestFitList.getCategoryEntry(categoryName);
                    if (c == null) {
                        continue;
                    }

                    // in tag mode, boost the category entry if the category is part of the URL
                    if (classificationTypeSetting.getClassificationType() == ClassificationTypeSetting.TAG
                            && classificationTypeSetting.getClassificationTypeTagSetting().isTagBoost()) {
                        if (classType == ClassificationTypeSetting.TAG
                                && document.getUrl().toLowerCase().indexOf(categoryName.toLowerCase()) > -1
                                && categoryName.length() > 3) {
                            c.addAbsoluteRelevance(weightedTerm.getValue() * categoryName.length());
                        }
                    }

                    // double relevance = calculateRelevance(c,categoryEntry,weightedTerm);
                    // double relevance = calculateRelevance(c,weightedTerm.getText());
                    // c.setRelevance(relevance);

                    // add the absolute weight of the term to the category
                    // c.addAbsoluteRelevance(weightedTerm.getValue());

                    // add the absolute weight of the term to the category
                    if (categoryEntry.getRelevance() > 0) {
                        // c.addAbsoluteRelevance(weightedTerm.getValue());
                        // c.addAbsoluteRelevance(weightedTerm.getValue() * categoryEntry.getRelevance());

                        // use prior AND relevance
                        // c.addAbsoluteRelevance(categoryEntry.getCategory().getPrior() * weightedTerm.getValue()
                        // * categoryEntry.getRelevance());

                        // use prior only
                        // c.addAbsoluteRelevance(categoryEntry.getCategory().getPrior());

                        // use relevance
                        c.addAbsoluteRelevance(weightedTerm.getValue() * categoryEntry.getRelevance()
                                * categoryEntry.getRelevance());

                        // double idf = categoryEntry.getAbsoluteRelevance() / (double)
                        // dictionary.getNumberOfDocuments();
                        // c.addAbsoluteRelevance(weightedTerm.getValue() * categoryEntry.getRelevance() * idf);
                        // c.addAbsoluteRelevance(weightedTerm.getValue() * categoryEntry.getAbsoluteRelevance());
                    }

                    // c.addAbsoluteRelevance(weightedTerm.getValue()*categoryEntry.getRelevance()*categoryEntry.getCategory().getPrior());
                    // c.addAbsoluteRelevance(weightedTerm.getValue()*categoryEntry.getRelevance());
                    // c.addAbsoluteRelevance(categoryEntry.getAbsoluteRelevance());

                    // using only the priors
                    // c.addAbsoluteRelevance(categoryEntry.getCategory().getPrior());

                    // Bayes with La Place smoothing
                    // see XXX comment above!
                    // if (c.getAbsoluteRelevance() == 0) {
                    // //c.addAbsoluteRelevance(1);
                    // c.addAbsoluteRelevance(categoryEntry.getCategory().getPrior());
                    // }
                    // c.multAbsRel(categoryEntry.getRelevance()+0.01);
                    // System.out.println("abc");
                }

            } else {
                LOGGER.trace("the term \"" + weightedTerm.getKey().getText()
                        + "\" is not in the learned dictionary and cannot be associated with any category");
            }

        }

        // XXX: experimental
        // for (Entry<String, Integer> relevantCategory : relevantCategories.entrySet()) {
        // CategoryEntry c = bestFitList.getCategoryEntry(relevantCategory.getKey());
        // c.multAbsRel(relevantCategory.getValue());
        // }

        // add all categories to processed document
        document.assignCategoryEntries(bestFitList);

        // co-occurrence boost (wcm has to be build in addToDictionary method)
        if (classificationTypeSetting.getClassificationType() == ClassificationTypeSetting.TAG
                && classificationTypeSetting.getClassificationTypeTagSetting().isUseCooccurrence()) {

            dictionary.getWcm().makeRelativeScores();

            // number one category entry
            CategoryEntry numberOne = null;
            for (CategoryEntry c : document.getAssignedCategoryEntriesByRelevance(classType)) {
                numberOne = c;
                break;
            }

            if (numberOne != null) {

                List<WordCorrelation> correlations = dictionary.getWcm().getCorrelations(
                        numberOne.getCategory().getName(), 1);
                for (WordCorrelation wc : correlations) {
                    String cooccuringCategory = wc.getTerm1().getText();
                    if (cooccuringCategory.equalsIgnoreCase(numberOne.getCategory().getName())) {
                        cooccuringCategory = wc.getTerm2().getText();
                    }

                    CategoryEntry cen = bestFitList.getCategoryEntry(cooccuringCategory);
                    cen.multAbsRel(wc.getRelativeCorrelation());
                }
            }
        }

        if (classType == ClassificationTypeSetting.HIERARCHICAL) {

            // set weights for this document in the node tree
            getDictionary().hierarchyRootNode.resetWeights();
            for (CategoryEntry c : bestFitList) {
                TreeNode tn = getDictionary().hierarchyRootNode.getNode(c.getCategory().getName());
                if (tn != null) {
                    tn.setWeight(c.getRelevance());
                }
            }

            // System.out.println("main category: " +document.getMainCategory().getName());
            CategoryEntries hiearchyCategories = new CategoryEntries();
            String mc = document.getMainCategoryEntry().getCategory().getName();
            try {
                mc = document.getMainCategoryEntry().getCategory().getName();
                List<TreeNode> nodes = getDictionary().hierarchyRootNode.getNode(mc).getFullPath();
                for (TreeNode node : nodes) {
                    if (node == null) {
                        continue;
                    }
                    CategoryEntry ce = bestFitList.getCategoryEntry(node.getLabel());
                    if (ce != null) {
                        hiearchyCategories.add(ce);
                    }
                }
                hiearchyCategories.setRelevancesInPercent(true);
                document.assignCategoryEntries(hiearchyCategories);
            } catch (Exception e) {
                Logger.getRootLogger().error(
                        "class " + mc + " was not learned and could not be found in hierarchy tree: "
                        + document.getMainCategoryEntry().getCategory().getName(), e);
                document.limitCategories(5, 5, 0.0);
            }
        }

        // keep only top X categories for tagging mode
        else if (classType == ClassificationTypeSetting.TAG) {
            document.limitCategories(classificationTypeSetting.getClassificationTypeTagSetting().getMinTags(),
                    classificationTypeSetting.getClassificationTypeTagSetting().getMaxTags(), classificationTypeSetting
                    .getClassificationTypeTagSetting().getTagConfidenceThreshold());
        }

        // keep only top category for single mode
        else if (classType == ClassificationTypeSetting.SINGLE) {
            document.limitCategories(1, 1, 0.0);
        }

        // calculate one regression value for the given documents
        else if (classType == ClassificationTypeSetting.REGRESSION) {
            double regressionValue = 0;
            for (CategoryEntry ce : document.getAssignedCategoryEntries()) {
                if (ce.getRelevance() > 0) {
                    regressionValue += Double.valueOf(ce.getCategory().getName()) * ce.getRelevance();
                }
            }
            document.getAssignedCategoryEntries().clear();
            document.addCategoryEntry(new CategoryEntry(document.getAssignedCategoryEntries(), new Category(String
                    .valueOf(regressionValue)), 1));
        }

        if (document.getAssignedCategoryEntries().isEmpty()) {
            Category unassignedCategory = new Category(null);
            categories.add(unassignedCategory);
            CategoryEntry defaultCE = new CategoryEntry(bestFitList, unassignedCategory, 1);
            document.addCategoryEntry(defaultCE);
        }

        document.setClassifiedAs(classType);

        LOGGER.debug("classified document (classType " + classType + ") in " + DateHelper.getRuntime(t1) + " "
                + " (" + document.getAssignedCategoryEntriesByRelevance(classType) + ")");

        return document;
    }

    @Override
    public TextInstance classify(TextInstance document) {
        return classify(document, true);
    }

    public void classifyTestDocuments(boolean loadDictionary) {
        for (TextInstance testDocument : getTestDocuments()) {
            classify(testDocument, loadDictionary);
        }
    }

    @Override
    public TextInstance preprocessDocument(String text, TextInstance classificationDocument) {
        return preprocessor.preProcessDocument(text, classificationDocument);
    }

    @Override
    public TextInstance preprocessDocument(String text) {
        return preprocessor.preProcessDocument(text);
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    public void setDictionaryPath(String dictionaryPath) {
        if (dictionaryPath.length() > 0 && !dictionaryPath.endsWith("/")) {
            dictionaryPath += "/";
        }
        this.dictionaryPath = dictionaryPath;
        dictionary.setIndexPath(dictionaryPath);
    }

    public String getDictionaryPath() {
        return dictionaryPath;
    }

    public String getDictionaryName() {
        return getName() + "Dictionary";
    }

    public void setIncrementalTraining(boolean incrementalTraining) {
        this.incrementalTraining = incrementalTraining;
        getDictionary().useIndex();
    }

    public boolean isIncrementalTraining() {
        return incrementalTraining;
    }

    public void close() {
        if (getDictionary().isUseIndex()) {
            getDictionary().closeIndex();
        }
    }

}