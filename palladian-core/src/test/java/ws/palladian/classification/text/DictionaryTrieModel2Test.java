package ws.palladian.classification.text;

public class DictionaryTrieModel2Test extends AbstractDictionaryModelTest {

    @Override
    protected DictionaryBuilder getBuilder() {
        return new DictionaryTrieModel2.Builder();
    }

}
