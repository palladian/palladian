package tud.iir.knowledge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import tud.iir.extraction.entity.EntityDateComparator;
import tud.iir.extraction.entity.EntityTrustComparator;
import tud.iir.helper.StringHelper;
import tud.iir.persistence.DatabaseManager;

/**
 * The knowledge unit concept.
 * 
 * @author David Urbansky
 */
public class Concept implements Serializable {

    private static final long serialVersionUID = -3556469024339891409L;

    /** determines how many entities are loaded from the database at once for fact extraction */
    private final static int ENTITY_SET_SIZE = 20;

    /** count the number of entity loads for the concept */
    private int entityLoads = 0;

    private String name;
    private String superClass;
    private Date lastSearched;
    private HashSet<String> synonyms;
    private HashSet<Attribute> attributes;
    private ArrayList<Entity> entities;
    private KnowledgeManager knowledgeManager = null; // TODO not needed
    private int id = -1;

    // needed for ontofly edit mode
    private String newName;
    private String newSuperClass;
    private HashSet<String> newSynonyms = new HashSet<String>();
    boolean hasNewSynonyms = false;
    private HashSet<Attribute> attributesToDelete = new HashSet<Attribute>();

    public Concept(String name) {
        this.setName(name);
        this.synonyms = new HashSet<String>();
        this.attributes = new HashSet<Attribute>();
        this.entities = new ArrayList<Entity>();
    }

    public Concept(String name, KnowledgeManager knowledgeManager) {
        this.setName(name);
        this.setKnowledgeManager(knowledgeManager);
        this.synonyms = new HashSet<String>();
        this.attributes = new HashSet<Attribute>();
        this.entities = new ArrayList<Entity>();
    }

