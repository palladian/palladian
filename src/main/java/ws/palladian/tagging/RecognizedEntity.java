package ws.palladian.tagging;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.helper.nlp.StringHelper;

public class RecognizedEntity {

    private String name = "";
    private CategoryEntries categoryEntries = null;
    private double trust = 0.0;

    public RecognizedEntity(String name, CategoryEntries categories, double trust) {
        setName(name);
        if (categories == null) {
            this.categoryEntries = new CategoryEntries();
        } else {
            this.categoryEntries = categories;
        }
        this.trust = trust;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = StringHelper.trim(name);
    }

    public boolean hasCategoryEntries() {
        if (categoryEntries.size() > 0)
            return true;
        return false;
    }

    public CategoryEntries getCategoryEntries() {
        return categoryEntries;
    }

    public void setCategoryEntries(CategoryEntries categories) {
        this.categoryEntries = categories;
    }

    public void addCategoryEntry(CategoryEntry categoryEntry) {
        this.categoryEntries.add(categoryEntry);
    }

    public void addCategoryEntries(CategoryEntries categoryEntries) {
        this.categoryEntries.addAll(categoryEntries);
    }

    public double getTrust() {
        // trust = Math.min(1.0, trust);
        return trust;
    }

    public void setTrust(double trust) {
        this.trust = trust;
    }

    public void addTrust(double trust) {
        this.trust += (this.trust * trust);
        // this.trust = Math.min(1.0, this.trust);
    }

    @Override
    public boolean equals(Object obj) {
        if (((RecognizedEntity) obj).getName().equalsIgnoreCase(this.getName())) {
            return true;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return name + " (" + categoryEntries + ", " + getTrust() + ")";
    }
}