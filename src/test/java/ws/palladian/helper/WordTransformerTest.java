package ws.palladian.helper;

import junit.framework.TestCase;

import org.junit.Test;

import ws.palladian.helper.WordTransformer;

/**
 * Test cases for the WordTransformer class.
 * 
 * @author David Urbansky
 */
public class WordTransformerTest extends TestCase {

    public WordTransformerTest(String name) {
        super(name);
    }

    @Test
    public void testWordToSingular() {
        assertEquals("elephant", WordTransformer.wordToSingular("elephants"));
        assertEquals("city", WordTransformer.wordToSingular("cities"));
        assertEquals("enemy", WordTransformer.wordToSingular("enemies"));
        assertEquals("tray", WordTransformer.wordToSingular("trays"));
        assertEquals("studio", WordTransformer.wordToSingular("studios"));
        assertEquals("box", WordTransformer.wordToSingular("boxes"));
        assertEquals("church", WordTransformer.wordToSingular("churches"));
        assertEquals("fish", WordTransformer.wordToSingular("fish"));
        assertEquals("lady", WordTransformer.wordToSingular("ladies"));
        assertEquals("thief", WordTransformer.wordToSingular("thieves"));
        assertEquals("wife", WordTransformer.wordToSingular("wives"));
        assertEquals("shelf", WordTransformer.wordToSingular("shelves"));
        assertEquals("tomato", WordTransformer.wordToSingular("tomatoes"));
        assertEquals("hero", WordTransformer.wordToSingular("heroes"));
        assertEquals("piano", WordTransformer.wordToSingular("pianos"));
        assertEquals("article", WordTransformer.wordToSingular("articles"));
        assertEquals("kiss", WordTransformer.wordToSingular("kisses"));
        assertEquals("dish", WordTransformer.wordToSingular("dishes"));
        assertEquals("phase", WordTransformer.wordToSingular("phases"));
        assertEquals("vertex", WordTransformer.wordToSingular("vertices"));
        assertEquals("index", WordTransformer.wordToSingular("indices"));
        assertEquals("matrix", WordTransformer.wordToSingular("matrices"));
        assertEquals("movie", WordTransformer.wordToSingular("movies"));
        assertEquals("status", WordTransformer.wordToSingular("status"));

        // this transformation makes no sense, but it caused a StringIndexOutOfBoundsException which I fixed.
        assertEquals("yf", WordTransformer.wordToSingular("yves"));
    }

