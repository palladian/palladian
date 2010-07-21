package tud.iir.extraction.fact;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.extraction.ExtractionType;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;
import tud.iir.helper.Tokenizer;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Fact;
import tud.iir.knowledge.FactValue;
import tud.iir.knowledge.Source;
import tud.iir.multimedia.ExtractedImage;
import tud.iir.multimedia.ImageHandler;
import tud.iir.normalization.DateNormalizer;
import tud.iir.normalization.StringNormalizer;
import tud.iir.normalization.UnitNormalizer;
import tud.iir.persistence.IndexManager;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;

/**
 * The EntityFactExtractionThread extracts facts for one given entity. Therefore, extracting facts can be parallelized
 * on the entity level.
 * 
 * @author David Urbansky
 */
public class EntityFactExtractionThread extends Thread {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(EntityFactExtractionThread.class);

    private Entity entity = null;

    /** temporarily save information about which url is being processed at the moment */
    private String currentSource = "";

    public EntityFactExtractionThread(ThreadGroup threadGroup, String name, Entity entity) {
        super(threadGroup, name);
        this.entity = entity;
    }

    @Override
    public void run() {
        FactExtractor.getInstance().increaseThreadCount();
        long t1 = System.currentTimeMillis();

        // create a fact queries for the entity that tries to extract many attributes from one single page
        FactQuery fq;
        LOGGER.info("### query with type x ###");
        fq = FactQueryFactory.getInstance().createManyAttributesQuery(entity, FactQueryFactory.TYPE_X);
        extractFromFactPages(fq);
        LOGGER.info("### query with type x facts ###");
        fq = FactQueryFactory.getInstance().createManyAttributesQuery(entity, FactQueryFactory.TYPE_X_FACTS);
        extractFromFactPages(fq);
        // logger.log("### query with type x yn ###");
        // fq = FactQueryFactory.getInstance().createFactQueryForManyAttributes(currentEntity,
        // FactQueryFactory.TYPE_X_YN);
        // extractFromFactPages(fq);

        // iterate through all attributes for current concept
        HashSet<Attribute> currentAttributes = entity.getConcept().getAttributes(true);

        // use an array because the iterator throws a concurrent modification exception when a new attribute is added to
        // the KnowledgeManager
        Attribute[] attributeArray = new Attribute[currentAttributes.size()];
        Iterator<Attribute> attributeIterator = currentAttributes.iterator();
        int i = 0;
        while (attributeIterator.hasNext()) {
            attributeArray[i] = attributeIterator.next();
            i++;
        }
        for (i = 0; i < attributeArray.length; i++) {

            Attribute currentAttribute = attributeArray[i];
            currentAttribute.setLastSearched(new Date(System.currentTimeMillis()));

            if (FactExtractor.getInstance().isStopped()) {
                LOGGER.info("fact extraction process stopped");
                break;
            }

            // the free text queries do not apply for booleans, skip these
            if (currentAttribute.getValueType() == Attribute.VALUE_BOOLEAN) {
                continue; // TODO alternative queries? "X has Y"?
            }

            // create a fact query that extracts images
            else if (currentAttribute.getValueType() == Attribute.VALUE_IMAGE) {
                LOGGER.info("### query for images ###");
                fq = FactQueryFactory.getInstance().createImageQuery(entity, currentAttribute);
                LOGGER.info("extract images for " + entity.getName() + " with query " + fq.getAttribute().getName());
                extractImages(fq, entity);

                // create attribute related fact queries
            } else {

                LOGGER.info("### query with type x'y is ###");
                fq = FactQueryFactory.getInstance().createSingleAttributeQuery(entity, currentAttribute,
                        FactQueryFactory.TYPE_X_Y_IS);
                extractFromFactPages(fq);
                LOGGER.info("### query with type y of x is ###");
                fq = FactQueryFactory.getInstance().createSingleAttributeQuery(entity, currentAttribute,
                        FactQueryFactory.TYPE_Y_OF_X_IS);
                extractFromFactPages(fq);
                LOGGER.info("### query with type x y ###");
                fq = FactQueryFactory.getInstance().createSingleAttributeQuery(entity, currentAttribute,
                        FactQueryFactory.TYPE_X_Y);
                extractFromFactPages(fq);
            }
        }

        LOGGER.info("Thread finished in " + DateHelper.getRuntime(t1) + "s, facts for \"" + entity.getName()
                + "\" were sought. " + entity.getFacts().size() + " facts about the entity in total.");
        FactExtractor.getInstance().decreaseThreadCount();
    }

