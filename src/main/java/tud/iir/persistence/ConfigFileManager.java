package tud.iir.persistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import tud.iir.control.Controller;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.knowledge.Source;

/**
 * The ConfigFileManager manages a config file.
 * 
 * @author David Urbansky
 */
class ConfigFileManager {

    private static String FORMATS_FILE = Controller.getConfig().getString("ontology.formats");
    private static String SOURCES_FILE = Controller.getConfig().getString("ontology.predefinedSources");

    public static ArrayList<Format> getFormats() {

        ArrayList<Format> formats = new ArrayList<Format>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document configFile = builder.parse(FORMATS_FILE);

            XPath xpath = new DOMXPath("//format");

            // System.out.println( "XPah:h " + xpath + configFile.getFirstChild().getNodeName());

            List<Node> results = xpath.selectNodes(configFile);
            System.out.println(results.size());
            Iterator<Node> formatIterator = results.iterator();

            while (formatIterator.hasNext()) {
                Node n = formatIterator.next();
                List<Node> parameterNodes;
                String concept = "";

                xpath = new DOMXPath("@concept");
                parameterNodes = xpath.selectNodes(n);
                concept = parameterNodes.get(0).getTextContent();
                System.out.println(concept + "___");

                xpath = new DOMXPath("description");
                List<Node> descriptions = xpath.selectNodes(n);

                Iterator<Node> descriptionsIterator = descriptions.iterator();
                while (descriptionsIterator.hasNext()) {
                    Node descriptionNode = descriptionsIterator.next();

                    String attribute = "";
                    String description = "";

                    xpath = new DOMXPath("@attribute");
                    parameterNodes = xpath.selectNodes(descriptionNode);
                    attribute = parameterNodes.get(0).getTextContent();

                    description = descriptionNode.getTextContent();

                    Format format = new Format(concept, attribute, description);
                    formats.add(format);
                }

                // System.out.println(parameterNodes.get(0).getTextContent());
                // System.out.println(n.getChildNodes().item(1).getTextContent() );
            }

        } catch (JaxenException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (DOMException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (SAXException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (ParserConfigurationException e) {
            Logger.getRootLogger().error(e.getMessage());
        }

        return formats;
    }

    public static ArrayList<PredefinedSource> getPredefinedSources(KnowledgeManager knowledgeManager) {

        ArrayList<PredefinedSource> predefinedSources = new ArrayList<PredefinedSource>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document configFile = builder.parse(SOURCES_FILE);

            XPath xpath = new DOMXPath("//concept");

            List<Node> results = xpath.selectNodes(configFile);
            Iterator<Node> conceptIterator = results.iterator();

            while (conceptIterator.hasNext()) {
                Node n = conceptIterator.next();
                List<Node> parameterNodes;
                String concept = "";

                xpath = new DOMXPath("@name");
                parameterNodes = xpath.selectNodes(n);
                concept = parameterNodes.get(0).getTextContent();

                xpath = new DOMXPath("source");
                List<Node> sourcesForConcept = xpath.selectNodes(n);

                Iterator<Node> sourceIterator = sourcesForConcept.iterator();
                while (sourceIterator.hasNext()) {
                    Node sourceNode = sourceIterator.next();

                    String url = "";
                    String trust = "";

                    xpath = new DOMXPath("@url");
                    parameterNodes = xpath.selectNodes(sourceNode);
                    url = parameterNodes.get(0).getTextContent();

                    xpath = new DOMXPath("@trust");
                    parameterNodes = xpath.selectNodes(sourceNode);
                    trust = parameterNodes.get(0).getTextContent();

                    // collect the attributes that are affected by the source, if none are given all are effected
                    HashSet<String> attributeNames = new HashSet<String>();
                    xpath = new DOMXPath("attribute");
                    List<Node> attributeNodes = xpath.selectNodes(sourceNode);

                    Iterator<Node> attributeIterator = attributeNodes.iterator();
                    while (attributeIterator.hasNext()) {
                        Node attributeNode = attributeIterator.next();
                        attributeNames.add(attributeNode.getTextContent());
                    }
                    if (attributeNames.size() == 0) {
                        Concept c = knowledgeManager.getConcept(concept);
                        if (c != null)
                            attributeNames = c.getAttributeNames();
                    }

                    Source source = new Source(url);
                    source.setTrust(Double.valueOf(trust));
                    PredefinedSource predefinedSoure = new PredefinedSource(source, concept, attributeNames);
                    predefinedSources.add(predefinedSoure);
                }

                // System.out.println(parameterNodes.get(0).getTextContent());
                // System.out.println(n.getChildNodes().item(1).getTextContent() );
            }

        } catch (JaxenException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (DOMException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (SAXException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (ParserConfigurationException e) {
            Logger.getRootLogger().error(e.getMessage());
        }

        return predefinedSources;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        getFormats();
        getPredefinedSources(new KnowledgeManager());
    }
}