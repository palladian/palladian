package ws.palladian.classification.page;

import java.util.HashMap;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Instance;
import ws.palladian.classification.Term;

/**
 * The document representation.
 * 
 * @author David Urbansky
 */
public class TextInstance extends Instance {

    /** The serial versionID. */
    private static final long serialVersionUID = -8267208138474092401L;

    // a document can be a test or a training document
    public static final int TEST = 1;
    public static final int TRAINING = 2;
    public static final int UNCLASSIFIED = 3;

    /**
     * The real categories are given for training documents (and test documents that are used to determine the quality
     * of the classifier).
     */
    protected Categories realCategories;

    /** Each document has a unique URL. */
    private String content = "";

    /** The weighted terms with term,weight representation. */
    private HashMap<Term, Double> weightedTerms;

    /** The type of the document (TEST, TRAINING or unknown). */
    private int documentType = UNCLASSIFIED;


    /**
     * The constructor.
     */
    public TextInstance() {
        weightedTerms = new HashMap<Term, Double>();
        assignedCategoryEntries = new CategoryEntries();
    }

    /**
     * Set the real categories (mainly for training documents).
     * 
     * @param categories The real categories.
     */
    public void setRealCategories(Categories categories) {
        this.realCategories = categories;
    }

    /**
     * Get the real categories of the document.
     * 
     * @return The real categories.
     */
    public Categories getRealCategories() {
        return realCategories;
    }

    public String getRealCategoriesString() {
        StringBuilder sb = new StringBuilder();
        for (Category c : realCategories) {
            sb.append(c.getName()).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public Category getFirstRealCategory() {
        if (realCategories != null && !realCategories.isEmpty()) {
            return realCategories.get(0);
        }
        return null;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public HashMap<Term, Double> getWeightedTerms() {
        return weightedTerms;
    }

    public void setWeightedTerms(HashMap<Term, Double> weightedTerms) {
        this.weightedTerms = weightedTerms;
    }

//    public int getDocumentType() {
//        return documentType;
//    }

    public void setDocumentType(int documentType) {
        this.documentType = documentType;
    }

    // @Override
    // public String toString() {
    // StringBuilder string = new StringBuilder();
    // //string.append(getUrl());
    // CategoryEntries sortedCategories = getAssignedCategoryEntriesByRelevance(getClassifiedAs());
    // for (CategoryEntry categoryEntry : sortedCategories) {
    // string.append(categoryEntry.getCategory().getName()).append(" (").append(MathHelper.round(100 *
    // categoryEntry.getRelevance(), 2)).append("%)\n");
    // }
    // return string.toString();
    // }
}