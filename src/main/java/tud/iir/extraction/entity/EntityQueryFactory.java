package tud.iir.extraction.entity;

import java.util.ArrayList;
import java.util.HashSet;

import tud.iir.helper.StringHelper;
import tud.iir.helper.WordTransformer;
import tud.iir.knowledge.Concept;
import tud.iir.persistence.DatabaseManager;

/**
 * The EntityQueryFactory creates EntityQuery objects.
 * 
 * @author David Urbansky
 */
public class EntityQueryFactory {

    // TODO put somewhere else | next three lines used at all?
    public static final int RETRIEVAL_EXTRACTION_TYPE_PHRASE = 1;
    public static final int RETRIEVAL_EXTRACTION_TYPE_FOCUSED_CRAWL = 2;
    public static final int RETRIEVAL_EXTRACTION_TYPE_SEED = 3;

    // XP = concept name plural (e.g. countries), XS = concept name singular (e.g. country)
    public static final int TYPE_XP_SUCH_AS = 1; // XP such as and XP (such as and variations
    public static final int TYPE_XP_LIKE = 2; // XP like
    public static final int TYPE_XP_INCLUDING = 3; // XP including
    public static final int TYPE_XP_ESPECIALLY = 4; // XP especially
    // TODO "are XP" (but often leads to retrieval of questions)

    // retrieval extraction focused crawl
    public static final int TYPE_LIST_OF_XP = 5; // list of XP
    public static final int TYPE_XS_LIST = 6; // XS list
    public static final int TYPE_BROWSE_XP = 8; // browse XP
    public static final int TYPE_INDEX_OF_XP = 9; // index of XP
    public static final int TYPE_XS_INDEX = 10; // XS index

    // TODO numbering before completely new re-initialization
    // retrieval extraction seed
    public static final int TYPE_SEED_2 = 11; // use 2 seeds
    public static final int TYPE_SEED_3 = 12; // use 3 seeds
    public static final int TYPE_SEED_4 = 13; // use 4 seeds
    public static final int TYPE_SEED_5 = 14; // use 5 seeds

    private static EntityQueryFactory instance = null;

    private EntityQueryFactory() {
    }

    public static EntityQueryFactory getInstance() {
        if (instance == null) {
            instance = new EntityQueryFactory();
        }
        return instance;
    }

    public static ArrayList<Integer> getExtractionTypes() {
        ArrayList<Integer> extractionTypes = new ArrayList<Integer>();

        extractionTypes.add(TYPE_XP_SUCH_AS);
        extractionTypes.add(TYPE_XP_LIKE);
        extractionTypes.add(TYPE_XP_INCLUDING);
        extractionTypes.add(TYPE_XP_ESPECIALLY);
        extractionTypes.add(TYPE_LIST_OF_XP);
        extractionTypes.add(TYPE_XS_LIST);
        extractionTypes.add(TYPE_BROWSE_XP);
        extractionTypes.add(TYPE_INDEX_OF_XP);
        extractionTypes.add(TYPE_XS_INDEX);
        extractionTypes.add(TYPE_SEED_2);
        extractionTypes.add(TYPE_SEED_3);
        extractionTypes.add(TYPE_SEED_4);
        extractionTypes.add(TYPE_SEED_5);

        return extractionTypes;
    }

    public EntityQuery createPhraseQuery(Concept concept, int type) {
        switch (type) {
            case TYPE_XP_SUCH_AS:
                return createPhraseQueryXPSuchAs(concept);
            case TYPE_XP_LIKE:
                return createPhraseQueryXPLike(concept);
            case TYPE_XP_INCLUDING:
                return createPhraseQueryXPIncluding(concept);
            case TYPE_XP_ESPECIALLY:
                return createPhraseQueryXPEspecially(concept);
        }
        return null;
    }

    private EntityQuery createPhraseQueryXPSuchAs(Concept concept) {
        EntityQuery eq = new EntityQuery();
        String xp = WordTransformer.wordToPlural(concept.getName());
        String[] querySet = { xp + " such as" };
        eq.setQuerySet(querySet);
        eq.setRegularExpression(xp + "((\\()|(\\,)|(\\s))*such as");
        eq.setQueryType(TYPE_XP_SUCH_AS);

        return eq;
    }

    private EntityQuery createPhraseQueryXPLike(Concept concept) {
        EntityQuery eq = new EntityQuery();
        String xp = WordTransformer.wordToPlural(concept.getName());
        String[] querySet = { xp + " like" };
        eq.setQuerySet(querySet);
        eq.setRegularExpression(xp + "((\\()|(\\,)|(\\s))*like");
        eq.setQueryType(TYPE_XP_LIKE);

        return eq;
    }

    private EntityQuery createPhraseQueryXPIncluding(Concept concept) {
        EntityQuery eq = new EntityQuery();
        String xp = WordTransformer.wordToPlural(concept.getName());
        String[] querySet = { xp + " including" };
        eq.setQuerySet(querySet);
        eq.setRegularExpression(xp + "((\\()|(\\,)|(\\s))*including");
        eq.setQueryType(TYPE_XP_INCLUDING);

        return eq;
    }

    private EntityQuery createPhraseQueryXPEspecially(Concept concept) {
        EntityQuery eq = new EntityQuery();
        String xp = WordTransformer.wordToPlural(concept.getName());
        String[] querySet = { xp + " especially" };
        eq.setQuerySet(querySet);
        eq.setRegularExpression(xp + "((\\()|(\\,)|(\\s))*especially");
        eq.setQueryType(TYPE_XP_ESPECIALLY);

        return eq;
    }

