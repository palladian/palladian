package tud.iir.extraction.entity;

import java.util.Date;
import java.util.regex.Matcher;

import tud.iir.helper.StringHelper;
import tud.iir.helper.Tokenizer;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Source;
import tud.iir.web.Crawler;

/**
 * An inductor for phrases.
 * 
 * @author David Urbansky
 */
class PhraseWrapperInductor extends WrapperInductor implements WrapperInductorInterface {

    private EntityExtractor ee = null;

    public PhraseWrapperInductor() {
        ee = EntityExtractor.getInstance();
    }

    // TODO! use quotes as boundary, not only proper noun phrases
    // TODO! catch both in "songs like 'When You Look Me In The Eyes' and 'Hello Beautiful'"
    @Override
    public void extract(String url, EntityQuery eq, Concept currentConcept) {

        Crawler crawler = new Crawler();
        String page = crawler.download(url, true);

        // extract the information (entities are capitalized and are separated with comma or "and")
        java.util.regex.Pattern pat = java.util.regex.Pattern.compile(eq.getRegularExpression(), java.util.regex.Pattern.CASE_INSENSITIVE);

        // logger.log("try to match "+searchTermRegExp);
        Matcher m = pat.matcher(page);

        while (m.find()) {

            int index = 0;
            String currentPhrase = "";

            // do not include the search term for the pattern matching
            index = m.end();
            currentPhrase = Tokenizer.getPhraseToEndOfSentence(page.substring(index, Math.min(page.length(),
                    index + 200)));

            // two spaces raise problems for the pattern matcher so delete them
            currentPhrase = StringHelper.trim(currentPhrase);

            // logger.log("phrase: "+currentPhrase+"|"+searchTerm+"_"+searchTerm.length()+","+index);
            ee.getLogger().info("phrase: " + currentPhrase);

            // grab whole listing sequence TODO Motorola RAZR v3 is not caught
            java.util.regex.Pattern pat2 = java.util.regex.Pattern
                    .compile("(([A-Z]{1}([A-Za-z-üäö0-9]{1,})|[0-9]{1,}){1,}(\\sand|\\sor|,\\sand|,\\sor|\\s,|,)?(\\s)?)+");
            Matcher m2 = pat2.matcher(currentPhrase);

            m2.region(0, currentPhrase.length());

            if (m2.find()) {

                ee.getLogger().info(m2.group());

                // do not take sequences found too far away from the pattern (e.g. "such as ......... ABC" do not take ABC as it is most likely not related to
                // the such as anymore)
                if (m2.start() > 20) {
                    continue;
                }

                String sequence = m2.group();

                // replace separators with #.# and split the list sequence
                sequence = sequence.replaceAll(",", "#.#").replaceAll(" or ", "#.#").replaceAll(" and ", "#.#");

                String[] entitiesSplit = sequence.split("#.#");
                for (int e = 0; e < entitiesSplit.length; ++e) {
                    String entityName = entitiesSplit[e].trim();
                    if (entityName.length() == 0)
                        continue;

                    Entity newEntity = new Entity(entityName, currentConcept);
                    newEntity.setExtractedAt(new Date(System.currentTimeMillis()));
                    Source s = new Source(Crawler.getCleanURL(url));
                    s.setExtractionType(eq.getQueryType());
                    newEntity.addSource(s);
                    ee.addExtraction(newEntity);
                    // ee.getKnowledgeManager().getConcept(currentConcept.getName()).addEntity(newEntity);
                    // ee.getExtractions().addExtraction(newEntity, eq.getRetrievalExtractionType(), eq.getQueryType());
                    // Entity newEntity = new Entity(entitiesSplit[e].trim(),currentConcept);
                    // newEntity.setExtractedAt(new Date(System.currentTimeMillis()));
                    // currentConcept.addEntity(newEntity,currentURL);
                }
            }
        }
    }
}