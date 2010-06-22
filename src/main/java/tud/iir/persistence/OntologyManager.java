package tud.iir.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

import tud.iir.control.Controller;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.AttributeRange;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Fact;
import tud.iir.knowledge.FactValue;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.knowledge.Source;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;

/**
 * Read and write the ontology.
 * 
 * @author David Urbansky
 * @author Robert Willner
 */
public class OntologyManager {

    // TODO attributes to concepts new table
    private static OntologyManager instance = null;

    private static final String ONTOLOGY_LOCATION = "http://www.webknox.com/owl/ontology.owl";// "http://www.semanticweb.org/ontologies/2008/8/Ontology1220883274332.owl";
    private static final String NAMESPACE_ONTOLOGY = ONTOLOGY_LOCATION + "#";
    private static final String ONTOLOGY_DATA_LOCATION = "http://www.semanticweb.org/ontologies/2008/8/Ontology1220977436192.owl";
    private static final String ONTOLOGY_DATA_CLEAN_LOCATION = "http://www.semanticweb.org/ontologies/2008/8/Ontology1220939700153.owl";
    private static final String SOURCES_LOCATION = "http://www.semanticweb.org/ontologies/2008/8/Ontology1220977436192.owl";
    private static final String NAMESPACE_SOURCES = SOURCES_LOCATION + "#";

    private static final String DATA_TYPE_NAMESPACE = "http://purl.org/dc/elements/1.1/";
    private static final String ONTOLOGY_LOCATION_LOCAL = Controller.getConfig().getString("ontology.model.local");
    private static final String ONTOLOGY_SOURCES_LOCATION_LOCAL = Controller.getConfig().getString("ontology.sources.local");
    private static final String ONTOLOGY_DATA_LOCATION_LOCAL = Controller.getConfig().getString("ontology.data.local");
    private static final String ONTOLOGY_DATA_CLEAN_LOCATION_LOCAL = Controller.getConfig().getString("ontology.dataClean.local");

    private OntologyManager() {
    }

    public static OntologyManager getInstance() {
        if (instance == null)
            instance = new OntologyManager();
        return instance;
    }

    private OntModel readOntology(String filePath) {
        OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
        m.setNsPrefix(Controller.ID, NAMESPACE_ONTOLOGY);
        OntDocumentManager dm = m.getDocumentManager();
        dm.addAltEntry(ONTOLOGY_LOCATION, "file:" + ONTOLOGY_LOCATION_LOCAL);
        dm.addAltEntry(SOURCES_LOCATION, "file:" + ONTOLOGY_SOURCES_LOCATION_LOCAL);
        dm.addAltEntry(ONTOLOGY_DATA_LOCATION, "file:" + ONTOLOGY_DATA_LOCATION_LOCAL);
        dm.addAltEntry(ONTOLOGY_DATA_CLEAN_LOCATION, "file:" + ONTOLOGY_DATA_CLEAN_LOCATION_LOCAL);
        m.read(filePath);

        return m;
    }

