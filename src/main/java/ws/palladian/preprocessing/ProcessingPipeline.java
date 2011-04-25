package ws.palladian.preprocessing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.helper.StopWatch;

public class ProcessingPipeline implements Serializable {

    private static final long serialVersionUID = -6173687204106619909L;

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ProcessingPipeline.class);

    private List<PipelineProcessor> pipelineProcessors;

    public ProcessingPipeline() {
        pipelineProcessors = new ArrayList<PipelineProcessor>();
    }

    public void add(PipelineProcessor pipelineProcessor) {
        pipelineProcessors.add(pipelineProcessor);
    }
    
    public List<PipelineProcessor> getPipelineProcessors() {
        return pipelineProcessors;
    }

    public PipelineDocument process(PipelineDocument document) {

        StopWatch stopWatch = new StopWatch();

        for (PipelineProcessor processor : pipelineProcessors) {
            StopWatch stopWatch2 = new StopWatch();
            processor.process(document);
            LOGGER.debug("processor " + processor + " took " + stopWatch2);
        }

        LOGGER.debug("pipeline took " + stopWatch);

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
