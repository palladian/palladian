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

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instance2;
import ws.palladian.classification.NaiveBayesClassifier;
import ws.palladian.classification.Predictor;
import ws.palladian.classification.WordCorrelation;
import ws.palladian.classification.WordCorrelationMatrix;
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
    
    private final ProcessingPipeline pipeline1;
    private final ProcessingPipeline pipeline2;
    private final TermCorpus termCorpus;
    private final TermCorpus assignedTermCorpus;
    private final Map<PipelineDocument, Set<String>> trainDocuments;
    private Predictor<String> classifier;
    private WordCorrelationMatrix correlationMatrix;
    private int trainCount;
    private final StemmerAnnotator stemmer;
    
    private static final int TRAIN_DOC_LIMIT = 20;
    
    private static final FeatureDescriptor<NumericFeature> PRIOR = FeatureDescriptorBuilder.build("keyphraseness", NumericFeature.class);
    private static final FeatureDescriptor<NumericFeature> COR1 = FeatureDescriptorBuilder.build("cor1", NumericFeature.class);
    private static final FeatureDescriptor<NominalFeature> IS_KEYWORD = FeatureDescriptorBuilder.build("isKeyword", NominalFeature.class);
    
    public ClassifierExtractor() {
        termCorpus = new TermCorpus();
        assignedTermCorpus = new TermCorpus();
        stemmer = new StemmerAnnotator(Language.ENGLISH, Mode.MODIFY);

        pipeline1 = new PerformanceCheckProcessingPipeline();
        pipeline1.add(new RegExTokenizer());
        pipeline1.add(new StopTokenRemover(Language.ENGLISH));
        pipeline1.add(stemmer);
        //pipeline1.add(new AdditionalFeatureExtractor());
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
                    double prior = (double)assignedTermCorpus.getCount(annotation.getValue())/assignedTermCorpus.getNumDocs();
                    annotation.getFeatureVector().add(new NumericFeature(PRIOR, prior));
                }
            }
        });
        pipeline2.add(new PipelineProcessor() {
            private static final long serialVersionUID = 1L;
            @Override
            public void process(PipelineDocument document) {
                AnnotationFeature annotationFeature = document.getFeatureVector().get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
                List<Annotation> annotations = annotationFeature.getValue();
                // avoid nulls
                for (int i = 0; i < annotations.size(); i++) {
                    FeatureVector fv = annotations.get(i).getFeatureVector();
                    if (fv.get(COR1)==null){
                        fv.add(new NumericFeature(COR1, 0.));
                    }
                }
                for (int i = 0; i < annotations.size(); i++) {
                    for (int j = i; j < annotations.size(); j++) {
                        Annotation a1 = annotations.get(i);
                        Annotation a2 = annotations.get(j);
                        WordCorrelation correlation = correlationMatrix.getCorrelation(a1.getValue(), a2.getValue());
                        if (correlation == null) {
                            continue;
                        }
                        FeatureVector a1Fv = a1.getFeatureVector();
                        FeatureVector a2Fv = a2.getFeatureVector();
                        double relCor = correlation.getRelativeCorrelation();
                        a1Fv.add(new NumericFeature(COR1, a1Fv.get(COR1).getValue() + relCor));
                        a2Fv.add(new NumericFeature(COR1, a2Fv.get(COR1).getValue() + relCor));
                    }
                }
            }
        });
        trainDocuments = new HashMap<PipelineDocument,Set<String>>();
        classifier = createClassifier();
        correlationMatrix = new WordCorrelationMatrix();
        trainCount = 0;
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
        correlationMatrix.updateGroup(keyphrases);
    }
    
    @Override
    public void endTraining() {
        System.out.println(pipeline1.toString());
        System.out.println("finished training, # train docs: " + trainDocuments.size());
        System.out.println("calc. wcm ...");
        correlationMatrix.makeRelativeScores();
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
            AnnotationFeature annotationFeature = currentDoc.getFeatureVector().get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
            markCandidates(annotationFeature, keywords);
            annotations.addAll(annotationFeature.getValue());
            trainDocIterator.remove();
        }
        System.out.println("# annotations: " + annotations.size());
        // writeData(annotations, new File("data.csv"));
        int posSamples=0;
        int negSamples=0;
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
        this.trainDocuments.clear();
        this.termCorpus.reset();
        this.classifier=createClassifier();
        trainCount = 0;
        super.reset();
    }

    private Predictor<String> createClassifier() {
        //return new WekaPredictor(new NaiveBayes());
        //return new WekaPredictor(new Bagging());
        //return new DecisionTreeClassifier();
        return new NaiveBayesClassifier();
    }

    private void writeData(List<Annotation> annotations, File outputCsvFile) {
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
        Set<String> temp = new HashSet<String>();
        // try to match multiple different variants
        for (String k : keywords) {
            temp.add(k.toLowerCase().trim());
            temp.add(k.toLowerCase().trim().replace("\\s", ""));
            temp.add(stem(k.toLowerCase()).trim());
            temp.add(stem(k.toLowerCase()).trim().replace("\\s", ""));
        }
        keywords = temp;
        List<Annotation> annotations = annotationFeature.getValue();
        for (Annotation annotation : annotations) {
            String stemmedValue = annotation.getValue();
            String unstemmedValue = annotation.getFeatureVector().get(StemmerAnnotator.UNSTEM).getValue();
            
            boolean isKeyword = keywords.contains(stemmedValue);
            isKeyword |= keywords.contains(stemmedValue.toLowerCase());
            isKeyword |= keywords.contains(stemmedValue.replace(" ", ""));
            isKeyword |= keywords.contains(stemmedValue.toLowerCase().replace(" ",""));
            isKeyword |= keywords.contains(unstemmedValue);
            isKeyword |= keywords.contains(unstemmedValue.toLowerCase());
            isKeyword |= keywords.contains(unstemmedValue.replace(" ", ""));
            isKeyword |= keywords.contains(unstemmedValue.toLowerCase().replace(" ", ""));
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
        AnnotationFeature annotationFeature = document.getFeatureVector().get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
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
