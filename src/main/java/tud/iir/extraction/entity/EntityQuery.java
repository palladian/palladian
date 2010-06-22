package tud.iir.extraction.entity;

import tud.iir.extraction.Query;

/**
 * The entity specific query.
 * 
 * @author David Urbansky
 */
class EntityQuery extends Query {

    private String regularExpression = "";
    private String[] seeds;

    public EntityQuery() {
    }

    public String getRegularExpression() {
        return regularExpression;
    }

    public void setRegularExpression(String regularExpression) {
        this.regularExpression = regularExpression;
    }

    public String[] getSeeds() {
        return seeds;
    }

    public void setSeeds(String[] seeds) {
        this.seeds = seeds;
    }

    public int getRetrievalExtractionType() {
        if (getQueryType() == EntityQueryFactory.TYPE_XP_SUCH_AS || getQueryType() == EntityQueryFactory.TYPE_XP_LIKE
                || getQueryType() == EntityQueryFactory.TYPE_XP_INCLUDING) {
            return EntityQueryFactory.RETRIEVAL_EXTRACTION_TYPE_PHRASE;
        } else if (getQueryType() == EntityQueryFactory.TYPE_LIST_OF_XP || getQueryType() == EntityQueryFactory.TYPE_XS_LIST
                || getQueryType() == EntityQueryFactory.TYPE_BROWSE_XP || getQueryType() == EntityQueryFactory.TYPE_INDEX_OF_XP
                || getQueryType() == EntityQueryFactory.TYPE_XS_INDEX) {
            return EntityQueryFactory.RETRIEVAL_EXTRACTION_TYPE_FOCUSED_CRAWL;
        } else if (getQueryType() == EntityQueryFactory.TYPE_SEED_2 || getQueryType() == EntityQueryFactory.TYPE_SEED_3
                || getQueryType() == EntityQueryFactory.TYPE_SEED_4 || getQueryType() == EntityQueryFactory.TYPE_SEED_5) {
            return EntityQueryFactory.RETRIEVAL_EXTRACTION_TYPE_SEED;
        }
        return 0;
    }
}