    /**
     * Load the ontology from the standard location. Instantiate all concepts and properties for the KnowledgeManager.
     */
    public KnowledgeManager loadOntology() {

        KnowledgeManager knowledgeManager = new KnowledgeManager();

        // create the ontology model and read the owl file (rdf/xml syntax)
        OntModel m = readOntology(ONTOLOGY_LOCATION);

        // find all root concepts with their synonyms and add them to the KnowledgeManager
        // ExtendedIterator i = m.listHierarchyRootClasses()
        // .filterDrop( new Filter() {
        // @Override
        // public boolean accept( Object o ) {
        // return ((Resource) o).isAnon();
        // }});

        ExtendedIterator i = m.listClasses().filterDrop(new Filter() {
            @Override
            public boolean accept(Object o) {
                return ((Resource) o).isAnon();
            }
        });

        // keep track of concepts already entered and under which other concept (for synonyms)
        // HashMap<String, String> enteredConcept = new HashMap<String, String>();
        while (i.hasNext()) {
            OntClass concept = (OntClass) i.next();
            String conceptName = StringHelper.makeViewName(concept.getLocalName());
            // do not enter "Source" as a concept
            if (conceptName.equalsIgnoreCase("Source")) {
                continue;
            }

            /*
             * boolean conceptEntered = false; String enteredConceptName = ""; // if a synonym is entered, save the concept it is synonym for // find equivalent
             * concepts // check whether one concept of the equivalent concepts has already been entered HashSet<String> equivalentConcepts = new
             * HashSet<String>(); equivalentConcepts = getAllEquivalentConcepts(concept, equivalentConcepts); equivalentConcepts.add(conceptName);
             * Iterator<String> ecIterator = equivalentConcepts.iterator(); while (ecIterator.hasNext()) { String equivalentConceptName = ecIterator.next(); if
             * (enteredConcept.containsKey(equivalentConceptName)) { conceptEntered = true; enteredConceptName = enteredConcept.get(equivalentConceptName); if
             * (enteredConceptName.length() == 0) enteredConceptName = equivalentConceptName; } } // if concept already entered, add all equivalent concepts as
             * synonyms for that... if (conceptEntered) { knowledgeManager.getConcept(enteredConceptName).addSynonym(conceptName); // otherwise add current
             * concept and add all equivalent concepts as synonyms for that } else { // if class has the comment "primary" it is the root concept and other
             * equivalent concepts are synonyms if (concept.getComment(null) != null && concept.getComment(null).equalsIgnoreCase("primary")) {
             * knowledgeManager.addConcept(new Concept(conceptName)); enteredConceptName = conceptName; enteredConcept.put(conceptName, ""); } }
             */

            String primaryConceptName = findPrimaryConcept(concept);
            if (conceptName.equals(primaryConceptName)) {
                Concept primaryConcept = knowledgeManager.getConcept(primaryConceptName);
                if (primaryConcept == null) {
                    knowledgeManager.addConcept(new Concept(conceptName));
                }

                // add all equivalent concepts TODO why are not all equivalent relationships saved in each entity?
                HashSet<OntClass> equivalentConcepts = new HashSet<OntClass>();
                equivalentConcepts = getAllEquivalentConcepts(concept, equivalentConcepts);
                Iterator<OntClass> ecIterator = equivalentConcepts.iterator();
                while (ecIterator.hasNext()) {
                    OntClass equivalentClass = ecIterator.next();
                    String equivalentConceptName = StringHelper.makeViewName(equivalentClass.getLocalName());
                    Concept equivalentConcept = knowledgeManager.getConcept(equivalentConceptName);
                    if (equivalentConcept != null) {
                        knowledgeManager.removeConcept(equivalentConcept);
                    }
                    knowledgeManager.getConcept(primaryConceptName).addSynonym(equivalentConceptName);
                    // System.out.println(equivalentConceptName+" equivalent");
                }

                // otherwise add current concept and add all equivalent concepts as synonyms for that
            } else {
                Concept primaryConcept = knowledgeManager.getConcept(primaryConceptName);
                if (primaryConcept == null) {
                    knowledgeManager.addConcept(new Concept(primaryConceptName));
                }
                knowledgeManager.getConcept(primaryConceptName).addSynonym(conceptName);
            }

            // add name of superclass to knowledgemanager concept
            if (concept.getSuperClass() != null) {
                Concept actConcept = knowledgeManager.getConcept(primaryConceptName);
                if (actConcept != null) {
                    actConcept.setSuperClass(concept.getSuperClass().getLocalName());
                }
            }
        }

        // add object attributes to concepts
        // ExtendedIterator ei = m.listObjectProperties();
        // while (ei.hasNext()) {
        // ObjectProperty op = (ObjectProperty) ei.next();
        // String conceptName = "";
        //			
        // if (op.getDomain() != null)
        // conceptName = op.getDomain().getLocalName();
        // else continue;
        //			
        // int valueType = -1;
        // if (op.getRange() != null)
        // valueType = Attribute.getValueTypeByName(op.getRange().getLocalName().replaceAll(" ",""));
        //			
        // String regExp = "";
        //			
        // // System.out.println(op.getLocalName()+" "+op.getRange());
        // // System.out.println(op.getRange().asClass());
        // if (op.getRange() != null) {
        // OntClass oc = m.getOntClass(op.getRange().toString());
        // ExtendedIterator instanceIterator = oc.listInstances();
        // Individual c = null;
        //				
        // if (instanceIterator.hasNext()) {
        // c = (Individual) instanceIterator.next();
        //					
        // StmtIterator ei2 = c.listProperties();
        // if (ei2.hasNext()) {
        // Statement s = ei2.nextStatement();
        // regExp = s.getObject().toString();
        // }
        // } else continue;
        // }
        //
        // Attribute a = new Attribute(op.getLocalName().replaceAll("_",""),valueType,regExp);
        // KnowledgeManager.getInstance().getConcept(conceptName,true).addAttribute(a);
        // }

        // keep track of attributes already entered and under which other attribute (for synonyms)
        HashMap<String, String> enteredAttributes = new HashMap<String, String>();
        ExtendedIterator ei = m.listDatatypeProperties();
        while (ei.hasNext()) {
            DatatypeProperty dp = (DatatypeProperty) ei.next();
            ArrayList<String> conceptNames = new ArrayList<String>(); // an attribute can belong to more than one concept (multiple domains)
            String attributeName = "";
            int valueCount = 1;

            // if attribute is not well formed (e.g. begins with a number) take the label as name, _entity_image_ stays unchanged
            if (!dp.getLocalName().equals("_entity_image_")) {
                if (dp.getLabel(null) != null)
                    attributeName = StringHelper.makeViewName(dp.getLabel(null));
                else
                    attributeName = StringHelper.makeViewName(dp.getLocalName());
            } else {
                attributeName = "_entity_image_";
            }

            // get the concept(s) the attribute belongs to
            if (dp.getDomain() != null) {
                // System.out.println("domain for "+attributeName+": "+dp.getDomain().getLocalName());

                // get all concepts (domains) the attribute belongs to
                ExtendedIterator conceptIterator = dp.listDomain();
                while (conceptIterator.hasNext()) {
                    OntResource domain = (OntResource) conceptIterator.next();
                    conceptNames.add(StringHelper.makeViewName(domain.getLocalName()));
                }
                String conceptName = conceptNames.get(0);
                if (conceptName.equalsIgnoreCase("Source"))
                    continue;
            } else
                continue;

            int valueType = -1;

            // System.out.println(dp.getLocalName()+" "+dp.getRange());

            // map xmlschema data types to extraction types
            String[] xsdTypeStringArray = dp.getRange().toString().split("#");
            String saveType = xsdTypeStringArray[1];

            if (saveType.equalsIgnoreCase("int") || saveType.equalsIgnoreCase("integer") || saveType.equalsIgnoreCase("double")
                    || saveType.equalsIgnoreCase("float") || saveType.equalsIgnoreCase("decimal") || saveType.equalsIgnoreCase("long")) {
                valueType = Attribute.VALUE_NUMERIC;
            } else if (saveType.equalsIgnoreCase("string")) {
                valueType = Attribute.VALUE_STRING;
            } else if (saveType.equalsIgnoreCase("date")) {
                valueType = Attribute.VALUE_DATE;
            } else if (saveType.equalsIgnoreCase("boolean")) {
                valueType = Attribute.VALUE_BOOLEAN;
            } else if (saveType.equalsIgnoreCase("anyURI")) {
                valueType = Attribute.VALUE_URI;
            } else if (saveType.equalsIgnoreCase("anyType")) {
                valueType = Attribute.VALUE_MIXED;
            }

            AnnotationProperty typeProperty = m.getAnnotationProperty(DATA_TYPE_NAMESPACE + "type");
            // System.out.println("type property "+typeProperty+"_"+dp.getProperty(typeProperty));
            if (dp.getProperty(typeProperty) != null) {
                // System.out.println("..."+dp.getPropertyValue(typeProperty));
                String[] typeInformation = dp.getPropertyValue(typeProperty).toString().split("_");
                if (typeInformation.length > 0) {
                    if (typeInformation[0].equalsIgnoreCase("image")) {
                        valueType = Attribute.VALUE_IMAGE;
                    } else if (typeInformation[0].equalsIgnoreCase("video")) {
                        valueType = Attribute.VALUE_VIDEO;
                    } else if (typeInformation[0].equalsIgnoreCase("audio")) {
                        valueType = Attribute.VALUE_AUDIO;
                    }

                    if (typeInformation.length > 1) {
                        valueCount = Integer.valueOf(typeInformation[1]);
                    }
                }
            }

            // get information about extracted attributes
            Date extractedAt = null;
            AnnotationProperty p = m.getAnnotationProperty("http://www.webknox.com/owl/ontology.owl#extractedAt");
            if (p != null && dp.getProperty(p) != null) {
                try {
                    extractedAt = new Date(Timestamp.valueOf(dp.getPropertyValue(p).toString()).getTime());
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // System.out.println("EXTRACTED AT "+extractedAt);
            }

            /*
             * TODO datatype with primary etc. if (dp.getComment(null) != null && dp.getComment(null).equalsIgnoreCase("primary")) { //return
             * StringHelper.makeViewName(concept.getLocalName()); }
             */

            boolean attributeEntered = false;
            String enteredAttributeName = ""; // if a synonym is entered, save the attribute it is synonym for

            // TODO find primary attribute
            // find equivalent attributes

            // check whether one attribute of the equivalent has already been entered
            HashSet<String> equivalentAttributes = new HashSet<String>();
            equivalentAttributes = getAllEquivalentDatatypeProperties(dp, equivalentAttributes);
            equivalentAttributes.add(attributeName);

            Iterator<String> eaIterator = equivalentAttributes.iterator();
            while (eaIterator.hasNext()) {
                String equivalentAttributeName = eaIterator.next();
                if (enteredAttributes.containsKey(equivalentAttributeName)) {
                    attributeEntered = true;
                    enteredAttributeName = enteredAttributes.get(equivalentAttributeName);
                    if (enteredAttributeName.length() == 0)
                        enteredAttributeName = equivalentAttributeName;
                }
            }

            // if attribute already entered, add all equivalent as synonyms for that...
            if (attributeEntered) {
                try {
                    for (int l = 0; l < conceptNames.size(); l++) {
                        String conceptName = conceptNames.get(l);
                        knowledgeManager.getConcept(conceptName).getAttribute(enteredAttributeName).addSynonym(attributeName);
                    }
                } catch (NullPointerException e) {
                    Logger.getRootLogger().error("Attribute for concept: " + conceptNames.get(0) + " (probably even more concepts) has not been added.", e);
                }

                // otherwise add current attribute and add all equivalent as synonyms for that
            } else {
                for (int l = 0; l < conceptNames.size(); l++) {
                    String conceptName = conceptNames.get(l);
                    Attribute a = new Attribute(attributeName, valueType, knowledgeManager.getConcept(conceptName));
                    a.setSaveType(saveType);
                    a.setValueCount(valueCount);
                    a.setExtractedAt(extractedAt);
                    a.setTrust(1.0);
                    // System.out.println("enter attribute " + attributeName + " to "+conceptName+"/"+a.getConcept().getName());
                    knowledgeManager.getConcept(conceptName).addAttribute(a);
                }
                enteredAttributeName = attributeName;
                enteredAttributes.put(attributeName, "");
            }

            // add all equivalent attributes
            eaIterator = equivalentAttributes.iterator();
            while (eaIterator.hasNext()) {

                String equivalentAttributeName = eaIterator.next();

                for (int l = 0; l < conceptNames.size(); l++) {
                    String conceptName = conceptNames.get(l);
                    knowledgeManager.getConcept(conceptName).getAttribute(enteredAttributeName).addSynonym(equivalentAttributeName);
                }

                enteredAttributes.put(equivalentAttributeName, enteredAttributeName);

                // System.out.println(equivalentConceptName+" equivalent");
            }

            // Attribute a = new Attribute(dp.getLocalName().replaceAll("_"," "),valueType);
            // a.setSaveType(saveType);
            // KnowledgeManager.getInstance().getConcept(conceptName,true).addAttribute(a);
        }

        // check formats.xml for additional processing instructions
        ArrayList<Format> formats = ConfigFileManager.getFormats();
        for (int j = 0, l = formats.size(); j < l; j++) {
            try {
                Format format = formats.get(j);

                Concept concept = knowledgeManager.getConcept(format.getConcept());
                if (concept == null)
                    continue;

                Attribute attribute = concept.getAttribute(format.getAttribute());
                if (attribute == null)
                    continue;

                if (format.getDescription().equalsIgnoreCase("image")) {
                    attribute.setValueType(Attribute.VALUE_IMAGE);
                } else if (format.getDescription().equalsIgnoreCase("video")) {
                    attribute.setValueType(Attribute.VALUE_VIDEO);
                } else if (format.getDescription().equalsIgnoreCase("audio")) {
                    attribute.setValueType(Attribute.VALUE_AUDIO);
                } else {
                    attribute.setRegExp(format.getDescription());
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        // add predefined sources
        ArrayList<PredefinedSource> predefinedSources = ConfigFileManager.getPredefinedSources(knowledgeManager);
        for (int j = 0, l = formats.size(); j < l; j++) {
            try {
                PredefinedSource predefinedSource = predefinedSources.get(j);

                Concept concept = knowledgeManager.getConcept(predefinedSource.getConceptName());
                if (concept == null)
                    continue;

                Source predefinedSourceForConcept = predefinedSource.getSource();
                HashSet<String> attributeNames = predefinedSource.getAttributeNames();
                Iterator<String> attributeNamesIterator = attributeNames.iterator();
                while (attributeNamesIterator.hasNext()) {
                    String attributeName = attributeNamesIterator.next();
                    Attribute attribute = concept.getAttribute(attributeName);
                    if (attribute != null)
                        attribute.addPredefinedSource(predefinedSourceForConcept);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        // System.out.println("\nentered concepts and attributes");
        Iterator<Concept> dIt = knowledgeManager.getConcepts().iterator();
        while (dIt.hasNext()) {
            Concept dEntry = dIt.next();
            // System.out.println(dEntry.getName()+" "+dEntry.getSynonymsToString());

            Iterator<Attribute> attributeIterator = dEntry.getAttributes().iterator();
            while (attributeIterator.hasNext()) {
                //Attribute attribute = attributeIterator.next();
                // System.out.println(" "+attribute.getName()+" "+attribute.getPredefinedSources().size());
            }
        }

        return knowledgeManager;
    }

    /**
     * Load ontology from given location into the KnowledgeManager.
     * 
     * @param filePath The file path.
     */
    public KnowledgeManager loadOntologyFile(String filePath) {
        KnowledgeManager knowledgeManager = new KnowledgeManager();

        // create the ontology model and read the owl file (rdf/xml syntax)
        OntModel m = readOntology(filePath);

        // find all root concepts with their synonyms and add them to the KnowledgeManager
        // ExtendedIterator i = m.listHierarchyRootClasses()
        // .filterDrop( new Filter() {
        // @Override
        // public boolean accept( Object o ) {
        // return ((Resource) o).isAnon();
        // }});

        ExtendedIterator i = m.listClasses().filterDrop(new Filter() {
            @Override
            public boolean accept(Object o) {
                return ((Resource) o).isAnon();
            }
        });

        // keep track of concepts already entered and under which other concept (for synonyms)
        // HashMap<String, String> enteredConcept = new HashMap<String, String>();
        while (i.hasNext()) {
            OntClass concept = (OntClass) i.next();
            String conceptName = StringHelper.makeViewName(concept.getLocalName());
            
            // do not enter "Source" as a concept
            if (conceptName.equalsIgnoreCase("Source")) {
                continue;
            }

            String primaryConceptName = findPrimaryConcept(concept);
            if (conceptName.equals(primaryConceptName)) {
                Concept primaryConcept = knowledgeManager.getConcept(primaryConceptName);
                if (primaryConcept == null) {
                    knowledgeManager.addConcept(new Concept(primaryConceptName));
                    primaryConcept = knowledgeManager.getConcept(primaryConceptName);
                }

                // add all equivalent concepts TODO why are not all equivalent relationships saved in each entity?
                HashSet<OntClass> equivalentConcepts = new HashSet<OntClass>();
                equivalentConcepts = getAllEquivalentConcepts(concept, equivalentConcepts);
                Iterator<OntClass> ecIterator = equivalentConcepts.iterator();
                while (ecIterator.hasNext()) {
                    OntClass equivalentClass = ecIterator.next();
                    String equivalentConceptName = StringHelper.makeViewName(equivalentClass.getLocalName());
                    Concept equivalentConcept = knowledgeManager.getConcept(equivalentConceptName);
                    if (equivalentConcept != null) {
                        knowledgeManager.removeConcept(equivalentConcept);
                    }
                    primaryConcept.addSynonym(equivalentConceptName);
                    // System.out.println(equivalentConceptName+" equivalent");
                }

                // otherwise add current concept and add all equivalent concepts as synonyms for that
            } else {
                Concept primaryConcept = knowledgeManager.getConcept(primaryConceptName);
                if (primaryConcept == null) {
                    knowledgeManager.addConcept(new Concept(primaryConceptName));
                }
                knowledgeManager.getConcept(primaryConceptName).addSynonym(conceptName);
            }

            // add name of superclass to knowledgemanager concept
            if (concept.getSuperClass() != null) {
                Concept actConcept = knowledgeManager.getConcept(primaryConceptName);
                if (actConcept != null) {
                    actConcept.setSuperClass(StringHelper.makeViewName(concept.getSuperClass().getLocalName()));
                }
            }
        }

        // keep track of attributes already entered and under which other attribute (for synonyms)
        HashMap<String, String> enteredAttributes = new HashMap<String, String>();
        ExtendedIterator ei = m.listDatatypeProperties();
        while (ei.hasNext()) {
            DatatypeProperty dp = (DatatypeProperty) ei.next();
            ArrayList<String> conceptNames = new ArrayList<String>(); // an attribute can belong to more than one concept (multiple domains)
            String attributeName = "";
            int valueCount = 1;

            // if attribute is not well formed (e.g. begins with a number) take the label as name, _entity_image_ stays unchanged
            if (!dp.getLocalName().equals("_entity_image_")) {
                if (dp.getLabel(null) != null)
                    attributeName = StringHelper.makeViewName(dp.getLabel(null));
                else
                    attributeName = StringHelper.makeViewName(dp.getLocalName());
            } else {
                attributeName = "_entity_image_";
            }

            // get the concept(s) the attribute belongs to
            if (dp.getDomain() != null) {
                // System.out.println("domain for "+attributeName+": "+dp.getDomain().getLocalName());

                // get all concepts (domains) the attribute belongs to
                ExtendedIterator conceptIterator = dp.listDomain();
                while (conceptIterator.hasNext()) {
                    OntResource domain = (OntResource) conceptIterator.next();
                    conceptNames.add(StringHelper.makeViewName(domain.getLocalName()));
                }
                String conceptName = conceptNames.get(0);
                if (conceptName.equalsIgnoreCase("Source"))
                    continue;
            } else
                continue;

            int valueType = -1;

            // System.out.println(dp.getLocalName()+" "+dp.getRange());

            // map xmlschema data types to extraction types
            String[] xsdTypeStringArray = dp.getRange().toString().split("#");
            String saveType = xsdTypeStringArray[1];

            if (saveType.equalsIgnoreCase("int") || saveType.equalsIgnoreCase("integer") || saveType.equalsIgnoreCase("double")
                    || saveType.equalsIgnoreCase("float") || saveType.equalsIgnoreCase("decimal") || saveType.equalsIgnoreCase("long")) {
                valueType = Attribute.VALUE_NUMERIC;
            } else if (saveType.equalsIgnoreCase("string")) {
                valueType = Attribute.VALUE_STRING;
            } else if (saveType.equalsIgnoreCase("date")) {
                valueType = Attribute.VALUE_DATE;
            } else if (saveType.equalsIgnoreCase("boolean")) {
                valueType = Attribute.VALUE_BOOLEAN;
            } else if (saveType.equalsIgnoreCase("anyURI")) {
                valueType = Attribute.VALUE_URI;
            } else if (saveType.equalsIgnoreCase("anyType")) {
                valueType = Attribute.VALUE_MIXED;
            }

            AnnotationProperty typeProperty = m.getAnnotationProperty(DATA_TYPE_NAMESPACE + "type");
            // System.out.println("type property "+typeProperty+"_"+dp.getProperty(typeProperty));
            if (dp.getProperty(typeProperty) != null) {
                // System.out.println("..."+dp.getPropertyValue(typeProperty));
                String[] typeInformation = dp.getPropertyValue(typeProperty).toString().split("_");
                if (typeInformation.length > 0) {
                    if (typeInformation[0].equalsIgnoreCase("image")) {
                        valueType = Attribute.VALUE_IMAGE;
                    } else if (typeInformation[0].equalsIgnoreCase("video")) {
                        valueType = Attribute.VALUE_VIDEO;
                    } else if (typeInformation[0].equalsIgnoreCase("audio")) {
                        valueType = Attribute.VALUE_AUDIO;
                    }

                    if (typeInformation.length > 1 && !typeInformation[0].contains("RANGENODE")) { // ignore BLANKNODES
                        valueCount = Integer.valueOf(typeInformation[1]);
                    }
                }
            }

            // get information about extracted attributes
            Date extractedAt = null;
            AnnotationProperty p = m.getAnnotationProperty("http://www.webknox.com/owl/ontology.owl#extractedAt");
            if (dp.getProperty(p) != null) {
                try {
                    extractedAt = new Date(Timestamp.valueOf(dp.getPropertyValue(p).toString()).getTime());
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // System.out.println("EXTRACTED AT "+extractedAt);
            }

            // GET RANGES
            ArrayList<AttributeRange> attributeRanges = new ArrayList<AttributeRange>();
            Property hasRange = m.getProperty("http://www.webknox.com/owl/ontology.owl#hasRange");
            AnnotationProperty rangeValue = m.getAnnotationProperty("http://www.webknox.com/owl/ontology.owl#rangeValue");
            AnnotationProperty rangeConcept = m.getAnnotationProperty("http://www.webknox.com/owl/ontology.owl#rangeConcept");
            AnnotationProperty rangeType = m.getAnnotationProperty("http://www.webknox.com/owl/ontology.owl#rangeType");

            if (dp.getProperty(hasRange) != null) {
                Property propHasRange = (Property) dp.getProperty(hasRange).getPredicate();
                StmtIterator it = dp.listProperties(propHasRange);
                while (it.hasNext()) {
                    Resource blanknode = it.nextStatement().getResource();

                    OntClass rangeConceptSchema = m.getOntClass(blanknode.getProperty(rangeConcept).getResource().getURI()); // get the connected concept of the
                    // blanknode
                    String rangeConceptString = StringHelper.makeViewName(rangeConceptSchema.getLocalName()); // get name of connected concept
                    String rangeTypeString = blanknode.getProperty(rangeType).getString(); // get type of rangenode

                    AttributeRange rangeValueItem = new AttributeRange(rangeConceptString); // create new AttributeRange and set all infos

                    if (rangeTypeString.equalsIgnoreCase("MINMAX")) {
                        rangeValueItem.setRangeType(AttributeRange.RANGETYPE_MINMAX);
                    } else if (rangeTypeString.equalsIgnoreCase("POSS")) {
                        rangeValueItem.setRangeType(AttributeRange.RANGETYPE_POSS);
                    }

                    if (blanknode.hasProperty(rangeValue)) { // check if there are values set
                        Property propRangeValue = (Property) blanknode.getProperty(rangeValue).getPredicate();
                        StmtIterator it2 = blanknode.listProperties(propRangeValue);
                        while (it2.hasNext()) { // get all values
                            String rangeValueString = it2.nextStatement().getString();
                            rangeValueItem.addRangeValue(rangeValueString, valueType);
                        }
                    }
                    attributeRanges.add(rangeValueItem);
                }

                // String rangeValueString = blanknode.getProperty(rangeValue).getString();
                // String realRangeTypeString = blanknode.getProperty(rangeType).getString();

                // System.out.println("rangevalue property:"+blanknode.getProperty(rangeValue));
                // System.out.println("blanknode properties: "+blanknode.listProperties());
                // Property prop = (Property) blanknode.getProperty(rangeValue).getPredicate();
                // StmtIterator it = blanknode.listProperties(prop);
                // while (it.hasNext()){
                // String rangeValueString = it.nextStatement().getString();
                // AttributeRange rangeValueItem = new AttributeRange(rangeValueString,rangeConceptString);
                // rangeValueItem.setRangeType(realRangeTypeString);
                // attributeRanges.add(rangeValueItem);
                // System.out.println("value gesetzt: " + rangeValueItem.getRangeValue() + " zum concept:" +rangeValueItem.getRangeConcept() + " rangetype: " +
                // rangeValueItem.getRangeType());
                // }
            }

            /*
             * TODO datatype with primary etc. if (dp.getComment(null) != null && dp.getComment(null).equalsIgnoreCase("primary")) { //return
             * StringHelper.makeViewName(concept.getLocalName()); }
             */

            boolean attributeEntered = false;
            String enteredAttributeName = ""; // if a synonym is entered, save the attribute it is synonym for

            // TODO find primary attribute
            // find equivalent attributes

            // check whether one attribute of the equivalent has already been entered
            HashSet<String> equivalentAttributes = new HashSet<String>();
            equivalentAttributes = getAllEquivalentDatatypeProperties(dp, equivalentAttributes);
            equivalentAttributes.add(attributeName);

            Iterator<String> eaIterator = equivalentAttributes.iterator();
            while (eaIterator.hasNext()) {
                String equivalentAttributeName = eaIterator.next();
                if (enteredAttributes.containsKey(equivalentAttributeName)) {
                    attributeEntered = true;
                    enteredAttributeName = enteredAttributes.get(equivalentAttributeName);
                    if (enteredAttributeName.length() == 0)
                        enteredAttributeName = equivalentAttributeName;
                }
            }

            // if attribute already entered, add all equivalent as synonyms for that...
            if (attributeEntered) {
                try {
                    for (int l = 0; l < conceptNames.size(); l++) {
                        String conceptName = conceptNames.get(l);
                        knowledgeManager.getConcept(conceptName).getAttribute(enteredAttributeName).addSynonym(attributeName);
                    }
                } catch (NullPointerException e) {
                    Logger.getRootLogger().error("Attribute for concept: " + conceptNames.get(0) + " (probably even more concepts) has not been added.", e);
                }

                // otherwise add current attribute and add all equivalent as synonyms for that
            } else {
                for (int l = 0; l < conceptNames.size(); l++) {
                    String conceptName = conceptNames.get(l);
                    Attribute a = new Attribute(attributeName, valueType, knowledgeManager.getConcept(conceptName));
                    a.setSaveType(saveType);
                    a.setValueCount(valueCount);
                    a.setExtractedAt(extractedAt);
                    a.setTrust(1.0);

                    Iterator<AttributeRange> itAttributeRanges = attributeRanges.iterator();
                    while (itAttributeRanges.hasNext()) {
                        a.addRangeValue(itAttributeRanges.next());
                    }
                    // System.out.println("enter attribute " + attributeName + " to "+conceptName+"/"+a.getConcept().getName());
                    knowledgeManager.getConcept(conceptName).addAttribute(a);
                }
                enteredAttributeName = attributeName;
                enteredAttributes.put(attributeName, "");
            }

            // add all equivalent attributes
            eaIterator = equivalentAttributes.iterator();
            while (eaIterator.hasNext()) {

                String equivalentAttributeName = eaIterator.next();

                for (int l = 0; l < conceptNames.size(); l++) {
                    String conceptName = conceptNames.get(l);
                    knowledgeManager.getConcept(conceptName).getAttribute(enteredAttributeName).addSynonym(equivalentAttributeName);
                }

                enteredAttributes.put(equivalentAttributeName, enteredAttributeName);

                // System.out.println(equivalentConceptName+" equivalent");
            }

            // Attribute a = new Attribute(dp.getLocalName().replaceAll("_"," "),valueType);
            // a.setSaveType(saveType);
            // KnowledgeManager.getInstance().getConcept(conceptName,true).addAttribute(a);
        }

        // check formats.xml for additional processing instructions
        ArrayList<Format> formats = ConfigFileManager.getFormats();
        for (int j = 0, l = formats.size(); j < l; j++) {
            try {
                Format format = formats.get(j);

                Concept concept = knowledgeManager.getConcept(format.getConcept());
                if (concept == null)
                    continue;

                Attribute attribute = concept.getAttribute(format.getAttribute());
                if (attribute == null)
                    continue;

                if (format.getDescription().equalsIgnoreCase("image")) {
                    attribute.setValueType(Attribute.VALUE_IMAGE);
                } else if (format.getDescription().equalsIgnoreCase("video")) {
                    attribute.setValueType(Attribute.VALUE_VIDEO);
                } else if (format.getDescription().equalsIgnoreCase("audio")) {
                    attribute.setValueType(Attribute.VALUE_AUDIO);
                } else {
                    attribute.setRegExp(format.getDescription());
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        // add predefined sources
        ArrayList<PredefinedSource> predefinedSources = ConfigFileManager.getPredefinedSources(knowledgeManager);
        for (int j = 0, l = formats.size(); j < l; j++) {
            try {
                PredefinedSource predefinedSource = predefinedSources.get(j);

                Concept concept = knowledgeManager.getConcept(predefinedSource.getConceptName());
                if (concept == null)
                    continue;

                Source predefinedSourceForConcept = predefinedSource.getSource();
                HashSet<String> attributeNames = predefinedSource.getAttributeNames();
                Iterator<String> attributeNamesIterator = attributeNames.iterator();
                while (attributeNamesIterator.hasNext()) {
                    String attributeName = attributeNamesIterator.next();
                    Attribute attribute = concept.getAttribute(attributeName);
                    if (attribute != null)
                        attribute.addPredefinedSource(predefinedSourceForConcept);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        // System.out.println("\nentered concepts and attributes");
        Iterator<Concept> dIt = knowledgeManager.getConcepts().iterator();
        while (dIt.hasNext()) {
            Concept dEntry = dIt.next();
            // System.out.println(dEntry.getName()+" "+dEntry.getSynonymsToString());

            Iterator<Attribute> attributeIterator = dEntry.getAttributes().iterator();
            while (attributeIterator.hasNext()) {
                Attribute attribute = attributeIterator.next();
                // System.out.println(" "+attribute.getName()+" "+attribute.getPredefinedSources().size());
            }
        }

        return knowledgeManager;
    }

    private String findPrimaryConcept(OntClass concept) {

        if (concept.getComment(null) != null && concept.getComment(null).equalsIgnoreCase("primary")) {
            return StringHelper.makeViewName(concept.getLocalName());
        }

        HashSet<OntClass> equivalentConcepts = new HashSet<OntClass>();
        equivalentConcepts = getAllEquivalentConcepts(concept, equivalentConcepts);

        Iterator<OntClass> ecIterator = equivalentConcepts.iterator();
        while (ecIterator.hasNext()) {
            OntClass equivalentClass = ecIterator.next();
            if (equivalentClass.getComment(null) != null && equivalentClass.getComment(null).equalsIgnoreCase("primary")) {
                return StringHelper.makeViewName(equivalentClass.getLocalName());
            }
        }

        return StringHelper.makeViewName(concept.getLocalName());
    }

    private HashSet<OntClass> getAllEquivalentConcepts(OntClass concept, HashSet<OntClass> equivalentConcepts) {

        boolean newConceptFound = false;
        ExtendedIterator ecIterator = concept.listEquivalentClasses();
        while (ecIterator.hasNext()) {
            OntClass equivalentClass = (OntClass) ecIterator.next();
            // String equivalentConceptName = equivalentClass.getLocalName().replaceAll("_"," ");
            if (equivalentConcepts.add(equivalentClass))
                newConceptFound = true;
        }
        if (newConceptFound) {
            equivalentConcepts.addAll(getAllEquivalentConcepts(concept, equivalentConcepts));
        }

        return equivalentConcepts;
    }

    private HashSet<String> getAllEquivalentDatatypeProperties(DatatypeProperty dp, HashSet<String> equivalentDatatypeProperties) {

        boolean newDatatypePropertyFound = false;
        ExtendedIterator edpIterator = dp.listEquivalentProperties();
        while (edpIterator.hasNext()) {
            DatatypeProperty equivalentDatatypeProperty = ((OntResource) edpIterator.next()).asDatatypeProperty();
            String equivalentDatatypePropertyName = StringHelper.makeViewName(equivalentDatatypeProperty.getLocalName());
            if (equivalentDatatypeProperties.add(equivalentDatatypePropertyName))
                newDatatypePropertyFound = true;
        }
        if (newDatatypePropertyFound) {
            equivalentDatatypeProperties.addAll(getAllEquivalentDatatypeProperties(dp, equivalentDatatypeProperties));
        }

        return equivalentDatatypeProperties;
    }

    /**
     * Store all extracted entities and facts into the owl knowledge base.
     * 
     * @param knowledgeManager The knowledge manager.
     */
    public void saveExtractions(KnowledgeManager knowledgeManager) {

        // load current data owl kb to update
        OntModel mData = readOntology(ONTOLOGY_DATA_LOCATION);

        // load current schema owl to update extracted attributes
        OntModel mSchema = readOntology(ONTOLOGY_LOCATION);

        // OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
        // m.read("file:"+ONTOLOGY_LOCATION);
        // m.setNsPrefix(Information.ID,NAMESPACE_ONTOLOGY);

        // OntClass c = m.getOntClass(NAMESPACE + "Country");
        // System.out.println(c);
        // Individual i = c.createIndividual(NAMESPACE + "Mexico");

        // FactValue fv = new FactValue("21300000",new Source(Source.SEMI_STRUCTURED,"www.www1.com"),ExtractionType.COLON_PHRASE);
        // System.out.println(knowledgeManager.getConcept("Country").getEntity("Australia"));
        // knowledgeManager.getConcept("Country").getEntity("Australia").addFactAndValue(new Fact(new Attribute("Population",Attribute.VALUE_STRING)), fv);

        ArrayList<Concept> concepts = knowledgeManager.getConcepts();
        for (Concept concept : concepts) {

            // get concept node
            OntClass conceptSchema = mSchema.getOntClass(NAMESPACE_ONTOLOGY + concept.getSafeName());

            // if concept does not exist in ontology, add it
            if (conceptSchema == null) {
                conceptSchema = mSchema.createClass(NAMESPACE_ONTOLOGY + concept.getSafeName());
            }

            ArrayList<Entity> entities = concept.getEntities();
            for (Entity entity : entities) {

                // add entity to the model
                OntClass c = mData.getOntClass(NAMESPACE_ONTOLOGY + concept.getSafeName());
                // System.out.println(concept.getSafeName() + " " + c);
                Individual i = c.createIndividual(NAMESPACE_ONTOLOGY + entity.getSafeName());

                ArrayList<Fact> facts = entity.getFacts();
                for (Fact fact : facts) {
                    FactValue factValue = fact.getFactValue();

                    // do not enter empty values
                    if (factValue == null || factValue.getValue().length() == 0)
                        continue;

                    // enter attribute to schema ontology if it does not exist already
                    Attribute attribute = fact.getAttribute();

                    // check whether attribute exists already
                    DatatypeProperty dp = mSchema.getDatatypeProperty(NAMESPACE_ONTOLOGY + attribute.getSafeName());
                    if (dp == null) {

                        // create the attribute and add the literals
                        dp = mSchema.createDatatypeProperty(NAMESPACE_ONTOLOGY + attribute.getSafeName());
                        dp.setDomain(c);
                        dp.setRange(attribute.getValueTypeXSD());
                        if (attribute.getExtractedAt() != null) {
                            AnnotationProperty p = mSchema.getAnnotationProperty("http://www.webknox.com/owl/ontology.owl#extractedAt");
                            dp.addProperty(p, attribute.getExtractedAtAsUTCString());
                        }
                    }

                    // add fact value with highest trust
                    // DatatypeProperty dp = mData.getDatatypeProperty(NAMESPACE_ONTOLOGY + attribute.getSafeName());
                    // System.out.println(dp+" "+fact.getAttribute().getSafeName());
                    i.addLiteral(dp, factValue.getValue());

                    // add source(s) for that value
                    ArrayList<Source> factValueSources = factValue.getSources();
                    Iterator<Source> factValueSourceIterator = factValueSources.iterator();
                    while (factValueSourceIterator.hasNext()) {
                        Source fvSource = factValueSourceIterator.next();
                        ObjectProperty op = mData.getObjectProperty(NAMESPACE_ONTOLOGY + "hasSource");

                        // check whether source exists already
                        Individual existingSourceIndividual = null;
                        OntClass oc = mData.getOntClass(NAMESPACE_SOURCES + "Source");
                        ExtendedIterator ei = oc.listInstances();
                        while (ei.hasNext()) {
                            Individual testExistingSourceIndividual = (Individual) ei.next();
                            Property urlProperty = mData.getDatatypeProperty(NAMESPACE_SOURCES + "url");
                            if (testExistingSourceIndividual.getProperty(urlProperty).getString().equalsIgnoreCase(fvSource.getUrl())) {
                                existingSourceIndividual = testExistingSourceIndividual;
                                break;
                            }
                            // System.out.println("individual "+i.getProperty(urlProperty).getString());
                        }

                        if (existingSourceIndividual == null) {

                            // create the source individual and add the literals
                            OntClass sourceClass = mData.getOntClass(NAMESPACE_SOURCES + "Source");
                            Individual sourceIndividual = sourceClass.createIndividual();
                            dp = mData.getDatatypeProperty(NAMESPACE_SOURCES + "url");
                            sourceIndividual.addLiteral(dp, fvSource.getUrl());
                            dp = mData.getDatatypeProperty(NAMESPACE_SOURCES + "trust");
                            sourceIndividual.addLiteral(dp, fvSource.getTrust());

                            existingSourceIndividual = sourceIndividual;

                        }

                        // add the object property to the entity
                        i.addProperty(op, existingSourceIndividual);
                    }
                }
            }
        }

        // write the ontology model
        try {
            FileOutputStream os = new FileOutputStream(ONTOLOGY_DATA_LOCATION_LOCAL);
            mData.write(os);
            os.close();

            os = new FileOutputStream(ONTOLOGY_LOCATION_LOCAL);
            mSchema.write(os);
            os.close();

        } catch (FileNotFoundException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    public void updateOntologyFile(KnowledgeManager knowledgeManager, File ontologyfile) {

        // load current data owl kb to update
        OntModel mData = readOntology("file:" + ontologyfile.toString());

        // load current schema owl to update extracted attributes
        OntModel mSchema = readOntology("file:" + ontologyfile.toString());
        ArrayList<Concept> concepts = knowledgeManager.getConcepts();

        for (Concept concept : concepts) {

            System.out.println(concept.getName());

            // SET CONCEPT
            OntClass conceptSchema = mSchema.getOntClass(NAMESPACE_ONTOLOGY + concept.getSafeName());
            // if concept does not exist in ontology, add it
            if (conceptSchema == null) {
                conceptSchema = mSchema.createClass(NAMESPACE_ONTOLOGY + concept.getSafeName());
            }

            // SET SUPERCLASS
            if (concept.getSuperClass() != null) {
                OntClass superClass = mSchema.getOntClass(NAMESPACE_ONTOLOGY + StringHelper.makeSafeName(concept.getSuperClass()));
                if (superClass != null)
                    conceptSchema.setSuperClass(superClass);
            }

            // UPDATED SUPERCLASS
            if (concept.getNewSuperClass() != null) {
                if (concept.getNewSuperClass().equalsIgnoreCase("")) { // remove the actual superclass
                    OntClass superClass = mSchema.getOntClass(NAMESPACE_ONTOLOGY + StringHelper.makeSafeName(concept.getSuperClass()));
                    conceptSchema.removeSuperClass(superClass);
                    System.out.println("removed superclass");
                } else {
                    OntClass newSuperClass = mSchema.getOntClass(NAMESPACE_ONTOLOGY + StringHelper.makeSafeName(concept.getNewSuperClass()));
                    if (newSuperClass != null) {
                        conceptSchema.setSuperClass(newSuperClass);
                        System.out.println("set superclass");
                    }
                }
            }

            // SET CONCEPT SYNONYMS
            if (!concept.getSynonyms().isEmpty()) {
                Iterator<String> syIt = concept.getSynonyms().iterator();
                while (syIt.hasNext()) {
                    String synonymConcept = StringHelper.makeSafeName(syIt.next());
                    OntClass newSynonymConceptSchema = mSchema.getOntClass(NAMESPACE_ONTOLOGY + synonymConcept);
                    if (newSynonymConceptSchema == null) {
                        newSynonymConceptSchema = mSchema.createClass(NAMESPACE_ONTOLOGY + synonymConcept);
                    }
                    conceptSchema.addEquivalentClass(newSynonymConceptSchema);
                }
            }

            // UPDATED CONCEPT SYNONYMS
            if (concept.hasNewSynonyms()) {
                Iterator<String> syIt = concept.getSynonyms().iterator();
                while (syIt.hasNext()) {
                    String synonymConcept = StringHelper.makeSafeName(syIt.next());
                    OntClass synonymConceptSchema = mSchema.getOntClass(NAMESPACE_ONTOLOGY + synonymConcept);
                    conceptSchema.removeEquivalentClass(synonymConceptSchema);
                    synonymConceptSchema.remove();
                }

                Iterator<String> newSyIt = concept.getNewSynonyms().iterator();
                while (newSyIt.hasNext()) {
                    String synonymConcept = StringHelper.makeSafeName(newSyIt.next());
                    OntClass newSynonymConceptSchema = mSchema.getOntClass(NAMESPACE_ONTOLOGY + synonymConcept);
                    if (newSynonymConceptSchema == null) {
                        newSynonymConceptSchema = mSchema.createClass(NAMESPACE_ONTOLOGY + synonymConcept);
                    }
                    conceptSchema.addEquivalentClass(newSynonymConceptSchema);

                }
            }

            // REMOVE CONCEPT ATTRIBUTES
            if (!concept.getAttributesToDelete().isEmpty()) {
                Iterator<Attribute> itATD = concept.getAttributesToDelete().iterator();
                while (itATD.hasNext()) {
                    Attribute attributeToDelete = (Attribute) itATD.next();
                    DatatypeProperty dp = mSchema.getDatatypeProperty(NAMESPACE_ONTOLOGY + attributeToDelete.getSafeName());

                    // REMOVE RANGES
                    Property hasRange = mSchema.getProperty("http://www.webknox.com/owl/ontology.owl#hasRange");
                    if (hasRange == null) {
                        mSchema.createProperty("http://www.webknox.com/owl/ontology.owl#hasRange");
                    }
                    Iterator<AttributeRange> attributeRangesToDelete = attributeToDelete.getAttributeRangesToDelete().iterator();
                    while (attributeRangesToDelete.hasNext()) {
                        AttributeRange attributeRange = attributeRangesToDelete.next();
                        if (attributeRange.getRangeType() == AttributeRange.RANGETYPE_MINMAX) {
                            System.out.println("removing min range!" + "http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_" + concept.getSafeName()
                                    + "_" + attributeToDelete.getSafeName());
                            Resource rangenodeMinMax = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                    + concept.getSafeName() + "_" + attributeToDelete.getSafeName());
                            rangenodeMinMax.removeProperties();
                            Statement statementMinMax = mSchema.createStatement(dp, hasRange, rangenodeMinMax);
                            mSchema.remove(statementMinMax);
                        } else if (attributeRange.getRangeType() == AttributeRange.RANGETYPE_POSS) {
                            System.out.println("removing poss range!" + "http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_" + concept.getSafeName()
                                    + "_" + attributeToDelete.getSafeName());
                            Resource rangenodePoss = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_" + concept.getSafeName()
                                    + "_" + attributeToDelete.getSafeName());
                            rangenodePoss.removeProperties();
                            Statement statementPoss = mSchema.createStatement(dp, hasRange, rangenodePoss);
                            mSchema.remove(statementPoss);
                        }
                    }

                    // REMOVE DOMAIN
                    dp.removeDomain(conceptSchema);
                    if (dp.getDomain() == null)
                        dp.remove();
                }
            }

            // SET ATTRIBUTES
            Iterator<Attribute> itA = concept.getAttributes().iterator();
            while (itA.hasNext()) {
                Attribute attribute = (Attribute) itA.next();
                //				
                // System.out.println("attr: " + attribute.getName() +
                // " rangetype" + attribute.getRangeType() +
                // " anzahl gespeicherter values: " + attribute.getRangeValueList().size());

                DatatypeProperty dp = mSchema.getDatatypeProperty(NAMESPACE_ONTOLOGY + attribute.getSafeName());

                // SET/UPDATE
                if (dp == null) {
                    dp = mSchema.createDatatypeProperty(NAMESPACE_ONTOLOGY + attribute.getSafeName());
                    dp.setDomain(conceptSchema);
                    dp.setRange(attribute.getValueTypeXSD());
                    if (attribute.getExtractedAt() != null) {
                        AnnotationProperty p = mSchema.getAnnotationProperty("http://www.webknox.com/owl/ontology.owl#extractedAt");
                        if (p == null)
                            p = mSchema.createAnnotationProperty("http://www.webknox.com/owl/ontology.owl#extractedAt");
                        dp.addProperty(p, attribute.getExtractedAtAsUTCString());
                    }
                } else {
                    dp.addDomain(conceptSchema);
                    dp.setRange(attribute.getValueTypeXSD());
                }

                // SET SYNONYMS
                if (!attribute.getSynonyms().isEmpty()) {
                    Iterator<String> atIt = attribute.getSynonyms().iterator();
                    while (atIt.hasNext()) {
                        String synonymAttribute = StringHelper.makeSafeName(atIt.next());
                        DatatypeProperty dpSynonym = mSchema.getDatatypeProperty(NAMESPACE_ONTOLOGY + synonymAttribute);

                        if (dpSynonym == null) {
                            dpSynonym = mSchema.createDatatypeProperty(NAMESPACE_ONTOLOGY + synonymAttribute);
                            // dpSynonym.setComment("equivalent", null);
                        }
                        dp.addEquivalentProperty(dpSynonym);
                    }
                }

                // UPDATED ATTRIBUTE SYNONYMS
                if (attribute.hasNewSynonyms()) {
                    Iterator<String> syIt = attribute.getSynonyms().iterator();
                    while (syIt.hasNext()) {
                        String synonymAttribute = StringHelper.makeSafeName(syIt.next());
                        DatatypeProperty dpSyn = mSchema.getDatatypeProperty(NAMESPACE_ONTOLOGY + synonymAttribute);
                        dp.removeEquivalentProperty(dpSyn);
                        dpSyn.remove();
                    }

                    Iterator<String> newSyIt = attribute.getNewSynonyms().iterator();
                    while (newSyIt.hasNext()) {
                        String synonymAttribute = StringHelper.makeSafeName(newSyIt.next());
                        DatatypeProperty dpSyn = mSchema.getDatatypeProperty(NAMESPACE_ONTOLOGY + synonymAttribute);
                        if (dpSyn == null) {
                            dpSyn = mSchema.createDatatypeProperty(NAMESPACE_ONTOLOGY + synonymAttribute);
                        }
                        dp.addEquivalentProperty(dpSyn);
                    }
                }

                // RANGES
                AnnotationProperty rangeType = mSchema.getAnnotationProperty("http://www.webknox.com/owl/ontology.owl#rangeType");
                if (rangeType == null)
                    rangeType = mSchema.createAnnotationProperty("http://www.webknox.com/owl/ontology.owl#rangeType");
                Property hasRange = mSchema.getProperty("http://www.webknox.com/owl/ontology.owl#hasRange");
                if (hasRange == null) {
                    mSchema.createProperty("http://www.webknox.com/owl/ontology.owl#hasRange");
                }
                AnnotationProperty rangeValue = mSchema.getAnnotationProperty("http://www.webknox.com/owl/ontology.owl#rangeValue");
                if (rangeValue == null)
                    rangeValue = mSchema.createAnnotationProperty("http://www.webknox.com/owl/ontology.owl#rangeValue");
                AnnotationProperty rangeConcept = mSchema.getAnnotationProperty("http://www.webknox.com/owl/ontology.owl#rangeConcept");
                if (rangeConcept == null)
                    rangeConcept = mSchema.createAnnotationProperty("http://www.webknox.com/owl/ontology.owl#rangeConcept");

                // SET RANGES
                Iterator<AttributeRange> attributeRanges = attribute.getAttributeRanges().iterator();
                while (attributeRanges.hasNext()) {
                    AttributeRange attributeRange = attributeRanges.next();

                    Concept actRangeConcept = knowledgeManager.getConcept(attributeRange.getRangeConcept());
                    if (actRangeConcept == null)
                        actRangeConcept = concept; // the range concept is the new one!
                    OntClass rangeConceptSchema = mSchema.getOntClass(NAMESPACE_ONTOLOGY + actRangeConcept.getSafeName());
                    if (rangeConceptSchema == null)
                        rangeConceptSchema = conceptSchema; // the range concept is the new one!

                    if (attributeRange.getRangeType() == AttributeRange.RANGETYPE_MINMAX) {
                        // MINMAX NODE
                        Resource rangenodeMinMax = null;
                        // check for name updates
                        if (actRangeConcept.getNewName() != null || attribute.getNewName() != null) {
                            Resource oldRangenodeMinMax = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                    + actRangeConcept.getSafeName() + "_" + attribute.getSafeName());
                            oldRangenodeMinMax.removeProperties();
                            Statement statementMinMax = mSchema.createStatement(dp, hasRange, oldRangenodeMinMax);
                            mSchema.remove(statementMinMax);
                            String newConceptName = actRangeConcept.getSafeName();
                            String newAttributeName = attribute.getSafeName();
                            if (actRangeConcept.getNewName() != null)
                                newConceptName = actRangeConcept.getSafeNewName();
                            if (attribute.getNewName() != null)
                                newAttributeName = attribute.getSafeNewName();
                            rangenodeMinMax = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_" + newConceptName + "_"
                                    + newAttributeName);
                            if (rangenodeMinMax == null) {
                                rangenodeMinMax = mSchema.createResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_" + newConceptName
                                        + "_" + newAttributeName);
                                System.out.println("range with new name created: " + "http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                        + newConceptName + "_" + newAttributeName);
                            } else {
                                System.out.println("behalte range mit neuem namen: " + "http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                        + newConceptName + "_" + newAttributeName);
                                rangenodeMinMax.removeProperties();
                            }
                        } else { // no name updates
                            rangenodeMinMax = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                    + actRangeConcept.getSafeName() + "_" + attribute.getSafeName());
                            if (rangenodeMinMax == null) {
                                rangenodeMinMax = mSchema.createResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                        + actRangeConcept.getSafeName() + "_" + attribute.getSafeName());
                                System.out.println("range created: " + "http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                        + actRangeConcept.getSafeName() + "_" + attribute.getSafeName());
                            } else {
                                rangenodeMinMax.removeProperties();
                                System.out.println("behalte range: " + "http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                        + actRangeConcept.getSafeName() + "_" + attribute.getSafeName());
                            }
                        }

                        rangenodeMinMax.addProperty(rangeType, "MINMAX"); // set rangetype
                        rangenodeMinMax.addProperty(rangeConcept, rangeConceptSchema); // set connected concept
                        if (attributeRange.hasMaxValue()) {
                            rangenodeMinMax.addProperty(rangeValue, attributeRange.getRangeMaxValue()); // set rangevalue
                        }
                        if (attributeRange.hasMinValue()) {
                            rangenodeMinMax.addProperty(rangeValue, attributeRange.getRangeMinValue()); // set rangevalue
                        }
                        Statement statementMinMax = mSchema.createStatement(dp, hasRange, rangenodeMinMax);
                        mSchema.add(statementMinMax);
                    } else if (attributeRange.getRangeType() == AttributeRange.RANGETYPE_POSS) {
                        // POSS NODES
                        Resource rangenodePoss = null;
                        // check for name updates
                        if (actRangeConcept.getNewName() != null || attribute.getNewName() != null) {
                            Resource oldRangenodePoss = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_"
                                    + actRangeConcept.getSafeName() + "_" + attribute.getSafeName());
                            oldRangenodePoss.removeProperties();
                            Statement statementPoss = mSchema.createStatement(dp, hasRange, oldRangenodePoss);
                            mSchema.remove(statementPoss);
                            String newConceptName = actRangeConcept.getSafeName();
                            String newAttributeName = attribute.getSafeName();
                            if (actRangeConcept.getNewName() != null)
                                newConceptName = actRangeConcept.getSafeNewName();
                            if (attribute.getNewName() != null)
                                newAttributeName = attribute.getSafeNewName();
                            rangenodePoss = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_" + newConceptName + "_"
                                    + newAttributeName);
                            if (rangenodePoss == null) {
                                rangenodePoss = mSchema.createResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_" + newConceptName + "_"
                                        + newAttributeName);
                                System.out.println("range with new name created: " + "http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                        + newConceptName + "_" + newAttributeName);
                            } else {
                                rangenodePoss.removeProperties();
                                System.out.println("behalte range with new name: " + "http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                        + newConceptName + "_" + newAttributeName);
                            }
                        } else { // no name updates
                            rangenodePoss = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_" + actRangeConcept.getSafeName()
                                    + "_" + attribute.getSafeName());
                            if (rangenodePoss == null) {
                                rangenodePoss = mSchema.createResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_"
                                        + actRangeConcept.getSafeName() + "_" + attribute.getSafeName());
                                System.out.println("range created: " + "http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_"
                                        + actRangeConcept.getSafeName() + "_" + attribute.getSafeName());
                            } else {
                                rangenodePoss.removeProperties();
                                System.out.println("behalte range: " + "http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_"
                                        + actRangeConcept.getSafeName() + "_" + attribute.getSafeName());
                            }
                        }

                        rangenodePoss.addProperty(rangeType, "POSS"); // set rangetype
                        rangenodePoss.addProperty(rangeConcept, rangeConceptSchema); // set connected concept
                        if (attributeRange.hasPossValue()) {
                            Iterator<String> itPossRangeValues = attributeRange.getRangePossValues().iterator();
                            while (itPossRangeValues.hasNext()) {
                                String possRangeValue = itPossRangeValues.next();
                                rangenodePoss.addProperty(rangeValue, possRangeValue); // set rangevalue
                            }
                        }
                        Statement statementPoss = mSchema.createStatement(dp, hasRange, rangenodePoss);
                        mSchema.add(statementPoss);

                    }
                }

                // REMOVE RANGES
                Iterator<AttributeRange> attributeRangesToDelete = attribute.getAttributeRangesToDelete().iterator();
                System.out.println("number of ranges to delete: " + attribute.getAttributeRangesToDelete().size());
                while (attributeRangesToDelete.hasNext()) {
                    AttributeRange attributeRange = attributeRangesToDelete.next();
                    Concept actRangeConcept = knowledgeManager.getConcept(attributeRange.getRangeConcept());
                    if (attributeRange.getRangeType() == AttributeRange.RANGETYPE_MINMAX) {

                        Resource rangenodeMinMax = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                + actRangeConcept.getSafeName() + "_" + attribute.getSafeName());
                        if (rangenodeMinMax != null) {
                            rangenodeMinMax.removeProperties();
                            Statement statementMinMax = mSchema.createStatement(dp, hasRange, rangenodeMinMax);
                            mSchema.remove(statementMinMax);
                        }
                    } else if (attributeRange.getRangeType() == AttributeRange.RANGETYPE_POSS) {

                        Resource rangenodePoss = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_"
                                + actRangeConcept.getSafeName() + "_" + attribute.getSafeName());
                        if (rangenodePoss != null) {
                            Statement statementPoss = mSchema.createStatement(dp, hasRange, rangenodePoss);
                            mSchema.remove(statementPoss);
                        }
                    }
                }

                // UPDATE ATTRIBUTENAME IF NEW
                if (attribute.getNewName() != null) {
                    ResourceUtils.renameResource(dp, NAMESPACE_ONTOLOGY + attribute.getSafeNewName());
                }
            }

            // UPDATE CONCEPTNAME IF NEW
            if (concept.getNewName() != null) {
                ResourceUtils.renameResource(conceptSchema, NAMESPACE_ONTOLOGY + concept.getSafeNewName());
            }
        }

        try {
            FileOutputStream os = new FileOutputStream(ontologyfile);
            mData.write(os);
            os.close();
            os = new FileOutputStream(ontologyfile);
            mSchema.write(os);
            os.close();
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    public void removeConcept(KnowledgeManager knowledgeManager, File ontologyfile, int conceptId) {

        // load current data owl kb to update
        OntModel mData = readOntology("file:" + ontologyfile.toString());

        // load current schema owl to update extracted attributes
        OntModel mSchema = readOntology("file:" + ontologyfile.toString());
        ArrayList<Concept> concepts = knowledgeManager.getConcepts();

        for (Concept concept : concepts) {

            // get concept node
            OntClass conceptSchema = mSchema.getOntClass(NAMESPACE_ONTOLOGY + concept.getSafeName());

            if (concept.getID() == conceptId) {
                // REMOVE ALL EQUIVALENT CLASSES
                ExtendedIterator ecIt = conceptSchema.listEquivalentClasses();
                HashSet<OntClass> conceptsToRemove = new HashSet<OntClass>();
                while (ecIt.hasNext()) {
                    OntClass equivalentConceptSchema = (OntClass) ecIt.next();
                    conceptsToRemove.add(equivalentConceptSchema);

                }
                Iterator<OntClass> itCTR = conceptsToRemove.iterator();
                while (itCTR.hasNext()) {
                    OntClass conceptToRemove = itCTR.next();
                    conceptToRemove.remove();
                }

                // remove concept
                conceptSchema.remove();

                // update range nodes
                Iterator<Attribute> itA = concept.getAttributes().iterator();
                while (itA.hasNext()) {
                    Attribute attribute = (Attribute) itA.next();
                    AttributeRange range = attribute.getRange(concept.getName());
                    Property hasRange = mSchema.getProperty("http://www.webknox.com/owl/ontology.owl#hasRange");
                    DatatypeProperty dp = mSchema.getDatatypeProperty(NAMESPACE_ONTOLOGY + attribute.getSafeName());
                    if (range.getRangeType() == AttributeRange.RANGETYPE_MINMAX) {
                        Resource rangenodeMinMax = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_" + concept.getSafeName()
                                + "_" + attribute.getSafeName());
                        rangenodeMinMax.removeProperties();
                        Statement statementMinMax = mSchema.createStatement(dp, hasRange, rangenodeMinMax);
                        mSchema.remove(statementMinMax);
                    } else if (range.getRangeType() == AttributeRange.RANGETYPE_POSS) {
                        Resource rangenodePoss = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_" + concept.getSafeName()
                                + "_" + attribute.getSafeName());
                        rangenodePoss.removeProperties();
                        Statement statementPoss = mSchema.createStatement(dp, hasRange, rangenodePoss);
                        mSchema.remove(statementPoss);
                    }
                }

            }

            // update attributes
            Iterator<Attribute> itA = concept.getAttributes().iterator();
            while (itA.hasNext()) {
                Attribute attribute = (Attribute) itA.next();
                DatatypeProperty dp = mSchema.getDatatypeProperty(NAMESPACE_ONTOLOGY + attribute.getSafeName());

                if (dp != null && dp.getDomain() == null) {
                    // delete if no concept is connected to the property
                    dp.remove();
                }

            }
        }

        try {

            FileOutputStream os = new FileOutputStream(ontologyfile);
            mData.write(os);
            os.close();

            os = new FileOutputStream(ontologyfile);
            mSchema.write(os);
            os.close();

        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    public void removeAttribute(KnowledgeManager knowledgeManager, File ontologyfile, int attributeId) {

        // load current data owl kb to update
        OntModel mData = readOntology("file:" + ontologyfile.toString());

        // load current schema owl to update extracted attributes
        OntModel mSchema = readOntology("file:" + ontologyfile.toString());
        ArrayList<Concept> concepts = knowledgeManager.getConcepts();

        for (Concept concept : concepts) {
            Iterator<Attribute> itA = concept.getAttributes().iterator();
            while (itA.hasNext()) {
                Attribute attribute = (Attribute) itA.next();
                if (attribute.getID() == attributeId) {
                    DatatypeProperty dp = mSchema.getDatatypeProperty(NAMESPACE_ONTOLOGY + attribute.getSafeName());
                    if (dp != null) {
                        // REMOVE ALL EQUIVALENT CLASSES
                        ExtendedIterator epIt = dp.listEquivalentProperties();
                        HashSet<OntProperty> attributesToRemove = new HashSet<OntProperty>();
                        while (epIt.hasNext()) {
                            OntProperty dpSynonym = (OntProperty) epIt.next();
                            attributesToRemove.add(dpSynonym);
                        }
                        Iterator<OntProperty> itATR = attributesToRemove.iterator();
                        while (itATR.hasNext()) {
                            OntProperty attributeToRemove = itATR.next();
                            attributeToRemove.remove();
                        }

                        // REMOVE ALL RANGES
                        AttributeRange range = attribute.getRange(concept.getName());
                        Property hasRange = mSchema.getProperty("http://www.webknox.com/owl/ontology.owl#hasRange");
                        if (range != null) {
                            if (range.getRangeType() == AttributeRange.RANGETYPE_MINMAX) {
                                Resource rangenodeMinMax = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_MINMAX_"
                                        + concept.getSafeName() + "_" + attribute.getSafeName());
                                rangenodeMinMax.removeProperties();
                                Statement statementMinMax = mSchema.createStatement(dp, hasRange, rangenodeMinMax);
                                mSchema.remove(statementMinMax);
                            } else if (range.getRangeType() == AttributeRange.RANGETYPE_POSS) {
                                Resource rangenodePoss = mSchema.getResource("http://www.webknox.com/owl/ontology.owl#" + "RANGENODE_POSS_"
                                        + concept.getSafeName() + "_" + attribute.getSafeName());
                                rangenodePoss.removeProperties();
                                Statement statementPoss = mSchema.createStatement(dp, hasRange, rangenodePoss);
                                mSchema.remove(statementPoss);
                            }
                        }

                        // REMOVE ATTRIBUTE
                        dp.remove();
                    }
                }
            }
        }

        try {

            FileOutputStream os = new FileOutputStream(ontologyfile);
            mData.write(os);
            os.close();

            os = new FileOutputStream(ontologyfile);
            mSchema.write(os);
            os.close();

        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    public void jenaDBTest() {
        try {
            String className = "com.mysql.jdbc.Driver"; // path of driver class
            Class.forName(className); // Load the Driver
            String DB_URL = "jdbc:mysql://localhost/testdb"; // URL of database
            String DB_USER = "root"; // database user id
            String DB_PASSWD = ""; // database password
            String DB = "MySQL"; // database type

            // Create database connection
            IDBConnection conn = new DBConnection(DB_URL, DB_USER, DB_PASSWD, DB);
            //ModelMaker maker = ModelFactory.createModelRDBMaker(conn);

            // OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, maker, null);
            // model.read("file:"+ONTOLOGY_LOCATION_LOCAL);
            // model.setNsPrefix(Controller.ID,NAMESPACE_ONTOLOGY);

            // KnowledgeManager.getInstance().createBenchmarkConcepts();
            // OntologyManager.getInstance().saveExtractions(model);

            // create or open the default model
            // Model model = maker.createDefaultModel();

            // Close the database connection
            conn.close();
        } catch (ClassNotFoundException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (SQLException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    public void clearCompleteKnowledgeBase() {

        // read dataClean.owl and write to data.owl
        OntModel m = readOntology(ONTOLOGY_DATA_CLEAN_LOCATION);

        // write the ontology model
        try {
            FileOutputStream os = new FileOutputStream(ONTOLOGY_DATA_LOCATION_LOCAL);
            m.write(os);

        } catch (FileNotFoundException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    public OntModel getOntModel(String filePath) {
        OntModel ontmodel = readOntology(filePath);
        return ontmodel;
    }

    public HashSet<OntClass> getConcepts(OntModel om) {

        // find all root concepts with their synonyms and add them to the KnowledgeManager
        ExtendedIterator i = om.listHierarchyRootClasses().filterDrop(new Filter() {
            @Override
            public boolean accept(Object o) {
                return ((Resource) o).isAnon();
            }
        });

        HashSet<OntClass> conceptList = new HashSet<OntClass>();
        while (i.hasNext()) {
            OntClass concept = (OntClass) i.next();
            conceptList.add(concept);
        }

        return conceptList;

    }

    // public Attribute getAttribute(int id){
    //		
    // }

    public String getConceptProperties(String conceptName, OntModel om) {
        ArrayList<String> propertyList = new ArrayList<String>();

        ExtendedIterator ei = om.listDatatypeProperties();
        while (ei.hasNext()) {
            DatatypeProperty dp = (DatatypeProperty) ei.next();
            String attributeName = "";

            // if attribute is not well formed (e.g. begins with a number) take the label as name, _entity_image_ stays unchanged
            if (!dp.getLocalName().equals("_entity_image_")) {
                if (dp.getLabel(null) != null)
                    attributeName = StringHelper.makeViewName(dp.getLabel(null));
                else
                    attributeName = StringHelper.makeViewName(dp.getLocalName());
            } else {
                attributeName = "_entity_image_";
            }

            if (dp.getDomain() != null) {
                // get all concepts (domains) the attribute belongs to
                ExtendedIterator conceptIterator = dp.listDomain();
                while (conceptIterator.hasNext()) {
                    OntResource domain = (OntResource) conceptIterator.next();

                    if (StringHelper.makeViewName(domain.getLocalName()).equalsIgnoreCase(conceptName)) {
                        propertyList.add(attributeName);
                    }
                }
            }
        }

        // convert propertylist to String
        String properties = "";
        for (int i = 0; i < propertyList.size(); i++) {
            properties += propertyList.get(i);
            if (i != propertyList.size() - 1)
                properties += ", ";
        }

        return properties;
    }

    public static void main(String[] args) {

        OntologyManager.getInstance().clearCompleteKnowledgeBase();

        KnowledgeManager dm = new KnowledgeManager();
        dm.createBenchmarkConcepts();
        dm.addConcept(new Concept("TEST"));
        Entity e = new Entity("Ford Explorer", dm.getConcept("Car"));
        Attribute a = new Attribute("number of doors", Attribute.VALUE_NUMERIC, dm.getConcept("car"));
        a.setExtractedAt(new Date(System.currentTimeMillis()));
        Fact f = new Fact(a);
        FactValue fv = new FactValue("4", new Source("http://www.test.com"), 0);
        e.addFactAndValue(f, fv);
        // dm.getConcept("Car").addEntity(new Entity("Ford Explorer",dm.getConcept("Car")));
        // OntologyManager.getInstance().clearCompleteKnowledgeBase();
        OntologyManager.getInstance().saveExtractions(dm);

        System.exit(1);

        KnowledgeManager dm2 = OntologyManager.getInstance().loadOntology();
        System.exit(0);

        // System.out.println("-----------------------------------");
        ArrayList<Concept> concepts = dm2.getConcepts();
        for (int i = 0; i < concepts.size(); i++) {
            // System.out.println(concepts.get(i).getName()+" syns: "+concepts.get(i).getSynonymsToString());
            HashSet<Attribute> attributes = concepts.get(i).getAttributes();
            Iterator<Attribute> ai = attributes.iterator();
            while (ai.hasNext()) {
                // Attribute a2 = ai.next();
                // System.out.println(" "+a2.getName()+" "+a2.getConcept().getName()+" "+a2.getValueType());
            }
        }

    }
}