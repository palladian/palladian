package tud.iir.knowledge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import tud.iir.extraction.Filter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;

/**
 * The knowledge unit entity.
 * 
 * @author David Urbansky
 */
public class Entity extends Extractable {

    private static final long serialVersionUID = 2481102464109115630L;

    private ArrayList<Fact> facts;
    private ArrayList<Snippet> snippets;

    // whether the entity has been given as an initial entity to find others
    private boolean initial = false;

    // the concept the entity belongs to
    private Concept concept;

    public Entity(String name, Concept concept, boolean initial) {
        init();
        setName(name);
        setConcept(concept);
        setInitial(initial);
        if (initial) {
            setTrust(1.0);
        }
    }

    public Entity(String name, Concept concept, double trust) {
        init();
        setName(name);
        setConcept(concept);
        setTrust(trust);
    }

    public Entity(String name, Concept concept) {
        init();
        setName(name);
        setConcept(concept);
    }

    public Entity(String name) {
        init();
        setName(name);
    }

    private void init() {
        this.facts = new ArrayList<Fact>();
        this.snippets = new ArrayList<Snippet>();
        this.sources = new Sources<Source>();
    }

    public Concept getConcept() {
        return concept;
    }

    public void setConcept(Concept concept) {
        // concept.addEntity(this); //IMPORTANT TODO changed without checking
        this.concept = concept;
    }

    public ArrayList<Snippet> getSnippets() {
        return snippets;
    }

    public void addSnippets(List<Snippet> snippets) {
        this.snippets.addAll(snippets);
    }

    public ArrayList<Fact> getFacts() {
        return facts;
    }

    public Fact getFactForAttribute(Attribute attribute) {
        Fact factEntry = null;
        Iterator<Fact> factIterator = this.facts.iterator();
        while (factIterator.hasNext()) {
            factEntry = factIterator.next();
            if (factEntry.getAttribute().getName().equalsIgnoreCase(attribute.getName())) {
                return factEntry;
            }
        }
        return factEntry;
    }

    public void setFacts(ArrayList<Fact> facts) {
        this.facts = facts;
    }

    public void addFactForBenchmark(Fact fact, FactValue factValue) {
        fact.setCorrectValue(factValue);
        facts.add(fact);
    }

    public void addFactAndValue(Fact fact, FactValue factValue) {
        // check whether there are already fact values for the attribute of the fact...
        boolean factForAttributeEntered = false;
        Iterator<Fact> factIterator = this.facts.iterator();
        while (factIterator.hasNext()) {
            Fact factEntry = factIterator.next();
            if (factEntry.getAttribute().getName().equalsIgnoreCase(fact.getAttribute().getName())) {

                // Fact factEntry = getFactForAttribute(fact.getAttribute());
                factEntry.addFactValue(factValue);
                // this.facts.add(factEntry);
                factForAttributeEntered = true;
                break;
            }
        }

        // ...if not enter it...
        if (!factForAttributeEntered) {
            fact.addFactValue(factValue);
            this.facts.add(fact);
        }
        // ...otherwise just enter the fact value, checking whether fact value has been entered already is done in Fact
        // class
        // else {
        // Fact factEntry = getFactForAttribute(fact.getAttribute());
        // factEntry.addFactValue(factValue);
        //
        // }
    }

    public int getNumberOfExtractions(int extractionType) {
        int numberOfExtractions = 0;

        Iterator<Source> sourceIterator = sources.iterator();
        while (sourceIterator.hasNext()) {
            Source currentSource = sourceIterator.next();
            if (currentSource.getExtractionType() == extractionType) {
                numberOfExtractions++;
            }
        }

        return numberOfExtractions;
    }

    public boolean isInitial() {
        return initial;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    /**
     * Return the number of times the entity has been extracted.
     * 
     * @return Number of times the entity has been extracted.
     */
    public int getExtractionCount() {
        // int count = 0;
        //
        // Iterator<Source> sIt = this.getSources().iterator();
        // while (sIt.hasNext()) {
        // sIt.next();
        // count++;
        // }
        // return count;

        return getSources().size();
    }

    /**
     * Return the distinct number extraction types used to extract the entity.
     * 
     * @return Number of times the entity has been extracted.
     */
    public int getExtractionTypeCount() {
        return getExtractionTypes().size();
    }

    /**
     * Return a set of the extraction types used to extract the entity.
     * 
     * @return Set of extractionTypes used to extract the entity:
     */
    public HashSet<Integer> getExtractionTypes() {
        HashSet<Integer> extractionTypesUsed = new HashSet<Integer>();

        Iterator<Source> sIt = getSources().iterator();
        while (sIt.hasNext()) {
            Source s = sIt.next();
            extractionTypesUsed.add(s.getExtractionType());
        }
        return extractionTypesUsed;
    }

    public boolean isCorrect() {
        if (getExtractionCount() >= Filter.minEntityCorroboration) {
            return true;
        }
        return false;
    }

    /**
     * Normalize the entity's name.
     */
    public void normalizeName() {
        String name = getName();

        // disallow entities to be a file name
        if (FileHelper.isFileName(name)) {
            name = "";
        }

        // remove everything in brackets
        name = StringHelper.removeBrackets(name);

        // complete trimming
        name = StringHelper.trim(name);

        // article in front
        name = StringHelper.putArticleInFront(name);
        setName(name);
    }

    @Override
    public String toString() {
        return getName() + " (Concept: " + getConcept() + " , Trust:" + getTrust() + ")";
    }

    public static void main(String[] a) {
        String name = "\"abc\"";
        // name = "\"abc";
        name = "\"abc\"def\"";
        name = "\" adf fs";
        name = "\"";
        // remove everything in brackets
        name = StringHelper.removeBrackets(name);

        // complete trimming
        name = StringHelper.trim(name);

        // article in front
        name = StringHelper.putArticleInFront(name);

        System.out.println(name);

    }
}