    /**
     * Get the contents of the pages where the fact query matches. Pass entity to specialized extraction algorithms that
     * add facts/fact values to the entity.
     * 
     * @param fq The fact query.
     */
    private void extractFromFactPages(FactQuery fq) {
        Entity entity = fq.getEntity();

        boolean freeTextOnly = fq.getFactStructureType() == FactQuery.FREE_TEXT_ONLY;
        HashSet<String> urls;
        String[] querySet;

        // //////////////////// retrieve from the web /////////////////////
        if (!FactExtractor.getInstance().isBenchmark()) {
            SourceRetriever sr = new SourceRetriever();
            sr.setSource(ExtractionProcessManager.getSourceRetrievalSite());
            sr.setResultCount(ExtractionProcessManager.getSourceRetrievalCount());

            // get all urls for all query strings
            urls = new HashSet<String>();
            querySet = fq.getQuerySet();
            int querySetSize = querySet.length;
            for (int i = 0; i < querySetSize; ++i) {
                List<String> retrievedURLs = sr.getURLs(querySet[i]);

                // filter urls
                retrievedURLs = FactExtractor.getInstance().filterURLs(retrievedURLs);

                urls.addAll(retrievedURLs);
            }
        }
        // //////////////////// retrieve from selected index (for benchmark) /////////////////////
        else {
            String resultID = entity.getName().toLowerCase().replaceAll(" ", "");
            resultID += fq.getQueryType();
            if (fq.targetsManyAttributes()) {
                resultID += "all";
            } else {
                resultID += fq.getAttribute().getName().toLowerCase().replaceAll(" ", "");
            }
            urls = new HashSet<String>();
            querySet = fq.getQuerySet();
            // logger.log("queryset for "+resultID+" "+querySet+" "+querySet.length);
            int querySetSize = querySet.length;
            for (int i = 0; i < querySetSize; ++i) {
                ArrayList<String> retrievedURLs = IndexManager.getInstance().getFromIndex("resultID", resultID);
                // for (int j = 0; j < Math.min(1, retrievedURLs.size()); j++) {
                // urls.add(retrievedURLs.get(j));
                // }
                urls.addAll(retrievedURLs);
            }
        }
        // ///////////////////////////////////////////////////////////////////////

        // get the contents of every retrieved url and apply extraction process
        for (String url : urls) {

            setCurrentSource(url);

            if (FactExtractor.getInstance().isStopped()) {
                LOGGER.info("fact extraction process stopped");
                break;
            }

            // //////////////////// for downloading a selection //////////////////////
            // String resultID = entity.getName().toLowerCase().replaceAll(" ","");
            // resultID += fq.getQueryType();
            // if (fq.targetsManyAttributes()) {
            // resultID += "all";
            // } else {
            // resultID += fq.getAttribute().getName().toLowerCase().replaceAll(" ","");
            // }
            // String path = IndexManager.getInstance().getIndexPath()+"/";
            // String filename = "website"+counter+".html";
            //			
            // // download urls only once but when they are free text queries (because these have not been tested on
            // general query results)
            // if (fq.getFactStructureType() == FactQuery.FREE_TEXT_ONLY || urlsSeenForEntity.add(url)) {
            // if (crawler.downloadAndSave(url,path+filename)) {
            // IndexManager.getInstance().writeIndex(filename, url, resultID);
            // logger.log("download and index "+url+" resultID "+resultID,true);
            // ++counter;
            //					
            // } else {
            // logger.log("error when downloading "+url,true);
            // }
            // } else {
            // logger.log("url has been downloaded already "+url,true);
            // }
            //			
            // ///////////////////////////////////////////////////////////////////////

            LOGGER.info("analyze url: " + url);

            // for reading from index
            if (FactExtractor.getInstance().isBenchmark()) {
                url = IndexManager.getInstance().getIndexPath() + "/" + url;
            }

            // testing with page stripping only
            // plainExtraction(entity, url, fq.getAttributes(), true);
            // System.out.println("url "+url);
            if (freeTextOnly) {
                // for reading from index TODO still working with line breaks?
                String pageString = FileHelper.readHTMLFileToString(url, freeTextOnly);
                // String pageString = crawler.download(url,freeTextOnly); // get the contents, strip html tags only if
                // fact pattern can only be found in free
                // text

                LOGGER.info("apply free text only query on " + url);
                extractFactFromPhrase(entity, querySet, pageString, fq.getAttribute());
                // extractFactsFromPage(entity, url, fq.getAttributes()); // -2% recall if used
            } else {
                LOGGER.info("apply semi structured query on " + url);
                System.out.println("url " + url);
                extractFactsFromPage(entity, url, fq.getAttributes());
            }
        }
    }

