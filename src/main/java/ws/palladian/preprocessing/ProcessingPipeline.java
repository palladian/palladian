package ws.palladian.preprocessing;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;

public class ProcessingPipeline {

    private List<PipelineProcessor> pipelineProcessors;

    public ProcessingPipeline() {
        pipelineProcessors = new ArrayList<PipelineProcessor>();
    }

    public void add(PipelineProcessor pipelineProcessor) {
        pipelineProcessors.add(pipelineProcessor);
    }

    public PipelineDocument process(PipelineDocument document) {
        for (PipelineProcessor processor : pipelineProcessors) {
            processor.process(document);
        }

        return document;
    }

    public static void main(String[] args) {

        // /////////////////// example usage /////////////////////

        // the usage of the stop word remover will change the classification outcome
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new StopWordRemover());
        pipeline.add(new WordCounter());

        DictionaryClassifier classifier = new DictionaryClassifier();
        ClassificationTypeSetting cts = new ClassificationTypeSetting();
        cts.setClassificationType(ClassificationTypeSetting.TAG);
        classifier.setClassificationTypeSetting(cts);
        classifier.setProcessingPipeline(pipeline);

        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();
        UniversalInstance instance = new UniversalInstance(instances);
        instance.setTextFeature("Das ist ein deutscher Text, ein Mann steht vor einem Gebäude und lacht.");
        instance.setInstanceCategory("German");
        classifier.train(instance);

        instance = new UniversalInstance(instances);
        instance.setTextFeature("This is English, a man stands in front of a building and laughs.");
        instance.setInstanceCategory("English");
        classifier.train(instance);

        System.out.println(classifier.classify("This is a sample text, whether you believe it or not."));
    }
}
