package tud.iir.extraction.entity;

import java.util.ArrayList;

/**
 * A xPath pattern.
 * 
 * @author David Urbansky
 */
class Pattern {
    private String xpath;
    private ArrayList<Integer> indexes;

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public ArrayList<Integer> getIndexes() {
        return indexes;
    }

    public void setIndexes(ArrayList<Integer> indexes) {
        this.indexes = indexes;
    }

    @Override
    public boolean equals(Object obj) {
        return ((String) obj).equals(this.getXpath());
    }
}