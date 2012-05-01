package ws.palladian.extraction.keyphrase.extractors;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import weka.classifiers.meta.Bagging;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instance2;
import ws.palladian.classification.Predictor;
import ws.palladian.classification.WekaPredictor;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PerformanceCheckProcessingPipeline;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.ProcessingPipeline;
import ws.palladian.extraction.feature.DuplicateTokenRemover;
import ws.palladian.extraction.feature.IdfAnnotator;
import ws.palladian.extraction.feature.LengthTokenRemover;
import ws.palladian.extraction.feature.NGramCreator2;
import ws.palladian.extraction.feature.RegExTokenRemover;
import ws.palladian.extraction.feature.StemmerAnnotator;
import ws.palladian.extraction.feature.StemmerAnnotator.Mode;
import ws.palladian.extraction.feature.StopTokenRemover;
import ws.palladian.extraction.feature.TermCorpus;
import ws.palladian.extraction.feature.TfIdfAnnotator;
import ws.palladian.extraction.feature.TokenMetricsCalculator;
import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.extraction.keyphrase.features.AdditionalFeatureExtractor;
import ws.palladian.extraction.keyphrase.features.PhrasenessAnnotator;
import ws.palladian.extraction.keyphrase.temp.CooccurrenceMatrix;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.model.features.NumericFeature;

public class ClassifierExtractor extends KeyphraseExtractor {


    /** The path to the model file for the LingPipe POS tagger. */
    private static final File LING_PIPE_POS_MODEL = new File("/Users/pk/Dropbox/Uni/Models/pos-en-general-brown.HiddenMarkovModel");
    
    /** The CSV file with classification data, i.e. all Annotations + their Feature Vector. */
    private static final File CLASSIFICATION_DATA = new File("data/temp/classifierKeyphraseExtractor.csv");
    
    private final ProcessingPipeline pipeline1;
    private final ProcessingPipeline pipeline2;
    private final TermCorpus termCorpus;
    private final TermCorpus assignedTermCorpus;
    private final Map<PipelineDocument, Set<String>> trainDocuments;

    private Predictor<String> classifier;
    private CooccurrenceMatrix<String> cooccurrenceMatrix;
    private int trainCount;
    private final StemmerAnnotator stemmer;

    private static final double COOCURRENCE_REWEIGHTING_FACTOR = 0.75;

    //usually 20, for testing 5.
//    private static final int TRAIN_DOC_LIMIT = 5;
    private static final int TRAIN_DOC_LIMIT = 20;
//    private static final int TRAIN_DOC_LIMIT = 35;

    private static final FeatureDescriptor<NumericFeature> PRIOR = FeatureDescriptorBuilder.build("keyphraseness",
            NumericFeature.class);
    private static final FeatureDescriptor<NumericFeature> COOCURRENCE_SCALED = FeatureDescriptorBuilder.build(
            "scaledCooccurrence", NumericFeature.class);
    private static final FeatureDescriptor<NumericFeature> COOCCURRENCE_MAX = FeatureDescriptorBuilder.build(
            "maxCooccurrence", NumericFeature.class);
    static final FeatureDescriptor<NominalFeature> IS_KEYWORD = FeatureDescriptorBuilder.build("isKeyword",
            NominalFeature.class);

