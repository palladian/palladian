package tud.iir.extraction.fact;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.extraction.ExtractionType;
import tud.iir.extraction.PageAnalyzer;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.StringHelper;
import tud.iir.helper.Tokenizer;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.RegExp;
import tud.iir.knowledge.Source;
import tud.iir.persistence.IndexManager;
import tud.iir.web.Crawler;

/**
 * The fact extraction decision tree creates a DOM of a given mark up and searches for a given attribute depending on where the attribute is, a decision about
 * the corresponding value will be made. e.g. whether attribute is in a table or in free text <img src="factExtractionDecisionTree.png" width="300" height="100"
 * />
 * 
 * @author David Urbansky
 */
public class FactExtractionDecisionTree {

    private Entity entity;
    private Attribute attribute;
    private int wordsForPhrase = 6; // how many words must a node have to consider it free text
    private PageAnalyzer pa = null;
    private Document document = null;

    public FactExtractionDecisionTree(Entity entity, String url) {
        init(entity, url, null);
    }

    public FactExtractionDecisionTree(Entity entity, String url, Attribute attribute) {
        init(entity, url, attribute);
    }

    private void init(Entity entity, String url, Attribute attribute) {
        setEntity(entity);
        setAttribute(attribute);

        pa = new PageAnalyzer();
        Crawler crawler = new Crawler();
        document = crawler.getWebDocument(url);
    }