    @Test
    public void testWordToPlural() {
        assertEquals(WordTransformer.wordToPlural("elephant"), "elephants");
        assertEquals(WordTransformer.wordToPlural("city"), "cities");
        assertEquals(WordTransformer.wordToPlural("enemy"), "enemies");
        assertEquals(WordTransformer.wordToPlural("tray"), "trays");
        assertEquals(WordTransformer.wordToPlural("studio"), "studios");
        assertEquals(WordTransformer.wordToPlural("box"), "boxes");
        assertEquals(WordTransformer.wordToPlural("church"), "churches");
        assertEquals("vertices", WordTransformer.wordToPlural("vertex"));
        assertEquals("movies", WordTransformer.wordToPlural("movie"));
        assertEquals("status", WordTransformer.wordToPlural("status"));

        // http://www.esldesk.com/vocabulary/irregular-nouns
        assertEquals("addenda", WordTransformer.wordToPlural("addendum"));
        assertEquals("algae", WordTransformer.wordToPlural("alga"));
        assertEquals("alumnae", WordTransformer.wordToPlural("alumna"));
        assertEquals("alumni", WordTransformer.wordToPlural("alumnus"));
        assertEquals("analyses", WordTransformer.wordToPlural("analysis"));
        assertEquals("antenna", WordTransformer.wordToPlural("antennas"));
        assertEquals("apparatuses", WordTransformer.wordToPlural("apparatus"));
        assertEquals("appendices", WordTransformer.wordToPlural("appendix"));
        assertEquals("axes", WordTransformer.wordToPlural("axis"));
        assertEquals("bacilli", WordTransformer.wordToPlural("bacillus"));
        assertEquals("bacteria", WordTransformer.wordToPlural("bacterium"));
        assertEquals("bases", WordTransformer.wordToPlural("basis"));
        assertEquals("beaux", WordTransformer.wordToPlural("beau"));
        assertEquals("bison", WordTransformer.wordToPlural("bison"));
        assertEquals("buffalos", WordTransformer.wordToPlural("buffalo"));
        assertEquals("bureaus", WordTransformer.wordToPlural("bureau"));
        assertEquals("buses", WordTransformer.wordToPlural("bus"));
        assertEquals("cactuses", WordTransformer.wordToPlural("cactus"));
        assertEquals("calves", WordTransformer.wordToPlural("calf"));
        assertEquals("children", WordTransformer.wordToPlural("child"));
        assertEquals("corps", WordTransformer.wordToPlural("corps"));
        assertEquals("corpuses", WordTransformer.wordToPlural("corpus"));
        assertEquals("crises", WordTransformer.wordToPlural("crisis"));
        assertEquals("criteria", WordTransformer.wordToPlural("criterion"));
        assertEquals("curricula", WordTransformer.wordToPlural("curriculum"));
        assertEquals("data", WordTransformer.wordToPlural("datum"));
        assertEquals("deer", WordTransformer.wordToPlural("deer"));
        assertEquals("dice", WordTransformer.wordToPlural("die"));
        assertEquals("dwarfs", WordTransformer.wordToPlural("dwarf"));
        assertEquals("diagnoses", WordTransformer.wordToPlural("diagnosis"));
        assertEquals("echoes", WordTransformer.wordToPlural("echo"));
        assertEquals("elves", WordTransformer.wordToPlural("elf"));
        assertEquals("ellipses", WordTransformer.wordToPlural("ellipsis"));
        assertEquals("embargoes", WordTransformer.wordToPlural("embargo"));
        assertEquals("emphases", WordTransformer.wordToPlural("emphasis"));
        assertEquals("errata", WordTransformer.wordToPlural("erratum"));
        assertEquals("firemen", WordTransformer.wordToPlural("fireman"));
        assertEquals("fish", WordTransformer.wordToPlural("fish"));
        assertEquals("focuses", WordTransformer.wordToPlural("focus"));
        assertEquals("feet", WordTransformer.wordToPlural("foot"));
        assertEquals("formulas", WordTransformer.wordToPlural("formula"));
        assertEquals("fungi", WordTransformer.wordToPlural("fungus"));
        assertEquals("genera", WordTransformer.wordToPlural("genus"));
        assertEquals("geese", WordTransformer.wordToPlural("goose"));
        assertEquals("halves", WordTransformer.wordToPlural("half"));
        assertEquals("heroes", WordTransformer.wordToPlural("hero"));
        assertEquals("hippopotami", WordTransformer.wordToPlural("hippopotamus"));
        assertEquals("hoofs", WordTransformer.wordToPlural("hoof"));
        assertEquals("hypotheses", WordTransformer.wordToPlural("hypothesis"));
        assertEquals("indices", WordTransformer.wordToPlural("index"));
        assertEquals("knives", WordTransformer.wordToPlural("knife"));
        assertEquals("leaves", WordTransformer.wordToPlural("leaf"));
        assertEquals("lives", WordTransformer.wordToPlural("life"));
        assertEquals("loaves", WordTransformer.wordToPlural("loaf"));
        assertEquals("lice", WordTransformer.wordToPlural("louse"));
        assertEquals("men", WordTransformer.wordToPlural("man"));
        assertEquals("matrices", WordTransformer.wordToPlural("matrix"));
        assertEquals("means", WordTransformer.wordToPlural("means"));
        assertEquals("media", WordTransformer.wordToPlural("medium"));
        assertEquals("memoranda", WordTransformer.wordToPlural("memorandum"));
        assertEquals("milennia", WordTransformer.wordToPlural("millennium"));
        assertEquals("moose", WordTransformer.wordToPlural("moose"));
        assertEquals("mosquitoes", WordTransformer.wordToPlural("mosquito"));
        assertEquals("mice", WordTransformer.wordToPlural("mouse"));
        assertEquals("nebulas", WordTransformer.wordToPlural("nebula"));
        assertEquals("neuroses", WordTransformer.wordToPlural("neurosis"));
        assertEquals("nuclei", WordTransformer.wordToPlural("nucleus"));
        assertEquals("oases", WordTransformer.wordToPlural("oasis"));
        assertEquals("octopuses", WordTransformer.wordToPlural("octopus"));
        assertEquals("ova", WordTransformer.wordToPlural("ovum"));
        assertEquals("oxen", WordTransformer.wordToPlural("ox"));
        assertEquals("paralyses", WordTransformer.wordToPlural("paralysis"));
        assertEquals("parentheses", WordTransformer.wordToPlural("parenthesis"));
        assertEquals("people", WordTransformer.wordToPlural("person"));
        assertEquals("phenomena", WordTransformer.wordToPlural("phenomenon"));
        assertEquals("potatoes", WordTransformer.wordToPlural("potato"));
        assertEquals("radiuses", WordTransformer.wordToPlural("radius"));
        assertEquals("scarfs", WordTransformer.wordToPlural("scarf"));
        assertEquals("series", WordTransformer.wordToPlural("series"));
        assertEquals("sheep", WordTransformer.wordToPlural("sheep"));
        assertEquals("shelves", WordTransformer.wordToPlural("shelf"));
        assertEquals("scissors", WordTransformer.wordToPlural("scissors"));
        assertEquals("species", WordTransformer.wordToPlural("species"));
        assertEquals("stimuli", WordTransformer.wordToPlural("stimulus"));
        assertEquals("strata", WordTransformer.wordToPlural("stratum"));
        assertEquals("syllabuses", WordTransformer.wordToPlural("syllabus"));
        assertEquals("symposia", WordTransformer.wordToPlural("symposium"));
        assertEquals("syntheses", WordTransformer.wordToPlural("synthesis"));
        assertEquals("synopses", WordTransformer.wordToPlural("synopsis"));
        assertEquals("tableaux", WordTransformer.wordToPlural("tableau"));
        assertEquals("theses", WordTransformer.wordToPlural("thesis"));
        assertEquals("thieves", WordTransformer.wordToPlural("thief"));
        assertEquals("tomatoes", WordTransformer.wordToPlural("tomato"));
        assertEquals("teeth", WordTransformer.wordToPlural("tooth"));
        assertEquals("torpedoes", WordTransformer.wordToPlural("torpedo"));
        assertEquals("vertebrae", WordTransformer.wordToPlural("vertebra"));
        assertEquals("vetoes", WordTransformer.wordToPlural("veto"));
        assertEquals("vitae", WordTransformer.wordToPlural("vita"));
        assertEquals("watches", WordTransformer.wordToPlural("watch"));
        assertEquals("wives", WordTransformer.wordToPlural("wife"));
        assertEquals("wolves", WordTransformer.wordToPlural("wolf"));
        assertEquals("women", WordTransformer.wordToPlural("woman"));
    }
}
