package ws.palladian.extraction.keyphrase.extractors;

import java.io.File;
import java.util.ArrayList;
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
import ws.palladian.extraction.keyphrase.features.PhrasenessAnnotator;
import ws.palladian.extraction.keyphrase.temp.CooccurrenceMatrix;
import ws.palladian.extraction.pos.LingPipePosTagger;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
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
    
    private final ProcessingPipeline pipeline1;
    private final ProcessingPipeline pipeline2;
    private final TermCorpus termCorpus;
    private final TermCorpus assignedTermCorpus;
    private final Map<PipelineDocument, Set<String>> trainDocuments;

    private Predictor<String> classifier;
    private CooccurrenceMatrix<String> cooccurrenceMatrix;
    private int trainCount;
    private final StemmerAnnotator stemmer;

    // private static final int TRAIN_DOC_LIMIT = 20;
    private static final int TRAIN_DOC_LIMIT = 50;

    private static final FeatureDescriptor<NumericFeature> PRIOR = FeatureDescriptorBuilder.build("keyphraseness",
            NumericFeature.class);
    private static final FeatureDescriptor<NumericFeature> COOCCURRENCE_MULT = FeatureDescriptorBuilder.build(
            "cooccurrenceMultiplied", NumericFeature.class);
    // private static final FeatureDescriptor<NumericFeature> COOCCURRENCE_ADD = FeatureDescriptorBuilder.build(
    // "cooccurrenceSummed", NumericFeature.class);
    private static final FeatureDescriptor<NominalFeature> IS_KEYWORD = FeatureDescriptorBuilder.build("isKeyword",
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
        pipeline1.add(new LingPipePosTagger(LING_PIPE_POS_MODEL));
        
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
        });
        pipeline1.add(new StopTokenRemover(Language.ENGLISH));
        pipeline1.add(stemmer);
        // pipeline1.add(new AdditionalFeatureExtractor());
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
        pipeline1.add(new LengthTokenRemover(4));
        pipeline1.add(new RegExTokenRemover("[^A-Za-z0-9-]+"));
        pipeline1.add(new NGramCreator2(3));

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
                    if (featureVector.get(COOCCURRENCE_MULT) == null) {
                        featureVector.add(new NumericFeature(COOCCURRENCE_MULT, 1.));
                        // featureVector.add(new NumericFeature(COOCCURRENCE_ADD, 0.));
                    }
                }
                for (int i = 0; i < annotations.size(); i++) {
                    Annotation annotation1 = annotations.get(i);
                    FeatureVector annotation1fv = annotation1.getFeatureVector();
                    String annotation1value = annotation1.getValue();
                    for (int j = 0; j < annotations.size(); j++) {
                        Annotation annotation2 = annotations.get(j);
                        String annotation2Value = annotation2.getValue();
                        if (annotation1value.equals(annotation2Value)) {
                            continue;
                        }
                        double prob = cooccurrenceMatrix.getConditionalProbabilityLaplace(annotation1value,
                                annotation2Value);
                        annotation1fv.add(new NumericFeature(COOCCURRENCE_MULT, annotation1fv.get(COOCCURRENCE_MULT)
                                .getValue() * prob));
                        // annotation1fv.add(new NumericFeature(COOCCURRENCE_ADD, annotation1fv.get(COOCCURRENCE_ADD)
                        // .getValue() + Math.log(prob)));
                    }
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
        assignedTermCorpus.addTermsFromDocument(keyphrases);
        // only keep the first n documents because of memory issues for now
        if (trainCount <= TRAIN_DOC_LIMIT) {
            trainDocuments.put(document, keyphrases);
        }
        trainCount++;
        cooccurrenceMatrix.addAll(keyphrases);
    }

    @Override
    public void endTraining() {
        System.out.println(pipeline1.toString());
        System.out.println("finished training, # train docs: " + trainDocuments.size());
        System.out.println("calc. wcm ...");
        System.out.println("finished wcm.");
        List<Annotation> annotations = new ArrayList<Annotation>();
        Iterator<Entry<PipelineDocument, Set<String>>> trainDocIterator = trainDocuments.entrySet().iterator();
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
            markCandidates(annotationFeature, keywords);
            annotations.addAll(annotationFeature.getValue());
            trainDocIterator.remove();
        }
        System.out.println("# annotations: " + annotations.size());
        // writeData(annotations, new File("data.csv"));
        int posSamples = 0;
        int negSamples = 0;
        List<Instance2<String>> instances = new ArrayList<Instance2<String>>();
        for (Annotation annotation : annotations) {
            FeatureVector featureVector = annotation.getFeatureVector();
            Instance2<String> instance = new Instance2<String>();
            instance.target = featureVector.get(IS_KEYWORD).getValue();
            pruneFeatureVector(featureVector);
            if ("true".equals(instance.target)) {
                posSamples++;
            } else {
                negSamples++;
            }
            instance.featureVector = featureVector;
            instances.add(instance);
        }
        System.out.println("positive Samples: " + posSamples);
        System.out.println("negative Samples: " + negSamples);
        System.out.println("... building classifier.");
        classifier.learn(instances);
        System.out.println(classifier.toString());
        System.out.println("... finished building classifier.");
    }

    private void pruneFeatureVector(FeatureVector featureVector) {
        featureVector.remove(IS_KEYWORD);
        featureVector.remove(StemmerAnnotator.UNSTEM);
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
        trainCount = 0;
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

    private void markCandidates(AnnotationFeature annotationFeature, Set<String> keywords) {
        Set<String> modifiedKeyowrds = new HashSet<String>();
        // try to match multiple different variants
        for (String keyword : keywords) {
            modifiedKeyowrds.add(keyword.toLowerCase().trim());
            modifiedKeyowrds.add(keyword.toLowerCase().trim().replace("\\s", ""));
            modifiedKeyowrds.add(stem(keyword.toLowerCase()).trim());
            modifiedKeyowrds.add(stem(keyword.toLowerCase()).trim().replace("\\s", ""));
        }
        List<Annotation> annotations = annotationFeature.getValue();
        for (Annotation annotation : annotations) {
            String stemmedValue = annotation.getValue();
            String unstemmedValue = annotation.getFeatureVector().get(StemmerAnnotator.UNSTEM).getValue();

            boolean isKeyword = modifiedKeyowrds.contains(stemmedValue);
            isKeyword |= modifiedKeyowrds.contains(stemmedValue.toLowerCase());
            isKeyword |= modifiedKeyowrds.contains(stemmedValue.replace(" ", ""));
            isKeyword |= modifiedKeyowrds.contains(stemmedValue.toLowerCase().replace(" ", ""));
            isKeyword |= modifiedKeyowrds.contains(unstemmedValue);
            isKeyword |= modifiedKeyowrds.contains(unstemmedValue.toLowerCase());
            isKeyword |= modifiedKeyowrds.contains(unstemmedValue.replace(" ", ""));
            isKeyword |= modifiedKeyowrds.contains(unstemmedValue.toLowerCase().replace(" ", ""));
            NominalFeature isKeywordFeature = new NominalFeature(IS_KEYWORD, String.valueOf(isKeyword));
            annotation.getFeatureVector().add(isKeywordFeature);
        }
    }

    private String stem(String string) {
        List<String> stems = new ArrayList<String>();
        for (String s : string.split("\\s")) {
            stems.add(stemmer.stem(s));
        }
        return StringUtils.join(stems, " ");
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
            pruneFeatureVector(featureVector);
            CategoryEntries predictionResult = classifier.predict(featureVector);
            CategoryEntry trueCategory = predictionResult.getCategoryEntry("true");
            if (trueCategory != null) {
                keywords.add(new Keyphrase(annotation.getValue(), trueCategory.getAbsoluteRelevance()));
            }
        }
        Collections.sort(keywords);
        if (keywords.size() > getKeyphraseCount()) {
            keywords.subList(getKeyphraseCount(), keywords.size()).clear();
        }
        return keywords;
    }

    @Override
    public String getExtractorName() {
        return "ClassifierExtractor";
    }

}