    /**
     * Extract fact values that are related to the given pattern. The pattern is supposed to be found in free text, e.g.
     * "the population of Germany is".
     * 
     * @param entity The entity.
     * @param patternSet A set of patterns.
     * @param pageString The content of the page.
     * @param attribute The attribute.
     */
    private void extractFactFromPhrase(Entity entity, String[] patternSet, String pageString, Attribute attribute) {
        int patternSetSize = patternSet.length;
        for (int i = 0; i < patternSetSize; ++i) {

            if (FactExtractor.getInstance().isStopped()) {
                LOGGER.info("fact extraction process stopped");
                break;
            }

            try {
                // remove "" that were entered for search engine
                String currentPattern = StringHelper.escapeForRegularExpression(patternSet[i].replaceAll("\"", ""));

                LOGGER.info("try to match free text query " + patternSet[i]);
                Pattern pat = Pattern.compile(currentPattern, Pattern.CASE_INSENSITIVE);

                Matcher m = pat.matcher(pageString);
                m.region(0, pageString.length());
                while (m.find()) {
                    String searchArea = pageString.substring(m.start() + currentPattern.length(), Math.min(
                            m.start() + 150, pageString.length()));
                    searchArea = Tokenizer.getPhraseToEndOfSentence(searchArea);
                    FactString factString = new FactString(searchArea, ExtractionType.PATTERN_PHRASE);
                    extractValue(entity, factString, attribute);
                }
                LOGGER.info("no more matches found");
            } catch (PatternSyntaxException e) {
                LOGGER.error(patternSet[i] + ", " + e.getMessage());
            }

        }
    }

