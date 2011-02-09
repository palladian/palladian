package tud.iir.tagging;

import java.io.File;

import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.FileFormatParser;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.web.Crawler;

public class StringTagger {

    public static void tagAndSaveString(File input) {
        String text = FileHelper.readFileToString(input.getAbsolutePath());
        String taggedText = tagString(text);
        FileHelper.writeToFile(FileHelper.rename(input, input.getName().replaceAll("\\..*", "") + "_tagged"),
                taggedText);
    }

    public static String tagString(File f) {
        String text = FileHelper.readFileToString(f.getAbsolutePath());
        return tagString(text);
    }

    public static String tagString(String s) {

        String entityTag = "<CANDIDATE>$0</CANDIDATE>";

//        Pattern pat = Pattern.compile(RegExp.ENTITY);
//        Matcher m = pat.matcher(s);
//        while (m.find()) {
//            System.out.println(m.group());
//            // s = s.replaceAll(m.group(), "#" + m.group() + "#");
//            s = s.replaceAll(m.group(), "<CANDIDATE>" + m.group() + "</CANDIDATE>");
//        }

        // tag obvious entities that are noun patterns
        // s = s.replaceAll("(?<!§.{0,20})" + RegExp.ENTITY + "(?!.*?#)", entityTag);
        // s = s.replaceAll("(?<!§\"[^#]{0,40})" + RegExp.ENTITY, entityTag);
        // s = s.replaceAll("(?<!\\<CANDIDATE\\>)" + RegExp.ENTITY, entityTag);
        // s = s.replaceAll("(?<!\"[^\"]{0,200}?)" + RegExp.ENTITY, entityTag);

        String regexp = "";
        regexp += "([A-Z][a-z]{0,2}\\.) [A-Z]{1}[A-Za-z]{1,100}";
        regexp += "|";
        regexp += "([A-Z]\\.)+";
        regexp += "|";
        regexp += "((([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9]+))+(( )?[A-Z]+([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})(?!(\\.[A-Z])+))";

        // s =
        // s.replaceAll("(([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9]+))+(( )?[A-Z]+([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10})",
        // entityTag);
        s = s.replaceAll(regexp, entityTag);
        // s = s.replaceAll(RegExp.ENTITY, entityTag);

        // ([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9.]*))+(( )?[A-Z0-9]+([A-Za-z-üäößãáàúùíìîéèê0-9]*))*
        // s = s.replaceAll(
        // "([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9.]*))+(( )?[A-Z0-9]+([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10}",
        // entityTag);
        // s = Pattern.compile(RegExp.ENTITY).matcher(s).replaceAll(entityTag);

        // s = s.replaceAll("(?<!\"[^\"]{0,200})" + RegExp.ENTITY, entityTag);
        // (\w)(?=(?:[^"]|"[^"]*")*$)
        // s = s.replaceAll("(" + RegExp.ENTITY + ")(?=(?:[^\"]|\"[^\"]*\")*$)", entityTag);
        // (?<!aaa((?!bbb)[\s\S])*)

        // Pattern pattern = Pattern.compile("(" + RegExp.ENTITY + ")(?=(?:[^\"]|\"[^\"]*\")*$)", Pattern.MULTILINE);
        // Pattern pattern = Pattern.compile("(" + RegExp.ENTITY +
        // ")(?=(?:[^\"]|\"[^\"]{0,300}\"){0,300}$)",Pattern.MULTILINE);
        // Matcher matcher = pattern.matcher(s);
        // s = matcher.replaceAll(entityTag);

        // tag entities in quotes
        // s = s.replaceAll("(?<=\").{1,300}?(?=\")", entityTag);
        // s = s.replaceAll("(?<=\")[^\"]+?(?=\")", entityTag);

        return s;
    }

    public static Annotations getTaggedEntities(String text) {
        Annotations annotations = new Annotations();
        String taggedText = tagString(text);
        // FileHelper.writeToFile("t.txt", taggedText);
        // System.out.println(taggedText);
        annotations = FileFormatParser.getAnnotationsFromXMLText(taggedText);
        return annotations;
    }

    public static void main(String[] args) {
        String testText = "\"All the Lilacs in Ohio Great Song John Hiatt\" is a song from John Hiatt's album THE TIKI BAR IS OPEN.";

        testText = "Abraham Lincoln ( no middle name ) was born on February 12, 1809, the second child to Thomas Lincoln and Nancy Lincoln ( n é e Hanks ), in a one-room log cabin on the Sinking Spring Farm in southeast Hardin County, Kentucky [ 5 ] ( now LaRue County ). His older sister, Sarah ( Grigsby ), died while giving birth at a young age. He is descended from Samuel Lincoln, who arrived in Hingham, Massachusetts, from Norfolk, England, in the 17th century.[ 6 ] His grandfather and namesake Abraham Lincoln, a substantial landholder, moved from Virginia to Kentucky, where he was ambushed and killed by an Indian raid in 1786, with his children Mordecai, Josiah, and Thomas looking on.[ 7 ] Mordecai' s marksmanship with a rifle saved Thomas from the same fate. As the eldest son, by law, Mordecai inherited his father' s entire estate.[ 8 ]\n"
                + "Further information: Mary Todd Lincoln; Sexuality of Abraham Lincoln; Medical and mental health of Abraham Lincoln\n"
                + "Mary Todd Lincoln, wife of Abraham Lincoln, age 28\n"
                + "Main articles: Abraham Lincoln' s early life and career and Abraham Lincoln in the Black Hawk War\n"
                + "Sketch of a young Abraham Lincoln\n"
                + "Main articles: Abraham Lincoln on slavery and Emancipation Proclamation\n";

        // testText =
        // "The United States of America are often called the USA, the U.S.A., or simply the U.S. The U.N. has its headquarter in N.Y.C. on the east coast.";

        // testText = "Mrs. Smithers is not called F. Smithers.";

        testText = "The so called BBC is the British Broadcasting Corporation (BBC) or ( BBC )";

        // CollectionHelper.print(Tokenizer.tokenize(testText));
        System.out.println(StringTagger.tagString(testText));

        System.exit(0);
        //
        System.out
        .println(StringTagger.tagString("Spiderman 3 is a movie. The new Nokia N95 is another mobile phone."));
        Crawler c = new Crawler();
        testText = c.download("http://localhost:8081/ManKB/testpageGerman.html", false, true, true, true);
        Annotations taggedEntities = StringTagger.getTaggedEntities(testText);
        CollectionHelper.print(taggedEntities);
        // StringTagger.tagAndSaveString(new File("data/test/sampleTextForTagging.txt"));
    }

}