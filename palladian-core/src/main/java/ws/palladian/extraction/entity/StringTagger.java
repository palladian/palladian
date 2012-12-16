package ws.palladian.extraction.entity;

import java.io.File;

import ws.palladian.helper.io.FileHelper;

public class StringTagger {

    public static void tagAndSaveString(File input) {
        String text = FileHelper.readFileToString(input.getAbsolutePath());
        String taggedText = tagString(text);
        FileHelper.writeToFile(
                FileHelper.getRenamedFilename(input, input.getName().replaceAll("\\..*", "") + "_tagged"), taggedText);
    }

    public static String tagString(File f) {
        String text = FileHelper.readFileToString(f.getAbsolutePath());
        return tagString(text);
    }

    public static String tagString(String s, String regexp) {
        String entityTag = "<CANDIDATE>$0</CANDIDATE>";
        return s.replaceAll(regexp, entityTag);
    }

    public static String tagString(String s) {

        String entityTag = "<CANDIDATE>$0</CANDIDATE>";

        String regexp = "";

        // dashes (such as "Ontario-based" "Victor" or St. Louis-based)
        regexp += "([A-Z][a-z]\\. )?([A-Z]{1}[A-Za-z]+(-[a-z]+)(-[A-Za-z]+)*)";

        // names
        regexp += "|";
        regexp += "([A-Z]\\.)( )?[A-Z]{1}['A-Za-z]{1,100}";
        regexp += "|";
        regexp += "[A-Z][a-z]+ [A-Z]{1}\\. [A-Za-z]{1,100}";
        regexp += "|";
        regexp += "([A-Z][a-z]{0,2}\\.) [A-Z]{1}[A-Za-z]{1,100}( [A-Z]{1}[A-Za-z]{1,100})?";
        regexp += "|";
        // regexp +=
        // "([A-Z]\\.)+ (([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9&]+))+(([ ])*[A-Z]+([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})";
        regexp += "([A-Z]\\.)+( ([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9&]+))+(([ ])*[A-Z]+([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})*";
        // regexp += "|";
        // regexp +=
        // "((([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9']+))+(( )?[A-Z]+('[A-Z])?([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})(?!(\\.[A-Z])+))";

        // ending with dash (Real- Rumble => should be two words, TOTALLY FREE- Abc => also two matches)
        regexp += "|";
        regexp += "([A-Z][A-Za-z]+ )*[A-Z][A-Za-z]+(?=-+? )";

        // small with dash (ex-President)
        regexp += "|";
        regexp += "([A-Z][A-Za-z]+ )?([a-z]+-[A-Z][A-Za-z0-9]+)";

        // ___ of ___ (such as "National Bank of Scotland" or "Duke of South Carolina") always in the form of X Y of Z
        // OR X of Y Z
        regexp += "|";
        regexp += "(([A-Z]{1}[A-Za-z]+ ){2,}of (([A-Z]{1}[A-Za-z-]+)(?!([a-z-]{0,20}\\s[A-Z]))))|([A-Z]{1}[A-Za-z-]+ of( [A-Z]{1}[A-Za-z]+){1,})";

        // prevent mixtures of mix camel cases => "Veronica Swenston VENICE" should be two matches
        regexp += "|";
        regexp += "([A-Z]{1}([a-z-0-9]+)(([ &])*([A-Z]')?[A-Z]{1}([a-z-0-9]+))*)";

        // names (such as "O'Sullivan"), compounds such as "D&G"
        regexp += "|";
        regexp += "((([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9&]+|'[A-Z][A-Za-z]{2,20}))+(([ &])*[A-Z]+('[A-Z])?([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})(?!(\\.[A-Z])+))";

        // regexp += "|";
        // regexp +=
        // "((([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9&]+))+(([ &])*[A-Z]+([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})(?!(\\.[A-Z])+))";

        // camel case (iPhone 4)
        regexp += "|";
        regexp += "([a-z][A-Z][A-Za-z0-9]+( [A-Z0-9][A-Za-z0-9]{0,20}){0,20})";

        s = s.replaceAll(regexp, entityTag);

        return s;
    }

