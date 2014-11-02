package ws.palladian.classification.text;

import java.io.File;

import ws.palladian.helper.io.FileHelper;

public class LuceneDictionaryModelTest extends AbstractDictionaryModelTest {

    @Override
    protected DictionaryBuilder getBuilder() {
        File directoryPath = new File(FileHelper.getTempDir(), "luceneIndexTest_" + System.currentTimeMillis());
        return new LuceneDictionaryModel.Builder(directoryPath);
    }

}
