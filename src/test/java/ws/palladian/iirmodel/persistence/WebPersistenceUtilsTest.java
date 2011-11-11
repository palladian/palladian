/**
 * Created on: 11.11.2011 17:15:52
 */
package ws.palladian.iirmodel.persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.iirmodel.Label;
import ws.palladian.iirmodel.LabelType;

/**
 * @author Klemens Muthmann
 * @since 0.0.1
 * @version 1.0.0
 */
public class WebPersistenceUtilsTest {
    private static final String TEST_PERSISTENCE_UNIT_NAME = System.getProperty("persistenceunitname");

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void test() throws Exception {
        LabelType questionType = new LabelType("QUESTION");
        LabelType answerType = new LabelType("ANSWER");
        Label questionLabel01 = new Label(null, questionType, "");
        Label questionLabel02 = new Label(null, questionType, "");
        Label answerLabel01 = new Label(null, answerType, "");
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(TEST_PERSISTENCE_UNIT_NAME);
        EntityManager manager = factory.createEntityManager();
        WebPersistenceUtils objectOfClassUnderTest = new WebPersistenceUtils(manager);

        objectOfClassUnderTest.saveLabelType(questionType);
        objectOfClassUnderTest.saveLabelType(answerType);
        objectOfClassUnderTest.saveLabel(answerLabel01);
        objectOfClassUnderTest.saveLabel(questionLabel02);
        objectOfClassUnderTest.saveLabel(questionLabel01);
        Map<String, String> result = objectOfClassUnderTest.countLabelTypes();
        assertThat(result.size(), is(2));
        assertThat(result.get("ANSWER"), is("1"));
        assertThat(result.get("QUESTION"), is("2"));

        objectOfClassUnderTest.shutdown();
        factory.close();
    }
}
