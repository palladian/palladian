package tud.iir.extraction.fact;

import java.util.HashSet;
import java.util.Iterator;

import tud.iir.extraction.Query;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Entity;

/**
 * A fact query is a search query to retrieve relevant pages for an entity to extract facts from.
 * 
 * @author David Urbansky
 */
class FactQuery extends Query {

    public static final int FREE_TEXT_ONLY = 1; // result pages will probably have the searched fact only in free text (due to the query structure)
    public static final int FREE_TEXT_AND_STRUCTURED = 2; // the query structure leaves it open whether facts on the result pages will appear in free text or in
    // tables (structured areas)
    private int factStructureType = FREE_TEXT_AND_STRUCTURED; // either free text only or free text and structured
    private Entity entity; // every fact query is about a certain entity
    private HashSet<Attribute> attributes; // a list of attributes that should be fetched with this query

    public FactQuery(Entity entity) {
        this.setEntity(entity);
        attributes = new HashSet<Attribute>();
    }

    /**
     * TYPE_X and TYPE_X_FACTS target many attributes other queries don't this is important for the indexer to know (performance issues)
     * 
     * @return True if many attributes are targeted.
     */
    public boolean targetsManyAttributes() {
        if (this.getAttributes().size() > 1)
            return true;
        return false;
    }

    public int getFactStructureType() {
        return factStructureType;
    }

    public void setFactStructureType(int factStructureType) {
        this.factStructureType = factStructureType;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public HashSet<Attribute> getAttributes() {
        return this.attributes;
    }

    /**
     * Some queries (e.g. "the population of Germany is") do only address one single attribute.
     * 
     * @return The attribute.
     */
    public Attribute getAttribute() {
        if (this.attributes.iterator().hasNext()) {
            return this.attributes.iterator().next();
        }
        return null;
    }

    public String[] getAttributeNames() {
        int attributeCount = this.attributes.size();
        String[] attributeNames = new String[attributeCount];

        Iterator<Attribute> attributeIterator = this.attributes.iterator();
        int count = 0;
        while (attributeIterator.hasNext()) {
            Attribute attribute = attributeIterator.next();
            attributeNames[count] = attribute.getName();
            ++count;
        }

        return attributeNames;
    }

    public void setAttributes(HashSet<Attribute> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(Attribute attribute) {
        this.attributes.add(attribute);
    }
}