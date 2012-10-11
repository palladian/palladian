package ws.palladian.classification.text;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instance;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.FeatureVector;

/**
 * The document representation.
 * 
 * @author David Urbansky
 */
public class TextInstance extends Instance {

    /**
     * The real categories are given for training documents (and test documents that are used to determine the quality
     * of the classifier).
     */
    // FIXME this is never set currently.
    private List<String> realCategories;

    private String content = "";

    /** The weighted terms with term,weight representation. */
    //private Map<String, Double> weightedTerms;
    private final Set<String> terms;

    /** Type of classification (tags or hierarchy). */
    private int classifiedAs = ClassificationTypeSetting.TAG;

    /** The category of the instance, null if not classified. */
    protected CategoryEntries assignedCategoryEntries = new CategoryEntries();

    /** If the class is nominal we have an instance category. */
    private String instanceCategory;

    public void assignCategoryEntries(CategoryEntries categoryEntries) {
        this.assignedCategoryEntries = categoryEntries;
    }

    /**
     * The constructor.
     */
    public TextInstance() {
        super(null, new FeatureVector()); // XXX
        //weightedTerms = new HashMap<String, Double>();
        terms = CollectionHelper.newHashSet();
        assignedCategoryEntries = new CategoryEntries();
    }
    
    public TextInstance(Set<String> terms) {
        super(null, new FeatureVector()); // XXX
        this.terms = terms;
        assignedCategoryEntries = new CategoryEntries();
    }

    /**
     * Get the real categories of the document.
     * 
     * @return The real categories.
     */
    public List<String> getRealCategories() {
        return realCategories;
    }

    public String getFirstRealCategory() {
        if (realCategories != null && realCategories.iterator().hasNext()) {
            return realCategories.iterator().next();
        }
        return null;
    }

    public String getContent() {
        return content;
    }

//    public Map<String, Double> getWeightedTerms() {
//        return weightedTerms;
//    }
    
    public Set<String> getTerms() {
        return terms;
    }

    /**
     * Get all categories for the document.
     * 
     * @param relevancesInPercent If true then the relevance will be output in percent.
     * @return All categories.
     */
    public CategoryEntries getAssignedCategoryEntries() {
        return assignedCategoryEntries;
    }

    public CategoryEntries getAssignedCategoryEntriesByRelevance(int classType) {
        if (classType == ClassificationTypeSetting.HIERARCHICAL) {
            return assignedCategoryEntries;
        }
        sortCategoriesByRelevance();
        return assignedCategoryEntries;
    }

    public int getClassifiedAs() {
        return classifiedAs;
    }

    public String getInstanceCategory() {
        return instanceCategory;
    }

    /**
     * Get the category that is most relevant to this document.
     * 
     * @param relevanceInPercent If true then the relevance will be output in percent.
     * @return The most relevant category.
     */
    public CategoryEntry getMainCategoryEntry() {
        CategoryEntry highestMatch = null;

        for (CategoryEntry ce : this.assignedCategoryEntries) {

            if (ce == null) {
                continue;
            }

            if (highestMatch == null) {
                highestMatch = ce;
                continue;
            }

            if (ce.getRelevance() > highestMatch.getRelevance()) {
                highestMatch = ce;
            }
        }

        if (highestMatch == null) {
            Logger.getRootLogger().warn("no assigned category found");
            return new CategoryEntry(this.assignedCategoryEntries, null, 0.0);
        }

        return highestMatch;
    }

    public void setInstanceCategory(String instanceCategory) {
        this.instanceCategory = instanceCategory;
    }

//    protected void setInstances(List<? extends UniversalInstance> instances) {
//        this.instances = instances;
//    }

    public void sortCategoriesByRelevance() {
        assignedCategoryEntries.sortByRelevance();
    }

}