    /**
     * The plain extraction treats the website as a long string without tags every mention of the attribute will be
     * found and the surrounding text is searched
     * for the attributes regular expression.
     * 
     * @param entity The entity.
     * @param url The url.
     * @param attributes A set of attributes.
     * @param lookBothDirections If true, values are searched in both directions.
     */
    // private void plainExtraction(Entity entity, String url, HashSet<Attribute> attributes, boolean
    // lookBothDirections) {
    //
    // String pageString = FileHelper.readHTMLFileToString(url, true);
    //
    // // try to find all attributes
    // Iterator<Attribute> attributeIterator = attributes.iterator();
    // while (attributeIterator.hasNext()) {
    // Attribute currentAttribute = attributeIterator.next();
    // String lowerCaseAttributeName = currentAttribute.getName().toLowerCase();
    //
    // if (FactExtractor.getInstance().isStopped()) {
    // LOGGER.info("fact extraction process stopped");
    // break;
    // }
    //
    // java.util.regex.Pattern pat = java.util.regex.Pattern.compile(lowerCaseAttributeName,
    // java.util.regex.Pattern.CASE_INSENSITIVE);
    //
    // Matcher m = pat.matcher(pageString);
    // m.region(0, pageString.length());
    // while (m.find()) {
    // int startIndex = m.start();
    // int lookRange = 150;
    // if (lookBothDirections) {
    // startIndex = Math.max(0, startIndex - lookRange);
    // lookRange *= 2;
    // }
    // startIndex += lowerCaseAttributeName.length();
    // // if (entity.getName().equalsIgnoreCase("Sony Ericsson V600") && lowerCaseAttributeName.equals("weight")) break;
    // try {
    // String searchArea = pageString.substring(startIndex, Math.min(startIndex + lookRange, pageString.length()));
    // // searchArea = StringHelper.getPhraseToEndOfSentence(searchArea);
    // // System.out.println("get sentence "+lowerCaseAttributeName+" for "+entity.getName());
    // // searchArea = StringHelper.getSentence(pageString, m.start());
    // FactString factString = new FactString(searchArea, ExtractionType.UNKNOWN);
    // extractValue(entity, factString, currentAttribute);
    // } catch (Exception e) {
    // LOGGER.error("Exception at FactExtractor", e);
    // }
    // }
    // }
    // }

    private void extractImages(FactQuery fq, Entity entity) {
        SourceRetriever sr = new SourceRetriever();
        ArrayList<ExtractedImage> images = new ArrayList<ExtractedImage>();

        int expectedImages = fq.getAttribute().getValueCount();
        sr.setResultCount(15 * expectedImages);

        // keywords that must appear in the image caption
        String[] matchContent = new String[2];
        matchContent[0] = entity.getName();
        if (!fq.getAttribute().getName().equalsIgnoreCase("_entity_image_")) {
            matchContent[1] = fq.getAttribute().getName();
        } else {
            matchContent[1] = "";
        }

        String[] querySet = fq.getQuerySet();
        for (int i = 0; i < querySet.length; i++) {
            images.addAll(sr.getImages(querySet[i], SourceRetrieverManager.GOOGLE, false, matchContent));
        }

        // start image analysis
        String[] imageURLs = ImageHandler.getMatchingImageURLs(images, expectedImages);
        images.clear();

        // download assigned images with name CONVENTION "entitySafeName_attributeSafeName_number.jpg"
        for (int i = 0; i < imageURLs.length; i++) {
            String currentURL = imageURLs[i];
            String imageType = FileHelper.getFileType(currentURL);
            if (imageType.length() == 0) {
                imageType = "jpg";
            }

            String fileName = entity.getSafeName();
            if (!fq.getAttribute().getName().equalsIgnoreCase("_entity_image_")) {
                fileName += "_" + fq.getAttribute().getSafeName();
            }
            fileName += "_" + (i + 1) + "." + imageType;
            FactValue fv = new FactValue(fileName, new Source(currentURL, ExtractionType.IMAGE), ExtractionType.IMAGE);
            fv.setExtractedAt(new Date(System.currentTimeMillis()));
            entity.addFactAndValue(new Fact(fq.getAttribute()), fv);
            ImageHandler.downloadAndSave(currentURL, "data/multimedia/images/" + fileName);
        }
    }

