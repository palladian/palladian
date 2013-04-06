package ws.palladian.extraction.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class StringTaggerTest {

    @Test
    public void testTagString() {

        Annotations annotations = null;
        String text = "";

        // abbreviations
        text = "the United States of America (USA) are often called the USA, the U.S.A., or simply the U.S., the U.S.S. Enterprise is a space ship.";

        annotations = StringTagger.getTaggedEntities(text);
        // CollectionHelper.print(annotations);

        assertEquals(6, annotations.size());
        assertEquals("United States of America", annotations.get(0).getEntity());
        assertEquals("USA", annotations.get(1).getEntity());
        assertEquals("USA", annotations.get(2).getEntity());
        assertEquals("U.S.A.", annotations.get(3).getEntity());
        assertEquals("U.S.", annotations.get(4).getEntity());
        assertEquals("U.S.S. Enterprise", annotations.get(5).getEntity());

        text = "The outfit that stages the festival, Black Rock City LLC, is now a $23 million-per-year concern with 40 full-time employees, hundreds of volunteers, and a non-profit arts foundation that doles out grants.";
        annotations = StringTagger.getTaggedEntities(text);
        CollectionHelper.print(annotations);
        assertEquals(2, annotations.size());
        assertEquals("Black Rock City LLC", annotations.get(1).getEntity());

        // names
        text = "Mr. Yakomoto, John J. Smith, and Bill Drody cooperate with T. Sheff, L.Carding, T.O'Brian, Harry O'Sullivan and O'Brody. they are partying on Saturday's night special, Friday's Night special or THURSDAY'S, in St. Petersburg there is Dr. Mark Litwin";

        annotations = StringTagger.getTaggedEntities(text);
        // CollectionHelper.print(annotations);

        assertEquals(14, annotations.size());
        assertEquals("Mr. Yakomoto", annotations.get(0).getEntity());
        assertEquals("John J. Smith", annotations.get(1).getEntity());
        assertEquals("Bill Drody", annotations.get(2).getEntity());
        assertEquals("T. Sheff", annotations.get(3).getEntity());
        assertEquals("L.Carding", annotations.get(4).getEntity());
        assertEquals("T.O'Brian", annotations.get(5).getEntity());
        assertEquals("Harry O'Sullivan", annotations.get(6).getEntity());
        assertEquals("O'Brody", annotations.get(7).getEntity());
        assertEquals("Saturday", annotations.get(8).getEntity());
        assertEquals("Friday", annotations.get(9).getEntity());
        assertEquals("Night", annotations.get(10).getEntity());
        assertEquals("THURSDAY", annotations.get(11).getEntity());
        assertEquals("St. Petersburg", annotations.get(12).getEntity());
        assertEquals("Dr. Mark Litwin", annotations.get(13).getEntity());
        // assertEquals("Google Inc.", annotations.get(12).getEntity());

        // composites
        text = "Dolce & Gabana as well as S&P are companies.";

        annotations = StringTagger.getTaggedEntities(text);
        // CollectionHelper.print(annotations);

        assertEquals(2, annotations.size());
        assertEquals("Dolce & Gabana", annotations.get(0).getEntity());
        assertEquals("S&P", annotations.get(1).getEntity());

        // containing numbers TODO make work, code in Stringtagger.tagString2 before revision r1952
        // taggedText =
        // "the Interstate 80 is dangerous, the Sony Playstation 3 looks more stylish than Microsoft's Xbox 360. the 1961 Ford Mustang is fast, H2 database just 30 ist not to tag though";
        //
        // taggedText = StringTagger.tagString2(taggedText);
        // annotations = FileFormatParser.getAnnotationsFromXMLText(taggedText);
        // CollectionHelper.print(annotations);
        //
        // assertEquals(6, annotations.size());
        // assertEquals("Interstate 80", annotations.get(0).getEntity());
        // assertEquals("Sony Playstation 3", annotations.get(1).getEntity());
        // assertEquals("Microsoft", annotations.get(2).getEntity());
        // assertEquals("Xbox 360", annotations.get(3).getEntity());
        // assertEquals("1961 Ford Mustang", annotations.get(4).getEntity());
        // assertEquals("H2", annotations.get(5).getEntity());

        // fill words
        text = "the Republic of Ireland, and Return of King Arthur, the National Bank of Scotland, Erin Purcell of Boston-based Reagan Communications";

        annotations = StringTagger.getTaggedEntities(text);
        // CollectionHelper.print(annotations);

        assertEquals(6, annotations.size());
        assertEquals("Republic of Ireland", annotations.get(0).getEntity());
        assertEquals("Return of King Arthur", annotations.get(1).getEntity());
        assertEquals("National Bank of Scotland", annotations.get(2).getEntity());
        assertEquals("Erin Purcell", annotations.get(3).getEntity());
        assertEquals("Boston-based", annotations.get(4).getEntity());
        assertEquals("Reagan Communications", annotations.get(5).getEntity());

        // dashes
        text = "Maria-Hillary Johnson lives on Chester-le-Street and Ontario-based Victor Vool, the All-England Club and Patricia Djate-Taillard were in the United Nations-sponsored ceasfire with St. Louis-based NFL coach trains in MG-Gym (MG-GYM), the Real- Rumble, TOTALLY FREE- Choice, Australia-- Germany";

        annotations = StringTagger.getTaggedEntities(text);
        // CollectionHelper.print(annotations);

        assertEquals(17, annotations.size());
        assertEquals("Maria-Hillary Johnson", annotations.get(0).getEntity());
        assertEquals("Chester-le-Street", annotations.get(1).getEntity());
        assertEquals("Ontario-based", annotations.get(2).getEntity());
        assertEquals("Victor Vool", annotations.get(3).getEntity());
        assertEquals("All-England Club", annotations.get(4).getEntity());
        assertEquals("Patricia Djate-Taillard", annotations.get(5).getEntity());
        assertEquals("United Nations-sponsored", annotations.get(6).getEntity());
        assertEquals("St. Louis-based", annotations.get(7).getEntity());
        assertEquals("NFL", annotations.get(8).getEntity());
        assertEquals("MG-Gym", annotations.get(9).getEntity());
        assertEquals("MG-GYM", annotations.get(10).getEntity());
        assertEquals("Real", annotations.get(11).getEntity());
        assertEquals("Rumble", annotations.get(12).getEntity());
        assertEquals("TOTALLY FREE", annotations.get(13).getEntity());
        assertEquals("Choice", annotations.get(14).getEntity());
        assertEquals("Australia", annotations.get(15).getEntity());
        assertEquals("Germany", annotations.get(16).getEntity());

        // apostrophes
        text = "Early in 1939, Georgia O’Keeffe, the artist most famous for depicting the arid Southwest, suddenly decided to paint America’s diametrically opposite landscape — the lush tropical valleys of Hawaii.";

        annotations = StringTagger.getTaggedEntities(text);
        // CollectionHelper.print(annotations);
        assertEquals(5, annotations.size());
        assertEquals("Early", annotations.get(0).getEntity());
        assertEquals("Georgia O’Keeffe", annotations.get(1).getEntity());
        assertEquals("Southwest", annotations.get(2).getEntity());
        assertEquals("America", annotations.get(3).getEntity());
        assertEquals("Hawaii", annotations.get(4).getEntity());

        text = "The Adam Opel GmbH unit is based in Germany.";
        annotations = StringTagger.getTaggedEntities(text);
        assertEquals("The Adam Opel GmbH", annotations.get(0).getEntity());
        // CollectionHelper.print(annotations);

        text = "In 2009, GM scrapped a plan — favored by Berlin — to sell a majority stake to a consortium led by Canadian auto parts maker Magna International Inc. and decided to restructure the brands itself instead.";
        annotations = StringTagger.getTaggedEntities(text);
        assertEquals("Magna International Inc.", annotations.get(4).getEntity());
        // CollectionHelper.print(annotations);

        text = "General Motors Co.'s Opel unit said Monday that it plans to end car production at one plant in Germany in 2016, but a slimmed-down factory may continue to make components.";
        annotations = StringTagger.getTaggedEntities(text);
        assertEquals("General Motors Co.", annotations.get(0).getEntity());
        // CollectionHelper.print(annotations);

        // starting small and camel case
        text = "the last ex-England, mid-SCORER player, al-Rama is a person Rami al-Sadani, the iPhone 4 is a phone. Veronica Swenston VENICE alternative Frank HERALD which was found at Universal Orlando® Resort";

        annotations = StringTagger.getTaggedEntities(text);
        // CollectionHelper.print(annotations);

        assertEquals(10, annotations.size());
        assertEquals("ex-England", annotations.get(0).getEntity());
        assertEquals("mid-SCORER", annotations.get(1).getEntity());
        assertEquals("al-Rama", annotations.get(2).getEntity());
        assertEquals("Rami al-Sadani", annotations.get(3).getEntity());
        assertEquals("iPhone 4", annotations.get(4).getEntity());
        assertEquals("Veronica Swenston", annotations.get(5).getEntity());
        assertEquals("VENICE", annotations.get(6).getEntity());
        assertEquals("Frank", annotations.get(7).getEntity());
        assertEquals("HERALD", annotations.get(8).getEntity());
        assertEquals("Universal Orlando® Resort", annotations.get(9).getEntity());

    }

}