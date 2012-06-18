package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.functors.InstanceofPredicate;
import org.junit.Test;

import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.ProcessingPipeline;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.AnnotationGroup;

/**
 * @author Philipp Katz
 */
public class NGramCreatorTest {
    
    @Test
    public void testNGramCreator() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new StopTokenRemover(Language.ENGLISH));
        pipeline.add(new NGramCreator(2));
        PipelineDocument<String> document = new PipelineDocument<String>("the quick brown fox jumps over the lazy dog");
        pipeline.process(document);
        
        AnnotationFeature annotationFeature = document.getFeatureVector().get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();
        
        // get all AnnotationGroups
        @SuppressWarnings("unchecked")
        List<AnnotationGroup> annotationGroups = new ArrayList<AnnotationGroup>(CollectionUtils.select(annotations, new InstanceofPredicate(AnnotationGroup.class)));
        
        assertEquals(4, annotationGroups.size(), 4);
        assertEquals("quick brown", annotationGroups.get(0).getValue());
        assertEquals("brown fox", annotationGroups.get(1).getValue());
        assertEquals("fox jumps", annotationGroups.get(2).getValue());
        assertEquals("lazy dog", annotationGroups.get(3).getValue());
    }

}
