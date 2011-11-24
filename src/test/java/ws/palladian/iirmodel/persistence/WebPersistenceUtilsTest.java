/**
 * Created on: 11.11.2011 17:15:52
 */
package ws.palladian.iirmodel.persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.iirmodel.Label;
import ws.palladian.iirmodel.LabelType;
import ws.palladian.iirmodel.Labeler;

/**
 * <p>
 * Tests whether the persistence layer necessary for the Effingo web presentation works correctly.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 3.0.0
 * @version 1.0.0
 */
public final class WebPersistenceUtilsTest {
    private static final String TEST_PERSISTENCE_UNIT_NAME = System.getProperty("persistenceunitname");
    private WebPersistenceUtils objectOfClassUnderTest;
    private EntityManagerFactory factory;

    // fixture
    private LabelType questionType;
    private LabelType answerType;
    private Label questionLabel01;
    private Label questionLabel02;
    private Label answerLabel01;

    @Before
    public void setUp() throws Exception {
        factory = Persistence.createEntityManagerFactory(TEST_PERSISTENCE_UNIT_NAME);
        objectOfClassUnderTest = new WebPersistenceUtils(factory.createEntityManager());
        questionType = new LabelType("QUESTION");
        answerType = new LabelType("ANSWER");
        questionLabel01 = new Label(null, questionType, "");
        questionLabel02 = new Label(null, questionType, "");
        answerLabel01 = new Label(null, answerType, "");
    }

    /**
     * <p>
     * Closes the connection to the in memory database thus resetting the whole data storage.
     * </p>
     * 
     * @throws Exception If some error occurs. Fails the test.
     */
    @After
    public void tearDown() throws Exception {
        objectOfClassUnderTest.shutdown();
        objectOfClassUnderTest = null;
        factory.close();
        factory = null;
        System.gc();
    }

    /**
     * <p>
     * Tests if saving of {@link Label}s with different {@link LabelType}s is successful or not.
     * </p>
     * 
     * @throws Exception If some error occurs. Fails the test.
     */
    @Test
    public void test() throws Exception {
        objectOfClassUnderTest.saveLabelType(questionType);
        objectOfClassUnderTest.saveLabelType(answerType);
        objectOfClassUnderTest.saveLabel(answerLabel01);
        objectOfClassUnderTest.saveLabel(questionLabel02);
        objectOfClassUnderTest.saveLabel(questionLabel01);
        Map<String, String> result = objectOfClassUnderTest.countLabelTypes();
        assertThat(result.size(), is(2));
        assertThat(result.get("ANSWER"), is("1"));
        assertThat(result.get("QUESTION"), is("2"));
    }

    /**
     * <p>
     * Tests whether saving a {@link Labeler} containing all {@code Label}s from the fixture is successful and checks by
     * loading that {@code Labeler} again to check the saved content.
     * </p>
     * 
     * @throws Exception If some error occurs. Fails the test.
     */
    @Test
    public void testSaveLabelers() throws Exception {
        Labeler labeler = new Labeler("test");
        labeler.addLabel(answerLabel01);
        labeler.addLabel(questionLabel01);
        labeler.addLabel(questionLabel02);
        objectOfClassUnderTest.saveLabelType(answerType);
        objectOfClassUnderTest.saveLabelType(questionType);
        objectOfClassUnderTest.saveLabel(answerLabel01);
        objectOfClassUnderTest.saveLabel(questionLabel01);
        objectOfClassUnderTest.saveLabel(questionLabel02);

        objectOfClassUnderTest.saveLabeler(labeler);
        Labeler result = objectOfClassUnderTest.loadLabeler("test");
        assertThat(result, is(notNullValue()));
        assertThat(result.getName(), is("test"));
        assertThat(result.getLabels().size(), is(3));
        assertThat(result.getLabels(), hasItem(answerLabel01));
        assertThat(result.getLabels(), hasItem(questionLabel02));
        assertThat(result.getLabels(), hasItem(questionLabel01));
    }

    @Test
    public void testLoadRandomNonSelfLabeledItem() throws Exception {
        Labeler labeler1 = new Labeler("labeler1");
        Labeler labeler2 = new Labeler("labeler2");
        labeler1.addLabel(questionLabel01);
        labeler2.addLabel(questionLabel02);

        objectOfClassUnderTest.saveLabelType(questionType);
        objectOfClassUnderTest.saveLabel(questionLabel01);
        objectOfClassUnderTest.saveLabel(questionLabel02);

        objectOfClassUnderTest.saveLabeler(labeler1);
        objectOfClassUnderTest.saveLabeler(labeler2);
    }
}
