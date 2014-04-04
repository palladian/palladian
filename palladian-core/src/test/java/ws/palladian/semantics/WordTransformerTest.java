package ws.palladian.semantics;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

/**
 * Test cases for the WordTransformer class.
 * 
 * @author David Urbansky
 */
public class WordTransformerTest {

    @Test
    public void testWordToSingularEnglish() {
        assertEquals("elephant", WordTransformer.wordToSingular("elephants", Language.ENGLISH));
        assertEquals("city", WordTransformer.wordToSingular("cities", Language.ENGLISH));
        assertEquals("enemy", WordTransformer.wordToSingular("enemies", Language.ENGLISH));
        assertEquals("tray", WordTransformer.wordToSingular("trays", Language.ENGLISH));
        assertEquals("studio", WordTransformer.wordToSingular("studios", Language.ENGLISH));
        assertEquals("box", WordTransformer.wordToSingular("boxes", Language.ENGLISH));
        assertEquals("church", WordTransformer.wordToSingular("churches", Language.ENGLISH));
        assertEquals("fish", WordTransformer.wordToSingular("fish", Language.ENGLISH));
        assertEquals("lady", WordTransformer.wordToSingular("ladies", Language.ENGLISH));
        assertEquals("thief", WordTransformer.wordToSingular("thieves", Language.ENGLISH));
        assertEquals("wife", WordTransformer.wordToSingular("wives", Language.ENGLISH));
        assertEquals("shelf", WordTransformer.wordToSingular("shelves", Language.ENGLISH));
        assertEquals("tomato", WordTransformer.wordToSingular("tomatoes", Language.ENGLISH));
        assertEquals("hero", WordTransformer.wordToSingular("heroes", Language.ENGLISH));
        assertEquals("piano", WordTransformer.wordToSingular("pianos", Language.ENGLISH));
        assertEquals("article", WordTransformer.wordToSingular("articles", Language.ENGLISH));
        assertEquals("kiss", WordTransformer.wordToSingular("kisses", Language.ENGLISH));
        assertEquals("dish", WordTransformer.wordToSingular("dishes", Language.ENGLISH));
        assertEquals("phase", WordTransformer.wordToSingular("phases", Language.ENGLISH));
        assertEquals("vertex", WordTransformer.wordToSingular("vertices", Language.ENGLISH));
        assertEquals("index", WordTransformer.wordToSingular("indices", Language.ENGLISH));
        assertEquals("matrix", WordTransformer.wordToSingular("matrices", Language.ENGLISH));
        assertEquals("movie", WordTransformer.wordToSingular("movies", Language.ENGLISH));
        assertEquals("status", WordTransformer.wordToSingular("status", Language.ENGLISH));

        // this transformation makes no sense, but it caused a StringIndexOutOfBoundsException which I fixed.
        assertEquals("yf", WordTransformer.wordToSingular("yves", Language.ENGLISH));
    }

    @Test
    public void testWordToSingularGerman() {
        assertEquals("Weihnachtsdeko", WordTransformer.wordToSingular("Weihnachtsdeko", Language.GERMAN));
        assertEquals("Eilsendungadresse", WordTransformer.wordToSingular("Eilsendungadressen", Language.GERMAN));
        assertEquals("Kette", WordTransformer.wordToSingular("Ketten", Language.GERMAN));
        assertEquals("Halskette", WordTransformer.wordToSingular("Halsketten", Language.GERMAN));
        assertEquals("Apfel", WordTransformer.wordToSingular("Äpfel", Language.GERMAN));
        assertEquals("Apfelkuchen", WordTransformer.wordToSingular("Apfelkuchen", Language.GERMAN));
    }

    @Test
    public void testWordToPluralGerman() {
        assertEquals("Ketten", WordTransformer.wordToPlural("Kette", Language.GERMAN));
        assertEquals("Apfelkuchen", WordTransformer.wordToPlural("Apfelkuchen", Language.GERMAN));
        assertEquals("Eilsendungadressen", WordTransformer.wordToPlural("Eilsendungadresse", Language.GERMAN));
    }

