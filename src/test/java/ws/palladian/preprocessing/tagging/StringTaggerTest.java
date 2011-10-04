package ws.palladian.preprocessing.tagging;

import junit.framework.Assert;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.preprocessing.nlp.ner.Annotations;
import ws.palladian.preprocessing.nlp.ner.FileFormatParser;
import ws.palladian.preprocessing.nlp.ner.StringTagger;

public class StringTaggerTest {

    @Test
    public void testTagString() {

        Annotations annotations = null;
        String taggedText = "";

        // abbreviations
        taggedText = "the United States of America (USA) are often called the USA, the U.S.A., or simply the U.S., the U.S.S. Enterprise is a space ship.";

        taggedText = StringTagger.tagString(taggedText);
        annotations = FileFormatParser.getAnnotationsFromXMLText(taggedText);
        CollectionHelper.print(annotations);

        Assert.assertEquals(6, annotations.size());
        Assert.assertEquals("United States of America", annotations.get(0).getEntity());
        Assert.assertEquals("USA", annotations.get(1).getEntity());
        Assert.assertEquals("USA", annotations.get(2).getEntity());
        Assert.assertEquals("U.S.A.", annotations.get(3).getEntity());
        Assert.assertEquals("U.S.", annotations.get(4).getEntity());
        Assert.assertEquals("U.S.S. Enterprise", annotations.get(5).getEntity());

        // names
        taggedText = "Mr. Yakomoto, John J. Smith, and Bill Drody cooperate with T. Sheff, L.Carding, T.O'Brian, Harry O'Sullivan and O'Brody. they are partying on Saturday's night special, Friday's Night special or THURSDAY'S, in St. Petersburg there is";

        taggedText = StringTagger.tagString(taggedText);
        annotations = FileFormatParser.getAnnotationsFromXMLText(taggedText);
        CollectionHelper.print(annotations);

        Assert.assertEquals(13, annotations.size());
        Assert.assertEquals("Mr. Yakomoto", annotations.get(0).getEntity());
        Assert.assertEquals("John J. Smith", annotations.get(1).getEntity());
        Assert.assertEquals("Bill Drody", annotations.get(2).getEntity());
        Assert.assertEquals("T. Sheff", annotations.get(3).getEntity());
        Assert.assertEquals("L.Carding", annotations.get(4).getEntity());
        Assert.assertEquals("T.O'Brian", annotations.get(5).getEntity());
        Assert.assertEquals("Harry O'Sullivan", annotations.get(6).getEntity());
        Assert.assertEquals("O'Brody", annotations.get(7).getEntity());
        Assert.assertEquals("Saturday", annotations.get(8).getEntity());
        Assert.assertEquals("Friday", annotations.get(9).getEntity());
        Assert.assertEquals("Night", annotations.get(10).getEntity());
        Assert.assertEquals("THURSDAY", annotations.get(11).getEntity());
        Assert.assertEquals("St. Petersburg", annotations.get(12).getEntity());
        // Assert.assertEquals("Google Inc.", annotations.get(12).getEntity());

        // composites
        taggedText = "Dolce & Gabana as well as S&P are companies.";

        taggedText = StringTagger.tagString(taggedText);
        annotations = FileFormatParser.getAnnotationsFromXMLText(taggedText);
        CollectionHelper.print(annotations);

        Assert.assertEquals(2, annotations.size());
        Assert.assertEquals("Dolce & Gabana", annotations.get(0).getEntity());
        Assert.assertEquals("S&P", annotations.get(1).getEntity());

        // containing numbers TODO make work, code in Stringtagger.tagString2 before revision r1952
        // taggedText =
        // "the Interstate 80 is dangerous, the Sony Playstation 3 looks more stylish than Microsoft's Xbox 360. the 1961 Ford Mustang is fast, H2 database just 30 ist not to tag though";
        //
        // taggedText = StringTagger.tagString2(taggedText);
        // annotations = FileFormatParser.getAnnotationsFromXMLText(taggedText);
        // CollectionHelper.print(annotations);
        //
        // Assert.assertEquals(6, annotations.size());
        // Assert.assertEquals("Interstate 80", annotations.get(0).getEntity());
        // Assert.assertEquals("Sony Playstation 3", annotations.get(1).getEntity());
        // Assert.assertEquals("Microsoft", annotations.get(2).getEntity());
        // Assert.assertEquals("Xbox 360", annotations.get(3).getEntity());
        // Assert.assertEquals("1961 Ford Mustang", annotations.get(4).getEntity());
        // Assert.assertEquals("H2", annotations.get(5).getEntity());

        // fill words
        taggedText = "the Republic of Ireland, and Return of King Arthur, the National Bank of Scotland, Erin Purcell of Boston-based Reagan Communications";

        taggedText = StringTagger.tagString(taggedText);
        annotations = FileFormatParser.getAnnotationsFromXMLText(taggedText);
        CollectionHelper.print(annotations);

        Assert.assertEquals(6, annotations.size());
        Assert.assertEquals("Republic of Ireland", annotations.get(0).getEntity());
        Assert.assertEquals("Return of King Arthur", annotations.get(1).getEntity());
        Assert.assertEquals("National Bank of Scotland", annotations.get(2).getEntity());
        Assert.assertEquals("Erin Purcell", annotations.get(3).getEntity());
        Assert.assertEquals("Boston-based", annotations.get(4).getEntity());
        Assert.assertEquals("Reagan Communications", annotations.get(5).getEntity());

        // dashes
        taggedText = "Maria-Hillary Johnson lives on Chester-le-Street and Ontario-based Victor Vool, the All-England Club and Patricia Djate-Taillard were in the United Nations-sponsored ceasfire with St. Louis-based NFL coach trains in MG-Gym (MG-GYM), the Real- Rumble, TOTALLY FREE- Choice, Australia-- Germany";

        taggedText = StringTagger.tagString(taggedText);
        annotations = FileFormatParser.getAnnotationsFromXMLText(taggedText);
        CollectionHelper.print(annotations);

        Assert.assertEquals(17, annotations.size());
        Assert.assertEquals("Maria-Hillary Johnson", annotations.get(0).getEntity());
        Assert.assertEquals("Chester-le-Street", annotations.get(1).getEntity());
        Assert.assertEquals("Ontario-based", annotations.get(2).getEntity());
        Assert.assertEquals("Victor Vool", annotations.get(3).getEntity());
        Assert.assertEquals("All-England Club", annotations.get(4).getEntity());
        Assert.assertEquals("Patricia Djate-Taillard", annotations.get(5).getEntity());
        Assert.assertEquals("United Nations-sponsored", annotations.get(6).getEntity());
        Assert.assertEquals("St. Louis-based", annotations.get(7).getEntity());
        Assert.assertEquals("NFL", annotations.get(8).getEntity());
        Assert.assertEquals("MG-Gym", annotations.get(9).getEntity());
        Assert.assertEquals("MG-GYM", annotations.get(10).getEntity());
        Assert.assertEquals("Real", annotations.get(11).getEntity());
        Assert.assertEquals("Rumble", annotations.get(12).getEntity());
        Assert.assertEquals("TOTALLY FREE", annotations.get(13).getEntity());
        Assert.assertEquals("Choice", annotations.get(14).getEntity());
        Assert.assertEquals("Australia", annotations.get(15).getEntity());
        Assert.assertEquals("Germany", annotations.get(16).getEntity());

        // starting small and camel case
        taggedText = "the last ex-England, mid-SCORER player, al-Rama is a person Rami al-Sadani, the iPhone 4 is a phone. Veronica Swenston VENICE alternative Frank HERALD";

        taggedText = StringTagger.tagString(taggedText);
        annotations = FileFormatParser.getAnnotationsFromXMLText(taggedText);
        CollectionHelper.print(annotations);

        Assert.assertEquals(9, annotations.size());
        Assert.assertEquals("ex-England", annotations.get(0).getEntity());
        Assert.assertEquals("mid-SCORER", annotations.get(1).getEntity());
        Assert.assertEquals("al-Rama", annotations.get(2).getEntity());
        Assert.assertEquals("Rami al-Sadani", annotations.get(3).getEntity());
        Assert.assertEquals("iPhone 4", annotations.get(4).getEntity());
        Assert.assertEquals("Veronica Swenston", annotations.get(5).getEntity());
        Assert.assertEquals("VENICE", annotations.get(6).getEntity());
        Assert.assertEquals("Frank", annotations.get(7).getEntity());
        Assert.assertEquals("HERALD", annotations.get(8).getEntity());

    }

}