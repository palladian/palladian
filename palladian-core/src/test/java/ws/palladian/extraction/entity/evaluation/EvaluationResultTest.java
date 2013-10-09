package ws.palladian.extraction.entity.evaluation;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.Collections;

import org.junit.Test;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.helper.io.ResourceHelper;

public class EvaluationResultTest {

    @Test
    public void testEvaluationResult() throws FileNotFoundException {
        String goldFile = ResourceHelper.getResourcePath("/ner/evaluation/goldStandardXml.txt");
        String resultFile = ResourceHelper.getResourcePath("/ner/evaluation/nerResultXml.txt");
        Annotations<ContextAnnotation> goldStandard = FileFormatParser.getAnnotationsFromXmlFile(goldFile);
        Annotations<ContextAnnotation> nerResult = FileFormatParser.getAnnotationsFromXmlFile(resultFile);

        EvaluationResult result = NamedEntityRecognizer.evaluate(goldStandard, nerResult,
                Collections.<String> emptySet());

        assertEquals(2, result.getActualAssignments("UNIT"));
        assertEquals(2, result.getActualAssignments("LANDMARK"));
        assertEquals(3, result.getActualAssignments("POI"));
        assertEquals(6, result.getActualAssignments("COUNTRY"));
        assertEquals(2, result.getActualAssignments("CITY"));

        assertEquals(1, result.getPossibleAssignments("UNIT"));
        assertEquals(3, result.getPossibleAssignments("LANDMARK"));
        assertEquals(5, result.getPossibleAssignments("POI"));
        assertEquals(3, result.getPossibleAssignments("COUNTRY"));
        assertEquals(2, result.getPossibleAssignments("CITY"));

        assertEquals(14, result.getPossibleAssignments());
        assertEquals(15, result.getActualAssignments());

        assertEquals(5, result.getResultTypeCount(ResultType.CORRECT));
        assertEquals(3, result.getResultTypeCount(ResultType.ERROR1));
        assertEquals(2, result.getResultTypeCount(ResultType.ERROR2));
        assertEquals(1, result.getResultTypeCount(ResultType.ERROR3));
        assertEquals(2, result.getResultTypeCount(ResultType.ERROR4));
        assertEquals(4, result.getResultTypeCount(ResultType.ERROR5));

        assertEquals(1, result.getResultTypeCount("UNIT", ResultType.CORRECT));
        assertEquals(0, result.getResultTypeCount("LANDMARK", ResultType.CORRECT));
        assertEquals(1, result.getResultTypeCount("POI", ResultType.CORRECT));
        assertEquals(2, result.getResultTypeCount("COUNTRY", ResultType.CORRECT));
        assertEquals(1, result.getResultTypeCount("CITY", ResultType.CORRECT));

        assertEquals(1, result.getResultTypeCount("UNIT", ResultType.ERROR1));
        assertEquals(0, result.getResultTypeCount("LANDMARK", ResultType.ERROR1));
        assertEquals(0, result.getResultTypeCount("POI", ResultType.ERROR1));
        assertEquals(1, result.getResultTypeCount("COUNTRY", ResultType.ERROR1));
        assertEquals(1, result.getResultTypeCount("CITY", ResultType.ERROR1));

        assertEquals(0, result.getResultTypeCount("UNIT", ResultType.ERROR2));
        assertEquals(1, result.getResultTypeCount("LANDMARK", ResultType.ERROR2));
        assertEquals(0, result.getResultTypeCount("POI", ResultType.ERROR2));
        assertEquals(1, result.getResultTypeCount("COUNTRY", ResultType.ERROR2));
        assertEquals(0, result.getResultTypeCount("CITY", ResultType.ERROR2));

        assertEquals(0, result.getResultTypeCount("UNIT", ResultType.ERROR3));
        assertEquals(0, result.getResultTypeCount("LANDMARK", ResultType.ERROR3));
        assertEquals(1, result.getResultTypeCount("POI", ResultType.ERROR3));
        assertEquals(0, result.getResultTypeCount("COUNTRY", ResultType.ERROR3));
        assertEquals(0, result.getResultTypeCount("CITY", ResultType.ERROR3));

        assertEquals(0, result.getResultTypeCount("UNIT", ResultType.ERROR4));
        assertEquals(1, result.getResultTypeCount("LANDMARK", ResultType.ERROR4));
        assertEquals(1, result.getResultTypeCount("POI", ResultType.ERROR4));
        assertEquals(0, result.getResultTypeCount("COUNTRY", ResultType.ERROR4));
        assertEquals(0, result.getResultTypeCount("CITY", ResultType.ERROR4));

        assertEquals(0, result.getResultTypeCount("UNIT", ResultType.ERROR5));
        assertEquals(1, result.getResultTypeCount("LANDMARK", ResultType.ERROR5));
        assertEquals(2, result.getResultTypeCount("POI", ResultType.ERROR5));
        assertEquals(0, result.getResultTypeCount("COUNTRY", ResultType.ERROR5));
        assertEquals(1, result.getResultTypeCount("CITY", ResultType.ERROR5));

        // exact match mode
        assertEquals(5. / 15, result.getPrecision(EvaluationMode.EXACT_MATCH), 0);
        assertEquals(5. / 14, result.getRecall(EvaluationMode.EXACT_MATCH), 0);

        // for specific tags
        assertEquals(1. / 2, result.getPrecisionFor("UNIT", EvaluationMode.EXACT_MATCH), 0);
        assertEquals(0. / 3, result.getPrecisionFor("LANDMARK", EvaluationMode.EXACT_MATCH), 0);
        assertEquals(1. / 3, result.getPrecisionFor("POI", EvaluationMode.EXACT_MATCH), 0);
        assertEquals(2. / 6, result.getPrecisionFor("COUNTRY", EvaluationMode.EXACT_MATCH), 0);
        assertEquals(1. / 2, result.getPrecisionFor("CITY", EvaluationMode.EXACT_MATCH), 0);

        assertEquals(1. / 1, result.getRecallFor("UNIT", EvaluationMode.EXACT_MATCH), 0);
        assertEquals(0. / 3, result.getRecallFor("LANDMARK", EvaluationMode.EXACT_MATCH), 0);
        assertEquals(1. / 5, result.getRecallFor("POI", EvaluationMode.EXACT_MATCH), 0);
        assertEquals(2. / 3, result.getRecallFor("COUNTRY", EvaluationMode.EXACT_MATCH), 0);
        assertEquals(1. / 2, result.getRecallFor("CITY", EvaluationMode.EXACT_MATCH), 0);

        // MUC mode
        assertEquals(13. / 30, result.getPrecision(EvaluationMode.MUC), 0);
        assertEquals(13. / 28, result.getRecall(EvaluationMode.MUC), 0);

        // for specific tags
        assertEquals(2. / 4, result.getPrecisionFor("UNIT", EvaluationMode.MUC), 0);
        assertEquals(1. / 4, result.getPrecisionFor("LANDMARK", EvaluationMode.MUC), 0);
        assertEquals(4. / 6, result.getPrecisionFor("POI", EvaluationMode.MUC), 0);
        assertEquals(4. / 12, result.getPrecisionFor("COUNTRY", EvaluationMode.MUC), 0);
        assertEquals(2. / 4, result.getPrecisionFor("CITY", EvaluationMode.MUC), 0);

        assertEquals(2. / 2, result.getRecallFor("UNIT", EvaluationMode.MUC), 0);
        assertEquals(1. / 6, result.getRecallFor("LANDMARK", EvaluationMode.MUC), 0);
        assertEquals(4. / 10, result.getRecallFor("POI", EvaluationMode.MUC), 0);
        assertEquals(4. / 6, result.getRecallFor("COUNTRY", EvaluationMode.MUC), 0);
        assertEquals(2. / 4, result.getRecallFor("CITY", EvaluationMode.MUC), 0);

        // System.out.println(result.getEvaluationDetails());
    }

}
