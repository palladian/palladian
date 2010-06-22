package tud.iir.extraction.snippet;

import tud.iir.extraction.Query;
import tud.iir.knowledge.Entity;

/**
 * A snippet query is a search query to retrieve relevant pages for an entity to extract snippets from.
 * 
 * @author Christopher Friedrich
 */
public class SnippetQuery extends Query {

    // every fact query is about a certain entity
    private Entity entity;

    public SnippetQuery(Entity entity) {
        this.setEntity(entity);
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}