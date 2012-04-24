package ws.palladian.extraction.keyphrase.extractors;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.lang3.StringUtils;

import com.hp.hpl.jena.query.extension.library.print;

import weka.classifiers.bayes.NaiveBayes;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instance2;
import ws.palladian.classification.NaiveBayesClassifier;
import ws.palladian.classification.Predictor;
import ws.palladian.classification.WekaPredictor;
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
import ws.palladian.extraction.feature.NGramCreator;
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
import ws.palladian.extraction.token.OpenNlpTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
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
//    private /*final*/ DecisionTreeClassifier classifier;
//    private WekaBaggingPredictor classifier;
    private NaiveBayesClassifier classifier;
    private WordCorrelationMatrix correlationMatrix;
    
    public static final FeatureDescriptor<NumericFeature> PRIOR = FeatureDescriptorBuilder.build("keyphraseness", NumericFeature.class);
    public static final FeatureDescriptor<NumericFeature> COR1 = FeatureDescriptorBuilder.build("cor1", NumericFeature.class);
//    public static final FeatureDescriptor<NumericFeature> COR2 = FeatureDescriptorBuilder.build("cor2", NumericFeature.class);
//    public static final FeatureDescriptor<NumericFeature> COR3 = FeatureDescriptorBuilder.build("cor3", NumericFeature.class);

    
    public ClassifierExtractor() {
        termCorpus = new TermCorpus();
        assignedTermCorpus = new TermCorpus();

        // trainingPipeline = new ProcessingPipeline();
        pipeline1 = new PerformanceCheckProcessingPipeline();
        pipeline1.add(new RegExTokenizer());
        pipeline1.add(new StopTokenRemover(Language.ENGLISH));
        stemmer = new StemmerAnnotator(Language.ENGLISH, Mode.MODIFY);
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
                AnnotationFeature annotationFeature = featureVector.get(RegExTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
                List<Annotation> annotations = annotationFeature.getValue();
                for (Annotation annotation : annotations) {
                    //double prior = assignedTermCorpus.getDf(annotation.getValue());
                    double prior = (double)assignedTermCorpus.getCount(annotation.getValue())/assignedTermCorpus.getNumDocs();
                    annotation.getFeatureVector().add(new NumericFeature(PRIOR, prior));
                }
            }
        });
        pipeline2.add(new PipelineProcessor() {
            @Override
            public void process(PipelineDocument document) {
                AnnotationFeature annotationFeature = document.getFeatureVector().get(RegExTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
                List<Annotation> annotations = annotationFeature.getValue();
                // avoid nulls
                for (int i = 0; i < annotations.size(); i++) {
                    FeatureVector fv = annotations.get(i).getFeatureVector();
                    if (fv.get(COR1)==null){
                        fv.add(new NumericFeature(COR1, 0.));
//                        fv.add(new NumericFeature(COR2, 0.));
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
//                        a1Fv.add(new NumericFeature(COR2, a1Fv.get(COR2).getValue() * relCor));
//                        a2Fv.add(new NumericFeature(COR2, a2Fv.get(COR2).getValue() * relCor));
                    }
                }
            }
        });
        trainDocuments = new HashMap<PipelineDocument,Set<String>>();
//        classifier = new WekaBaggingPredictor(new NaiveBayes());
        classifier = new NaiveBayesClassifier();
        correlationMatrix = new WordCorrelationMatrix();
    }

    @Override
    public boolean needsTraining() {
        return true;
    }
    
    int trainCount = 0;
    private StemmerAnnotator stemmer;
    
    @Override
    public void train(String inputText, Set<String> keyphrases) {
//        if (trainCount >= 5) {
//            return;
//        }
        PipelineDocument document = new PipelineDocument(inputText);
        try {
            pipeline1.process(document);
        } catch (DocumentUnprocessableException e) {
            throw new IllegalStateException(e);
        }
        AnnotationFeature feature = document.getFeatureVector().get(RegExTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = feature.getValue();
        Set<String> terms = new HashSet<String>();
        for (Annotation annotation : annotations) {
            FeatureVector featureVector = annotation.getFeatureVector();
            // String value = featureVector.get(StemmerAnnotator.PROVIDED_FEATURE_DESCRIPTOR).getValue();
            String value = annotation.getValue();
            terms.add(value);
        }
        termCorpus.addTermsFromDocument(terms);
        assignedTermCorpus.addTermsFromDocument(keyphrases);
        // only keep the first n documents because of memory issues for now
        if (trainCount <= 20) {
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
            AnnotationFeature annotationFeature = currentDoc.getFeatureVector().get(RegExTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
            markCandidates(annotationFeature, keywords);
            annotations.addAll(annotationFeature.getValue());
            trainDocIterator.remove();
        }
        System.out.println("# annotations: " + annotations.size());
        writeData(annotations, new File("data5.1.csv"));
        int posSamples=0;
        int negSamples=0;
        List<Instance2<String>> instances = new ArrayList<Instance2<String>>();
        for (Annotation annotation : annotations) {
            FeatureVector featureVector = annotation.getFeatureVector();

            Instance2<String> instance = new Instance2<String>();
            instance.target = ((NominalFeature)featureVector.get("isKeyword")).getValue();
            processFeatureVector(featureVector);
            if (outputFeatures == false) {
                outputFeatures=true;
                System.out.println(featureVector);
            }
            if ("true".equals(instance.target)) {
                posSamples++;
            } else {
                negSamples++;
            }
//            classifier.train(featureVector);
//            NominalFeature feature = (NominalFeature)featureVector.get("isKeyword");
//            if ("true".equals(feature.getValue())) {
//                posSamples++;
//            } else {
//                negSamples++;
//            }
            instance.featureVector = featureVector;
            instances.add(instance);
        }
        System.out.println("positive Samples: " + posSamples);
        System.out.println("negative Samples: " + negSamples);
        //classifier.build2();
        System.out.println("... building decision tree.");
        classifier.learn(instances);
//        classifier.build();
        System.out.println(classifier.toString());
        System.out.println("... finished building decision tree.");
    }

    private void processFeatureVector(FeatureVector featureVector) {
        boolean okay = featureVector.remove("nextCaseSignature");
        okay &= featureVector.remove("prevCaseSignature");
//        okay &= featureVector.remove(TokenMetricsCalculator.COUNT);
//        okay &= featureVector.remove(TokenMetricsCalculator.FREQUENCY);
//        okay &= featureVector.remove(TokenMetricsCalculator.WORD_LENGTH);
//        okay &= featureVector.remove(TokenMetricsCalculator.FIRST);
//        okay &= featureVector.remove(TokenMetricsCalculator.LAST);
//        okay &= featureVector.remove(TokenMetricsCalculator.CHAR_LENGTH);
        okay &= featureVector.remove(StemmerAnnotator.UNSTEM);
        okay &= featureVector.remove("isKeyword");
//        if (!okay) {
//            throw new IllegalStateException();
//        }
    }

    @Override
    public void reset() {
        this.trainDocuments.clear();
        this.termCorpus.reset();
//        this.classifier = new DecisionTreeClassifier("isKeyword");
//        this.classifier=new WekaBaggingPredictor(new NaiveBayes());
        this.classifier=new NaiveBayesClassifier();
        // TODO Auto-generated method stub
        trainCount = 0;
        super.reset();
    }
    

    private void writeData(List<Annotation> annotations, File outputCsvFile) {
        System.out.println("writing data to " + outputCsvFile);
//        if (outputCsvFile.exists()) {
//            System.out.println("output file exists ... deleting.");
//            outputCsvFile.delete();
//        }
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
            String value = annotation.getValue();
            String value2 = annotation.getFeatureVector().get(StemmerAnnotator.UNSTEM).getValue();
            NominalFeature isKeywordFeature;
            
            boolean isKeyword = keywords.contains(value);
            isKeyword |= keywords.contains(value.toLowerCase());
            isKeyword |= keywords.contains(value.replace(" ", ""));
            isKeyword |= keywords.contains(value.toLowerCase().replace(" ",""));
            isKeyword |= keywords.contains(value2);
            isKeyword |= keywords.contains(value2.toLowerCase());
            isKeyword |= keywords.contains(value2.replace(" ", ""));
            isKeyword |= keywords.contains(value2.toLowerCase().replace(" ", ""));
            
            if (isKeyword) {
                isKeywordFeature = new NominalFeature("isKeyword", "true");
            } else {
                isKeywordFeature = new NominalFeature("isKeyword", "false");
            }
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
    
    boolean outputFeatures = false;


    @Override
    public List<Keyphrase> extract(String inputText) {
        PipelineDocument document = new PipelineDocument(inputText);
        try {
            pipeline1.process(document);
            pipeline2.process(document);
        } catch (DocumentUnprocessableException e) {
            throw new IllegalStateException();
        }
        AnnotationFeature annotationFeature = document.getFeatureVector().get(RegExTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();
        List<Keyphrase> keywords = new ArrayList<Keyphrase>();
        //Bag<String> keywords = new HashBag<String>();
        for (Annotation annotation : annotations) {
            FeatureVector featureVector = annotation.getFeatureVector();
            processFeatureVector(featureVector);
//            Serializable klass = classifier.classify(featureVector);
            CategoryEntries ret = classifier.predict(featureVector);
//            System.out.println(ret);
            
            
            
//            String cN = ret.getMostLikelyCategoryEntry().getCategory().getName();
//            if ("true".equals(cN)) {
//                //keywords.add(annotation.getValue());
//                keywords.add(new Keyphrase(annotation.getValue(),ret.getMostLikelyCategoryEntry().getRelevance()));
//            }
            

            //XXX
            CategoryEntry trueCategory = ret.getCategoryEntry("true");
//XXX            System.out.println(annotation.getValue() + " " + trueCategory);
            keywords.add(new Keyphrase(annotation.getValue(), trueCategory.getAbsoluteRelevance()));
//   XXX         keywords.add(new Keyphrase(annotation.getValue(), trueCategory.getRelevance()));
            
            
        }
        //System.out.println(keywords);
//        for (String keyword : keywords.uniqueSet()) {
//            ret.add(new Keyphrase(keyword, keywords.getCount(keyword)));
//        }
        Collections.sort(keywords);
        if (keywords.size() > getKeyphraseCount()) {
            keywords.subList(getKeyphraseCount()
                    , keywords.size()).clear();
        }
        for (Keyphrase keyphrase : keywords) {
            System.out.println(keyphrase.getValue() + "////" + keyphrase.getWeight());
        }
        return keywords;
    }

    @Override
    public String getExtractorName() {
        return "ClassifierExtractor";
    }

}
