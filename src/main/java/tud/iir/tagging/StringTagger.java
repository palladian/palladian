package tud.iir.tagging;

import java.io.File;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.knowledge.RegExp;
import tud.iir.web.Crawler;

public class StringTagger {

    public static void tagAndSaveString(File input) {
        String text = FileHelper.readFileToString(input.getAbsolutePath());
        String taggedText = tagString(text);
        FileHelper.writeToFile(FileHelper.rename(input, input.getName().replaceAll("\\..*", "") + "_tagged"), taggedText);
    }

    public static String tagString(File f) {
        String text = FileHelper.readFileToString(f.getAbsolutePath());
        return tagString(text);
    }

    public static String tagString(String s) {

        Pattern pat = Pattern.compile(RegExp.ENTITY);
        Matcher m = pat.matcher(s);
        while (m.find()) {
            // s = s.replaceAll(m.group(), "#" + m.group() + "#");
        }

        String entityTag = "§$0#";

        // tag entities in quotes
        s = s.replaceAll("\".*?\"", entityTag);

        // tag obvious entities that are noun patterns
        // s = s.replaceAll("(?<!§.{0,20})"+RegExp.ENTITY+"(?!.*?#)", entityTag);
        s = s.replaceAll("(?<!§\"[^#]{0,40})" + RegExp.ENTITY, entityTag);

        return s;
    }

    public static HashSet<String> getTaggedEntities(String text) {

        HashSet<String> taggedEntities = new HashSet<String>();

        String taggedText = tagString(text);
        System.out.println(taggedText);

        Pattern pat = Pattern.compile("§.*?#");
        Matcher m = pat.matcher(taggedText);
        while (m.find()) {
            taggedEntities.add(m.group().replaceAll("§", "").replaceAll("#", ""));
        }

        return taggedEntities;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String testText = "\"All the Lilacs in Ohio Great Song John Hiatt\" is a song from John Hiatt's album THE TIKI BAR IS OPEN.";
        System.out.println(StringTagger.tagString(testText));
        // System.out.println(StringTagger.tagString("Spiderman 3 is a movie. The new Nokia N95 is another mobile phone."));

        Crawler c = new Crawler();
        testText = c.download("http://localhost:8081/ManKB/testpageGerman.html", false, true, true, true);

        HashSet<String> taggedEntities = StringTagger.getTaggedEntities(testText);
        CollectionHelper.print(taggedEntities);

        // StringTagger.tagAndSaveString(new File("data/test/sampleTextForTagging.txt"));
    }

}