    public int getID() {
        return this.id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public String getSuperClass() {
        return this.superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public String getNewSuperClass() {
        return newSuperClass;
    }

    public void setNewSuperClass(String newSuperClass) {
        this.newSuperClass = newSuperClass;
    }

    public String getName() {
        return name;
    }

    public String getSafeName() {
        return StringHelper.makeSafeName(getName());
    }

    public boolean hasSynonym(String name) {
        Iterator<String> synIterator = getSynonyms().iterator();
        while (synIterator.hasNext()) {
            String synonym = synIterator.next();
            if (synonym.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNewName() {
        return newName;
    }

    public String getSafeNewName() {
        return StringHelper.makeSafeName(getNewName());
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public HashSet<String> getSynonyms() {
        return synonyms;
    }

    public HashSet<String> getNewSynonyms() {
        return newSynonyms;
    }

    public void setNewSynonyms(HashSet<String> newSynonyms) {
        this.newSynonyms = newSynonyms;
        this.hasNewSynonyms = true;
    }

    public boolean hasNewSynonyms() {
        return hasNewSynonyms;
    }

    public String getSynonymsToString() {
        StringBuilder synonymString = new StringBuilder();

        Iterator<String> sIterator = getSynonyms().iterator();
        while (sIterator.hasNext()) {
            synonymString.append(sIterator.next()).append("; ");
        }

        return synonymString.toString().substring(0, Math.max(0, synonymString.length() - 2));
    }

    public void setSynonyms(HashSet<String> synonyms) {
        this.synonyms = synonyms;
    }

    public void addSynonym(String synonym) {
        if (!synonym.equalsIgnoreCase(getName()))
            this.synonyms.add(synonym);
    }

    public KnowledgeManager getKnowledgeManager() {
        return knowledgeManager;
    }

    public void setKnowledgeManager(KnowledgeManager knowledgeManager) {
        knowledgeManager.addConcept(this);
        this.knowledgeManager = knowledgeManager;
    }

    public Date getLastSearched() {
        return lastSearched;
    }

    public void setLastSearched(Date lastSearched) {
        this.lastSearched = lastSearched;
    }

    public HashSet<Attribute> getAttributes() {
        return getAttributes(true);
    }

    public HashSet<Attribute> getAttributes(boolean onlyManuallyAdded) {
        if (onlyManuallyAdded) {
            HashSet<Attribute> manuallyAdded = new HashSet<Attribute>();

            for (Attribute currentAttribute : attributes) {
                if (currentAttribute.getTrust() == 1) {
                    manuallyAdded.add(currentAttribute);
                }
            }
            return manuallyAdded;
        }

        return attributes;
    }

    public HashSet<Attribute> getAttributesToDelete() {
        return attributesToDelete;
    }

    public ArrayList<Attribute> getAttributesAsList(boolean onlyManuallyAdded) {
        ArrayList<Attribute> attributeList = new ArrayList<Attribute>();
        Iterator<Attribute> attributeIterator = attributes.iterator();
        while (attributeIterator.hasNext()) {
            Attribute currentAttribute = attributeIterator.next();
            if (onlyManuallyAdded && currentAttribute.getExtractedAt() == null) {
                attributeList.add(currentAttribute);
            } else if (!onlyManuallyAdded) {
                attributeList.add(currentAttribute);
            }
        }
        return attributeList;
    }

    public HashSet<String> getAttributeNames() {
        HashSet<String> attributeNames = new HashSet<String>();
        Iterator<Attribute> attributeIterator = getAttributes().iterator();
        while (attributeIterator.hasNext()) {
            attributeNames.add(attributeIterator.next().getName());
        }
        return attributeNames;
    }

    public void setAttributes(HashSet<Attribute> attributes) {
        this.attributes = attributes;
    }

    public boolean addAttribute(Attribute attribute) {
        // check whether attribute has been entered already
        if (!hasAttribute(attribute.getName())) {
            this.attributes.add(attribute);
            return true;
        } else {
            return false;
        }
    }

    public boolean hasAttribute(String attributeName) {
        return hasAttribute(attributeName, false);
    }

    public boolean hasAttribute(String attributeName, boolean onlyManuallyAdded) {
        Iterator<Attribute> aIt = getAttributes(onlyManuallyAdded).iterator();
        while (aIt.hasNext()) {
            Attribute a = aIt.next();
            if (a.getName().equalsIgnoreCase(attributeName))
                return true;
        }
        return false;
    }

    public Attribute getAttribute(String attributeName) {
        return getAttribute(attributeName, true);
    }

    public Attribute getAttribute(String attributeName, boolean useSynonyms) {
        Iterator<Attribute> aIt = this.attributes.iterator();
        while (aIt.hasNext()) {
            Attribute a = aIt.next();
            if (a.getName().equalsIgnoreCase(attributeName))
                return a;
            if (useSynonyms && a.hasSynonym(attributeName)) {
                return a;
            }
        }
        return null;
    }

    public Attribute getAttribute(int attributeId) {
        Iterator<Attribute> aIt = this.attributes.iterator();
        while (aIt.hasNext()) {
            Attribute a = aIt.next();
            if (a.getID() == attributeId)
                return a;
        }
        return null;
    }

    public boolean removeAttribute(int attributeId) {
        Attribute attribute = this.getAttribute(attributeId);
        this.attributesToDelete.add(attribute);
        return attributes.remove(attribute);
    }

    public ArrayList<Entity> getEntities() {
        return entities;
    }

    public ArrayList<Entity> getEntitiesByTrust() {
        // sort by trust
        Collections.sort(entities, new EntityTrustComparator());
        return entities;
    }

    public ArrayList<Entity> getEntitiesByDate() {
        // sort by lastSearched date
        Collections.sort(entities, new EntityDateComparator());
        return entities;
    }

    public void clearEntities() {
        this.entities = new ArrayList<Entity>();
    }

    public void setEntities(ArrayList<Entity> entities) {
        this.entities = entities;
    }

    public synchronized void addEntity(Entity entity) {
        entity.normalizeName();

        // EntityExtractor.getInstance().getLogger().log("enter entity: " +
        // entity.getName());

        String entityName = entity.getName();

        if (this.hasEntity(entityName)) {
            Entity processEntity = this.getEntity(entityName);
            processEntity.addSources(entity.getSources());
            // EntityExtractor.getInstance().getLogger().log("...count updated");
        } else {
            this.entities.add(entity);
            // EntityExtractor.getInstance().getLogger().log("...new entity");
        }
    }

    public boolean hasEntity(String entityName) {
        synchronized (entities) {
            for (Entity e : entities) {
                if (e.getName().equalsIgnoreCase(entityName))
                    return true;
            }
        }
        return false;
    }

    public Entity getEntity(String entityName) {
        synchronized (entities) {
            for (Entity e : entities) {
                if (e.getName().equalsIgnoreCase(entityName))
                    return e;
            }
        }
        return null;
    }

    /**
     * Load entities for the concept from the rdb. Load oldest (lastSearched) first.
     * 
     * @param continueFromLastExtraction If true, the counter is set to the last extraction and it will be continued from there.
     */
    public void loadEntities(boolean continueFromLastExtraction) {
        setEntities(DatabaseManager.getInstance().loadEntities(this, ENTITY_SET_SIZE, entityLoads, continueFromLastExtraction));
        entityLoads++;
    }

    @Override
    public final String toString() {
        return this.getName() + "(" + this.getID() + ")";
    }

}