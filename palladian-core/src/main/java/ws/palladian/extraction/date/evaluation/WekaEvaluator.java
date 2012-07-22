package ws.palladian.extraction.date.evaluation;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import ws.palladian.extraction.date.evaluation.weka.WekaClassifierEval;
import ws.palladian.extraction.date.technique.PageDateType;

public class WekaEvaluator {

    /**
     * @param args
     */
    public static void main(String[] args) {

        // 0 - load Url and Serializer
        work0();
    }

    private static void work0() {
        Classifier classifier = null;
        String classifierString = "data/wekaClassifier/classifier.model";
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(classifierString)));
            classifier = (Classifier)ois.readObject();

            WekaClassifierEval wce = new WekaClassifierEval();
            PageDateType classIndex = PageDateType.publish;
            String classAttributeName;
            Attribute classAttribute = null;
            Enumeration<Attribute> attributes;

            if (classIndex.equals(PageDateType.publish)) {
                classAttributeName = "pub";
            } else {
                classAttributeName = "mod";
            }

            BufferedReader reader;
            Instances instances = null;
            classifier = wce.getAtributeSelectedClassifier();

            // classifier = wce.getThreshold();
            try {
                reader = new BufferedReader(new FileReader("d:/wekaout/datesets/pubtrainee.arff"));
                instances = new Instances(reader);
                attributes = instances.enumerateAttributes();
                while (attributes.hasMoreElements()) {
                    Attribute attribute = attributes.nextElement();
                    if (attribute.name().equals(classAttributeName)) {
                        classAttribute = attribute;
                        break;
                    }
                }
                instances.setClass(classAttribute);
                classifier.buildClassifier(instances);
                SerializationHelper.write(classifierString, classifier);
            } catch (FileNotFoundException e2) {
                e2.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        File file = new File("d:/wekaout/datesets/pubtest.arff");
        BufferedReader reader;
        try {
            ArrayList<Integer> idList = new ArrayList<Integer>();
            reader = new BufferedReader(new FileReader(file));
            file = new File("d:/wekaout/datesets/pubTestTemp.arff");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
            String line;
            int i = 1;
            while ((line = reader.readLine()) != null) {
                if (i > 32) {
                    idList.add(Integer.valueOf(line.substring(0, 5)));
                    line = line.substring(6);
                }
                writer.write(line + "\n");
                i++;
            }
            writer.close();
            reader.close();
            reader = new BufferedReader(new FileReader(file));
            Instances instances = new Instances(reader);
            instances.setClassIndex(1);
            Enumeration<Instance> instanceEnum = instances.enumerateInstances();
            HashMap<Integer, Double> resultMap = new HashMap<Integer, Double>();
            int j = 0;
            while (instanceEnum.hasMoreElements()) {
                Instance instance = instanceEnum.nextElement();
                int id = Integer.valueOf(instance.toString(0));
                // instance.setMissing(0);
                instance.setClassMissing();
                try {
                    double[] dbl = classifier.distributionForInstance(instance);
                    resultMap.put(idList.get(j), dbl[1]);
                } catch (Exception e) {
                    System.out.println(classifier == null);
                    e.printStackTrace();
                }
                j++;
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
