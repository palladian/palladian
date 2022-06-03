package ws.palladian.extraction.location.scope.evaluation;

import java.io.IOException;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.DictionaryTrieModel;
import ws.palladian.classification.text.PruningStrategies;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeModel;
import ws.palladian.helper.io.FileHelper;

public class Test {
	
	public static void main(String[] args) throws IOException {
		DictionaryScopeModel dictionaryScopeModel = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/enwiki-20220501-locationDictionary-1-word-0.3515625.ser.gz");
//		System.out.println(dictionaryScopeModel.dictionaryModel.getCategoryEntries("and"));
//		System.out.println(dictionaryScopeModel.dictionaryModel.getCategoryEntries("and").getEntropy());
//		System.out.println(dictionaryScopeModel.dictionaryModel.getCategoryEntries("munich"));
//		System.out.println(dictionaryScopeModel.dictionaryModel.getCategoryEntries("munich").getEntropy());
//		System.exit(0);
		
		System.out.println(dictionaryScopeModel.dictionaryModel);
		
		// original: 191.174.761 Byte 
		
		// 172.706.057  bytes
		// [main] INFO ws.palladian.classification.text.DictionaryTrieModel$Builder - Removed 61.511 % terms (2055844) with TermCountPruningStrategy [minCount=2]
		// DictionaryModel cleaned = new DictionaryTrieModel.Builder().addDictionary(dictionaryScopeModel.dictionaryModel).setPruningStrategy(PruningStrategies.termCount(2)).create(); 
		
		// 163.082.863  bytes
		// [main] INFO ws.palladian.classification.text.DictionaryTrieModel$Builder - Removed 83.839 % terms (2802091) with TermCountPruningStrategy [minCount=5]
		// DictionaryModel cleaned = new DictionaryTrieModel.Builder().setFeatureSetting(dictionaryScopeModel.dictionaryModel.getFeatureSetting()).addDictionary(dictionaryScopeModel.dictionaryModel).setPruningStrategy(PruningStrategies.termCount(5)).create();
		
		// 152.042.283  bytes
		// [main] INFO ws.palladian.classification.text.DictionaryTrieModel$Builder - Removed 95.544 % terms (3193285) with TermCountPruningStrategy [minCount=25]
		// DictionaryModel cleaned = new DictionaryTrieModel.Builder().setFeatureSetting(dictionaryScopeModel.dictionaryModel.getFeatureSetting()).addDictionary(dictionaryScopeModel.dictionaryModel).setPruningStrategy(PruningStrategies.termCount(25)).create();

		// 147.560.428  bytes
		// [main] INFO ws.palladian.classification.text.DictionaryTrieModel$Builder - Removed 97.279 % terms (3251278) with TermCountPruningStrategy [minCount=50]
		DictionaryModel cleaned = new DictionaryTrieModel.Builder().setFeatureSetting(dictionaryScopeModel.dictionaryModel.getFeatureSetting()).addDictionary(dictionaryScopeModel.dictionaryModel).setPruningStrategy(PruningStrategies.termCount(50)).create();
		
		System.out.println(cleaned);
		DictionaryScopeModel cleanScopeModel = new DictionaryScopeModel(dictionaryScopeModel.gridSize, cleaned, dictionaryScopeModel.cellToCoordinate);
		FileHelper.serialize(cleanScopeModel, "enwiki-20220501-locationDictionary-1-word-0.3515625.ser.gz");
	}

}
