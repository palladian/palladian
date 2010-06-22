package tud.iir.extraction;

/**
 * Abstract Query class for entity, fact and snippet queries that are sent to a search engine.
 * 
 * @author David Urbansky
 */
public abstract class Query {
    protected String[] querySet; // all queries for that fact query type, e.g. "the population of Germany is" etc.
    protected int queryType; // an id that identifies the fact query type

    public int getQueryType() {
        return queryType;
    }

    public void setQueryType(int queryType) {
        this.queryType = queryType;
    }

    public String[] getQuerySet() {
        return querySet;
    }

    public void setQuerySet(String[] querySet) {
        this.querySet = querySet;
    }
}