    public ClassifierExtractor() {
        termCorpus = new TermCorpus();
        assignedTermCorpus = new TermCorpus();
        trainDocuments = new HashMap<PipelineDocument, Set<String>>();
        classifier = createClassifier();
        cooccurrenceMatrix = new CooccurrenceMatrix<String>();
        trainCount = 0;

        stemmer = new StemmerAnnotator(Language.ENGLISH, Mode.MODIFY);

        pipeline1 = new PerformanceCheckProcessingPipeline();
        pipeline1.add(new RegExTokenizer());
        
        /* pipeline1.add(new LingPipePosTagger(LING_PIPE_POS_MODEL));
        
        // abbreviate the POS tags, e.g. NNS = N
        pipeline1.add(new PipelineProcessor() {
            private static final long serialVersionUID = 1L;
            @Override
            public void process(PipelineDocument document) throws DocumentUnprocessableException {
                List<Annotation> annotations = document.getFeatureVector().get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR).getValue();
                for (Annotation annotation : annotations) {
                    FeatureVector featureVector = annotation.getFeatureVector();
                    NominalFeature posFeature = featureVector.get(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR);
                    String abbreviatedPosTag = posFeature.getValue().substring(0, 1);
                    featureVector.add(new NominalFeature(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR, abbreviatedPosTag));
                }
            }
        }); */
        // pipeline1.add(new StopTokenRemover(Language.ENGLISH));
        try {
            pipeline1.add(new StopTokenRemover(ResourceHelper.getResourceFile("/stopwords_en_small.txt")));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
        pipeline1.add(stemmer);
        // further features to consider:
        // startsUppercase
        // completeUppercase
        // containsNumbers
        // containsSpecialCharacters
        // isNumber
        // caseSignature
        // previousStopword, nextStopword, ...
        // NER
        // isInBrackets
        // isInQuotes
        // positionInSentence (begin|middle|end)
        // gerund (-ing?)
        pipeline1.add(new LengthTokenRemover(4));
        pipeline1.add(new RegExTokenRemover("[^A-Za-z0-9-]+"));
        pipeline1.add(new NGramCreator2(3));
        pipeline1.add(new AdditionalFeatureExtractor());

        pipeline1.add(new TokenMetricsCalculator());
        pipeline1.add(new PhrasenessAnnotator());
        pipeline1.add(new DuplicateTokenRemover());

        pipeline2 = new ProcessingPipeline();
        pipeline2.add(new IdfAnnotator(termCorpus));
        pipeline2.add(new TfIdfAnnotator());

        // keyphraseness annotation; i.e. the "prior probability" of a keyphrase occurrence in the training corpus
        pipeline2.add(new PipelineProcessor() {
            private static final long serialVersionUID = 1L;

            @Override
            public void process(PipelineDocument document) {
                FeatureVector featureVector = document.getFeatureVector();
                AnnotationFeature annotationFeature = featureVector.get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
                List<Annotation> annotations = annotationFeature.getValue();
                for (Annotation annotation : annotations) {
                    double prior = (double)assignedTermCorpus.getCount(annotation.getValue())
                            / assignedTermCorpus.getNumDocs();
                    annotation.getFeatureVector().add(new NumericFeature(PRIOR, prior));
                }
            }
        });

        // co-occurrence annotation
        pipeline2.add(new PipelineProcessor() {
            private static final long serialVersionUID = 1L;

            @Override
            public void process(PipelineDocument document) {
                AnnotationFeature annotationFeature = document.getFeatureVector().get(
                        TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
                List<Annotation> annotations = annotationFeature.getValue();
                // pre initialize the co-occurrence feature
                for (Annotation annotation : annotations) {
                    FeatureVector featureVector = annotation.getFeatureVector();
                    if (featureVector.get(COOCCURRENCE_MAX) == null) {
                        featureVector.add(new NumericFeature(COOCCURRENCE_MAX, 0.));
                    }
                }
                
                Map<String, Double> summedCooccurrences = new HashMap<String,Double>();
                double maxCooccurrence = Integer.MIN_VALUE;

                for (int i = 0; i < annotations.size(); i++) {
                    Annotation annotation1 = annotations.get(i);
                    FeatureVector annotation1fv = annotation1.getFeatureVector();
                    String value1 = annotation1.getValue();

                    double annotation1sum = 0;

                    for (int j = 0; j < annotations.size(); j++) {
                        Annotation annotation2 = annotations.get(j);
                        String value2 = annotation2.getValue();
                        if (value1.equals(value2)) {
                            continue;
                        }
                        double condProb = cooccurrenceMatrix.getConditionalProbability(value1, value2);

                        // double condProbLap = cooccurrenceMatrix.getConditionalProbabilityLaplace(value1, value2);
                        annotation1fv.add(new NumericFeature(COOCCURRENCE_MAX, Math.max(
                                annotation1fv.get(COOCCURRENCE_MAX).getValue(), condProb)));
                        annotation1sum += condProb;
                    }
                    summedCooccurrences.put(value1, annotation1sum);
                    maxCooccurrence = Math.max(annotation1sum, maxCooccurrence);
                    annotation1sum = 0;
                }
                
                for (Annotation annotation : annotations) {
                    String annotationValue = annotation.getValue();
                    double scaledCoocurrence = summedCooccurrences.get(annotationValue) / maxCooccurrence;
                    annotation.getFeatureVector().add(new NumericFeature(COOCURRENCE_SCALED, scaledCoocurrence));
                }
            }
        });
    }

    @Override
    public boolean needsTraining() {
        return true;
    }

    @Override
    public void train(String inputText, Set<String> keyphrases) {
        PipelineDocument document = new PipelineDocument(inputText);
        try {
            pipeline1.process(document);
        } catch (DocumentUnprocessableException e) {
            throw new IllegalStateException(e);
        }
        AnnotationFeature feature = document.getFeatureVector().get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = feature.getValue();
        Set<String> terms = new HashSet<String>();
        for (Annotation annotation : annotations) {
            terms.add(annotation.getValue());
        }
        termCorpus.addTermsFromDocument(terms);
        assignedTermCorpus.addTermsFromDocument(new HashSet<String>(canonicalize(stem(keyphrases))));
        // only keep the first n documents because of memory issues for now
        if (trainCount <= TRAIN_DOC_LIMIT) {
            trainDocuments.put(document, keyphrases);
        }
        trainCount++;
        cooccurrenceMatrix.addAll(canonicalize(stem(keyphrases)));
    }

    @Override
    public void endTraining() {
        System.out.println(pipeline1.toString());
        System.out.println("finished building corpus, # train docs: " + trainDocuments.size());
        List<Annotation> annotations = new ArrayList<Annotation>();
        Iterator<Entry<PipelineDocument, Set<String>>> trainDocIterator = trainDocuments.entrySet().iterator();
        int totalKeyphrases = 0;
        int totallyMarked = 0;
        while (trainDocIterator.hasNext()) {
            Entry<PipelineDocument, Set<String>> currentEntry = trainDocIterator.next();
            PipelineDocument currentDoc = currentEntry.getKey();
            Set<String> keywords = currentEntry.getValue();
            try {
                pipeline2.process(currentDoc);
            } catch (DocumentUnprocessableException e) {
                throw new IllegalStateException(e);
            }
            AnnotationFeature annotationFeature = currentDoc.getFeatureVector().get(
                    TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
            totalKeyphrases += keywords.size();
            totallyMarked += markCandidates(annotationFeature, keywords);
            annotations.addAll(annotationFeature.getValue());
            trainDocIterator.remove();
            System.out.println(trainDocuments.size());
        }
        System.out.println("# annotations: " + annotations.size());
        System.out.println("% sample coverage: " + (double) totallyMarked / totalKeyphrases);
        // writeData(annotations, CLASSIFICATION_DATA);
        int posSamples = 0;
        int negSamples = 0;
        List<Instance2<String>> instances = new ArrayList<Instance2<String>>();
        for (Annotation annotation : annotations) {
            FeatureVector featureVector = annotation.getFeatureVector();
            Instance2<String> instance = new Instance2<String>();
            instance.target = featureVector.get(IS_KEYWORD).getValue();
            FeatureVector cleanedFv = cleanFeatureVector(featureVector);
            if ("true".equals(instance.target)) {
                posSamples++;
            } else {
                negSamples++;
            }
            instance.featureVector = cleanedFv;
            instances.add(instance);
        }
        System.out.println("# negative samples: " + negSamples);
        System.out.println("# positive samples: " + posSamples);
        System.out.println("% positive sample rate: " + (double) posSamples / (negSamples+posSamples));
        System.out.println("building classifier ...");
        classifier.learn(instances);
        System.out.println(classifier.toString());
        System.out.println("... finished building classifier.");
    }

    private FeatureVector cleanFeatureVector(FeatureVector featureVector) {
        FeatureVector result = new FeatureVector(featureVector);
        result.remove(IS_KEYWORD);
        result.remove(StemmerAnnotator.UNSTEM);
        return result;
    }

    @Override
    public void reset() {
        //
        // make sure to reset/re-initialize all instance variables here, to allow a clean new run.
        //
        this.trainDocuments.clear();
        this.termCorpus.reset();
        this.assignedTermCorpus.reset();
        this.classifier = createClassifier();
        this.cooccurrenceMatrix = new CooccurrenceMatrix<String>();
        this.trainCount = 0;
        super.reset();
        System.out.println("reset the classifier");
    }

    private Predictor<String> createClassifier() {
        // return new WekaPredictor(new NaiveBayes());
        return new WekaPredictor(new Bagging());
        // return new DecisionTreeClassifier();
        // return new NaiveBayesClassifier();
    }

    public static void writeData(List<Annotation> annotations, File outputCsvFile) {
        System.out.println("writing data to " + outputCsvFile);
        if (outputCsvFile.exists()) {
            System.out.println("output file exists ... deleting.");
            outputCsvFile.delete();
        }
        // get feature names for header
        Annotation firstAnnotation = annotations.iterator().next();
        FeatureVector featureVector = firstAnnotation.getFeatureVector();
        Feature<?>[] valueArray = featureVector.toArray();
        List<String> featureNames = new ArrayList<String>();
        for (Feature<?> feature : valueArray) {
            String featureName = feature.getName();
            featureNames.add(featureName);
        }
        String headerLine = StringUtils.join(featureNames, ";");
        FileHelper.appendFile(outputCsvFile.getAbsolutePath(), headerLine + "\n");

        // get values
        int progress = 0;
        for (Annotation annotation : annotations) {
            if (progress % 1000 == 0) {
                System.out.println(progress + "/" + annotations.size());
            }
            FeatureVector featureVector2 = annotation.getFeatureVector();
            Feature<?>[] valueArray2 = featureVector2.toArray();
            List<String> featureValues = new ArrayList<String>();
            for (Feature<?> feature : valueArray2) {
                String featureValue = feature.getValue().toString();
                featureValues.add(featureValue);
            }
            String featureLine = StringUtils.join(featureValues, ";");
            FileHelper.appendFile(outputCsvFile.getAbsolutePath(), featureLine + "\n");
            progress++;
        }
        System.out.println("wrote data to " + outputCsvFile);
    }

    /**
     * <p>
     * Takes a list of candidates in form of {@link Annotation}s and a list of "real" keyphrases and tries to match
     * those keyphrases in the supplied annotations. Use fuzzy/multiple variant matching for high recall (i.e. match as
     * many annotations as possible).
     * </p>
     * 
     * @param annotationFeature
     * @param keywords
     * @return
     */
    private int markCandidates(AnnotationFeature annotationFeature, Set<String> keywords) {
        Set<String> modifiedKeywords = new HashSet<String>();
        int marked = 0;
        // try to match multiple different variants
        for (String keyword : keywords) {
            modifiedKeywords.add(keyword.toLowerCase().trim());
            modifiedKeywords.add(keyword.toLowerCase().trim().replaceAll("\\s", ""));
            modifiedKeywords.add(stem(keyword.toLowerCase()).trim());
            modifiedKeywords.add(stem(keyword.toLowerCase()).trim().replaceAll("\\s", ""));
            modifiedKeywords.add(canonicalize(keyword.toLowerCase().trim()));
            modifiedKeywords.add(canonicalize(keyword.toLowerCase().trim().replaceAll("\\s", "")));
            modifiedKeywords.add(canonicalize(stem(keyword.toLowerCase()).trim()));
            modifiedKeywords.add(canonicalize(stem(keyword.toLowerCase()).trim().replaceAll("\\s", "")));
        }
        List<Annotation> annotations = annotationFeature.getValue();
        for (Annotation annotation : annotations) {
            String stemmedValue = annotation.getValue();
            String unstemmedValue = annotation.getFeatureVector().get(StemmerAnnotator.UNSTEM).getValue();

            boolean isKeyword = modifiedKeywords.contains(stemmedValue);
            isKeyword |= modifiedKeywords.contains(stemmedValue.toLowerCase());
            isKeyword |= modifiedKeywords.contains(stemmedValue.replaceAll("\\s", ""));
            isKeyword |= modifiedKeywords.contains(stemmedValue.toLowerCase().replaceAll("\\s", ""));
            isKeyword |= modifiedKeywords.contains(unstemmedValue);
            isKeyword |= modifiedKeywords.contains(unstemmedValue.toLowerCase());
            isKeyword |= modifiedKeywords.contains(unstemmedValue.replaceAll("\\s", ""));
            isKeyword |= modifiedKeywords.contains(unstemmedValue.toLowerCase().replaceAll("\\s", ""));
            isKeyword |= modifiedKeywords.contains(canonicalize(stemmedValue));
            isKeyword |= modifiedKeywords.contains(canonicalize(stemmedValue.toLowerCase()));
            isKeyword |= modifiedKeywords.contains(canonicalize(stemmedValue.replaceAll("\\s", "")));
            isKeyword |= modifiedKeywords.contains(canonicalize(stemmedValue.toLowerCase().replaceAll("\\s", "")));
            isKeyword |= modifiedKeywords.contains(canonicalize(unstemmedValue));
            isKeyword |= modifiedKeywords.contains(canonicalize(unstemmedValue.toLowerCase()));
            isKeyword |= modifiedKeywords.contains(canonicalize(unstemmedValue.replaceAll("\\s", "")));
            isKeyword |= modifiedKeywords.contains(canonicalize(unstemmedValue.toLowerCase().replaceAll("\\s", "")));
            NominalFeature isKeywordFeature = new NominalFeature(IS_KEYWORD, String.valueOf(isKeyword));
            annotation.getFeatureVector().add(isKeywordFeature);
            if (isKeyword) {
                marked++;
            }
        }
        return marked;
    }
    
    /**
     * <p>
     * Re-orders all tokens alphabetically, i.e.
     * <code>the quick brown fox<code> will be transformed to <code>brown fox quick the<code>.
     * </p>
     * 
     * @param string
     * @return
     */
    private static String canonicalize(String string) {
        List<String> result = CollectionHelper.newArrayList();
        for (String s : string.split("\\s")) {
            result.add(s);
        }
        Collections.sort(result);
        return StringUtils.join(result, " ");
    }
    
    private static List<String> canonicalize(Collection<String> strings) {
        List<String> result = CollectionHelper.newArrayList();
        for (String s : strings) {
            result.add(canonicalize(s));
        }
        return result;
    }

    private String stem(String string) {
        List<String> stems = new ArrayList<String>();
        for (String s : string.split("\\s")) {
            stems.add(stemmer.stem(s));
        }
        return StringUtils.join(stems, " ");
    }
    
    private Set<String> stem(Set<String> strings) {
        Set<String> stems = CollectionHelper.newHashSet();
        for (String string : strings) {
            stems.add(stem(string));
        }
        return stems;
    }

    @Override
    public List<Keyphrase> extract(String inputText) {
        PipelineDocument document = new PipelineDocument(inputText);
        try {
            pipeline1.process(document);
            pipeline2.process(document);
        } catch (DocumentUnprocessableException e) {
            throw new IllegalStateException();
        }
        AnnotationFeature annotationFeature = document.getFeatureVector().get(
                TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();
        List<Keyphrase> keywords = new ArrayList<Keyphrase>();
        for (Annotation annotation : annotations) {
            FeatureVector featureVector = annotation.getFeatureVector();
            FeatureVector cleanFv = cleanFeatureVector(featureVector);
            CategoryEntries predictionResult = classifier.predict(cleanFv);
            CategoryEntry trueCategory = predictionResult.getCategoryEntry("true");
            if (trueCategory != null) {
                keywords.add(new Keyphrase(annotation.getValue(), trueCategory.getAbsoluteRelevance()));
            }
        }
        reRankCooccurrences(keywords);
        //reRankOverlaps(keywords);
        Collections.sort(keywords);
        if (keywords.size() > getKeyphraseCount()) {
            keywords.subList(getKeyphraseCount(), keywords.size()).clear();
        }
        return keywords;
    }

    
    /**
     * <p>
     * Re-calculate the weight of the list of {@link Keyphrase}s, based on their overlap. Reduce the weights of those
     * candidates which are contained in another candiate. E.g. list contains <code>web</code> with weight
     * <code>0.5</code> and <code>web browser</code> with weight <code>0.7</code>, then weight of <code>web</code> if
     * re-calcualted to <code>0.7 - 0.5 = 0.2</code>.
     * </p>
     * 
     * @param keywords
     */
    private void reRankOverlaps(List<Keyphrase> keywords) {
        for (Keyphrase k1 : keywords) {
            for (Keyphrase k2 : keywords) {
                if (k1.getValue().equals(k2.getValue())) {
                    continue;
                }
                if (k1.getValue().contains(k2.getValue())) {
                     k2.setWeight(k2.getWeight() - k1.getWeight());
                }
            }
        }
    }
    
    private void reRankCooccurrences(List<Keyphrase> keywords) {
        for (Keyphrase k1 : keywords) {
            double oldWeight = k1.getWeight();
            double summedConditionalProbs = 0;
            String value1 = k1.getValue();
            for (Keyphrase k2 : keywords) {
                String value2 = k2.getValue();
                if (value1.equals(value2)) {
                    continue;
                }
                summedConditionalProbs += cooccurrenceMatrix.getConditionalProbabilityLaplace(value2, value1);
            }
            double newWeight = oldWeight + COOCURRENCE_REWEIGHTING_FACTOR * summedConditionalProbs * oldWeight;
            k1.setWeight(newWeight);
        }
    }

    @Override
    public String getExtractorName() {
        return "ClassifierExtractor";
    }
    
    @Override
    public String toString() {
        StringBuilder description = new StringBuilder();
        description.append("ClassifierExtractor");
        description.append(" Pipeline1=").append(pipeline1);
        description.append(" Pipeline2=").append(pipeline2);
        description.append(" #trainDocuments=").append(TRAIN_DOC_LIMIT);
        description.append(" coocurrenceReWeightingFactor=").append(COOCURRENCE_REWEIGHTING_FACTOR);
        return description.toString();
    }

}
