package ws.palladian.extraction.sentence;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ws.palladian.core.Token;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class SentenceDetectorTest {
    private static String fixture;

    @BeforeClass
    public static void setUp() throws Exception {
        fixture = FileHelper.readFileToString(ResourceHelper.getResourceFile("/texts/contribution02.txt"));
    }

    @AfterClass
    public static void tearDown() {
        fixture = null;
    }

    @Test
    public void testPalladianSentenceChunker() {
        SentenceDetector sentenceDetector = new PalladianSentenceDetector(Language.ENGLISH);
        List<Token> sentences = CollectionHelper.newArrayList(sentenceDetector.iterateTokens(fixture));
        //        assertThat(sentences.size(), Matchers.is(269)); <- .java recognized as domain
        assertThat(sentences.get(sentences.size() - 1).getValue(), is("DBConnection disconnect\r\nINFO: disconnected"));
    }

    @Test
    public void testPalladianSentenceChunkerWithMaskAtEndOfText() {
        SentenceDetector sentenceDetector = new PalladianSentenceDetector(Language.ENGLISH);
        List<Token> sentences = CollectionHelper.newArrayList(sentenceDetector.iterateTokens(
                "Web Dynpro is just in ramp up now. You can't use Web Dynpro in production environments.\n\nYou can develop BSP and J2EE Applications with 6.20. You connect to your R/3 System through RFC. This applications can also be used in 4.7."));
        assertThat(sentences.size(), is(5));
        assertThat(sentences.get(sentences.size() - 1).getValue(), is("This applications can also be used in 4.7."));
    }

    @Test
    public void testPalladianSentenceChunkerWithLineBreakAtEndOfText() throws IOException {
        SentenceDetector sentenceDetector = new PalladianSentenceDetector(Language.ENGLISH);
        String text = FileHelper.readFileToString(ResourceHelper.getResourceFile("/texts/contribution03.txt"));
        List<Token> sentences = CollectionHelper.newArrayList(sentenceDetector.iterateTokens(text));
        assertThat(sentences.size(), is(75));
        assertThat(sentences.get(sentences.size() - 1).getValue(), is("Return code: 4"));
    }

    @Test
    public void testGetSentences() {
        // this is the LingPipe example (last sentence ends with "!" to make it more difficult:
        // http://alias-i.com/lingpipe/demos/tutorial/sentences/read-me.html
        String inputText = "";
        List<String> sentences;

        inputText = "Whatever (eye irrit. 2), you say. Sentence number 2.";
        sentences = Tokenizer.getSentences(inputText, false, Language.ENGLISH);
        CollectionHelper.print(sentences);
        assertEquals(2, sentences.size());

        inputText = "Covers approximately 150 sq. ft. per kit. Such a great place.";
        sentences = Tokenizer.getSentences(inputText, false, Language.ENGLISH);
        CollectionHelper.print(sentences);
        assertEquals(2, sentences.size());

        inputText = "Inkl. Wettervorhersage (Thermometer, Hygrometer) und Wetterindikator.";
        sentences = Tokenizer.getSentences(inputText, false, Language.GERMAN);
        // CollectionHelper.print(sentences);
        assertEquals(1, sentences.size());

        inputText = "Das Leben ist wie eine Schachtel Pralinen - man weiß nie was man kriegt. Bei uns ist jedoch der Satz am B abgeschnitten.";
        sentences = Tokenizer.getSentences(inputText, false, Language.GERMAN);
        // CollectionHelper.print(sentences);
        assertEquals("Bei uns ist jedoch der Satz am B abgeschnitten.", sentences.get(1));

        inputText = "Die originale Druckpatrone Nr. 920XL (CD975AE) von HP liefert professionelle Texte und Grafiken in Laserqualität.";
        sentences = Tokenizer.getSentences(inputText, false, Language.GERMAN);
        // CollectionHelper.print(sentences);
        assertEquals(1, sentences.size());

        inputText = "Die originale Druckpatrone Nr. 920XL (CD975AE) von HP liefert professionelle Texte und Grafiken in Laserqualität.";
        sentences = Tokenizer.getSentences(inputText, false, Language.GERMAN);
        // CollectionHelper.print(sentences);
        assertEquals(1, sentences.size());

        inputText = "Die SFX Power 2 Serie bietet die perfekte Kombination aus Qualität, Funktionalität, Effizienz und dem für be quiet! bekannten zuverlässigen, leisen Betrieb für kompakte Systeme mit überdurchschnittlicher Leistung.";
        sentences = Tokenizer.getSentences(inputText, false, Language.GERMAN);
        // CollectionHelper.print(sentences);
        assertEquals(1, sentences.size());

        inputText = "Kräftige Kontraste und das große ----spektrum sorgen für eine natürliche Lebendigkeit v.a. bei Fotos und Videos.";
        sentences = Tokenizer.getSentences(inputText, false, Language.GERMAN);
        // CollectionHelper.print(sentences);
        assertEquals(1, sentences.size());

        inputText = "Die integrierte GPU läuft mit 350 Mhz (max. 1100 mit Turbo) und teilt sich den gemeinsamen, 3 MB großen L3-Cache mit der CPU.";
        sentences = Tokenizer.getSentences(inputText, false, Language.GERMAN);
        // CollectionHelper.print(sentences);
        assertEquals(1, sentences.size());

        inputText = "Schlüsselfunktionen des E2500 sind u. a. einen passwortgeschützten Gastzugang mit separatem Netzwerk zu erstellen, die Zugangszeit zu begrenzen und Webseiten zu sperren (Kinderschutz durch die Eltern).";
        sentences = Tokenizer.getSentences(inputText, false, Language.GERMAN);
        // CollectionHelper.print(sentences);
        assertEquals(1, sentences.size());

        inputText = "Außerdem bietet die HyperX SSD Hochgeschwindigkeitsübertragung mit SATA Rev. 3.0 (6 Gbit/s) für eine größere Band-----e, die Anwender für leistungshungrige Spiele, Multitasking und schnelle Multimedia-Nutzung benötigen.";
        sentences = Tokenizer.getSentences(inputText, false, Language.GERMAN);
        // CollectionHelper.print(sentences);
        assertEquals(1, sentences.size());

        inputText = "Tintenpatrone T1576 Vivid Light Magenta Ultra Chrome K3 Vivid Magenta - Artikel-Nr.: C13T15764010";
        sentences = Tokenizer.getSentences(inputText, false, Language.GERMAN);
        // CollectionHelper.print(sentences);
        assertEquals(1, sentences.size());

        inputText = "A 7.2 magnitude earthquake struck in the central Philippines Tuesday morning, killing at least four people and damaging buildings.";
        sentences = Tokenizer.getSentences(inputText);
        // CollectionHelper.print(sentences);
        assertEquals(1, sentences.size());

        inputText = "\"Not the \"what happenend?\" :) But this problem is one of the worst mistakes we made (I did!) in a very long time.\"";
        sentences = Tokenizer.getSentences(inputText);
        // CollectionHelper.print(sentences);
        assertEquals(2, sentences.size());

        inputText = "The induction of immediate-early (IE) response genes, such as egr-1, c-fos, and c-jun, occurs rapidly after the activation of T lymphocytes. The process of activation involves calcium mobilization, activation of protein kinase C (PKC), and phosphorylation of tyrosine kinases. p21(ras), a guanine nucleotide binding factor, mediates T-cell signal transduction through PKC-dependent and PKC-independent pathways. The involvement of p21(ras) in the regulation of calcium-dependent signals has been suggested through analysis of its role in the activation of NF-AT. We have investigated the inductions of the IE genes in response to calcium signals in Jurkat cells (in the presence of activated p21(ras)) and their correlated consequences!";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(5, sentences.size());
        assertEquals("The induction of immediate-early (IE) response genes, such as egr-1, c-fos, and c-jun, occurs rapidly after the activation of T lymphocytes.",
                sentences.get(0));
        assertEquals(
                "We have investigated the inductions of the IE genes in response to calcium signals in Jurkat cells (in the presence of activated p21(ras)) and their correlated consequences!",
                sentences.get(4));

        inputText = "This Paragraph is more difficult...or isn't it? hm, well (!), I don't know!!! I really don't.";
        sentences = Tokenizer.getSentences(inputText);
        // CollectionHelper.print(sentences);

        assertEquals(3, sentences.size());
        assertEquals("This Paragraph is more difficult...or isn't it?", sentences.get(0));
        assertEquals("hm, well (!), I don't know!!!", sentences.get(1));
        assertEquals("I really don't.", sentences.get(2));
        // CollectionHelper.print(sentences);

        inputText = "ActionScript 3.0 (or Flex 3.0.1) supports flash.stage.MovieClip(), cool he?";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals("ActionScript 3.0 (or Flex 3.0.1) supports flash.stage.MovieClip(), cool he?", sentences.get(0));

        inputText = "Mr. X is sometimes called Mr. X Jr., too!";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals("Mr. X is sometimes called Mr. X Jr., too!", sentences.get(0));

        inputText = "Although, St. Paul is a holy man, he is a man of earth too!";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals("Although, St. Paul is a holy man, he is a man of earth too!", sentences.get(0));

        inputText = "The largest in the U.S. is New York City, with a population of several million.";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals("The largest in the U.S. is New York City, with a population of several million.", sentences.get(0));

        inputText = "Some, ca. 200 pilots of the US A.F. think they would win vs. others said Mr. X on Tuesday.";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals("Some, ca. 200 pilots of the US A.F. think they would win vs. others said Mr. X on Tuesday.", sentences.get(0));

        // those patterns were causing an Exception which is fixed now : java.lang.StringIndexOutOfBoundsException
        // at tud.iir.helper.StringHelper.getSubstringBetween(StringHelper.java:984)
        inputText = "  Dont repeat yourself. Dont repeat yourself.";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(2, sentences.size());
        assertEquals("Dont repeat yourself.", sentences.get(0));
        assertEquals("Dont repeat yourself.", sentences.get(1));

        inputText = "Mr. T's kill count is ca. 4,500. Right?";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(2, sentences.size());
        assertEquals("Mr. T's kill count is ca. 4,500.", sentences.get(0));
        assertEquals("Right?", sentences.get(1));

        inputText = "Mr. T's website is not www.mrt.com or is it?";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals("Mr. T's website is not www.mrt.com or is it?", sentences.get(0));

        inputText = "Mr. T's website is not mrt.com or is it?";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals("Mr. T's website is not mrt.com or is it?", sentences.get(0));

        inputText = "Mr. T's website is not mrt.de/ or is it?";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals("Mr. T's website is not mrt.de/ or is it?", sentences.get(0));

        inputText = "You can't have a rainbow without rain ... think about it! Did you...think about it?";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(2, sentences.size());
        assertEquals("You can't have a rainbow without rain ... think about it!", sentences.get(0));
        assertEquals("Did you...think about it?", sentences.get(1));

        inputText = "Dies    ist  ein toller Test. Hallo Tag wird toll";
        sentences = Tokenizer.getSentences(inputText, Language.GERMAN);
        assertEquals(2, sentences.size());
        assertEquals("Dies    ist  ein toller Test.", sentences.get(0));
        assertEquals("Hallo Tag wird toll", sentences.get(1));

        inputText = "Ok I donated man million dollars in cash http://images.icanhascheezburger.com/completestore/2008/12/22/128744482782438694.jpg";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals("Ok I donated man million dollars in cash http://images.icanhascheezburger.com/completestore/2008/12/22/128744482782438694.jpg", sentences.get(0));

        inputText = "MAIDUGURI, Nigeria, Apr. 30, 2012 (Reuters) -- Nigerian Islamist 2. January 2009, sect 15.06.2004 Boko Haram killed four people.";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals(inputText, sentences.get(0));

        inputText = "And then he said: \"no way?\" and I said, 'yes way!' and she said 'ha ha.' and 'ho ho'.";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals(inputText, sentences.get(0));

        inputText = "And then he said:no way (but why did he say that?) and I said, 'yes way!' (and I meant it!) yes (I really meant it!!!!!!!) 'ho ho' (she likes to laugh.).";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals(inputText, sentences.get(0));

        inputText = "it happened again :) soo coool!";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals(inputText, sentences.get(0));

        inputText = "it happened again :). soo coool!";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(2, sentences.size());

        inputText = "it happened again :( soo mean!";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals(inputText, sentences.get(0));

        inputText = "it happened again :(. soo mean!";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(2, sentences.size());

        inputText = "it happened again ;-(. soo mean!";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(2, sentences.size());

        inputText = "It happened again. ;-( Soo mean!";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(2, sentences.size());

        inputText = "Not the \"what happenend?\" :) But this problem is one of the worst mistakes we made (I did!) in a very long time.";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(2, sentences.size());

        inputText = "IT IS three years since Senator Barack Obama pronounced that America “is no longer a Christian nation—at least, not just.” The words sounded harsher than he intended: bla.";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(2, sentences.size());

        inputText = "My name is Dr. No. I'm No. 1.";
        sentences = Tokenizer.getSentences(inputText);
        // XXX should give "My name is Dr. No.", "I'm No. 1."

        inputText = "Das ist z.B. sooo groß.";
        sentences = Tokenizer.getSentences(inputText, Language.GERMAN);
        assertEquals(1, sentences.size());

        inputText = "It added: \"Its government was consequently responsible for those acts performed by foreign officials. It had failed to submit any arguments explaining or justifying the degree of force used or the necessity of the invasive and potentially debasing measures. Those measures had been used with premeditation, the aim being to cause Mr Masri severe pain or suffering in order to obtain information. In the court's view, such treatment had amounted to torture, in violation of Article 3 [of the European human rights convention].\"\n\n In Afghanistan, Masri was incarcerated for more than four months in a small, dirty, dark concrete cell in a brick factory near the capital, Kabul, where he was repeatedly interrogated and was beaten, kicked and threatened. His repeated requests to meet with a representative of the German government were ignored, said the court.";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(6, sentences.size());

        inputText = "This isn’t the first time Texas has debated the perceived presence of too much Islam in its school books. In 2010, the Texas Board of Education banned any books that “paint Islam in too favorable of a light.” The reasoning was head-scratching: “the resolution adopted Friday cites ‘politically-correct whitewashes of Islamic culture and stigmas on Christian civilization’ in current textbooks and warns that ‘more such discriminatory treatment of religion may occur as Middle Easterners buy into the US public school textbook oligopoly.’” A Texas based civil liberties advocate said at the time that “the members who voted for this resolution were solely interested in playing on fear and bigotry in order to pit Christians against Muslims.”";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(4, sentences.size());
        assertTrue(sentences.get(3).startsWith("A Texas based"));

        inputText = "RSS (engl. ursprünglich Rich Site Summary, später Really Simple Syndication) ist eine seit dem Anfang des Jahres 2000 kontinuierlich weiterentwickelte Familie von Formaten für die einfache und strukturierte Veröffentlichung von Änderungen auf Websites (z. B. News-Seiten, Blogs, Audio-/Video-Logs etc.) in einem standardisierten Format (XML).";
        sentences = Tokenizer.getSentences(inputText, Language.GERMAN);
        // CollectionHelper.print(sentences);
        assertEquals(1, sentences.size());

        // XXX
        // inputText = "Former National Security Agency chief Michael V. Hayden learned a lesson about eavesdropping";
        // sentences = Tokenizer.getSentences(inputText);
        // CollectionHelper.print(sentences);
        // assertEquals(1, sentences.size());
        //        System.out.println(stopWatch.getElapsedTimeString());
    }
}