    public EntityQuery createFocusedCrawlQuery(Concept concept, int type) {
        switch (type) {
            case TYPE_LIST_OF_XP:
                return createFocusedCrawlQueryListOfXP(concept);
            case TYPE_XS_LIST:
                return createFocusedCrawlQueryXSList(concept);
            case TYPE_BROWSE_XP:
                return createFocusedCrawlQueryBrowseXP(concept);
            case TYPE_INDEX_OF_XP:
                return createFocusedCrawlQueryIndexOfXP(concept);
            case TYPE_XS_INDEX:
                return createFocusedCrawlQueryXSIndex(concept);

        }
        return null;
    }

    private EntityQuery createFocusedCrawlQueryListOfXP(Concept concept) {
        EntityQuery eq = new EntityQuery();
        String xp = WordTransformer.wordToPlural(concept.getName());
        String[] querySet = { "list of " + xp };
        eq.setQuerySet(querySet);
        eq.setRegularExpression("list of " + xp);
        eq.setQueryType(TYPE_LIST_OF_XP);

        return eq;
    }

    private EntityQuery createFocusedCrawlQueryXSList(Concept concept) {
        EntityQuery eq = new EntityQuery();
        // String xp = StringHelper.wordToPlural(concept.getName()); // TODO test with plural
        String xs = concept.getName();
        String[] querySet = { xs + " list" };
        eq.setQuerySet(querySet);
        eq.setRegularExpression(xs + " list");
        eq.setQueryType(TYPE_XS_LIST);

        return eq;
    }

    /*
     * private EntityQuery createFocusedCrawlQueryXSListing(Concept concept) {
     * EntityQuery eq = new EntityQuery();
     * // String xp = StringHelper.wordToPlural(concept.getName()); // TODO test with plural
     * String xs = concept.getName();
     * String[] querySet = { xs + " listing" };
     * eq.setQuerySet(querySet);
     * eq.setRegularExpression(xs + " listing");
     * eq.setQueryType(TYPE_XS_LIST);
     * return eq;
     * }
     */

    private EntityQuery createFocusedCrawlQueryBrowseXP(Concept concept) {
        EntityQuery eq = new EntityQuery();
        String xp = WordTransformer.wordToPlural(concept.getName());
        String[] querySet = { "browse " + xp };
        eq.setQuerySet(querySet);
        eq.setRegularExpression("browse " + xp);
        eq.setQueryType(TYPE_BROWSE_XP);

        return eq;
    }

    private EntityQuery createFocusedCrawlQueryIndexOfXP(Concept concept) {
        EntityQuery eq = new EntityQuery();
        String xp = WordTransformer.wordToPlural(concept.getName());
        String[] querySet = { "index of " + xp };
        eq.setQuerySet(querySet);
        eq.setRegularExpression("index of " + xp);
        eq.setQueryType(TYPE_INDEX_OF_XP);

        return eq;
    }

    private EntityQuery createFocusedCrawlQueryXSIndex(Concept concept) {
        EntityQuery eq = new EntityQuery();
        String xs = concept.getName();
        String[] querySet = { xs + " index" };
        eq.setQuerySet(querySet);
        eq.setRegularExpression(xs + " index");
        eq.setQueryType(TYPE_XS_INDEX);

        return eq;
    }

    public EntityQuery createSeedQuery(Concept concept, int type, int numberOfCombinations) {
        switch (type) {
            case TYPE_SEED_2:
                return createSeedQuerySeedNumber(concept, 2, numberOfCombinations);
            case TYPE_SEED_3:
                return createSeedQuerySeedNumber(concept, 3, numberOfCombinations);
            case TYPE_SEED_4:
                return createSeedQuerySeedNumber(concept, 4, numberOfCombinations);
            case TYPE_SEED_5:
                return createSeedQuerySeedNumber(concept, 5, numberOfCombinations);
        }
        return null;
    }

    private EntityQuery createSeedQuerySeedNumber(Concept concept, int numberOfSeeds, int numberOfCombinations) {
        EntityQuery eq = new EntityQuery();

        String[] querySet = new String[numberOfCombinations];
        String[] querySeeds = new String[numberOfSeeds * numberOfCombinations];

        // find random (highly ranked) previously extracted entities for the concept
        int maximumSeeds = Math.min(2000, numberOfSeeds * numberOfCombinations * 20); // number of results is limited to 2000 in DatabaseManager!
        ArrayList<String> seedCandidates = DatabaseManager.getInstance().getSeeds(concept, maximumSeeds);

        if (seedCandidates.size() == maximumSeeds) {

            for (int i = 0; i < numberOfCombinations; i++) {
                String[] seeds = new String[numberOfSeeds];

                HashSet<Integer> randomNumbersUsed = new HashSet<Integer>();
                while (randomNumbersUsed.size() < numberOfSeeds) {
                    int randomNumber = (int) (Math.random() * seedCandidates.size());
                    if (randomNumbersUsed.add(randomNumber)) {
                        seeds[randomNumbersUsed.size() - 1] = seedCandidates.get(randomNumber);
                    }
                }

                StringBuilder seedString = new StringBuilder();
                for (int j = 0; j < numberOfSeeds; j++) {
                    seedString.append("\"").append(seeds[j]).append("\" ");
                    querySeeds[i * numberOfSeeds + j] = seeds[j];
                }

                querySet[i] = StringHelper.trim(seedString.toString());
            }

        }

        eq.setQuerySet(querySet);
        eq.setSeeds(querySeeds);

        switch (numberOfSeeds) {
            case 2:
                eq.setQueryType(TYPE_SEED_2);
                break;
            case 3:
                eq.setQueryType(TYPE_SEED_3);
                break;
            case 4:
                eq.setQueryType(TYPE_SEED_4);
                break;
            case 5:
                eq.setQueryType(TYPE_SEED_5);
                break;
        }

        return eq;
    }

}