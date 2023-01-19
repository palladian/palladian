package ws.palladian.classification.text;

import ws.palladian.helper.io.FileHelper;

import java.io.File;

public class LuceneDictionaryModelTest extends AbstractDictionaryModelTest {

    @Override
    protected DictionaryBuilder getBuilder() {
        File directoryPath = new File(FileHelper.getTempDir(), "luceneIndexTest_" + System.currentTimeMillis());
        return new LuceneDictionaryModel.Builder(directoryPath);
    }

}
