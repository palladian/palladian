package ws.palladian.extraction.entity.ner.tagger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.classification.Category;
import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalClassifier;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.numeric.NumericInstance;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextInstance;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.extraction.entity.ner.Annotation;
import ws.palladian.extraction.entity.ner.Annotations;
import ws.palladian.extraction.entity.ner.FileFormatParser;
import ws.palladian.extraction.entity.ner.TaggingFormat;
import ws.palladian.extraction.entity.ner.evaluation.EvaluationResult;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.math.MathHelper;

public class TUDNER2 extends TUDNER implements Serializable {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TUDNER2.class);

    private static final long serialVersionUID = 4630752782811857313L;

    /** The classifier to use for classifying the annotations. */
    private UniversalClassifier universalClassifier;
    private DictionaryClassifier tc1n;
    private DictionaryClassifier tc2n;
    private DictionaryClassifier tc3n;
    private DictionaryClassifier tc4n;

    ArrayList<List<Double>> regPer;
    ArrayList<List<Double>> regOrg;
    ArrayList<List<Double>> regLoc;
    ArrayList<List<Double>> regMisc;

    public TUDNER2() {
        setName("TUD NER 2");
        universalClassifier = new UniversalClassifier();
        universalClassifier.getTextClassifier().getClassificationTypeSetting()
        .setClassificationType(ClassificationTypeSetting.TAG);
        universalClassifier.getNumericClassifier().getClassificationTypeSetting()
        .setClassificationType(ClassificationTypeSetting.TAG);
        universalClassifier.getNominalClassifier().getClassificationTypeSetting()
        .setClassificationType(ClassificationTypeSetting.TAG);

        universalClassifier.getTextClassifier().getDictionary().setName("dictionary");
        universalClassifier.getTextClassifier().getFeatureSetting().setMinNGramLength(2);
        universalClassifier.getTextClassifier().getFeatureSetting().setMaxNGramLength(8);

        tc1n = new DictionaryClassifier();
        tc1n.getDictionary().setName("dict1n");
        tc1n.getFeatureSetting().setMinNGramLength(2);
        tc1n.getFeatureSetting().setMaxNGramLength(8);
        tc1n.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);

        tc2n = new DictionaryClassifier();
        tc2n.getDictionary().setName("dict2n");
        tc2n.getFeatureSetting().setMinNGramLength(2);
        tc2n.getFeatureSetting().setMaxNGramLength(8);
        tc2n.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);

        tc3n = new DictionaryClassifier();
        tc3n.getDictionary().setName("dict3n");
        tc3n.getFeatureSetting().setMinNGramLength(2);
        tc3n.getFeatureSetting().setMaxNGramLength(8);
        tc3n.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);

        tc4n = new DictionaryClassifier();
        tc4n.getDictionary().setName("dict4n");
        tc4n.getFeatureSetting().setMinNGramLength(2);
        tc4n.getFeatureSetting().setMaxNGramLength(8);
        tc4n.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);

        universalClassifier.switchClassifiers(true, false, false);

        regPer = new ArrayList<List<Double>>();
        regPer.add(new ArrayList<Double>());
        regPer.add(new ArrayList<Double>());

        regOrg = new ArrayList<List<Double>>();
        regOrg.add(new ArrayList<Double>());
        regOrg.add(new ArrayList<Double>());

        regLoc = new ArrayList<List<Double>>();
        regLoc.add(new ArrayList<Double>());
        regLoc.add(new ArrayList<Double>());

        regMisc = new ArrayList<List<Double>>();
        regMisc.add(new ArrayList<Double>());
        regMisc.add(new ArrayList<Double>());

    }

    @Override
    public String getModelFileEnding() {
        return "model";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        StopWatch stopWatch = new StopWatch();

        TUDNER2 n = (TUDNER2) FileHelper.deserialize(configModelFilePath);
        this.universalClassifier = n.universalClassifier;
        setModel(n);
        LOGGER.info("model " + configModelFilePath + " successfully loaded in " + stopWatch.getElapsedTimeString());

        return true;
    }

    private void saveModel(String modelFilePath) {

        LOGGER.info("serializing NERCer");
        FileHelper.serialize(this, modelFilePath);

        // write model meta information
        StringBuilder supportedConcepts = new StringBuilder();
        for (Category c : universalClassifier.getTextClassifier().getDictionary().getCategories()) {
            supportedConcepts.append(c.getName()).append("\n");
        }

        FileHelper.writeToFile(FileHelper.getFilePath(modelFilePath) + FileHelper.getFileName(modelFilePath)
                + "_meta.txt", supportedConcepts);

        LOGGER.info("model meta information written");
    }

    @Override
    public Annotations getAnnotations(String inputText, String modelPath) {
        loadModel(modelPath);
        return getAnnotations(inputText);
    }

    private Annotations getEntityCandidates(String inputText) {

        // // every token is an entity candidate
        // List<String> entityCandidateTokens = Tokenizer.tokenize(inputText);

        Annotations annotations = new Annotations();

        String entityTag = "<CANDIDATE>$0</CANDIDATE>";

        String regexp = "([A-Z]\\.)+|([\\p{L}\\w]+)([-\\.,]([\\p{L}\\w]+))*|\\.([\\p{L}\\w]+)|</?([\\p{L}\\w]+)>|(\\$\\d+\\.\\d+)|([^\\w\\s<]+)";

        inputText = inputText.replaceAll(regexp, entityTag);

        annotations = FileFormatParser.getAnnotationsFromXMLText(inputText);

        return annotations;
    }

    private Annotations verifyAnnotationsWithUniversalClassifier_(Annotations entityCandidates) {
        Annotations annotations = new Annotations();

        int i = 0;
        Annotation lastAnnotation = null;
        for (Annotation annotation : entityCandidates) {

            universalClassifier.classify(annotation);

            // try to classify with the last annotation
            if (lastAnnotation != null && lastAnnotation.getLength() > 1 && annotation.getLength() > 1
                    && annotation.leftWhitespace) {

                String combinedEntityName = lastAnnotation.getEntity() + " " + annotation.getEntity();
                TextInstance ti = universalClassifier.getTextClassifier().classify(combinedEntityName);
                if (!ti.getMainCategoryEntry().getCategory().getName().equalsIgnoreCase("O")) {

                    annotations.remove(lastAnnotation);
                    Annotation combinedAnnotation = new Annotation(lastAnnotation.getOffset(), combinedEntityName, ti
                            .getMainCategoryEntry().getCategory().getName());
                    annotations.add(combinedAnnotation);

                } else if (!annotation.getMostLikelyTagName().equalsIgnoreCase("O") && annotation.getLength() > 1) {
                    annotations.add(annotation);
                }

            } else if (!annotation.getMostLikelyTagName().equalsIgnoreCase("O") && annotation.getLength() > 1) {
                annotations.add(annotation);
            }

            if (i % 100 == 0) {
                LOGGER.info("classified " + MathHelper.round(100 * i / entityCandidates.size(), 0) + "%");
            }
            i++;

            lastAnnotation = annotation;
        }

        return annotations;
    }

    private Annotations verifyAnnotationsWithUniversalClassifier(Annotations entityCandidates) {
        Annotations annotations = new Annotations();

        int i = 0;
        for (Annotation annotation : entityCandidates) {

            universalClassifier.classify(annotation);
            if (!annotation.getMostLikelyTagName().equalsIgnoreCase("###NO_ENTITY###")) {
                annotations.add(annotation);
            }

            if (i % 100 == 0) {
                LOGGER.info("classified " + MathHelper.round(100 * i / entityCandidates.size(), 0) + "%");
            }
            i++;
        }

        return annotations;
    }

    private Annotations verifyAnnotationsNDict(Annotations entityCandidates) {
        Annotations annotations = new Annotations();

        int i = 0;

        // n = 1
        //        for (Annotation annotation : entityCandidates) {
        //
        //            TextInstance ti = tc1n.classify(annotation.getEntity());
        //            annotation.assignCategoryEntries(ti.getAssignedCategoryEntries());
        //
        //            if (!annotation.getMostLikelyTagName().equalsIgnoreCase("###NO_ENTITY###")) {
        //                annotations.add(annotation);
        //            }
        //
        //            if (i % 100 == 0) {
        //                LOGGER.info("classified " + MathHelper.round(100 * i / entityCandidates.size(), 0) + "%");
        //            }
        //            i++;
        //        }

        i = 0;

        // n = 2
        Annotation lastAnnotation = null;
        for (Annotation annotation : entityCandidates) {

            if (i == 0) {
                lastAnnotation = annotation;
                i++;
                continue;
            }
            String combinedEntity = lastAnnotation.getEntity() + " " + annotation.getEntity();

            TextInstance ti = tc2n.classify(combinedEntity);

            // weight them
            if (ti.getCategoryEntry("per") != null) {
                ti.getCategoryEntry("per").addAbsoluteRelevance(51.188877);
            }
            if (ti.getCategoryEntry("org") != null) {
                ti.getCategoryEntry("org").addAbsoluteRelevance(30.995);
            }
            if (ti.getCategoryEntry("loc") != null) {
                ti.getCategoryEntry("loc").addAbsoluteRelevance(47.7989);
            }
            if (ti.getCategoryEntry("misc") != null) {
                ti.getCategoryEntry("misc").addAbsoluteRelevance(49.07199);
            }

            if (ti.getMainCategoryEntry().getCategory().getName().length() > 1 || i == 1) {
                lastAnnotation.assignCategoryEntries(ti.getAssignedCategoryEntries());
            }
            annotation.assignCategoryEntries(ti.getAssignedCategoryEntries());

            if (!ti.getMainCategoryEntry().getCategory().getName().equalsIgnoreCase("###NO_ENTITY###")) {
                annotations.add(lastAnnotation);
                annotations.add(annotation);
            }

            if (i % 100 == 0) {
                LOGGER.info("classified " + MathHelper.round(100 * i / entityCandidates.size(), 0) + "%");
            }

            lastAnnotation = annotation;
            i++;
        }

        return annotations;
    }

    private void weight(String trainingFilePath) {
        // get all training annotations including their features
        Annotations annotations = FileFormatParser.getAnnotationsFromColumnTokenBased(trainingFilePath);

        LOGGER.info("start creating " + annotations.size() + " annotations for training");

        int i = 0;

        // n = 2
        Annotation lastAnnotation = null;
        for (Annotation annotation : annotations) {

            if (i == 0) {
                lastAnnotation = annotation;
                i++;
                continue;
            }
            String combinedEntity = lastAnnotation.getEntity() + " " + annotation.getEntity();

            TextInstance ti = tc2n.classify(combinedEntity);
            if (ti.getMainCategoryEntry().getCategory().getName().length() > 1 || i == 1) {
                lastAnnotation.assignCategoryEntries(ti.getAssignedCategoryEntries());
            }
            annotation.assignCategoryEntries(ti.getAssignedCategoryEntries());

            double highestCategoryRelevance = annotation.getMainCategoryEntry().getAbsoluteRelevance();

            if (annotation.getInstanceCategoryName().equalsIgnoreCase("per")) {
                regPer.get(0).add(annotation.getCategoryEntry("per").getAbsoluteRelevance());
                regPer.get(1).add(highestCategoryRelevance + 1);
            } else if (annotation.getInstanceCategoryName().equalsIgnoreCase("org")) {
                regOrg.get(0).add(annotation.getCategoryEntry("org").getAbsoluteRelevance());
                regOrg.get(1).add(highestCategoryRelevance + 1);
            } else if (annotation.getInstanceCategoryName().equalsIgnoreCase("loc")) {
                regLoc.get(0).add(annotation.getCategoryEntry("loc").getAbsoluteRelevance());
                regLoc.get(1).add(highestCategoryRelevance + 1);
            } else if (annotation.getInstanceCategoryName().equalsIgnoreCase("misc")) {
                regMisc.get(0).add(annotation.getCategoryEntry("misc").getAbsoluteRelevance());
                regMisc.get(1).add(highestCategoryRelevance + 1);
            }

            if (!ti.getMainCategoryEntry().getCategory().getName().equalsIgnoreCase("###NO_ENTITY###")) {
                annotations.add(lastAnnotation);
                annotations.add(annotation);
            }

            if (i % 100 == 0) {
                LOGGER.info("classified " + MathHelper.round(100 * i / annotations.size(), 0) + "%");
            }

            lastAnnotation = annotation;
            i++;
        }

        // show regression values
        double[] x = new double[regPer.get(0).size()];
        double[] y = new double[regPer.get(1).size()];
        for (int j = 0; j < y.length; j++) {
            x[j] = regPer.get(0).get(j);
            y[j] = regPer.get(1).get(j);
        }
        double[] v = MathHelper.performLinearRegression(x, y);
        System.out.println("lin reg per: " + v[0] + ", " + v[1]);

        x = new double[regOrg.get(0).size()];
        y = new double[regOrg.get(1).size()];
        for (int j = 0; j < y.length; j++) {
            x[j] = regOrg.get(0).get(j);
            y[j] = regOrg.get(1).get(j);
        }
        v = MathHelper.performLinearRegression(x, y);
        System.out.println("lin reg org: " + v[0] + ", " + v[1]);

        x = new double[regLoc.get(0).size()];
        y = new double[regLoc.get(1).size()];
        for (int j = 0; j < y.length; j++) {
            x[j] = regLoc.get(0).get(j);
            y[j] = regLoc.get(1).get(j);
        }
        v = MathHelper.performLinearRegression(x, y);
        System.out.println("lin reg loc: " + v[0] + ", " + v[1]);

        x = new double[regMisc.get(0).size()];
        y = new double[regMisc.get(1).size()];
        for (int j = 0; j < y.length; j++) {
            x[j] = regMisc.get(0).get(j);
            y[j] = regMisc.get(1).get(j);
        }
        v = MathHelper.performLinearRegression(x, y);
        System.out.println("lin reg misc: " + v[0] + ", " + v[1]);
    }

    @Override
    public Annotations getAnnotations(String inputText) {

        Annotations annotations = new Annotations();

        // get the candates, every token is potentially a (part of) an entity
        Annotations entityCandidates = getEntityCandidates(inputText);

        // classify annotations with the UniversalClassifier
        annotations.addAll(verifyAnnotationsWithUniversalClassifier(entityCandidates));
        // annotations.addAll(verifyAnnotationsNDict(entityCandidates));

        // combine annotations that are right next to each other having the same tag
        Annotations combinedAnnotations = new Annotations();
        annotations.sort();
        Annotation lastAnnotation = new Annotation(-2, "", "");
        // int lastIndex = -2;
        Annotation lastCombinedAnnotation = null;
        for (Annotation annotation : annotations) {
            if (!annotation.getMostLikelyTagName().equalsIgnoreCase("o")
                    && annotation.getMostLikelyTagName().equalsIgnoreCase(lastAnnotation.getMostLikelyTagName())
                    && annotation.getOffset() == lastAnnotation.getEndIndex() + 1) {

                if (lastCombinedAnnotation == null) {
                    lastCombinedAnnotation = lastAnnotation;
                }

                Annotation combinedAnnotation = new Annotation(lastCombinedAnnotation.getOffset(),
                        lastCombinedAnnotation.getEntity() + " " + annotation.getEntity(),
                        annotation.getMostLikelyTagName(), combinedAnnotations);
                combinedAnnotations.add(combinedAnnotation);
                lastCombinedAnnotation = combinedAnnotation;
                combinedAnnotations.remove(lastCombinedAnnotation);
            } else {
                combinedAnnotations.add(annotation);
                lastCombinedAnnotation = null;
            }

            lastAnnotation = annotation;
        }

        // remove all "O"
        Annotations cleanAnnotations = new Annotations();
        for (Annotation annotation : combinedAnnotations) {
            if (!annotation.getMostLikelyTagName().equalsIgnoreCase("o") && annotation.getLength() > 1) {
                cleanAnnotations.add(annotation);
            }
        }

        FileHelper.writeToFile("data/temp/tudNER2Output.txt", tagText(inputText, cleanAnnotations));

        // weight("data/datasets/ner/conll/training_verysmall.txt");

        return cleanAnnotations;
    }

    public boolean train_(String trainingFilePath, String modelFilePath) {

        // get all training annotations including their features
        Annotations annotations = FileFormatParser.getAnnotationsFromColumnTokenBased(trainingFilePath);

        // create instances with nominal and numeric features
        Instances<UniversalInstance> textInstances = new Instances<UniversalInstance>();

        LOGGER.info("start creating " + annotations.size() + " annotations for training");

        int i = 0;

        // n = 2
        Annotation lastAnnotation = null;
        String combinedEntity = "";
        for (Annotation annotation : annotations) {

            if (i == 0) {
                lastAnnotation = annotation;
                i++;
                continue;
            }
            combinedEntity = lastAnnotation.getEntity() + " " + annotation.getEntity();

            UniversalInstance textInstance = new UniversalInstance(textInstances);
            textInstance.setTextFeature(combinedEntity);

            // get the instance category, only not "O" if all pooled annotations have the same other tag
            // String instanceCategory = "O";
            // String lastCategory = "";
            // // for (Annotation annotation2 : annotationPool) {
            // if (!lastAnnotation.getInstanceCategoryName().equalsIgnoreCase(lastCategory) && lastCategory.length() >
            // 0) {
            // lastCategory = "O";
            // break;
            // } else {
            // lastCategory = lastAnnotation.getInstanceCategoryName();
            // }
            // // }
            // instanceCategory = lastCategory;

            if (lastAnnotation.getInstanceCategoryName().equalsIgnoreCase(annotation.getInstanceCategoryName())) {
                textInstance.setInstanceCategory(lastAnnotation.getInstanceCategory());
            } else {
                textInstance.setInstanceCategory("O");
            }

            textInstances.add(textInstance);

            lastAnnotation = annotation;
            // annotationPool.clear();
            i++;
        }

        // train the text classifier
        tc2n.setTrainingInstances(textInstances);
        tc2n.train();

        saveModel(modelFilePath);

        return true;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {

        // get all training annotations including their features
        Annotations annotations = FileFormatParser.getAnnotationsFromColumnTokenBased(trainingFilePath);

        // create instances with nominal and numeric features
        Instances<UniversalInstance> textInstances = new Instances<UniversalInstance>();
        Instances<UniversalInstance> nominalInstances = new Instances<UniversalInstance>();
        Instances<NumericInstance> numericInstances = new Instances<NumericInstance>();

        LOGGER.info("start creating " + annotations.size() + " annotations for training");
        for (Annotation annotation : annotations) {
            UniversalInstance textInstance = new UniversalInstance(textInstances);
            textInstance.setTextFeature(annotation.getEntity());
            textInstance.setInstanceCategory(annotation.getInstanceCategory());
            textInstances.add(textInstance);

            UniversalInstance nominalInstance = new UniversalInstance(nominalInstances);
            nominalInstance.setNominalFeatures(annotation.getNominalFeatures());
            nominalInstance.setInstanceCategory(annotation.getInstanceCategory());
            nominalInstances.add(nominalInstance);

            NumericInstance numericInstance = new NumericInstance(numericInstances);
            numericInstance.setFeatures(annotation.getNumericFeatures());
            numericInstance.setInstanceCategory(annotation.getInstanceCategory());
            numericInstances.add(numericInstance);
        }

        // train the text classifier
        universalClassifier.getTextClassifier().setTrainingInstances(textInstances);

        // train the numeric classifier with numeric features from the annotations
        universalClassifier.getNumericClassifier().setTrainingInstances(numericInstances);
        universalClassifier.getNumericClassifier().getTrainingInstances().normalize();

        // train the nominal classifier with nominal features from the annotations
        universalClassifier.getNominalClassifier().setTrainingInstances(nominalInstances);

        LOGGER.info("start training classifiers now...");
        universalClassifier.trainAll();
        // universalClassifier.learnClassifierWeights(annotations);

        saveModel(modelFilePath);

        return true;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        TUDNER2 tagger = new TUDNER2();

        // using a column training and testing file
        StopWatch stopWatch = new StopWatch();
        // tagger.train("data/datasets/ner/conll/training_small.txt", "data/temp/tudner2.model");
        // System.exit(0);
        tagger.loadModel("data/temp/tudner2.model");
        // tagger.calculateRemoveAnnotatations(FileFormatParser.getText("data/datasets/ner/conll/training.txt",TaggingFormat.COLUMN));
        EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_validation.txt",
                "data/temp/tudner2.model",
                TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

        System.out.println(stopWatch.getElapsedTimeString());

    }

}