    @Test
    public void testWordToPluralEnglish() {
        assertEquals("qualcomm msm7627 3", WordTransformer.wordToPlural("qualcomm msm7627 3", Language.ENGLISH));
        assertEquals(WordTransformer.wordToPlural("elephant", Language.ENGLISH), "elephants");
        assertEquals(WordTransformer.wordToPlural("city", Language.ENGLISH), "cities");
        assertEquals(WordTransformer.wordToPlural("enemy", Language.ENGLISH), "enemies");
        assertEquals(WordTransformer.wordToPlural("tray", Language.ENGLISH), "trays");
        assertEquals(WordTransformer.wordToPlural("studio", Language.ENGLISH), "studios");
        assertEquals(WordTransformer.wordToPlural("box", Language.ENGLISH), "boxes");
        assertEquals(WordTransformer.wordToPlural("church", Language.ENGLISH), "churches");
        assertEquals("vertices", WordTransformer.wordToPlural("vertex", Language.ENGLISH));
        assertEquals("movies", WordTransformer.wordToPlural("movie", Language.ENGLISH));
        assertEquals("status", WordTransformer.wordToPlural("status", Language.ENGLISH));
        assertEquals("computer mice", WordTransformer.wordToPlural("computer mouse", Language.ENGLISH));
        assertEquals("computer keys", WordTransformer.wordToPlural("computer key", Language.ENGLISH));

        // http://www.esldesk.com/vocabulary/irregular-nouns
        assertEquals("addenda", WordTransformer.wordToPlural("addendum", Language.ENGLISH));
        assertEquals("algae", WordTransformer.wordToPlural("alga", Language.ENGLISH));
        assertEquals("alumnae", WordTransformer.wordToPlural("alumna", Language.ENGLISH));
        assertEquals("alumni", WordTransformer.wordToPlural("alumnus", Language.ENGLISH));
        assertEquals("analyses", WordTransformer.wordToPlural("analysis", Language.ENGLISH));
        assertEquals("appendices", WordTransformer.wordToPlural("appendix", Language.ENGLISH));
        assertEquals("axes", WordTransformer.wordToPlural("axis", Language.ENGLISH));
        assertEquals("bacilli", WordTransformer.wordToPlural("bacillus", Language.ENGLISH));
        assertEquals("bacteria", WordTransformer.wordToPlural("bacterium", Language.ENGLISH));
        assertEquals("bases", WordTransformer.wordToPlural("basis", Language.ENGLISH));
        assertEquals("beaux", WordTransformer.wordToPlural("beau", Language.ENGLISH));
        assertEquals("bison", WordTransformer.wordToPlural("bison", Language.ENGLISH));
        assertEquals("buffaloes", WordTransformer.wordToPlural("buffalo", Language.ENGLISH));
        assertEquals("bureaus", WordTransformer.wordToPlural("bureau", Language.ENGLISH));
        assertEquals("buses", WordTransformer.wordToPlural("bus", Language.ENGLISH));
        assertEquals("cacti", WordTransformer.wordToPlural("cactus", Language.ENGLISH));
        assertEquals("calves", WordTransformer.wordToPlural("calf", Language.ENGLISH));
        assertEquals("children", WordTransformer.wordToPlural("child", Language.ENGLISH));
        assertEquals("corps", WordTransformer.wordToPlural("corps", Language.ENGLISH));
        assertEquals("corpora", WordTransformer.wordToPlural("corpus", Language.ENGLISH));
        assertEquals("crises", WordTransformer.wordToPlural("crisis", Language.ENGLISH));
        assertEquals("criteria", WordTransformer.wordToPlural("criterion", Language.ENGLISH));
        assertEquals("curricula", WordTransformer.wordToPlural("curriculum", Language.ENGLISH));
        assertEquals("data", WordTransformer.wordToPlural("datum", Language.ENGLISH));
        assertEquals("deer", WordTransformer.wordToPlural("deer", Language.ENGLISH));
        assertEquals("dice", WordTransformer.wordToPlural("die", Language.ENGLISH));
        assertEquals("dwarves", WordTransformer.wordToPlural("dwarf", Language.ENGLISH));
        assertEquals("diagnoses", WordTransformer.wordToPlural("diagnosis", Language.ENGLISH));
        assertEquals("echoes", WordTransformer.wordToPlural("echo", Language.ENGLISH));
        assertEquals("elves", WordTransformer.wordToPlural("elf", Language.ENGLISH));
        assertEquals("ellipses", WordTransformer.wordToPlural("ellipsis", Language.ENGLISH));
        assertEquals("embargoes", WordTransformer.wordToPlural("embargo", Language.ENGLISH));
        assertEquals("emphases", WordTransformer.wordToPlural("emphasis", Language.ENGLISH));
        assertEquals("errata", WordTransformer.wordToPlural("erratum", Language.ENGLISH));
        assertEquals("firemen", WordTransformer.wordToPlural("fireman", Language.ENGLISH));
        assertEquals("fish", WordTransformer.wordToPlural("fish", Language.ENGLISH));
        assertEquals("foci", WordTransformer.wordToPlural("focus", Language.ENGLISH));
        assertEquals("feet", WordTransformer.wordToPlural("foot", Language.ENGLISH));
        assertEquals("formulae", WordTransformer.wordToPlural("formula", Language.ENGLISH));
        assertEquals("fungi", WordTransformer.wordToPlural("fungus", Language.ENGLISH));
        assertEquals("genera", WordTransformer.wordToPlural("genus", Language.ENGLISH));
        assertEquals("geese", WordTransformer.wordToPlural("goose", Language.ENGLISH));
        assertEquals("halves", WordTransformer.wordToPlural("half", Language.ENGLISH));
        assertEquals("heroes", WordTransformer.wordToPlural("hero", Language.ENGLISH));
        assertEquals("hippopotami", WordTransformer.wordToPlural("hippopotamus", Language.ENGLISH));
        assertEquals("hooves", WordTransformer.wordToPlural("hoof", Language.ENGLISH));
        assertEquals("hypotheses", WordTransformer.wordToPlural("hypothesis", Language.ENGLISH));
        assertEquals("indices", WordTransformer.wordToPlural("index", Language.ENGLISH));
        assertEquals("knives", WordTransformer.wordToPlural("knife", Language.ENGLISH));
        assertEquals("leaves", WordTransformer.wordToPlural("leaf", Language.ENGLISH));
        assertEquals("lives", WordTransformer.wordToPlural("life", Language.ENGLISH));
        assertEquals("loaves", WordTransformer.wordToPlural("loaf", Language.ENGLISH));
        assertEquals("lice", WordTransformer.wordToPlural("louse", Language.ENGLISH));
        assertEquals("men", WordTransformer.wordToPlural("man", Language.ENGLISH));
        assertEquals("matrices", WordTransformer.wordToPlural("matrix", Language.ENGLISH));
        assertEquals("means", WordTransformer.wordToPlural("means", Language.ENGLISH));
        assertEquals("media", WordTransformer.wordToPlural("medium", Language.ENGLISH));
        assertEquals("memoranda", WordTransformer.wordToPlural("memorandum", Language.ENGLISH));
        assertEquals("milennia", WordTransformer.wordToPlural("millennium", Language.ENGLISH));
        assertEquals("moose", WordTransformer.wordToPlural("moose", Language.ENGLISH));
        assertEquals("mosquitoes", WordTransformer.wordToPlural("mosquito", Language.ENGLISH));
        assertEquals("mice", WordTransformer.wordToPlural("mouse", Language.ENGLISH));
        assertEquals("nebulae", WordTransformer.wordToPlural("nebula", Language.ENGLISH));
        assertEquals("neuroses", WordTransformer.wordToPlural("neurosis", Language.ENGLISH));
        assertEquals("nuclei", WordTransformer.wordToPlural("nucleus", Language.ENGLISH));
        assertEquals("oases", WordTransformer.wordToPlural("oasis", Language.ENGLISH));
        assertEquals("octopi", WordTransformer.wordToPlural("octopus", Language.ENGLISH));
        assertEquals("ova", WordTransformer.wordToPlural("ovum", Language.ENGLISH));
        assertEquals("oxen", WordTransformer.wordToPlural("ox", Language.ENGLISH));
        assertEquals("paralyses", WordTransformer.wordToPlural("paralysis", Language.ENGLISH));
        assertEquals("parentheses", WordTransformer.wordToPlural("parenthesis", Language.ENGLISH));
        assertEquals("people", WordTransformer.wordToPlural("person", Language.ENGLISH));
        assertEquals("phenomena", WordTransformer.wordToPlural("phenomenon", Language.ENGLISH));
        assertEquals("potatoes", WordTransformer.wordToPlural("potato", Language.ENGLISH));
        assertEquals("radiuses", WordTransformer.wordToPlural("radius", Language.ENGLISH));
        assertEquals("scarves", WordTransformer.wordToPlural("scarf", Language.ENGLISH));
        assertEquals("series", WordTransformer.wordToPlural("series", Language.ENGLISH));
        assertEquals("sheep", WordTransformer.wordToPlural("sheep", Language.ENGLISH));
        assertEquals("shelves", WordTransformer.wordToPlural("shelf", Language.ENGLISH));
        assertEquals("scissors", WordTransformer.wordToPlural("scissors", Language.ENGLISH));
        assertEquals("species", WordTransformer.wordToPlural("species", Language.ENGLISH));
        assertEquals("stimuli", WordTransformer.wordToPlural("stimulus", Language.ENGLISH));
        assertEquals("strata", WordTransformer.wordToPlural("stratum", Language.ENGLISH));
        assertEquals("syllabuses", WordTransformer.wordToPlural("syllabus", Language.ENGLISH));
        assertEquals("symposia", WordTransformer.wordToPlural("symposium", Language.ENGLISH));
        assertEquals("syntheses", WordTransformer.wordToPlural("synthesis", Language.ENGLISH));
        assertEquals("synopses", WordTransformer.wordToPlural("synopsis", Language.ENGLISH));
        assertEquals("tableaux", WordTransformer.wordToPlural("tableau", Language.ENGLISH));
        assertEquals("theses", WordTransformer.wordToPlural("thesis", Language.ENGLISH));
        assertEquals("thieves", WordTransformer.wordToPlural("thief", Language.ENGLISH));
        assertEquals("tomatoes", WordTransformer.wordToPlural("tomato", Language.ENGLISH));
        assertEquals("teeth", WordTransformer.wordToPlural("tooth", Language.ENGLISH));
        assertEquals("torpedoes", WordTransformer.wordToPlural("torpedo", Language.ENGLISH));
        assertEquals("vertebrae", WordTransformer.wordToPlural("vertebra", Language.ENGLISH));
        assertEquals("vetoes", WordTransformer.wordToPlural("veto", Language.ENGLISH));
        assertEquals("vitae", WordTransformer.wordToPlural("vita", Language.ENGLISH));
        assertEquals("watches", WordTransformer.wordToPlural("watch", Language.ENGLISH));
        assertEquals("wives", WordTransformer.wordToPlural("wife", Language.ENGLISH));
        assertEquals("wolves", WordTransformer.wordToPlural("wolf", Language.ENGLISH));
        assertEquals("women", WordTransformer.wordToPlural("woman", Language.ENGLISH));
        assertEquals("antennae", WordTransformer.wordToPlural("antenna", Language.ENGLISH));
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

        List<Annotation> tas = CollectionHelper.newArrayList();
        tas.add(new ImmutableAnnotation(0, "DUMMY", "VB"));
        //        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("Do you like bugs?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("Do you like bugs?",tas));

        tas.clear();
        tas.add(new ImmutableAnnotation(0, "DUMMY", "BEZ"));
        tas.add(new ImmutableAnnotation(0, "DUMMY", "VBN"));
        tas.add(new ImmutableAnnotation(0, "DUMMY", "BE"));
        //        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("He is said to be nice?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("He is said to be nice?",tas));

        tas.clear();
        tas.add(new ImmutableAnnotation(0, "DUMMY", "VBN"));
        //        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("The books are written?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("The books are written?",tas));

        tas.clear();
        tas.add(new ImmutableAnnotation(0, "DUMMY", "VBD"));
        //        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("They wrote the books?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("They wrote the books?",tas));

        tas.clear();
        tas.add(new ImmutableAnnotation(0, "DUMMY", "VB"));
        tas.add(new ImmutableAnnotation(0, "DUMMY", "DOD"));
        //        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("I did not go there.",posTagger));
        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("I did not go there.",tas));