    /**
     * Extract given attribute(s) from page, look in free text and in structures.
     * 
     * @param entity The entity.
     * @param url The URL.
     * @param attributes A set of attributes.
     */
    private void extractFactsFromPage(Entity entity, String url, HashSet<Attribute> attributes) {

        FactExtractionDecisionTree dt = new FactExtractionDecisionTree(entity, url);

        // use an array because the iterator throws a concurrent modification exception when a new attribute is added to
        // the KnowledgeManager
        Attribute[] attributeArray = new Attribute[attributes.size()];
        Iterator<Attribute> attributeIterator = attributes.iterator();
        int i = 0;
        while (attributeIterator.hasNext()) {
            attributeArray[i] = attributeIterator.next();
            attributeArray[i].setTrust(0.25);
            i++;
        }

        // try to find all attributes
        for (i = 0; i < attributeArray.length; i++) {
            Attribute currentAttribute = attributeArray[i];

            if (FactExtractor.getInstance().isStopped()) {
                LOGGER.info("fact extraction process stopped");
                break;
            }

            LOGGER.info("search attribute " + currentAttribute.getName() + " for " + entity.getName());
            dt.setAttribute(currentAttribute);
            HashMap<Attribute, ArrayList<FactString>> factStrings = dt.getFactStrings(currentAttribute);
            LOGGER.info("found " + (factStrings.size() - 1) + " new attribute candidates, " + factStrings.keySet());

            // iterate through all attributes found
            Iterator<Map.Entry<Attribute, ArrayList<FactString>>> factIterator = factStrings.entrySet().iterator();
            while (factIterator.hasNext()) {
                Map.Entry<Attribute, ArrayList<FactString>> currentEntry = factIterator.next();
                Attribute attribute = currentEntry.getKey();
                ArrayList<FactString> attributeFactStrings = currentEntry.getValue();

                System.out.println("\n" + currentEntry.getKey().getName() + " ("
                        + currentEntry.getKey().getValueTypeName() + ") with " + attributeFactStrings.size()
                        + " values");
                // CollectionHelper.print(currentEntry.getValue());

                // extract the fact values from each of the strings for the current attribute
                for (int j = 0, l = attributeFactStrings.size(); j < l; ++j) {

                    // do not try to extract booleans only from sentences
                    if (attribute.getValueType() == Attribute.VALUE_BOOLEAN
                            && attributeFactStrings.get(j).getExtractionType() == ExtractionType.FREE_TEXT_SENTENCE) {
                        continue;
                    }

                    // extract the value
                    extractValue(entity, attributeFactStrings.get(j), attribute);
                }
            }
        }
    }

