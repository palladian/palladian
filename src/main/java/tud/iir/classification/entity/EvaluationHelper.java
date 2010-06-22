package tud.iir.classification.entity;

import java.util.ArrayList;
import java.util.HashSet;

import tud.iir.extraction.entity.EntityExtractor;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Fact;
import tud.iir.knowledge.FactValue;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.knowledge.Source;
import tud.iir.persistence.DatabaseManager;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;

/**
 * The EvaluationHelper supports functions to create an evaluation set for entity assessment.
 * 
 * @author David
 * 
 */
public class EvaluationHelper {

    /**
     * Extract entities for given concept in the ontology. Also extract Search engine hit counts to estimate popularity.
     */
    public void extract() {

        // set up source retriever
        SourceRetriever sr = new SourceRetriever();
        sr.setSource(SourceRetrieverManager.GOOGLE);

        SourceRetrieverManager.getInstance().setResultCount(10);
        SourceRetrieverManager.getInstance().setSource(SourceRetrieverManager.GOOGLE);

        // run extractions and fill the knowledge manager without saving to database
        // EntityExtractor.getInstance().setExtractionLimit(1000);
        EntityExtractor.getInstance().startExtraction(true, true, true);

        // find hit counts and save as facts
        Source hitCountSource = new Source("http://www.google.com");
        for (Concept concept : EntityExtractor.getInstance().getKnowledgeManager().getConcepts()) {
            Attribute popularityAttribute = concept.getAttribute("popularity");
            Attribute popularity2Attribute = concept.getAttribute("popularity2");

            for (Entity entity : concept.getEntities()) {

                int popularity = sr.getHitCount("\"" + entity.getName() + "\"");
                int popularity2 = sr.getHitCount("\"" + entity.getName() + "\"" + " \"" + concept.getName() + "\"");

                Fact f = new Fact(popularityAttribute);
                Fact f2 = new Fact(popularity2Attribute);

                entity.addFactAndValue(f, new FactValue(String.valueOf(popularity), hitCountSource, -1));
                entity.addFactAndValue(f2, new FactValue(String.valueOf(popularity2), hitCountSource, -1));

            }
        }

        // save knowledge manager again
        EntityExtractor.getInstance().getKnowledgeManager().saveExtractions();
    }

