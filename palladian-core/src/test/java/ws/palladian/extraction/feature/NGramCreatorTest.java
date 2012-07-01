package ws.palladian.extraction.feature;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.functors.InstanceofPredicate;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.pos.LingPipePosTagger;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.LingPipeTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.AnnotationGroup;

/**
 * <p>
 * Tests the correct behavior of the N-Gram creating {@code PipelineProcessor}s.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.1.7
 */
public class NGramCreatorTest {

    PipelineDocument<String> document;
    ProcessingPipeline pipeline;

    @Before
    public void setUp() {
        document = new PipelineDocument<String>("the quick brown fox jumps over the lazy dog");
        pipeline = new ProcessingPipeline();
    }

    public void tearDown() {
        document = null;
        pipeline = null;
    }

    @Test
    public void testNGramCreator() throws DocumentUnprocessableException {
        pipeline.add(new RegExTokenizer());
        pipeline.add(new StopTokenRemover(Language.ENGLISH));
        pipeline.add(new NGramCreator(2));
        pipeline.process(document);

        AnnotationFeature annotationFeature = document.getFeatureVector()
                .get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();

        // get all AnnotationGroups
        @SuppressWarnings("unchecked")
        List<AnnotationGroup> annotationGroups = new ArrayList<AnnotationGroup>(CollectionUtils.select(annotations,
                new InstanceofPredicate(AnnotationGroup.class)));

        for (AnnotationGroup annotationGroup : annotationGroups) {
            System.out.println(annotationGroup.getValue());
        }
        assertEquals(4, annotationGroups.size(), 4);
        assertEquals("quick brown", annotationGroups.get(0).getValue());
        assertEquals("brown fox", annotationGroups.get(1).getValue());
        assertEquals("fox jumps", annotationGroups.get(2).getValue());
        assertEquals("lazy dog", annotationGroups.get(3).getValue());
    }

    /**
     * <p>
     * Test the NGramCreator that preserves annotations added to token.
     * </p>
     * 
     * @throws Exception
     */
    @Test
    public void testNGramCreatorPreserveAnnotations() throws Exception {
        pipeline.add(new LingPipeTokenizer());
        pipeline.add(new LingPipePosTagger(ResourceHelper
                .getResourceFile("/model/pos-en-general-brown.HiddenMarkovModel")));
        pipeline.add(new NGramCreator(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR));
        pipeline.process(document);

        AnnotationFeature annotationFeature = document.getFeatureVector()
                .get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();

        List<AnnotationGroup> annotationGroups = new ArrayList<AnnotationGroup>(CollectionUtils.select(annotations,
                new InstanceofPredicate(AnnotationGroup.class)));
        assertEquals(annotationGroups.size(), 8);
        assertEquals("the quick", annotationGroups.get(0).getValue());
        assertEquals("quick brown", annotationGroups.get(1).getValue());
        assertEquals("brown fox", annotationGroups.get(2).getValue());
        assertEquals("fox jumps", annotationGroups.get(3).getValue());

        assertThat(annotationGroups.get(0).getAnnotations().size(), is(2));
        assertThat(
                (String)annotationGroups.get(0).getAnnotations().get(0)
                        .getFeature(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue(), is("AT"));
        assertThat(
                (String)annotationGroups.get(0).getAnnotations().get(1)
                        .getFeature(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue(), is("JJ"));
        
        assertThat((String)annotationGroups.get(0).getFeature(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue(), is("ATJJ"));
        assertThat((String)annotationGroups.get(1).getFeature(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue(), is("JJJJ"));
        assertThat((String)annotationGroups.get(2).getFeature(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue(), is("JJNN"));
        assertThat((String)annotationGroups.get(3).getFeature(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue(), is("NNNNS"));
        assertThat((String)annotationGroups.get(4).getFeature(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue(), is("NNSIN"));
    }

}
