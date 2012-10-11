package ws.palladian.classification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class NominalClassifierTest {
    
    @Test
    public void testNominalClassifier() {
        NominalClassifier bc = new NominalClassifier();

        List<UniversalInstance> instances = CollectionHelper.newArrayList();

        List<String> nominalFeatures = new ArrayList<String>();

        // create an instance to classify
        UniversalInstance newInstance = new UniversalInstance();
        newInstance.setInstanceCategory("A");
        nominalFeatures.add("f1");
        newInstance.setNominalFeatures(nominalFeatures);
        instances.add(newInstance);
        
        newInstance = new UniversalInstance();
        nominalFeatures = new ArrayList<String>();
        newInstance.setInstanceCategory("B");
        nominalFeatures.add("f1");
        newInstance.setNominalFeatures(nominalFeatures);
        instances.add(newInstance);
        instances.add(newInstance);

        newInstance = new UniversalInstance();
        nominalFeatures = new ArrayList<String>();
        newInstance.setInstanceCategory("A");
        nominalFeatures.add("f2");
        newInstance.setNominalFeatures(nominalFeatures);
        instances.add(newInstance);
        instances.add(newInstance);
        instances.add(newInstance);

        newInstance = new UniversalInstance();
        nominalFeatures = new ArrayList<String>();
        newInstance.setInstanceCategory("B");
        nominalFeatures.add("f2");
        newInstance.setNominalFeatures(nominalFeatures);
        instances.add(newInstance);
        instances.add(newInstance);
        instances.add(newInstance);
        instances.add(newInstance);

        bc.train(instances);

        newInstance = new UniversalInstance();
        nominalFeatures = new ArrayList<String>();
        nominalFeatures.add("f2");
        newInstance.setNominalFeatures(nominalFeatures);

        bc.classify(newInstance);
        
        CategoryEntry categoryA = newInstance.getAssignedCategoryEntries().getCategoryEntry("A");
        CategoryEntry categoryB = newInstance.getAssignedCategoryEntries().getCategoryEntry("B");
        
        assertNotNull(categoryA);
        assertNotNull(categoryB);
        assertEquals(0.4286, categoryA.getRelevance(), 0.0001);
        assertEquals(0.5714, categoryB.getRelevance(), 0.0001);
    }

}