    /**
     * retrieve PMI scores for evaluation entities popularity: hit count of entity alone popularity2: hit count of entity + concept of entity isX: hit count of
     * query "ENTITY is a CONCEPT" XsuchAs: hit count of query "CONCEPTs such as ENTITY" XLike: hit count of query "CONCEPTs like ENTITY" Xincluding: hit count
     * of query "CONCEPTs including ENTITY" AndOtherX: hit count of query "ENTITY and other CONCEPTs"
     */
    public void retreiveHitCounts() {

        // set up source retriever
        SourceRetriever sr = new SourceRetriever();
        sr.setSource(SourceRetrieverManager.GOOGLE);

        SourceRetrieverManager.getInstance().setResultCount(10);
        SourceRetrieverManager.getInstance().setSource(SourceRetrieverManager.GOOGLE);

        Source hitCountSource = new Source("http://www.google.com");

        KnowledgeManager km = new KnowledgeManager();

        HashSet<String> conceptNames = new HashSet<String>();
        conceptNames.add("University");
        conceptNames.add("Actor");
        conceptNames.add("Airplane");
        conceptNames.add("Animal");
        conceptNames.add("Band");
        conceptNames.add("Fish");
        conceptNames.add("Board Game");
        conceptNames.add("Mineral");
        conceptNames.add("Plant");
        conceptNames.add("Song");
        conceptNames.add("City");
        conceptNames.add("Video Game");
        conceptNames.add("Movie");
        conceptNames.add("Guitar");
        conceptNames.add("Insect");
        conceptNames.add("Perfume");
        conceptNames.add("Mobile Phone");
        conceptNames.add("Island");
        conceptNames.add("TV Show");
        conceptNames.add("Mountain");
        conceptNames.add("Car");
        conceptNames.add("Company");
        conceptNames.add("Sport");
        conceptNames.add("Printer");
        conceptNames.add("Drug");

        for (String conceptName : conceptNames) {

            Concept concept = new Concept(conceptName);
            /*
             * Attribute popularityAttribute = new Attribute("popularity", Attribute.VALUE_NUMERIC, concept); concept.addAttribute(popularityAttribute);
             * Attribute popularity2Attribute = new Attribute("popularity2", Attribute.VALUE_NUMERIC, concept); concept.addAttribute(popularity2Attribute);
             */

            Attribute attributeIsX = new Attribute("is_x", Attribute.VALUE_NUMERIC, concept);
            concept.addAttribute(attributeIsX);
            Attribute attributeXSuchAs = new Attribute("x_such_as", Attribute.VALUE_NUMERIC, concept);
            concept.addAttribute(attributeXSuchAs);
            Attribute attributeXLike = new Attribute("x_like", Attribute.VALUE_NUMERIC, concept);
            concept.addAttribute(attributeXLike);
            Attribute attributeXIncluding = new Attribute("x_including", Attribute.VALUE_NUMERIC, concept);
            concept.addAttribute(attributeXIncluding);
            Attribute attributeAndOtherX = new Attribute("and_other_x", Attribute.VALUE_NUMERIC, concept);
            concept.addAttribute(attributeAndOtherX);

            km.addConcept(concept);

            // find hit counts and save as facts
            ArrayList<Entity> entities = DatabaseManager.getInstance().loadEvaluationEntities(concept);

            for (Entity entity : entities) {

                concept.addEntity(entity);

                // int popularity = sr.getHitCount("\"" + entity.getName() + "\"");
                // int popularity2 = sr.getHitCount("\"" + entity.getName() + "\"" + " \"" + concept.getName() + "\"");

                int hitCountIsX = sr.getHitCount("\"" + entity.getName() + " is a " + concept.getName() + "\"");
                int hitCountXSuchAs = sr.getHitCount("\"" + StringHelper.wordToPlural(concept.getName()) + " such as " + entity.getName() + "\"");
                int hitCountXLike = sr.getHitCount("\"" + StringHelper.wordToPlural(concept.getName()) + " like " + entity.getName() + "\"");
                int hitCountXIncluding = sr.getHitCount("\"" + StringHelper.wordToPlural(concept.getName()) + " including " + entity.getName() + "\"");
                int hitCountAndOtherX = sr.getHitCount("\"" + entity.getName() + " and other " + StringHelper.wordToPlural(concept.getName()) + "\"");

                System.out.println("is X       :" + hitCountIsX);
                System.out.println("X such as  :" + hitCountXSuchAs);
                System.out.println("X like     :" + hitCountXLike);
                System.out.println("X including:" + hitCountXIncluding);
                System.out.println("and other X:" + hitCountAndOtherX);

                // Fact f = new Fact(popularityAttribute);
                // Fact f2 = new Fact(popularity2Attribute);
                Fact f = new Fact(attributeIsX);
                Fact f2 = new Fact(attributeXSuchAs);
                Fact f3 = new Fact(attributeXLike);
                Fact f4 = new Fact(attributeXIncluding);
                Fact f5 = new Fact(attributeAndOtherX);

                // entity.addFactAndValue(f, new FactValue(String.valueOf(popularity),hitCountSource,-1));
                // entity.addFactAndValue(f2, new FactValue(String.valueOf(popularity2),hitCountSource,-1));
                entity.addFactAndValue(f, new FactValue(String.valueOf(hitCountIsX), hitCountSource, -1));
                entity.addFactAndValue(f2, new FactValue(String.valueOf(hitCountXSuchAs), hitCountSource, -1));
                entity.addFactAndValue(f3, new FactValue(String.valueOf(hitCountXLike), hitCountSource, -1));
                entity.addFactAndValue(f4, new FactValue(String.valueOf(hitCountXIncluding), hitCountSource, -1));
                entity.addFactAndValue(f5, new FactValue(String.valueOf(hitCountAndOtherX), hitCountSource, -1));
            }
        }

        System.out.println("finished");
        DatabaseManager.getInstance().saveExtractions(km);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        /*
         * SourceRetriever sr = new SourceRetriever(); sr.setSource(SourceRetrieverManager.GOOGLE); SourceRetrieverManager.getInstance().setResultCount(10);
         * SourceRetrieverManager.getInstance().setSource(SourceRetrieverManager.GOOGLE); int popularity = sr.getHitCount("\"Iaid?\""); int popularity2 =
         * sr.getHitCount("\"Iaid?\"" + " \"Sport\""); System.out.println(popularity+","+popularity2); Source hitCountSource = new
         * Source("http://www.google.com"); DatabaseManager dbm = DatabaseManager.getInstance(); String conceptName = "Mobile Phone"; HashMap<Integer,String>
         * entities = new HashMap<Integer,String>(); // entities.put(1600797, "Lietuvi?"); // entities.put(1600901, "??????????"); // entities.put(1600903,
         * "?????????"); // entities.put(1600910, "?????"); // entities.put(1600916, "?????? / Srpski"); // entities.put(1600917,
         * "Srpskohrvatski / ??????????????"); // // entities.put(2004158, "? gunshot tutorial"); // entities.put(2004159, "? ipw tribute");
         * entities.put(2517487, "???GPRS Traffic Monitor for PPC v1.2"); for (Map.Entry<Integer, String> entry : entities.entrySet()) { int hitCountIsX =
         * sr.getHitCount("\"" + entry.getValue() + " is a " + conceptName + "\""); int hitCountXSuchAs = sr.getHitCount("\"" +
         * StringHelper.wordToPlural(conceptName) + " such as " + entry.getValue() + "\""); int hitCountXLike = sr.getHitCount("\"" +
         * StringHelper.wordToPlural(conceptName) + " like " + entry.getValue()+ "\""); int hitCountXIncluding = sr.getHitCount("\"" +
         * StringHelper.wordToPlural(conceptName) + " including " + entry.getValue() + "\""); int hitCountAndOtherX = sr.getHitCount("\"" + entry.getValue() +
         * " and other " + StringHelper.wordToPlural(conceptName) + "\""); System.out.println("is X       :"+hitCountIsX);
         * System.out.println("X such as  :"+hitCountXSuchAs); System.out.println("X like     :"+hitCountXLike);
         * System.out.println("X including:"+hitCountXIncluding); System.out.println("and other X:"+hitCountAndOtherX); dbm.addFact(new
         * FactValue(String.valueOf(hitCountIsX),hitCountSource,-1), entry.getKey(), 805); dbm.addFact(new
         * FactValue(String.valueOf(hitCountXSuchAs),hitCountSource,-1), entry.getKey(), 808); dbm.addFact(new
         * FactValue(String.valueOf(hitCountXLike),hitCountSource,-1), entry.getKey(), 809); dbm.addFact(new
         * FactValue(String.valueOf(hitCountXIncluding),hitCountSource,-1), entry.getKey(), 806); dbm.addFact(new
         * FactValue(String.valueOf(hitCountAndOtherX),hitCountSource,-1), entry.getKey(), 807); } System.exit(0);
         */

        EvaluationHelper eh = new EvaluationHelper();
        // eh.extract();
        eh.retreiveHitCounts();
    }
}
