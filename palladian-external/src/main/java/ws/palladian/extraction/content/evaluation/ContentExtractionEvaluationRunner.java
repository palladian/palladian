package ws.palladian.extraction.content.evaluation;

import java.io.File;

import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ArticleSentencesExtractor;
import de.l3s.boilerpipe.extractors.CanolaExtractor;
import de.l3s.boilerpipe.extractors.DefaultExtractor;
import de.l3s.boilerpipe.extractors.LargestContentExtractor;
import de.l3s.boilerpipe.extractors.NumWordsRulesExtractor;
import ws.palladian.extraction.content.BoilerpipeContentExtractor;
import ws.palladian.extraction.content.PalladianContentExtractor;
import ws.palladian.extraction.content.ReadabilityContentExtractor;
import ws.palladian.extraction.content.evaluation.BoilerpipeDataset.Mode;

public class ContentExtractionEvaluationRunner {
    
    public static void main(String[] args) {
        ContentExtractorEvaluation evaluation = new ContentExtractorEvaluation();

        evaluation.addExtractor(new ReadabilityContentExtractor());
        evaluation.addExtractor(new PalladianContentExtractor());
        evaluation.addExtractor(new BoilerpipeContentExtractor(ArticleExtractor.INSTANCE));
        evaluation.addExtractor(new BoilerpipeContentExtractor(ArticleSentencesExtractor.INSTANCE));
        evaluation.addExtractor(new BoilerpipeContentExtractor(CanolaExtractor.INSTANCE));
        evaluation.addExtractor(new BoilerpipeContentExtractor(DefaultExtractor.INSTANCE));
        evaluation.addExtractor(new BoilerpipeContentExtractor(LargestContentExtractor.INSTANCE));
        evaluation.addExtractor(new BoilerpipeContentExtractor(NumWordsRulesExtractor.INSTANCE));
        
        evaluation.addDataset(new CleanevalDataset(new File("/Users/pk/Desktop/CleanEval")));
        evaluation.addDataset(new TudContentExtractionDataset(new File("/Users/pk/Desktop/TUD_ContentExtractionDataset_2014-01-28")));
        evaluation.addDataset(new BoilerpipeDataset(new File("/Users/pk/Desktop/L3S-GN1-20100130203947-00001"), Mode.MAIN_CONTENT));
        evaluation.addDataset(new BoilerpipeDataset(new File("/Users/pk/Desktop/L3S-GN1-20100130203947-00001"), Mode.WHOLE_CONTENT));
        evaluation.evaluate();
    }

}
