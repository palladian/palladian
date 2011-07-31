package com.newsseecr.xperimental;

import ws.palladian.classification.page.Stopwords.Predefined;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PerformanceCheckProcessingPipeline;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.ProcessingPipeline;
import ws.palladian.preprocessing.featureextraction.AnnotationFeature;
import ws.palladian.preprocessing.featureextraction.DuplicateTokenRemover;
import ws.palladian.preprocessing.featureextraction.FrequencyCalculator;
import ws.palladian.preprocessing.featureextraction.NGramCreator2;
import ws.palladian.preprocessing.featureextraction.RegExTokenRemover;
import ws.palladian.preprocessing.featureextraction.StopTokenRemover;
import ws.palladian.preprocessing.featureextraction.TokenSpreadCalculator;
import ws.palladian.preprocessing.featureextraction.Tokenizer;

public class PipelineTest {

    public static void main(String[] args) {

        // ProcessingPipeline pipeline = new ProcessingPipeline();
        ProcessingPipeline pipeline = new PerformanceCheckProcessingPipeline();
         pipeline.add(new Tokenizer());
//        pipeline.add(new OpenNlpTokenizer("/Users/pk/Desktop/de-token.bin"));
//        pipeline.add(new OpenNlpTokenizer("/Users/pk/Desktop/en-token.bin"));
//        pipeline.add(new OpenNlpPosTagger("/Users/pk/Desktop/de-pos-maxent.bin"));
//        pipeline.add(new OpenNlpPosTagger("/Users/pk/Desktop/en-pos-maxent.bin"));
        // pipeline.add(new NGramCreator(2, 4));
        pipeline.add(new NGramCreator2(2,2));
        pipeline.add(new TokenSpreadCalculator());
        pipeline.add(new StopTokenRemover(Predefined.EN));
//        pipeline.add(new StopTokenRemover(Predefined.DE));
//         pipeline.add(new StemmerAnnotator(new englishStemmer()));
        pipeline.add(new FrequencyCalculator());
        // pipeline.add(new WordCounter());
        // pipeline.add(new
        // ControlledVocabularyFilter("/Users/pk/Desktop/WikipediaData/dewiki-20110410-all-titles-in-ns0"));
//         pipeline.add(new TokenOverlapRemover());
        pipeline.add(new RegExTokenRemover("\\p{Punct}"));
        pipeline.add(new RegExTokenRemover(".{1,2}"));
        pipeline.add(new DuplicateTokenRemover());
         //pipeline.add(new IdfAnnotator(TroveTermCorpus.deserialize("data/titleCountCorpus.ser")));
//        pipeline.add(new IdfAnnotator(new TroveTermCorpus("/Users/pk/Uni/workspace/newsseecr/data/titleCountCorpus.txt")));

//         pipeline.add(new TfIdfAnnotator());
        
//         pipeline.add(new TokenOverlapRemover());

        // String text = FileHelper.readFileToString("/Users/pk/Desktop/aptgetupdate.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/tagesschau2.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/pg76.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/pg2229.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/SPIEGEL.txt");
        // String text = "Philipp Katz, Naußlitzer Str. 20, 01187 Dresden, philipp@philippkatz.de";
        // String text = "Irren ist menschlich.";
//         String text = FileHelper.readFileToString("/Users/pk/Desktop/tagesschau.txt");
//         String text = FileHelper.readFileToString("/Users/pk/Desktop/faz.txt");
//         String text = FileHelper.readFileToString("/Users/pk/Desktop/ntv.txt");
//         String text = FileHelper.readFileToString("/Users/pk/Desktop/taz.txt");
//         String text = FileHelper.readFileToString("/Users/pk/Desktop/spiegel.txt");
//         String text = FileHelper.readFileToString("/Users/pk/Desktop/vbush.txt");
//         String text = FileHelper.readFileToString("/Users/pk/Desktop/openReport.txt");
//         String text = FileHelper.readFileToString("/Users/pk/Desktop/text.txt");
//        String text = FileHelper.readFileToString("/Users/pk/Desktop/bbc.txt");
         String text = "Pakistan threatens to cut off access to a facility used by NATO forces, signaling a growing rift that began when U.S. commandos killed Osama bin Laden in Pakistan.";

        PipelineDocument document = pipeline.process(new PipelineDocument(text));
        System.out.println(pipeline);

        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature tokenList = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        System.out.println("# tokens " + tokenList.getValue().size());
        System.out.println(tokenList.toStringList());

//        Feature<List<String>> keywords = (Feature<List<String>>) featureVector.get(KeywordAnnotator.PROVIDED_FEATURE);
//        CollectionHelper.print(keywords.getValue());

    }

}