    public static Annotations getTaggedEntities(String text, String regexp) {
        Annotations annotations = new Annotations();
        String taggedText = tagString(text, regexp);
        annotations = FileFormatParser.getAnnotationsFromXmlText(taggedText);
        return annotations;
    }

    public static Annotations getTaggedEntities(String text) {
        Annotations annotations = new Annotations();
        String taggedText = tagString(text);
        annotations = FileFormatParser.getAnnotationsFromXmlText(taggedText);
        return annotations;
    }

    public static void main(String[] args) {
        // String testText =
        // "\"All the Lilacs in Ohio Great Song John Hiatt\" is a song from John Hiatt's album THE TIKI BAR IS OPEN.";
        //
        // testText =
        // "Abraham Lincoln ( no middle name ) was born on February 12, 1809, the second child to Thomas Lincoln and Nancy Lincoln ( n é e Hanks ), in a one-room log cabin on the Sinking Spring Farm in southeast Hardin County, Kentucky [ 5 ] ( now LaRue County ). His older sister, Sarah ( Grigsby ), died while giving birth at a young age. He is descended from Samuel Lincoln, who arrived in Hingham, Massachusetts, from Norfolk, England, in the 17th century.[ 6 ] His grandfather and namesake Abraham Lincoln, a substantial landholder, moved from Virginia to Kentucky, where he was ambushed and killed by an Indian raid in 1786, with his children Mordecai, Josiah, and Thomas looking on.[ 7 ] Mordecai' s marksmanship with a rifle saved Thomas from the same fate. As the eldest son, by law, Mordecai inherited his father' s entire estate.[ 8 ]\n"
        // +
        // "Further information: Mary Todd Lincoln; Sexuality of Abraham Lincoln; Medical and mental health of Abraham Lincoln\n"
        // + "Mary Todd Lincoln, wife of Abraham Lincoln, age 28\n"
        // + "Main articles: Abraham Lincoln' s early life and career and Abraham Lincoln in the Black Hawk War\n"
        // + "Sketch of a young Abraham Lincoln\n"
        // + "Main articles: Abraham Lincoln on slavery and Emancipation Proclamation\n";
        //
        // testText =
        // "The United States of America are often called the USA, the U.S.A., or simply the U.S. The U.N. has its headquarter in N.Y.C. on the east coast. The U.S. Gulf stream fleet is...while the U.N. is ";
        //
        // // testText =
        // //
        // "Mrs. Smithers is not called F. Smithers. Tim O'Brien, or O'Brien or just T.O'Brien or T. O'Brien, not Saturday's night, Friday's Night or THURSDAY'S NIGHT.";
        //
        // // testText =
        // //
        // "The so called BBC is the British Broadcasting Corporation (BBC) or ( BBC ). The Saturday's World Cup and Keith O'Neill played. Gold Fields of East, with S&P. Douglas& Lomason CO is a company. Henson & Sons.";
        // // testText = "in the U.S. Gulf there are";
        // // CollectionHelper.print(Tokenizer.tokenize(testText));
        // System.out.println(StringTagger.tagString(testText));
        //
        // System.exit(0);
        // //
        // System.out
        // .println(StringTagger.tagString("Spiderman 3 is a movie. The new Nokia N95 is another mobile phone."));
        // DocumentRetriever c = new DocumentRetriever();
        // testText = c.download("http://localhost:8081/ManKB/testpageGerman.html", false, true, true, true);
        // Annotations taggedEntities = StringTagger.getTaggedEntities(testText);
        // CollectionHelper.print(taggedEntities);
        // // StringTagger.tagAndSaveString(new File("data/test/sampleTextForTagging.txt"));
    }

}