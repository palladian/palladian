package ws.palladian.helper.nlp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.preprocessing.nlp.TagAnnotation;
import ws.palladian.preprocessing.nlp.TagAnnotations;

/**
 * Test cases for the WordTransformer class.
 * 
 * @author David Urbansky
 */
public class WordTransformerTest {

    @Test
    public void testWordToSingular() {
        assertEquals("elephant", WordTransformer.wordToSingular("elephants", "en"));
        assertEquals("city", WordTransformer.wordToSingular("cities", "en"));
        assertEquals("enemy", WordTransformer.wordToSingular("enemies", "en"));
        assertEquals("tray", WordTransformer.wordToSingular("trays", "en"));
        assertEquals("studio", WordTransformer.wordToSingular("studios", "en"));
        assertEquals("box", WordTransformer.wordToSingular("boxes", "en"));
        assertEquals("church", WordTransformer.wordToSingular("churches", "en"));
        assertEquals("fish", WordTransformer.wordToSingular("fish", "en"));
        assertEquals("lady", WordTransformer.wordToSingular("ladies", "en"));
        assertEquals("thief", WordTransformer.wordToSingular("thieves", "en"));
        assertEquals("wife", WordTransformer.wordToSingular("wives", "en"));
        assertEquals("shelf", WordTransformer.wordToSingular("shelves", "en"));
        assertEquals("tomato", WordTransformer.wordToSingular("tomatoes", "en"));
        assertEquals("hero", WordTransformer.wordToSingular("heroes", "en"));
        assertEquals("piano", WordTransformer.wordToSingular("pianos", "en"));
        assertEquals("article", WordTransformer.wordToSingular("articles", "en"));
        assertEquals("kiss", WordTransformer.wordToSingular("kisses", "en"));
        assertEquals("dish", WordTransformer.wordToSingular("dishes", "en"));
        assertEquals("phase", WordTransformer.wordToSingular("phases", "en"));
        assertEquals("vertex", WordTransformer.wordToSingular("vertices", "en"));
        assertEquals("index", WordTransformer.wordToSingular("indices", "en"));
        assertEquals("matrix", WordTransformer.wordToSingular("matrices", "en"));
        assertEquals("movie", WordTransformer.wordToSingular("movies", "en"));
        assertEquals("status", WordTransformer.wordToSingular("status", "en"));

        // this transformation makes no sense, but it caused a StringIndexOutOfBoundsException which I fixed.
        assertEquals("yf", WordTransformer.wordToSingular("yves", "en"));
    }

