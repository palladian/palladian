package ws.palladian.extraction.date.weka;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import weka.core.Instances;
import ws.palladian.extraction.date.evaluation.weka.WekaTraineeSetHelper;
import ws.palladian.helper.io.ResourceHelper;

public class WekaTraineeSetHelperTest {
    
    @Test
    public void testGetXFoldSets() {
        Instances[] instances;
        String path = "/wekaClassifier/allDates.arff";
        instances = WekaTraineeSetHelper.getXFoldSets(path, 5);
        WekaTraineeSetHelper.createTraineeAndTestSets(instances, 3);

        int cntNewInstances = 0;
        for (int i = 0; i < instances.length; i++) {
            cntNewInstances += instances[i].numInstances();
        }

        FileReader in;
        BufferedReader bufferedReader;
        int cntOrgInstances = 0;
        try {
        	File file = new File(ResourceHelper.getResourcePath(path));
            in = new FileReader(file);
            bufferedReader = new BufferedReader(in);
            String line;
            boolean preData = true;
            while ((line = bufferedReader.readLine()) != null) {
                if (preData) {
                    if (line.toLowerCase().indexOf("@data") != -1) {
                        preData = false;
                    }
                } else {
                    cntOrgInstances++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertThat(cntOrgInstances, is(cntNewInstances));
    }

    @Test
    public void createTraineeAndTestSets() {
        Instances[] instances;
        Instances[] instancesXFold;
        String path = "/wekaClassifier/allDates.arff";
        instancesXFold = WekaTraineeSetHelper.getXFoldSets(path, 5);
        WekaTraineeSetHelper.createTraineeAndTestSets(instancesXFold, 3);
        int cntAllInstances = 0;
        for (int i = 0; i < instancesXFold.length; i++) {
            cntAllInstances += instancesXFold[i].numInstances();
        }

        instances = WekaTraineeSetHelper.createTraineeAndTestSets(instancesXFold, 0);
        int cntNewInstances = 0;
        for (int i = 0; i < instances.length; i++) {
            cntNewInstances += instances[i].numInstances();
        }
        assertThat(cntNewInstances, is(cntAllInstances));

        instances = WekaTraineeSetHelper.createTraineeAndTestSets(instancesXFold, 1);
        cntNewInstances = 0;
        for (int i = 0; i < instances.length; i++) {
            cntNewInstances += instances[i].numInstances();
        }
        assertThat(cntNewInstances, is(cntAllInstances));
        instances = WekaTraineeSetHelper.createTraineeAndTestSets(instancesXFold, 2);
        cntNewInstances = 0;
        for (int i = 0; i < instances.length; i++) {
            cntNewInstances += instances[i].numInstances();
        }
        assertThat(cntNewInstances, is(cntAllInstances));

        instances = WekaTraineeSetHelper.createTraineeAndTestSets(instancesXFold, 3);
        cntNewInstances = 0;
        for (int i = 0; i < instances.length; i++) {
            cntNewInstances += instances[i].numInstances();
        }
        assertThat(cntNewInstances, is(cntAllInstances));

        instances = WekaTraineeSetHelper.createTraineeAndTestSets(instancesXFold, 4);
        cntNewInstances = 0;
        for (int i = 0; i < instances.length; i++) {
            cntNewInstances += instances[i].numInstances();
        }
        assertThat(cntNewInstances, is(cntAllInstances));

    }
}