    public void setDocument(String url) {
        Crawler crawler = new Crawler();
        document = crawler.getWebDocument(url);
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    /**
     * Run the decision tree and find the string where the fact value for the given attribute is most likely to be found extract the value and add it to the
     * entity facts (fact values).
     * 
     * @param attribute The initial attribute.
     * @return All strings with the values for the given attribute.
     */
    public HashMap<Attribute, ArrayList<FactString>> getFactStrings(Attribute attribute) {

        HashMap<Attribute, ArrayList<FactString>> attributesFactStrings = new HashMap<Attribute, ArrayList<FactString>>();

        ArrayList<FactString> factStrings = new ArrayList<FactString>();

        // do nothing if document couldn't be created
        if (document == null) {
            return attributesFactStrings;
        }

        // keep track of all xpath that have been evaluated, do not evaluate one xpath twice
        // this could happen when the parent node is searched for /div/p/a[3] and /div/p/a[4] => /div/p would be taken twice
        // this counts two times for the same value which is not eligible
        HashSet<String> xpathEvaluatedForPage = new HashSet<String>();

        String lowerCaseAttributeName = getAttribute().getName().toLowerCase();

        // find xpaths to the attribute (can be several)
        HashSet<String> xpaths = pa.constructAllXPaths(document, lowerCaseAttributeName);
        Iterator<String> xpathIterator = xpaths.iterator();
        while (xpathIterator.hasNext()) {

            String currentXPath = xpathIterator.next();
            if (!xpathEvaluatedForPage.add(currentXPath)) {
                continue; // do not evaluate xpath twice
            }

            String xPathText = pa.getTextByXPath(document, currentXPath).trim();

            // catch error
            if (xPathText.equals("#error#")) {
                return attributesFactStrings;
            }

            // find out whether attribute location is in a table (look back maximum 4 tags)
            boolean nodeInTable = pa.nodeInTable(currentXPath, 4);

            // more than wordsForPhrase words
            boolean considerItFreeText = StringHelper.countWords(xPathText) >= wordsForPhrase;

            // if word in table count words of cell
            if (nodeInTable && !considerItFreeText) {
                String wordsInCell = pa.getTextByXPath(document, pa.getTableCellPath(currentXPath));
                considerItFreeText = StringHelper.countWords(wordsInCell) >= wordsForPhrase;
            }

            // check if there is a colon ":" and some text after the attribute
            int indexOfColon = colonIndexAfterAttribute(xPathText);
            int indexOfAttribute = xPathText.toLowerCase().indexOf(lowerCaseAttributeName);

            boolean textAfterColon = false;
            if (indexOfColon > -1 && StringHelper.trim(xPathText, ":").length() > indexOfColon + 1) {
                textAfterColon = true;
            }

            // check all text after colon if it is in a table cell
            if (nodeInTable && indexOfColon > -1) {
                String wordsInCell = pa.getTextByXPath(document, pa.getTableCellPath(currentXPath));
                int indexOfColonInTableCell = colonIndexAfterAttribute(wordsInCell);
                if (StringHelper.trim(wordsInCell, ":").length() > indexOfColonInTableCell + 1) {
                    textAfterColon = true;
                }
            }

            // decision tree, find the corresponding string for the attribute, extract fact value from the found content area in the end

            // the area where the answer for the attribute is expected
            String currentFactStringText = "";

            // the type of the way the attribute has been found (in sentence or in table)
            int currentDecisionType = ExtractionType.FREE_TEXT_SENTENCE;

            // attribute in table, not more than wordsForPhrase words => take sibling content (move up tags if no content is found)
            if (nodeInTable && !considerItFreeText && (!textAfterColon || indexOfColon == -1)) {
                String siblingXPath = pa.getNextTableCell(currentXPath);
                currentFactStringText = pa.getTextByXPath(document, siblingXPath);

                int maxTags = 4; // move up max. 4 tags but do not step out of table cell
                while (currentFactStringText.length() == 0 && maxTags > 0) {
                    siblingXPath = PageAnalyzer.getParentNode(siblingXPath);
                    if (pa.getTargetNode(siblingXPath).equalsIgnoreCase("tr")) {
                        break;
                    }
                    currentFactStringText = pa.getTextByXPath(document, siblingXPath);
                    --maxTags;
                }

                if (!xpathEvaluatedForPage.add(siblingXPath)) {
                    currentFactStringText = "";
                }

                currentDecisionType = ExtractionType.TABLE_CELL;

                // // find other attributes and fact values in the current table (only for vertical tables)
                if (ExtractionProcessManager.isFindNewAttributesAndValues() && currentFactStringText.length() > 0) {
                    // get all attribute and value xPath for all rows of the table
                    ArrayList<String[]> rowXPaths = new ArrayList<String[]>();
                    rowXPaths = pa.getTableRows(document, currentXPath, siblingXPath);

                    // extract attributes and values in each row
                    int rowCount = rowXPaths.size();
                    for (int i = 0; i < rowCount; i++) {
                        String newAttributeName = pa.getTextByXPath(document, rowXPaths.get(i)[0]);
                        newAttributeName = StringHelper.removeBrackets(newAttributeName);
                        newAttributeName = StringHelper.trim(newAttributeName);

                        // TODO check whether this and the following code works, are sources added? (also in colon attribute extraction)
                        // check whether attribute exists for entity / concept of entity, if so, it will be or has been searched separately
                        if (attribute.getConcept().hasAttribute(newAttributeName, true)) {
                            continue;
                        }

                        // skip the initial attribute
                        if (newAttributeName.equalsIgnoreCase(attribute.getName()) || newAttributeName.length() == 0) {
                            continue;
                        }

                        String newAttributeFactStringText = pa.getTextByXPath(document, rowXPaths.get(i)[1]);

                        // remove values in brackets and trim
                        newAttributeFactStringText = StringHelper.trim(StringHelper.removeBrackets(newAttributeFactStringText));

                        // do not extract attributes for facts without a value
                        if (newAttributeFactStringText.length() == 0) {
                            continue;
                        }

                        int newAttributeValueType = Attribute.guessValueType(newAttributeFactStringText, 1);
                        FactString newAttributeFS = new FactString(newAttributeFactStringText, ExtractionType.TABLE_CELL);
                        ArrayList<FactString> newAttributeFactStrings = new ArrayList<FactString>();
                        newAttributeFactStrings.add(newAttributeFS);
                        Attribute newAttribute = new Attribute(newAttributeName, newAttributeValueType, attribute.getConcept());
                        newAttribute.setExtractedAt(new Date(System.currentTimeMillis()));

                        // source
                        Source currentSource = new Source(document.getDocumentURI(), ExtractionType.TABLE_CELL);

                        // check whether attribute exists already
                        Attribute existingAttribute = null;
                        Iterator<Attribute> attributeCheckIterator = attributesFactStrings.keySet().iterator();
                        while (attributeCheckIterator.hasNext()) {
                            Attribute a = attributeCheckIterator.next();
                            if (a.getName().equalsIgnoreCase(newAttributeName)) {
                                existingAttribute = a;
                            }
                        }
                        if (existingAttribute != null) {
                            attributesFactStrings.get(existingAttribute).add(newAttributeFS);
                            existingAttribute.addSource(currentSource);
                        } else {
                            attributesFactStrings.put(newAttribute, newAttributeFactStrings);
                            newAttribute.addSource(currentSource);
                            Logger.getRootLogger().info(
                                    "New attribute found in TABLE: " + newAttribute.getName() + " (" + newAttribute.getValueTypeName()
                                            + "), first possible value: " + newAttributeFactStrings);
                        }
                    }
                }

                // more than wordsForPhrase words => take text after column (if there is one) until end of sentence
                // } else if (considerItFreeText || (!considerItFreeText && indexOfColumn > -1)) {
            } else {

                if (indexOfColon > -1) {
                    currentFactStringText = getTextAfterColon(currentXPath, lowerCaseAttributeName, indexOfColon);
                    currentDecisionType = ExtractionType.COLON_PHRASE;

                    // // find other attributes and fact values in colon patterns in the neighborhood
                    if (ExtractionProcessManager.isFindNewAttributesAndValues() && currentFactStringText.length() > 0) {

                        String pageText = pa.getTextByXPath(document, "//BODY");
                        int attributeColonIndex = pageText.toLowerCase().indexOf(lowerCaseAttributeName + ":");
                        if (attributeColonIndex == -1) {
                            attributeColonIndex = pageText.toLowerCase().indexOf(lowerCaseAttributeName + " :");
                        }
                        String neighborhood = pageText
                                .substring(Math.max(0, attributeColonIndex - 400), Math.min(pageText.length(), attributeColonIndex + 400));

                        int colonIndex = neighborhood.indexOf(":");
                        while (colonIndex > -1) {

                            Pattern cp = Pattern.compile(RegExp.getRegExp(Attribute.VALUE_STRING) + ":$");
                            Matcher cpm = cp.matcher(neighborhood.substring(Math.max(0, colonIndex - 30), colonIndex + 1));
                            // System.out.println("String before colon: "+neighborhood.substring(Math.max(0,colonIndex-30),colonIndex+1));
                            String newAttributeName = "";
                            while (cpm.find()) {
                                // System.out.println((i++)+" "+cpm.group());
                                newAttributeName = cpm.group();
                            }

                            int nextColonIndex = neighborhood.indexOf(":", colonIndex + 1);
                            newAttributeName = StringHelper.removeBrackets(newAttributeName);
                            newAttributeName = StringHelper.trim(newAttributeName);

                            if (newAttributeName.length() > 0 && !newAttributeName.equalsIgnoreCase(attribute.getName())) {

                                int nextLookOut = nextColonIndex;
                                if (nextColonIndex == -1) {
                                    nextLookOut = colonIndex + 61;
                                }

                                // System.out.println("==> "+newAttributeName+":"+value);

                                String newAttributeFactStringText = StringHelper.trim(neighborhood.substring(colonIndex + 1, Math.min(neighborhood.length(),
                                        nextLookOut)));

                                // remove values in brackets and trim
                                newAttributeFactStringText = StringHelper.trim(StringHelper.removeBrackets(newAttributeFactStringText));

                                // do not extract attributes for facts without a value
                                if (newAttributeFactStringText.length() > 0) {
                                    int newAttributeValueType = Attribute.guessValueType(newAttributeFactStringText, 2);
                                    FactString newAttributeFS = new FactString(newAttributeFactStringText, ExtractionType.COLON_PHRASE);
                                    ArrayList<FactString> newAttributeFactStrings = new ArrayList<FactString>();
                                    newAttributeFactStrings.add(newAttributeFS);
                                    Attribute newAttribute = new Attribute(newAttributeName, newAttributeValueType, attribute.getConcept());
                                    newAttribute.setExtractedAt(new Date(System.currentTimeMillis()));

                                    // source
                                    Source currentSource = new Source(document.getDocumentURI(), ExtractionType.COLON_PHRASE);

                                    // // check whether attribute exists already
                                    // check whether attribute exists for entity / concept of entity, if so, it will be or has been searched separately
                                    Attribute foundAttribute = attribute.getConcept().getAttribute(newAttributeName);
                                    if (foundAttribute == null) {

                                        // check whether attribute exists in current extraction
                                        Attribute existingAttribute = null;
                                        Iterator<Attribute> attributeCheckIterator = attributesFactStrings.keySet().iterator();
                                        while (attributeCheckIterator.hasNext()) {
                                            Attribute a = attributeCheckIterator.next();
                                            if (a.getName().equalsIgnoreCase(newAttributeName)) {
                                                existingAttribute = a;
                                            }
                                        }

                                        if (existingAttribute != null) {
                                            attributesFactStrings.get(existingAttribute).add(newAttributeFS);
                                            existingAttribute.addSource(currentSource);
                                        } else {
                                            attributesFactStrings.put(newAttribute, newAttributeFactStrings);
                                            newAttribute.addSource(currentSource);
                                            Logger.getRootLogger().info(
                                                    "New attribute found in COLON PATTERN: " + newAttribute.getName() + " (" + newAttribute.getValueTypeName()
                                                            + "), first possible value: " + newAttributeFactStrings);
                                        }
                                    } else if (foundAttribute.isExtracted()) {
                                        foundAttribute.addSource(currentSource);
                                    }
                                }
                            }

                            colonIndex = nextColonIndex;
                        }
                    }

                } else if (considerItFreeText) {
                    // take the whole sentence
                    currentFactStringText = pa.getTextByXPath(document, currentXPath);
                    currentFactStringText = Tokenizer.getSentence(currentFactStringText, indexOfAttribute);
                    // do not take facts from questions (end with "?")
                    if (currentFactStringText.endsWith("?")) {
                        currentFactStringText = "";
                    }
                    currentDecisionType = ExtractionType.FREE_TEXT_SENTENCE;
                }

                // attribute is not in a table, not more than wordsForPhrase words and no column was found => move up nodes and find text after node
            }
            // else if (!nodeInTable && !considerItFreeText && indexOfColumn == -1) {
            // String shortenedXPath = pa.findLastBoxSection(currentXPath); // TODO get parent node only (loop)
            //				
            // if (xpathEvaluatedForPage.add(shortenedXPath)) {
            // currentFactStringText = pa.getTextByXpath(document, shortenedXPath);
            // int newIndexOfAttribute = currentFactStringText.toLowerCase().indexOf(lowerCaseAttributeName);
            // if (newIndexOfAttribute > -1) {
            // currentFactStringText =
            // StringHelper.getPhraseToEndOfSentence(currentFactStringText.substring(newIndexOfAttribute+lowerCaseAttributeName.length()));
            // // do not take facts from questions (end with "?")
            // if (currentFactStringText.endsWith("?")) currentFactStringText = "";
            // }
            // } else {
            // currentFactStringText = "";
            // }
            //								
            // currentDecisionType = ExtractionType.STRUCTURED_PHRASE;
            // }

            FactString factString = new FactString(currentFactStringText, currentDecisionType);
            factStrings.add(factString);
        }

        attributesFactStrings.put(attribute, factStrings);
        return attributesFactStrings;
    }

    /**
     * Find out whether there is a ":" or "=" after the attribute, which would indicate that the value is after this character.
     * 
     * @param text The search text.
     * @return True if there is such a character, else false.
     */
    private int colonIndexAfterAttribute(String text) {
        String lowerCaseAttributeName = getAttribute().getName().toLowerCase();

        boolean colon;
        int indexOfAttribute = text.toLowerCase().indexOf(lowerCaseAttributeName);

        // colon is maximum 2 characters away
        int indexOfColon = text.indexOf(":");
        int distance = indexOfColon - (indexOfAttribute + lowerCaseAttributeName.length());
        colon = distance >= 0 && distance < 3;

        if (colon) {
            return indexOfColon;
        }
        return -1;
    }

    /**
     * Move up in the DOM tree until there is text after the ":" or "=".
     * 
     * @param text The search text.
     * @return The text after the colon until the end of sentence.
     */
    private String getTextAfterColon(String currentXPath, String attributeName, int indexOfColon) {

        String text = "";

        boolean textAfterColon = false;
        if (text.length() > indexOfColon + 1) {
            textAfterColon = true;
        }

        int c = 0;
        while (!textAfterColon) {
            String shortenedXPath = PageAnalyzer.getParentNode(currentXPath);
            text = pa.getTextByXPath(document, shortenedXPath);
            currentXPath = shortenedXPath;
            if (text != null) {
                int attributeIndex = text.toLowerCase().indexOf(attributeName);
                indexOfColon = text.indexOf(":", attributeIndex);
                if (text.length() > indexOfColon + 1) {
                    textAfterColon = true;
                }
            }
            ++c;
            if (c >= 10) {
                break;
            }
        }

        // take text after colon
        text = Tokenizer.getPhraseToEndOfSentence(text.substring(indexOfColon + 1));

        return text;
    }

    // TODO delete (is just a copy for testing)
    /*
     * private void extractValue(Entity entity, FactString factString, Attribute attribute) { String searchString = factString.getFactString(); // clean up the
     * search string searchString = StringHelper.unescapeHTMLEntities(searchString); // entity from search string as it is also a noun and might be matched
     * searchString = searchString.replaceAll("(?<=(\\.|,|(\\W)|(\\A)|(\\Z)|\\s))(?i)"+entity.getName()+"(?=(\\.|,|(\\W)|(\\Z)|\\s))",""); // attribute appears
     * in sentence, to determine the distance between the potential fact value get the index of the attribute int attributeIndex =
     * searchString.toLowerCase().indexOf(attribute.getName().toLowerCase()); // replace all instances of the attribute searchString =
     * searchString.replaceAll("(?<=(\\.|,|(\\W)|(\\A)|(\\Z)|\\s))(?i)"+attribute.getName()+"(?=(\\.|,|(\\W)|(\\Z)|\\s))",""); // remove any information from
     * brackets "()" and "[]" they often hold additional information that might be matched which is not wanted //searchString =
     * searchString.replaceAll("\\(.*\\)","").replaceAll("\\[.*\\]",""); //searchString = StringHelper.removeStopWords(searchString); // trim string length to
     * 40 characters do not cut in between words // if (searchString.length() > 40) { // int nextSpaceIndex = searchString.indexOf(" ",40); // if
     * (nextSpaceIndex > -1) // searchString = searchString.substring(0,nextSpaceIndex); // } // if (attribute.getValueType() == Attribute.VALUE_NUMBER) { //
     * searchString = searchString.replaceAll(RegExp.getRegExp(Attribute.VALUE_DATE),""); // } java.util.regex.Pattern pat =
     * java.util.regex.Pattern.compile(attribute.getRegExp()); Matcher m = pat.matcher(searchString);
     * Logger.getInstance().log("try to match "+attribute.getRegExp()+" on "+searchString,true); m.region(0,searchString.length()); String afterValueText = "";
     * // for numbers this text can hold information about units // search string have different types depending on which way they have been extracted, handle
     * them accordingly if (factString.getExtractionType() == ExtractionType.FREE_TEXT_SENTENCE || factString.getExtractionType() == ExtractionType.UNKNOWN) {
     * // for strings, numbers and dates take the appearance closest to the attribute int shortestDistance = -1; String shortestDistanceValue = ""; // the value
     * that was closest to the attribute while (m.find()) { int distance = (int)Math.abs(m.start()+Math.round((m.group().length())/2.0) - attributeIndex); if
     * (distance < shortestDistance || shortestDistance == -1) { shortestDistance = distance; shortestDistanceValue = m.group(); afterValueText =
     * searchString.substring(m.end(),Math.min(searchString.length(),m.end()+26)); }
     * //System.out.println("found in sentence ("+factString.getType()+") "+m.group
     * ()+" (closest so far: "+shortestDistanceValue+") after value:"+afterValueText+distance+" "+attributeIndex+" "+shortestDistance);
     * Logger.getInstance().log(
     * "found in sentence ("+factString.getExtractionType()+") "+m.group()+" (closest so far: "+shortestDistanceValue+") after value:"+afterValueText
     * +distance+" "+attributeIndex+" "+shortestDistance,true); } // add fact candidate for entity and attribute, checking whether fact or fact value has been
     * entered already is done in the entity and fact class respectively //entity.addFactAndValue(new Fact(attribute),new FactValue(shortestDistanceValue,new
     * Source(Source.SEMI_STRUCTURED,getCurrentSource()))); addFactValue(entity,attribute,shortestDistanceValue,factString.getExtractionType(),afterValueText);
     * } else if (factString.getExtractionType() == ExtractionType.PATTERN_PHRASE || factString.getExtractionType() == ExtractionType.COLON_PHRASE ||
     * factString.getExtractionType() == ExtractionType.STRUCTURED_PHRASE || factString.getExtractionType() == ExtractionType.TABLE_CELL) { if
     * (searchString.length() > 26) { int nextSpaceIndex = searchString.indexOf(" ",26); if (nextSpaceIndex > -1) searchString =
     * searchString.substring(0,nextSpaceIndex); } // // if fact value type is string, the found string is short and in a table it might be belong all to the
     * fact so take everything // if (attribute.getValueType() == Attribute.VALUE_STRING && searchString.length() < 40 && factString.getType() ==
     * ExtractionType.TABLE_CELL) { // // // add fact candidate for entity and attribute, checking whether fact or fact value has been entered already is done
     * in the entity and fact class respectively // Logger.getInstance().log("found in phrase (take everything) ("+factString.getType()+") "+searchString,true);
     * // //entity.addFactAndValue(new Fact(attribute),new FactValue(searchString,new Source(Source.SEMI_STRUCTURED,getCurrentSource()))); //
     * addFactValue(entity,attribute,searchString,factString.getType()); // // // } else // for strings, numbers, booleans and dates take the first appearance
     * only if (m.find()) { if (m.end() < searchString.length()) afterValueText = searchString.substring(m.end(),Math.min(searchString.length(),m.end()+26)); //
     * add fact candidate for entity and attribute, checking whether fact or fact value has been entered already is done in the entity and fact class
     * respectively Logger.getInstance().log("found in phrase ("+factString.getExtractionType()+") "+m.group()+" after value:"+afterValueText,true);
     * //entity.addFactAndValue(new Fact(attribute),new FactValue(m.group(),new Source(Source.SEMI_STRUCTURED,getCurrentSource())));
     * addFactValue(entity,attribute,m.group(),factString.getExtractionType(),afterValueText); } } } // TODO also delete private void addFactValue(Entity
     * entity, Attribute attribute, String factString, int extractionType, String unitText) { // prepare fact String to insert as a fact value (and for fact
     * value comparison) factString = StringHelper.removeStopWords(factString); factString = StringHelper.trim(factString); // if fact value is supposed to be a
     * number, normalize it before inserting if (attribute.getValueType() == Attribute.VALUE_NUMERIC) { // make string a parsable (double) number try {
     * factString = StringNormalizer.normalizeNumber(factString); } catch (NumberFormatException e) {
     * FactExtractor.getInstance().getLogger().logError(factString,e); } Logger.getInstance().log("number before unit normalization "+factString); String bn =
     * factString; // normalize units when given if (factString.length() > 0) { try { factString =
     * String.valueOf(UnitNormalizer.getNormalizedNumber(Double.valueOf(factString),unitText)); // make it a normalized string again (no .0) factString =
     * StringNormalizer.normalizeNumber(factString); Logger.getInstance().log("number after unit normalization "+factString); } catch (NumberFormatException e)
     * { Logger.getInstance().logError(factString, e); } } // if value is a date normalize it to UTC YYYY-MM-DD format } else if (attribute.getValueType() ==
     * Attribute.VALUE_DATE) { factString = DateNormalizer.normalizeDate(factString); } if (factString.length() > 0) { //if (extractionType !=
     * ExtractionType.COLON_PHRASE) entity.addFactAndValue(new Fact(attribute),new FactValue(factString,new Source("",extractionType),extractionType)); } } //
     * TODO delete public void dump(String pageString) { try { DOMParser parser = new DOMParser(); InputSource is = new InputSource(new BufferedInputStream(new
     * FileInputStream("data/test/callingCodes3.html"))); parser.parse(is);//"http://www.mobileburn.com/review.jsp?Id=4993");
     * //parser.parse("http://www.countrycallingcodes.com/"); document = parser.getDocument(); // String pageString1 =
     * FileHelper.readHTMLFileToString("data/test/callingCodes3.html",false); // DOMParser domParser = new DOMParser(); // StringReader stringReader = new
     * StringReader(pageString1); // InputSource is = new InputSource(stringReader); // domParser.parse(is); // document = domParser.getDocument(); } catch
     * (FileNotFoundException e) { e.printStackTrace(); } catch (SAXException e) { e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); } try {
     * DOMParser domParser = new DOMParser(); StringReader stringReader = new StringReader(pageString); InputSource is = new InputSource(stringReader);
     * domParser.parse(is); document = domParser.getDocument(); } catch (SAXException e) { e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); }
     * }
     */
    /**
     * @param args
     */
    public static void main(String[] args) {

        // Document document = null;
        // PageAnalyzer pa = new PageAnalyzer();
        //		
        // long st = System.currentTimeMillis();
        // document = pa.getDocument("http://www.factmonster.com/us.html");
        // //document = pa.getDocument("data/test/3180.html",true);
        //		
        // long et = System.currentTimeMillis();
        // System.out.println(document.getFirstChild().getNodeName()+" "+(et-st));

        //		

        // String pageString = c.download("http://www.mobileburn.com/review.jsp?Id=4993");
        // TODO namespace support: http://www.rte.ie/news/2005/0503/esri.html
        // TODO dom not supported exception: http://www.letsgodigital.org/en/19496/pentax-optio-m60/
        // String pageString = c.download("http://wikitravel.org/en/Australia");

        // System.out.println(pageString.length());

        // String pageString = FileHelper.readFileToString("data/test/australiaFactBook.html"); // "data/test/reviewPage.html"

        // KnowledgeManager knowledgeManager = new KnowledgeManager();
        // knowledgeManager.createBenchmarkConcepts();
        Concept concept = new Concept("test Concept");
        Entity e = new Entity("2006 Bugatti Veyron 16.4", concept);

        ExtractionProcessManager.setBenchmarkSet(ExtractionProcessManager.YAHOO_8);
        FactExtractionDecisionTree dt = new FactExtractionDecisionTree(e, IndexManager.getInstance().getIndexPath() + "/website2205.html");
        // FactExtractionDecisionTree dt = new FactExtractionDecisionTree(e,pageString);

        // Attribute currentAttribute = new Attribute("memory",Attribute.VALUE_NUMBER);
        // Attribute currentAttribute = new Attribute("capital",Attribute.VALUE_STRING,concept);
        Attribute currentAttribute = new Attribute("genre", Attribute.VALUE_NUMERIC, concept);
        // Attribute currentAttribute = new Attribute("death rate",Attribute.VALUE_MIXED);

        dt.setAttribute(currentAttribute);
        // ArrayList<FactString> factStrings = dt.getFactStrings();
        HashMap<Attribute, ArrayList<FactString>> factStrings = dt.getFactStrings(currentAttribute);

        // extract the fact values from each of the strings for the current attribute
        Iterator<Map.Entry<Attribute, ArrayList<FactString>>> factIterator = factStrings.entrySet().iterator();
        while (factIterator.hasNext()) {
            Map.Entry<Attribute, ArrayList<FactString>> currentEntry = factIterator.next();
            System.out.println("\n" + currentEntry.getKey().getName() + " (" + currentEntry.getKey().getValueTypeName() + ")");
            CollectionHelper.print(currentEntry.getValue());
        }

        System.out.println("\n" + factStrings.size() + " facts extracted");

        // for (int i = 0,l = factStrings.size(); i < l; ++i) {
        // System.out.println("fact string: "+factStrings.get(i).getFactString());
        // dt.extractValue(e, factStrings.get(i), currentAttribute);
        // }

        // try to find all attributes
        // Iterator<Attribute> attributeIterator = attributes.iterator();
        // while (attributeIterator.hasNext()) {
        // Attribute currentAttribute = attributeIterator.next();
        //			
        // dt.setAttribute(currentAttribute);
        // ArrayList<String> factStrings = dt.getFactStrings();
        //			
        // // extract the fact values from each of the strings for the current attribute
        // for (int i = 0,l = factStrings.size(); i < l; ++i) {
        // extractValue(entity, factStrings.get(i), currentAttribute);
        // }
        //				
        // }
    }

}