    @Test
    public void testWordToPlural() {
        assertEquals(WordTransformer.wordToPlural("elephant", "en"), "elephants");
        assertEquals(WordTransformer.wordToPlural("city", "en"), "cities");
        assertEquals(WordTransformer.wordToPlural("enemy", "en"), "enemies");
        assertEquals(WordTransformer.wordToPlural("tray", "en"), "trays");
        assertEquals(WordTransformer.wordToPlural("studio", "en"), "studios");
        assertEquals(WordTransformer.wordToPlural("box", "en"), "boxes");
        assertEquals(WordTransformer.wordToPlural("church", "en"), "churches");
        assertEquals("vertices", WordTransformer.wordToPlural("vertex", "en"));
        assertEquals("movies", WordTransformer.wordToPlural("movie", "en"));
        assertEquals("status", WordTransformer.wordToPlural("status", "en"));
        assertEquals("computer mice", WordTransformer.wordToPlural("computer mouse", "en"));
        assertEquals("computer keys", WordTransformer.wordToPlural("computer key", "en"));

        // http://www.esldesk.com/vocabulary/irregular-nouns
        assertEquals("addenda", WordTransformer.wordToPlural("addendum", "en"));
        assertEquals("algae", WordTransformer.wordToPlural("alga", "en"));
        assertEquals("alumnae", WordTransformer.wordToPlural("alumna", "en"));
        assertEquals("alumni", WordTransformer.wordToPlural("alumnus", "en"));
        assertEquals("analyses", WordTransformer.wordToPlural("analysis", "en"));
        assertEquals("antenna", WordTransformer.wordToPlural("antennas", "en"));
        assertEquals("apparatuses", WordTransformer.wordToPlural("apparatus", "en"));
        assertEquals("appendices", WordTransformer.wordToPlural("appendix", "en"));
        assertEquals("axes", WordTransformer.wordToPlural("axis", "en"));
        assertEquals("bacilli", WordTransformer.wordToPlural("bacillus", "en"));
        assertEquals("bacteria", WordTransformer.wordToPlural("bacterium", "en"));
        assertEquals("bases", WordTransformer.wordToPlural("basis", "en"));
        assertEquals("beaux", WordTransformer.wordToPlural("beau", "en"));
        assertEquals("bison", WordTransformer.wordToPlural("bison", "en"));
        assertEquals("buffalos", WordTransformer.wordToPlural("buffalo", "en"));
        assertEquals("bureaus", WordTransformer.wordToPlural("bureau", "en"));
        assertEquals("buses", WordTransformer.wordToPlural("bus", "en"));
        assertEquals("cactuses", WordTransformer.wordToPlural("cactus", "en"));
        assertEquals("calves", WordTransformer.wordToPlural("calf", "en"));
        assertEquals("children", WordTransformer.wordToPlural("child", "en"));
        assertEquals("corps", WordTransformer.wordToPlural("corps", "en"));
        assertEquals("corpuses", WordTransformer.wordToPlural("corpus", "en"));
        assertEquals("crises", WordTransformer.wordToPlural("crisis", "en"));
        assertEquals("criteria", WordTransformer.wordToPlural("criterion", "en"));
        assertEquals("curricula", WordTransformer.wordToPlural("curriculum", "en"));
        assertEquals("data", WordTransformer.wordToPlural("datum", "en"));
        assertEquals("deer", WordTransformer.wordToPlural("deer", "en"));
        assertEquals("dice", WordTransformer.wordToPlural("die", "en"));
        assertEquals("dwarfs", WordTransformer.wordToPlural("dwarf", "en"));
        assertEquals("diagnoses", WordTransformer.wordToPlural("diagnosis", "en"));
        assertEquals("echoes", WordTransformer.wordToPlural("echo", "en"));
        assertEquals("elves", WordTransformer.wordToPlural("elf", "en"));
        assertEquals("ellipses", WordTransformer.wordToPlural("ellipsis", "en"));
        assertEquals("embargoes", WordTransformer.wordToPlural("embargo", "en"));
        assertEquals("emphases", WordTransformer.wordToPlural("emphasis", "en"));
        assertEquals("errata", WordTransformer.wordToPlural("erratum", "en"));
        assertEquals("firemen", WordTransformer.wordToPlural("fireman", "en"));
        assertEquals("fish", WordTransformer.wordToPlural("fish", "en"));
        assertEquals("focuses", WordTransformer.wordToPlural("focus", "en"));
        assertEquals("feet", WordTransformer.wordToPlural("foot", "en"));
        assertEquals("formulas", WordTransformer.wordToPlural("formula", "en"));
        assertEquals("fungi", WordTransformer.wordToPlural("fungus", "en"));
        assertEquals("genera", WordTransformer.wordToPlural("genus", "en"));
        assertEquals("geese", WordTransformer.wordToPlural("goose", "en"));
        assertEquals("halves", WordTransformer.wordToPlural("half", "en"));
        assertEquals("heroes", WordTransformer.wordToPlural("hero", "en"));
        assertEquals("hippopotami", WordTransformer.wordToPlural("hippopotamus", "en"));
        assertEquals("hoofs", WordTransformer.wordToPlural("hoof", "en"));
        assertEquals("hypotheses", WordTransformer.wordToPlural("hypothesis", "en"));
        assertEquals("indices", WordTransformer.wordToPlural("index", "en"));
        assertEquals("knives", WordTransformer.wordToPlural("knife", "en"));
        assertEquals("leaves", WordTransformer.wordToPlural("leaf", "en"));
        assertEquals("lives", WordTransformer.wordToPlural("life", "en"));
        assertEquals("loaves", WordTransformer.wordToPlural("loaf", "en"));
        assertEquals("lice", WordTransformer.wordToPlural("louse", "en"));
        assertEquals("men", WordTransformer.wordToPlural("man", "en"));
        assertEquals("matrices", WordTransformer.wordToPlural("matrix", "en"));
        assertEquals("means", WordTransformer.wordToPlural("means", "en"));
        assertEquals("media", WordTransformer.wordToPlural("medium", "en"));
        assertEquals("memoranda", WordTransformer.wordToPlural("memorandum", "en"));
        assertEquals("milennia", WordTransformer.wordToPlural("millennium", "en"));
        assertEquals("moose", WordTransformer.wordToPlural("moose", "en"));
        assertEquals("mosquitoes", WordTransformer.wordToPlural("mosquito", "en"));
        assertEquals("mice", WordTransformer.wordToPlural("mouse", "en"));
        assertEquals("nebulas", WordTransformer.wordToPlural("nebula", "en"));
        assertEquals("neuroses", WordTransformer.wordToPlural("neurosis", "en"));
        assertEquals("nuclei", WordTransformer.wordToPlural("nucleus", "en"));
        assertEquals("oases", WordTransformer.wordToPlural("oasis", "en"));
        assertEquals("octopuses", WordTransformer.wordToPlural("octopus", "en"));
        assertEquals("ova", WordTransformer.wordToPlural("ovum", "en"));
        assertEquals("oxen", WordTransformer.wordToPlural("ox", "en"));
        assertEquals("paralyses", WordTransformer.wordToPlural("paralysis", "en"));
        assertEquals("parentheses", WordTransformer.wordToPlural("parenthesis", "en"));
        assertEquals("people", WordTransformer.wordToPlural("person", "en"));
        assertEquals("phenomena", WordTransformer.wordToPlural("phenomenon", "en"));
        assertEquals("potatoes", WordTransformer.wordToPlural("potato", "en"));
        assertEquals("radiuses", WordTransformer.wordToPlural("radius", "en"));
        assertEquals("scarfs", WordTransformer.wordToPlural("scarf", "en"));
        assertEquals("series", WordTransformer.wordToPlural("series", "en"));
        assertEquals("sheep", WordTransformer.wordToPlural("sheep", "en"));
        assertEquals("shelves", WordTransformer.wordToPlural("shelf", "en"));
        assertEquals("scissors", WordTransformer.wordToPlural("scissors", "en"));
        assertEquals("species", WordTransformer.wordToPlural("species", "en"));
        assertEquals("stimuli", WordTransformer.wordToPlural("stimulus", "en"));
        assertEquals("strata", WordTransformer.wordToPlural("stratum", "en"));
        assertEquals("syllabuses", WordTransformer.wordToPlural("syllabus", "en"));
        assertEquals("symposia", WordTransformer.wordToPlural("symposium", "en"));
        assertEquals("syntheses", WordTransformer.wordToPlural("synthesis", "en"));
        assertEquals("synopses", WordTransformer.wordToPlural("synopsis", "en"));
        assertEquals("tableaux", WordTransformer.wordToPlural("tableau", "en"));
        assertEquals("theses", WordTransformer.wordToPlural("thesis", "en"));
        assertEquals("thieves", WordTransformer.wordToPlural("thief", "en"));
        assertEquals("tomatoes", WordTransformer.wordToPlural("tomato", "en"));
        assertEquals("teeth", WordTransformer.wordToPlural("tooth", "en"));
        assertEquals("torpedoes", WordTransformer.wordToPlural("torpedo", "en"));
        assertEquals("vertebrae", WordTransformer.wordToPlural("vertebra", "en"));
        assertEquals("vetoes", WordTransformer.wordToPlural("veto", "en"));
        assertEquals("vitae", WordTransformer.wordToPlural("vita", "en"));
        assertEquals("watches", WordTransformer.wordToPlural("watch", "en"));
        assertEquals("wives", WordTransformer.wordToPlural("wife", "en"));
        assertEquals("wolves", WordTransformer.wordToPlural("wolf", "en"));
        assertEquals("women", WordTransformer.wordToPlural("woman", "en"));
    }

