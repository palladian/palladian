package tud.iir.extraction.entity;

import tud.iir.extraction.ExtractionType;
import tud.iir.knowledge.Concept;

/**
 * The PhraseExtraction technique.
 * 
 * @author David Urbansky
 */
public class PhraseExtractor extends EntityExtractionTechnique {

    PhraseWrapperInductor pwi = null;

    public PhraseExtractor() {
        pwi = new PhraseWrapperInductor();
        setExtractionTechnique(ExtractionType.ENTITY_PHRASE);
    }

    @Override
    public Integer[] getPatterns() {
        Integer[] patterns = { EntityQueryFactory.TYPE_XP_SUCH_AS, EntityQueryFactory.TYPE_XP_LIKE, EntityQueryFactory.TYPE_XP_INCLUDING,
                EntityQueryFactory.TYPE_XP_ESPECIALLY };
        return patterns;
    }

    @Override
    public EntityQuery getEntityQuery(Concept concept, int entityQueryType) {
        return EntityQueryFactory.getInstance().createPhraseQuery(concept, entityQueryType);
    }

    @Override
    public void extract(String url, EntityQuery eq, Concept concept) {
        pwi.extract(url, eq, concept);
    }
}