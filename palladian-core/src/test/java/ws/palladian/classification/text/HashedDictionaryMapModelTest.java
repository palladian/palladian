package ws.palladian.classification.text;

public class HashedDictionaryMapModelTest extends AbstractDictionaryModelTest {

    @Override
    protected DictionaryBuilder getBuilder() {
        return new HashedDictionaryMapModel.Builder();
    }

}