    @Test
    public void testGetThirdPersonSingular() {
        assertEquals("jumps", WordTransformer.getThirdPersonSingular("jump"));
        assertEquals("done", WordTransformer.getThirdPersonSingular("done"));
        assertEquals("did", WordTransformer.getThirdPersonSingular("did"));
        assertEquals("jumped", WordTransformer.getThirdPersonSingular("jumped"));
        assertEquals("misses", WordTransformer.getThirdPersonSingular("miss"));
        assertEquals("flies", WordTransformer.getThirdPersonSingular("fly"));
        assertEquals("boxes", WordTransformer.getThirdPersonSingular("box"));
        assertEquals("searches", WordTransformer.getThirdPersonSingular("search"));
        assertEquals("searched", WordTransformer.getThirdPersonSingular("searched"));
        assertEquals("wishes", WordTransformer.getThirdPersonSingular("wish"));
        assertEquals("goes", WordTransformer.getThirdPersonSingular("go"));
    }

    @Test
    public void testGetSimplePast() {
        assertEquals("jumped", WordTransformer.getSimplePast("jump"));
        assertEquals("jumped", WordTransformer.getSimplePast("jumped"));
        assertEquals("equipped", WordTransformer.getSimplePast("equip"));
        assertEquals("tried", WordTransformer.getSimplePast("tried"));
        assertEquals("found", WordTransformer.getSimplePast("find"));
        assertEquals("grated", WordTransformer.getSimplePast("grate"));
        assertEquals("went", WordTransformer.getSimplePast("go"));
    }

