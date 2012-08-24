/**
 * 
 */
package ws.palladian.extraction.date.evaluation.weka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;
import ws.palladian.helper.io.ResourceHelper;

/**
 * Provides date Instances for weka learner classifier.
 * 
 * @author Martin Gregor
 * 
 */
public class WekaTraineeSetHelper {

    public static Instances[] getXFoldSets(String path, int numberOfFolds) {
        Instances[] instancesSet = new Instances[numberOfFolds];
        ArrayList<String> preData = new ArrayList<String>();
        ArrayList<String> data = new ArrayList<String>();

        FileReader fileReader = null;
        BufferedReader bufferReader = null;
        StringBuffer tempBuffer = null;
        try {
            File file = new File(ResourceHelper.getResourcePath(path));
            fileReader = new FileReader(file);
            bufferReader = new BufferedReader(fileReader);
            String line;
            boolean isPreData = true;

            while ((line = bufferReader.readLine()) != null) {
                if (isPreData) {
                    preData.add(line);
                    if (line.toLowerCase().indexOf("@data") != -1) {
                        isPreData = false;
                    }
                } else {
                    data.add(line);
                }
            }

            Collections.shuffle(data);
            int sizeOfFold = (int)Math.ceil((double)data.size() / (double)numberOfFolds);
            List<?>[] subLists = new List[numberOfFolds];
            for (int i = 0; i < numberOfFolds; i++) {
                subLists[i] = data.subList(i * sizeOfFold, Math.min((i + 1) * sizeOfFold, data.size()));
            }
            for (int i = 0; i < numberOfFolds; i++) {
                tempBuffer = new StringBuffer();
                for (int j = 0; j < preData.size(); j++) {
                    tempBuffer.append(preData.get(j) + "\n");
                }
                for (int j = 0; j < subLists[i].size(); j++) {
                    tempBuffer.append(subLists[i].get(j) + "\n");
                }
                StringReader reader = new StringReader(tempBuffer.toString());
                ArffReader arffReader = new ArffReader(reader);
                instancesSet[i] = arffReader.getData();
                // instancesSet[i] = new Instances(fileReader);
            }

            bufferReader.close();
            fileReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            if (tempBuffer != null) {
                System.out.println(tempBuffer.toString());
            }
            e.printStackTrace();
        }

        return instancesSet;
    }

    public static Instances[] createTraineeAndTestSets(Instances[] allInstances, int traineeSetNumber) {
        Instances[] instances = new Instances[2];
        Instances testInstances = null;
        for (int i = 0; i < allInstances.length; i++) {
            if (i != traineeSetNumber) {
                if (testInstances == null) {
                    testInstances = new Instances(allInstances[i]);
                } else {
                    Enumeration<Instance> instanceEnum = allInstances[i].enumerateInstances();
                    while (instanceEnum.hasMoreElements()) {
                        testInstances.add(new Instance(instanceEnum.nextElement()));
                    }
                }
            }
        }

        instances[0] = new Instances(allInstances[traineeSetNumber]);
        instances[1] = testInstances;
        return instances;
    }

    public static ArrayList<String> removeAttribute(Instances instances, String attributeString) {
        ArrayList<String> ids = new ArrayList<String>();
        Attribute attribute = instances.attribute(attributeString);
        int attributeIndex = attribute.index();
        Enumeration<Instance> instanceEnum = instances.enumerateInstances();
        while (instanceEnum.hasMoreElements()) {
            Instance instance = instanceEnum.nextElement();
            ids.add(String.valueOf(Math.round(instance.value(attributeIndex))));
        }

        instances.deleteAttributeAt(attributeIndex);
        return ids;
    }

    // public static void main(String[] arg) {
    // WekaTraineeSetHelper wtsh = new WekaTraineeSetHelper();
    // Instances[] instances;
    // try {
    // instances = wtsh.getXFoldSets("/wekaClassifier/allAttributes.arff",
    // 5);
    // Instances testSet = wtsh.createTraineeAndTestSets(instances, 3)[1];
    // ArrayList<String> ids = wtsh.removeAttribute(testSet, "id");
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }

}
