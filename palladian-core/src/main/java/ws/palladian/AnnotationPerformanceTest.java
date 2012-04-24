package ws.palladian;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.ProcessingPipeline;
import ws.palladian.extraction.keyphrase.temp.Dataset2;
import ws.palladian.extraction.keyphrase.temp.DatasetHelper;
import ws.palladian.extraction.keyphrase.temp.DatasetItem;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;

public class AnnotationPerformanceTest {
    
    public static void main(String[] args) throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        List<PipelineDocument> docs = new ArrayList<PipelineDocument>();
        int i = 0;
        
        Dataset2 dataset = DatasetHelper.loadDataset(new File("/Users/pk/Dropbox/Uni/Datasets/citeulike180/citeulike180index.txt"), "#");
        StopWatch sw = new StopWatch();
        for (DatasetItem datasetItem : dataset) {
            String content = FileHelper.readFileToString(datasetItem.getFile());
            PipelineDocument doc = pipeline.process(new PipelineDocument(content));
            docs.add(doc);
            if (i++ == 10) {
                break;
            }
            System.out.println(i);
        }
        System.out.println("took " + sw);
    }
    

}