    @Test
    public void testGetPastParticiple() {
        assertEquals("caused", WordTransformer.getPastParticiple("causes"));
        assertEquals("jumped", WordTransformer.getPastParticiple("jump"));
        assertEquals("jumped", WordTransformer.getPastParticiple("jumped"));
        assertEquals("equipped", WordTransformer.getPastParticiple("equip"));
        assertEquals("tried", WordTransformer.getPastParticiple("tried"));
        assertEquals("found", WordTransformer.getPastParticiple("find"));
        assertEquals("grated", WordTransformer.getPastParticiple("grate"));
        assertEquals("gone", WordTransformer.getPastParticiple("go"));
    }

    @Test
    public void testGetTense() {
        
//        LingPipePosTagger posTagger = new LingPipePosTagger();
//        posTagger.loadModel("data/models/lingpipe/pos-en-general-brown.HiddenMarkovModel");
        
        TagAnnotations tas = new TagAnnotations();
        tas.add(new TagAnnotation(0, "VB", ""));
//        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("Do you like bugs?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("Do you like bugs?",tas));
        
        tas = new TagAnnotations();
        tas.add(new TagAnnotation(0, "BEZ", ""));
        tas.add(new TagAnnotation(0, "VBN", ""));
        tas.add(new TagAnnotation(0, "BE", ""));
//        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("He is said to be nice?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("He is said to be nice?",tas));
        
        tas = new TagAnnotations();
        tas.add(new TagAnnotation(0, "VBN", ""));
//        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("The books are written?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("The books are written?",tas));
        
        tas = new TagAnnotations();
        tas.add(new TagAnnotation(0, "VBD", ""));
//        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("They wrote the books?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("They wrote the books?",tas));
        
        tas = new TagAnnotations();
        tas.add(new TagAnnotation(0, "VB", ""));
        tas.add(new TagAnnotation(0, "DOD", ""));
//        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("I did not go there.",posTagger));
        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("I did not go there.",tas));
        
        tas = new TagAnnotations();
        tas.add(new TagAnnotation(0, "BEDZ", ""));
//        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("Where was Woodstock?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("Where was Woodstock?",tas));
        
        tas = new TagAnnotations();
        tas.add(new TagAnnotation(0, "BEZ", ""));
//        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("When is Easter this year?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("When is Easter this year?",tas));

        tas = new TagAnnotations();
        tas.add(new TagAnnotation(0, "VB", ""));
//         assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("I jump over a fence.",posTagger));
         assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("I jump over a fence.",tas));
         
         tas = new TagAnnotations();
         tas.add(new TagAnnotation(0, "VBD", ""));
//         assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("I jumped over a fence.",posTagger));
         assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("I jumped over a fence.",tas));
         
         tas = new TagAnnotations();
         tas.add(new TagAnnotation(0, "HV", ""));
         tas.add(new TagAnnotation(0, "HVN", ""));
//         assertEquals(EnglishTense.PRESENT_PERFECT, WordTransformer.getTense("Have you ever had pancakes?",posTagger));
         assertEquals(EnglishTense.PRESENT_PERFECT, WordTransformer.getTense("Have you ever had pancakes?",tas));
         
         tas = new TagAnnotations();
         tas.add(new TagAnnotation(0, "HVD", ""));
         tas.add(new TagAnnotation(0, "VBN", ""));
//         assertEquals(EnglishTense.PAST_PERFECT,WordTransformer.getTense("No, I never had eaten pancakes before today?",posTagger));
         assertEquals(EnglishTense.PAST_PERFECT,WordTransformer.getTense("No, I never had eaten pancakes before today?",tas));
    }
}
