package ws.palladian.classification.page;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;

/**
 * A test document is a document that has given information about the correct category but is classified using a classifier It is used to determine the accuracy
 * of the classifier.
 * 
 * @author David Urbansky
 */
public class TestDocument extends TextInstance {

    /** If true, the classification has been checked. */
    private boolean checkedClassification = false;

    /** If true, the document has been classified correctly. */
    private boolean correctClassified = false;

    public TestDocument() {
        setDocumentType(TextInstance.TEST);
    }

    public CategoryEntries getCorrectlyAssignedCategoryEntries() {
        CategoryEntries correctlyAssignedCategoryEntries = new CategoryEntries();

        for (CategoryEntry ace : assignedCategoryEntries) {
            for (Category rc : realCategories) {
                if (ace.getCategory().getName().equalsIgnoreCase(rc.getName())) {
                    correctlyAssignedCategoryEntries.add(ace);
                }
            }
        }

        return correctlyAssignedCategoryEntries;
    }

    public double getPrecisionAt(int rank) {
        double precision = 0.0;
        CategoryEntries entries = getAssignedCategoryEntriesByRelevance(getClassifiedAs());

        int correct = 0;
        int rankCount = 1;
        for (CategoryEntry ace : entries) {
            for (Category rc : realCategories) {
                if (ace.getCategory().getName().equalsIgnoreCase(rc.getName())) {
                    correct++;
                }
            }
            if (rankCount == rank) {
                break;
            }
            rankCount++;
        }

        precision = (double) correct / (double) rank;

        return precision;
    }

    /**
     * Returns true if the document is correct classified. Hierarchical classified documents count as correct if main category matches. Tag classified documents
     * count as correct if first (main) tags matches any real tag.
     * 
     * @return True if the document is correct classified, false otherwise.
     */
    public boolean isCorrectClassified() {
        if (checkedClassification) {
            return correctClassified;
        }
        checkedClassification = true;

        String mcn = getMainCategoryEntry().getCategory().getName();

        // in simple or hieararchy mode, main categories have to match
        if (getClassifiedAs() == ClassificationTypeSetting.SINGLE
                || getClassifiedAs() == ClassificationTypeSetting.HIERARCHICAL) {
            if (mcn.equals(getFirstRealCategory().getName())) {
                correctClassified = true;
                return correctClassified;
            }
        }

        // in tag mode, main tag has to match one of the real tags
        else if (getClassifiedAs() == ClassificationTypeSetting.TAG) {
            for (Category realCategory : getRealCategories()) {
                if (mcn.equals(realCategory.getName())) {
                    correctClassified = true;
                    return correctClassified;
                }
            }
        }

        // any assigned category must match any of the real categories to make the classification correct
        /*
         * for (Category assignedCategory : getAssignedCategories()) { for (Category realCategory : getRealCategories()) { if
         * (assignedCategory.getName().equals(realCategory.getName())) { correctClassified = true; checkedClassification = true; return correctClassified; } } }
         */

        correctClassified = false;
        return correctClassified;
    }
}