    /**
     * Try to find the fact value in the given search string and add fact value to the entity.
     * 
     * @param entity The entity for which the facts will be extracted.
     * @param searchString The string where the fact value is expected.
     * @param attribute The attribute that is being searched.
     */
    protected void extractValue(Entity entity, FactString factString, Attribute attribute) {

        // TODO problem with boolean values and more! probably problem in dt because PageAnalyzer changed
        String searchString = factString.getFactString();
        // logger.log("1 try to match on "+searchString,true);

        // clean up the search string
        searchString = StringEscapeUtils.unescapeHtml(searchString);
        // logger.log("2 try to match on "+searchString,true);

        // entity from search string as it is also a noun and might be matched
        try {
            searchString = searchString.replaceAll("(?<=(\\.|,|(\\W)|(\\A)|(\\Z)|\\s))(?i)"
                    + StringHelper.escapeForRegularExpression(entity.getName()) + "(?=(\\.|,|(\\W)|(\\Z)|\\s))", "");
        } catch (PatternSyntaxException e) {
            LOGGER.error(entity.getName(), e);
            return;
        }

        // logger.log("3 try to match on "+searchString,true);

        // attribute appears in sentence, to determine the distance between the potential fact value get the index of
        // the attribute
        int attributeIndex = searchString.toLowerCase().indexOf(attribute.getName().toLowerCase());

        // replace all instances of the attribute
        searchString = searchString.replaceAll("(?<=(\\.|,|(\\W)|(\\A)|(\\Z)|\\s))(?i)"
                + StringHelper.escapeForRegularExpression(attribute.getName()) + "(?=(\\.|,|(\\W)|(\\Z)|\\s))", "");
        // logger.log("4 try to match on "+searchString,true);

        // remove any information from brackets "()" and "[]" they often hold additional information that might be
        // matched which is not wanted
        // searchString = searchString.replaceAll("\\(.*\\)","").replaceAll("\\[.*\\]","");

        // searchString = StringHelper.removeStopWords(searchString); // TODO test here or later, if removed here false
        // extraction can happen, e.g.
        // "Sydney is Australia's largest city" => "Sydney Australia's largest city" => extract "Sydney Australia"
        // because "is" is a stop word

        // trim string length to 40 characters do not cut in between words
        // TODO changes many values examine in more detail!
        // if (searchString.length() > 40) {
        // int nextSpaceIndex = searchString.indexOf(" ",40);
        // if (nextSpaceIndex > -1)
        // searchString = searchString.substring(0,nextSpaceIndex);
        // }

        // if looking for a number, rule out dates first TODO destroys values sometimes
        // if (attribute.getValueType() == Attribute.VALUE_NUMBER) {
        // searchString = searchString.replaceAll(RegExp.getRegExp(Attribute.VALUE_DATE),"");
        // }

        java.util.regex.Pattern pat = null;
        try {
            pat = java.util.regex.Pattern.compile(attribute.getRegExp());
        } catch (PatternSyntaxException e) {
            LOGGER.error("PatternSyntaxException for " + attribute.getName() + " with regExp " + attribute.getRegExp(),
                    e);
            return;
        }
        Matcher m = pat.matcher(searchString);
        LOGGER.info("try to match " + attribute.getRegExp() + " on " + searchString);
        m.region(0, searchString.length());

        String afterValueText = ""; // for numbers this text can hold information about units

        // search string have different types depending on which way they have been extracted, handle them accordingly
        if (factString.getExtractionType() == ExtractionType.FREE_TEXT_SENTENCE
                || factString.getExtractionType() == ExtractionType.UNKNOWN) {

            // for strings, numbers and dates take the appearance closest to the attribute
            int shortestDistance = -1;
            String shortestDistanceValue = ""; // the value that was closest to the attribute
            while (m.find()) {
                int distance = (int) Math.abs(m.start() + Math.round(m.group().length() / 2.0) - attributeIndex);
                if (distance < shortestDistance || shortestDistance == -1) {
                    shortestDistance = distance;
                    shortestDistanceValue = m.group();
                    afterValueText = searchString.substring(m.end(), Math.min(searchString.length(), m.end() + 26));

                }
                // System.out.println("found in sentence ("+factString.getType()+") "+m.group()+" (closest so far: "+shortestDistanceValue+") after value:"+afterValueText/*+distance+" "+attributeIndex+" "+shortestDistance*/);
                LOGGER.info("found in sentence (" + factString.getExtractionType() + ") " + m.group()
                        + " (closest so far: " + shortestDistanceValue + ") after value:" + afterValueText/*
                                                                                                           * +distance+" "
                                                                                                           * +
                                                                                                           * attributeIndex
                                                                                                           * +" "+
                                                                                                           * shortestDistance
                                                                                                           */);
            }

            // add fact candidate for entity and attribute, checking whether fact or fact value has been entered already
            // is done in the entity and fact class
            // respectively
            // entity.addFactAndValue(new Fact(attribute),new FactValue(shortestDistanceValue,new
            // Source(Source.SEMI_STRUCTURED,getCurrentSource())));
            addFactValue(entity, attribute, shortestDistanceValue, factString.getExtractionType(), afterValueText);

        } else if (factString.getExtractionType() == ExtractionType.PATTERN_PHRASE
                || factString.getExtractionType() == ExtractionType.COLON_PHRASE
                || factString.getExtractionType() == ExtractionType.STRUCTURED_PHRASE
                || factString.getExtractionType() == ExtractionType.TABLE_CELL) {

            // if (searchString.length() > 34) {
            // int nextSpaceIndex = searchString.indexOf(" ",34);
            // if (nextSpaceIndex > -1)
            // searchString = searchString.substring(0,nextSpaceIndex);
            // }

            // // if fact value type is string, the found string is short and in a table it might be belong all to the
            // fact so take everything
            // if (attribute.getValueType() == Attribute.VALUE_STRING && searchString.length() < 40 &&
            // factString.getType() == ExtractionType.TABLE_CELL) {
            //        		
            // // add fact candidate for entity and attribute, checking whether fact or fact value has been entered
            // already is done in the entity and fact class
            // respectively
            // logger.log("found in phrase (take everything) ("+factString.getType()+") "+searchString,true);
            // //entity.addFactAndValue(new Fact(attribute),new FactValue(searchString,new
            // Source(Source.SEMI_STRUCTURED,getCurrentSource())));
            // addFactValue(entity,attribute,searchString,factString.getType());
            //    			
            //        	       	
            // } else

            // for strings, numbers, booleans and dates take the first appearance only
            if (m.find()) {

                if (m.end() < searchString.length()) {
                    afterValueText = searchString.substring(m.end(), Math.min(searchString.length(), m.end() + 26));
                }

                // add fact candidate for entity and attribute, checking whether fact or fact value has been entered
                // already is done in the entity and fact
                // class respectively
                LOGGER.info("found in phrase (" + factString.getExtractionType() + ") " + m.group() + " after value:"
                        + afterValueText);
                // entity.addFactAndValue(new Fact(attribute),new FactValue(m.group(),new
                // Source(Source.SEMI_STRUCTURED,getCurrentSource())));
                addFactValue(entity, attribute, m.group(), factString.getExtractionType(), afterValueText);
            }
        }
    }

