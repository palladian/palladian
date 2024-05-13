package ws.palladian.helper.normalization;

import ws.palladian.helper.collection.StringLengthComparator;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.nlp.PatternHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by David on 30.01.2018.
 */
public class UnitTranslator {
    private static final Map<Language, Map<String, String>> unitTranslations = new HashMap<>();
    private static final Map<Language, List<String>> lengthSortedLanguageKeys = new HashMap<>();

    static {
        //// GERMAN
        Map<String, String> germanTranslationMap = new HashMap<>();

        // OTHER
        germanTranslationMap.put("prozent", "percent");
        germanTranslationMap.put("billion", "trillion");
        germanTranslationMap.put("billionen", "trillions");
        germanTranslationMap.put("milliarde", "billion");
        germanTranslationMap.put("milliarden", "billions");
        germanTranslationMap.put("tausend", "thousand");

        // LENGTH
        germanTranslationMap.put("meile", "mile");
        germanTranslationMap.put("meilen", "miles");
        germanTranslationMap.put("dezimeter", "decimeter");
        germanTranslationMap.put("fuß", "ft");
        germanTranslationMap.put("fuss", "ft");
        germanTranslationMap.put("zoll", "inch");
        germanTranslationMap.put("zentimeter", "cm");

        // TIME
        germanTranslationMap.put("jahr", "year");
        germanTranslationMap.put("jahre", "years");
        germanTranslationMap.put("jahren", "years");
        germanTranslationMap.put("monat", "month");
        germanTranslationMap.put("monate", "months");
        germanTranslationMap.put("monaten", "months");
        germanTranslationMap.put("woche", "week");
        germanTranslationMap.put("wochen", "weeks");
        germanTranslationMap.put("tag", "day");
        germanTranslationMap.put("tage", "days");
        germanTranslationMap.put("tagen", "days");
        germanTranslationMap.put("std.", "hour");
        germanTranslationMap.put("stunde", "hour");
        germanTranslationMap.put("stunden", "hours");
        germanTranslationMap.put("minuten", "minutes");
        germanTranslationMap.put("sekunde", "second");
        germanTranslationMap.put("sekunden", "seconds");
        germanTranslationMap.put("sek", "seconds");
        germanTranslationMap.put("millisekunde", "millisecond");
        germanTranslationMap.put("millisekunden", "milliseconds");

        // WEIGHT
        germanTranslationMap.put("tonne", "ton");
        germanTranslationMap.put("tonnen", "tons");
        germanTranslationMap.put("kilogramm", "kg");
        germanTranslationMap.put("pfund", "pounds");
        germanTranslationMap.put("pfd", "pounds");
        germanTranslationMap.put("unze", "ounce");
        germanTranslationMap.put("unzen", "ounces");
        germanTranslationMap.put("gramm", "gram");

        // AREA
        germanTranslationMap.put("quadratmeile", "square mile");
        germanTranslationMap.put("quadratmeilen", "square miles");
        germanTranslationMap.put("quadratkilometer", "square kilometers");
        germanTranslationMap.put("quadrat kilometer", "square kilometers");
        germanTranslationMap.put("qkm", "square kilometers");
        germanTranslationMap.put("hektar", "hectares");

        // VOLUME
        germanTranslationMap.put("gallone", "gallon");
        germanTranslationMap.put("gallonen", "gallons");
        germanTranslationMap.put("tasse", "cup");
        germanTranslationMap.put("tassen", "cups");
        germanTranslationMap.put("zentiliter", "cl");

        // POWER
        germanTranslationMap.put("pferdestärke", "horsepower");
        germanTranslationMap.put("pferdestärken", "horsepower");
        germanTranslationMap.put("ps", "hp");
        germanTranslationMap.put("kilojoule", "kilo joules");
        germanTranslationMap.put("kilokalorien", "kilocalories");
        germanTranslationMap.put("kalorien", "calories");

        // ENERGY
        germanTranslationMap.put("wattstunde", "watt hour");
        germanTranslationMap.put("wattstunden", "watt hours");
        germanTranslationMap.put("watt stunden", "calories");

        // ROTATION_SPEED
        germanTranslationMap.put("u/minute", "rpm");
        germanTranslationMap.put("umdrehungen pro minute", "rpm");
        germanTranslationMap.put("u/min", "rpm");

        // ELECTRIC_CHARGE
        germanTranslationMap.put("kilowattstunde", "kwh");
        germanTranslationMap.put("kilowattstunden", "kwh");
        germanTranslationMap.put("amperestunde", "Ah");
        germanTranslationMap.put("amperestunden", "Ah");
        germanTranslationMap.put("ampere-stunde", "Ah");
        germanTranslationMap.put("ampere-stunden", "Ah");
        germanTranslationMap.put("milliamperestunde", "mAh");
        germanTranslationMap.put("milliamperestunden", "mAh");

        unitTranslations.put(Language.GERMAN, germanTranslationMap);

        //// FRENCH
        Map<String, String> frenchTranslationMap = new HashMap<>();

        // OTHER
        frenchTranslationMap.put("pourcent", "percent");
        frenchTranslationMap.put("pour cent", "percent");
        frenchTranslationMap.put("billion", "trillion");
        frenchTranslationMap.put("billions", "trillions");
        frenchTranslationMap.put("milliard", "billion");
        frenchTranslationMap.put("mille", "thousand");

        // LENGTH
        frenchTranslationMap.put("centimètre", "cm");
        frenchTranslationMap.put("centimetre", "cm");

        // TIME
        frenchTranslationMap.put("an", "year");
        frenchTranslationMap.put("année", "years");
        frenchTranslationMap.put("annee", "years");
        frenchTranslationMap.put("mois", "month");
        frenchTranslationMap.put("semaine", "week");
        frenchTranslationMap.put("semaines", "weeks");
        frenchTranslationMap.put("jour", "day");
        frenchTranslationMap.put("journée", "days");
        frenchTranslationMap.put("journee", "days");
        frenchTranslationMap.put("heure", "hour");
        frenchTranslationMap.put("heures", "hours");
        frenchTranslationMap.put("minutes", "minutes");
        frenchTranslationMap.put("seconde", "second");

        // WEIGHT
        frenchTranslationMap.put("tonne", "ton");
        frenchTranslationMap.put("tonnes", "tons");
        frenchTranslationMap.put("kilogramme", "kg");
        frenchTranslationMap.put("livre", "pounds");
        frenchTranslationMap.put("once", "ounce");
        frenchTranslationMap.put("gramme", "gram");

        // AREA
        frenchTranslationMap.put("mile carré", "square mile");
        frenchTranslationMap.put("mètres carrés", "square meter");
        frenchTranslationMap.put("hectares", "hectares");

        // VOLUME
        frenchTranslationMap.put("gallone", "gallon");

        // POWER
        frenchTranslationMap.put("cheval-vapeur", "horsepower");

        // ENERGY
        frenchTranslationMap.put("wattheure", "watt hour");
        frenchTranslationMap.put("wattheures", "watt hours");

        // ROTATION_SPEED
        frenchTranslationMap.put("tours par minute", "rpm");

        // ELECTRIC_CHARGE
        frenchTranslationMap.put("kilowattheure", "kwh");
        frenchTranslationMap.put("kilowattheures", "kwh");
        frenchTranslationMap.put("ampère-heure", "Ah");
        frenchTranslationMap.put("ampère-heures", "Ah");

        unitTranslations.put(Language.FRENCH, frenchTranslationMap);

        // create the length sorted keymap
        for (Map.Entry<Language, Map<String, String>> languageMapEntry : unitTranslations.entrySet()) {
            Language language = languageMapEntry.getKey();
            Map<String, String> languageMappings = languageMapEntry.getValue();
            List<String> keySet = new ArrayList<>(languageMappings.keySet());
            keySet.sort(StringLengthComparator.INSTANCE);
            lengthSortedLanguageKeys.put(language, keySet);
        }
    }

    public static String translate(String unitString, Language language) {
        Map<String, String> translationMap = unitTranslations.get(language);

        if (translationMap != null) {
            String translation = translationMap.get(unitString.toLowerCase());
            if (translation != null) {
                return translation;
            }
        }

        // if we could not find a translation we leave it as it is
        return unitString;
    }

    /**
     * For a given input string all occurences of langauge dependant units are translated to english and replaced by the english version
     */
    public static String translateUnitsOfInput(String inputString, Language language) {
        List<String> keys = lengthSortedLanguageKeys.get(language);
        if (keys == null) {
            return inputString;
        }
        inputString = inputString.toLowerCase();
        for (String key : keys) {
            Pattern pattern = PatternHelper.compileOrGet("(?<=[\\d^\\s])(" + key.toLowerCase() + ")(?=($|\\s|[^A-Za-z0-9]))");
            Matcher matcher = pattern.matcher(inputString);
            if(matcher.find()) {
                inputString = matcher.replaceFirst(unitTranslations.get(language).get(key));
                break;
            }
        }
        return inputString;
    }
}
