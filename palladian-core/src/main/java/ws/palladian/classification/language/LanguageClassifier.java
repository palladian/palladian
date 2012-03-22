package ws.palladian.classification.language;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.collection.CollectionHelper;

public abstract class LanguageClassifier {

    private Map<String, String> languageCodes;

    public LanguageClassifier() {
        languageCodes = new HashMap<String, String>();

        languageCodes.put("Afrikaans", "ar");
        languageCodes.put("Albanian", "sq");
        languageCodes.put("Arabic", "ar");
        languageCodes.put("Aragonese", "an");
        languageCodes.put("Azerbaijani", "az");
        languageCodes.put("Basque", "eu");
        languageCodes.put("Belarusian", "be");
        languageCodes.put("Bengali", "bn");
        languageCodes.put("Bosnian", "bs");
        languageCodes.put("Breton", "br");
        languageCodes.put("Bulgarian", "bg");
        languageCodes.put("Catalan", "ca");
        languageCodes.put("Chinese", "zh");
        languageCodes.put("Chuvash", "cv");
        languageCodes.put("Croatian", "hr");
        languageCodes.put("Czech", "cs");
        languageCodes.put("Danish", "da");
        languageCodes.put("Dutch", "nl");
        languageCodes.put("English", "en");
        languageCodes.put("Esperanto", "eo");
        languageCodes.put("Estonian", "et");
        languageCodes.put("Finnish", "fi");
        languageCodes.put("French", "fr");
        languageCodes.put("Galician", "gl");
        languageCodes.put("German", "de");
        languageCodes.put("Greek", "el");
        languageCodes.put("Gujarati", "gu");
        languageCodes.put("Haitian", "ht");
        languageCodes.put("Hebrew", "he");
        languageCodes.put("Hindi", "hi");
        languageCodes.put("Hungarian", "hu");
        languageCodes.put("Icelandic", "is");
        languageCodes.put("Ido", "io");
        languageCodes.put("Indonesian", "id");
        languageCodes.put("Irish", "ga");
        languageCodes.put("Italian", "it");
        languageCodes.put("Japanese", "ja");
        languageCodes.put("Javanese", "jv");
        languageCodes.put("Kartuli", "ka");
        languageCodes.put("Korean", "ko");
        languageCodes.put("Kurdish", "ku");
        languageCodes.put("Latin", "la");
        languageCodes.put("Latvian", "lv");
        languageCodes.put("Lithuanian", "lt");
        languageCodes.put("Luxembourgish", "lb");
        languageCodes.put("Macedonian", "mk");
        languageCodes.put("Malay", "ms");
        languageCodes.put("Malayalam", "ml");
        languageCodes.put("Marathi", "mr");
        languageCodes.put("Nepali", "ne");
        languageCodes.put("Norwegian", "no");
        languageCodes.put("Occitan", "oc");
        languageCodes.put("Persian", "fa");
        languageCodes.put("Polish", "pl");
        languageCodes.put("Portuguese", "pt");
        languageCodes.put("Quechua", "qu");
        languageCodes.put("Romanian", "ro");
        languageCodes.put("Russian", "ru");
        languageCodes.put("Serbian", "sr");
        languageCodes.put("Slovak", "sk");
        languageCodes.put("Slovenian", "sl");
        languageCodes.put("Spanish", "es");
        languageCodes.put("Sundanese", "su");
        languageCodes.put("Swahili", "sw");
        languageCodes.put("Swedish", "sv");
        languageCodes.put("Tagalog", "tl");
        languageCodes.put("Tamil", "ta");
        languageCodes.put("Telugu", "te");
        languageCodes.put("Thai", "th");
        languageCodes.put("Turkish", "tr");
        languageCodes.put("Ukrainian", "uk");
        languageCodes.put("Urdu", "ur");
        languageCodes.put("Vietnamese", "vi");
        languageCodes.put("Volapuek", "vo");
        languageCodes.put("Walloon", "wa");
        languageCodes.put("Welsh", "cy");
        languageCodes.put("Western_Frisian", "fy");
    }

    public String mapLanguage(String language) {
        // return languageCodes.get(language);
        String result = "";
        for (Entry<String, String> entry : languageCodes.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(language)) { // make mapping case insensitive.
                result = entry.getValue();
                break;
            }
        }
        return result;
    }

    public String mapLanguageCode(String languageCode) {
        return (String) CollectionHelper.getKeyByValue(languageCodes, languageCode);
    }

    public abstract String classify(String text);

}
