package ws.palladian.classification.text;

public class DictionaryMapModelTest extends AbstractDictionaryModelTest {

	@Override
	protected DictionaryBuilder getBuilder() {
		return new DictionaryMapModel.Builder();
	}

}