        tas.clear();
        tas.add(new ImmutableAnnotation(0, "DUMMY", "BEDZ"));
        //        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("Where was Woodstock?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("Where was Woodstock?",tas));

        tas.clear();
        tas.add(new ImmutableAnnotation(0, "DUMMY", "BEZ"));
        //        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("When is Easter this year?",posTagger));
        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("When is Easter this year?",tas));

        tas.clear();
        tas.add(new ImmutableAnnotation(0, "DUMMY", "VB"));
        //         assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("I jump over a fence.",posTagger));
        assertEquals(EnglishTense.SIMPLE_PRESENT, WordTransformer.getTense("I jump over a fence.",tas));

        tas.clear();
        tas.add(new ImmutableAnnotation(0, "DUMMY", "VBD"));
        //         assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("I jumped over a fence.",posTagger));
        assertEquals(EnglishTense.SIMPLE_PAST, WordTransformer.getTense("I jumped over a fence.",tas));

        tas.clear();
        tas.add(new ImmutableAnnotation(0, "DUMMY", "HV"));
        tas.add(new ImmutableAnnotation(0, "DUMMY", "HVN"));
        //         assertEquals(EnglishTense.PRESENT_PERFECT, WordTransformer.getTense("Have you ever had pancakes?",posTagger));
        assertEquals(EnglishTense.PRESENT_PERFECT, WordTransformer.getTense("Have you ever had pancakes?",tas));

        tas.clear();
        tas.add(new ImmutableAnnotation(0, "DUMMY", "HVD"));
        tas.add(new ImmutableAnnotation(0, "DUMMY", "VBN"));
        //         assertEquals(EnglishTense.PAST_PERFECT,WordTransformer.getTense("No, I never had eaten pancakes before today?",posTagger));
        assertEquals(EnglishTense.PAST_PERFECT,WordTransformer.getTense("No, I never had eaten pancakes before today?",tas));
    }
}