    // private void addFactValue(Entity entity, Attribute attribute, String factString, int extractionType) {
    // addFactValue(entity, attribute, factString, extractionType, "");
    // }

    private void addFactValue(Entity entity, Attribute attribute, String factString, int extractionType, String unitText) {

        // prepare fact String to insert as a fact value (and for fact value comparison)
        factString = StringHelper.removeStopWords(factString);
        factString = StringHelper.trim(factString);

        // if fact value is supposed to be a number, normalize it before inserting
        if (attribute.getValueType() == Attribute.VALUE_NUMERIC) {

            // make string a parsable (double) number
            try {
                factString = StringNormalizer.normalizeNumber(factString);
            } catch (NumberFormatException e) {
                LOGGER.error(factString + ", " + e.getMessage());
            }

            LOGGER.info("number before unit normalization " + factString);

            // normalize units when given
            if (factString.length() > 0) {
                try {
                    factString = String.valueOf(UnitNormalizer
                            .getNormalizedNumber(Double.valueOf(factString), unitText));

                    // make it a normalized string again (no .0)
                    factString = StringNormalizer.normalizeNumber(factString);
                    LOGGER.info("number after unit normalization " + factString);

                } catch (NumberFormatException e) {
                    LOGGER.error(factString + ", " + e.getMessage());
                }
            }

            // if value is a date normalize it to UTC YYYY-MM-DD format
        } else if (attribute.getValueType() == Attribute.VALUE_DATE) {
            factString = DateNormalizer.normalizeDate(factString);
        }

        if (factString.length() > 0) {
            // if (!(extractionType == ExtractionType.COLON_PHRASE)) {
            FactValue fv = new FactValue(factString, new Source(getCurrentSource(), extractionType), extractionType);
            fv.setExtractedAt(new Date(System.currentTimeMillis()));
            entity.addFactAndValue(new Fact(attribute), fv);
            // }
        }

    }

    public String getCurrentSource() {
        return currentSource;
    }

    public void setCurrentSource(String currentSource) {
        this.currentSource = currentSource;
    }

}