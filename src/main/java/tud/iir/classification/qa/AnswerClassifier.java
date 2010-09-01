package tud.iir.classification.qa;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import tud.iir.extraction.PageAnalyzer;
import tud.iir.extraction.qa.QAExtractor;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;

/**
 * Classify an answer for a question.
 * 
 * @author David Urbansky
 * 
 */
public class AnswerClassifier extends Classifier {

    private String[] featureNames;

    public AnswerClassifier(int type) {
        super(type);
        featureNames = new String[14];
        featureNames[0] = "answer word count";
        featureNames[1] = "similarity1";
        featureNames[2] = "similarity2";
        featureNames[3] = "similarity3";
        featureNames[4] = "similarity4";
        featureNames[5] = "similarity5";
        featureNames[6] = "similarity6";
        featureNames[7] = "similarity7";
        featureNames[8] = "similarity8";
        featureNames[9] = "A or Answer hint found";
        featureNames[10] = "tag distance to question";
        featureNames[11] = "word distance to question";
        featureNames[12] = "tag count";
        featureNames[13] = "distinct tag count";
    }

    /**
     * Use an already trained classifier.
     * 
     * TODO pull this method up? I have copied this to NewsRankingClassifier for now. We should have the possibility to
     * set file names for the serialized model to avoid conflicts between different Classifier subclasses -- Philipp.
     */
    public void useTrainedClassifier() {
        weka.classifiers.Classifier trainedAnswerClassifier;
        try {
            trainedAnswerClassifier = (weka.classifiers.Classifier) weka.core.SerializationHelper.read("data/learnedClassifiers/"
                    + getChosenClassifierName() + ".model");
            createWekaAttributes(featureNames.length, featureNames);
            setClassifier(trainedAnswerClassifier);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Train and save a classifier. Use all html documents in the specified path.
     */
    @Override
    public void trainClassifier(String dirPath) {
        ArrayList<FeatureObject> featureObjects = new ArrayList<FeatureObject>();
        QAExtractor qae = QAExtractor.getInstance();
        PageAnalyzer pa = new PageAnalyzer();

        // load the questions to the web pages in an array
        if (dirPath.endsWith("/")) {
            dirPath = dirPath.substring(0, dirPath.length() - 1);
        }
        List<String> questions = FileHelper.readFileToArray(dirPath + "/questions.txt");
        List<String> answers = FileHelper.readFileToArray(dirPath + "/answers.txt");

        // iterate through all web pages of the qa test set
        File folder = new File(dirPath);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (!listOfFiles[i].isFile()) {
                continue;
            }
            if (!listOfFiles[i].getName().endsWith("html")) {
                continue;
            }

            // if (i < 31) continue;
            // i = 29;

            int index = Integer.valueOf(listOfFiles[i].getName().replaceAll("\\.html", "").replaceAll("webpage", "")) - 1;

            pa.setDocument(listOfFiles[i].getAbsolutePath());
            qae.setPa(pa);

            FeatureObject fo = null;

            // get the question that appears on the page
            String question = questions.get(index);
            LinkedHashSet<String> questionXPaths = pa.constructAllXPaths(question);

            // get the correct answer from the manually created file
            // String answerXPath = answers.get(index).toUpperCase();
            String answerKeywords = answers.get(index);
            LinkedHashSet<String> answerXPaths = new LinkedHashSet<String>();
            if (answerKeywords.startsWith("/html")) {
                answerXPaths.add(answerKeywords.toUpperCase());
            } else {
                answerXPaths = pa.constructAllXPaths(answerKeywords);
            }

            System.out.println("index " + index + ", " + answerKeywords);
            String answerHTML = pa.getHTMLTextByXPath(answerXPaths.iterator().next()).trim();
            // String answer = pa.getTextByXPath(answerXPath).trim();

            // add the correct answer as a positive example
            fo = qae.getAnswerFeatures(question, answerHTML).getAsFeatureObject(1);
            featureObjects.add(fo);
            System.out.println(fo);

            // get features for each incorrect candidate as negative example
            LinkedHashSet<String> answerCandidates = pa.constructAllXPaths(" ");
            LinkedHashSet<String> filteredAnswerCandidates = qae.filterAnswerCandidates(questionXPaths, answerCandidates);

            // chose 2 negative examples
            int randomNumber1 = (int) (Math.random() * filteredAnswerCandidates.size());
            int randomNumber2 = (int) (Math.random() * filteredAnswerCandidates.size());

            int count = -1;
            for (Iterator<String> iterator = filteredAnswerCandidates.iterator(); iterator.hasNext();) {
                count++;

                if (count != randomNumber1 && count != randomNumber2) {
                    iterator.next();
                    continue;
                }

                String answerCandidateXPath = iterator.next();

                // skip entries that point to the correct answer
                if (answerXPaths.contains(answerCandidateXPath)) {
                    continue;
                }

                // String answerContent = pa.getTextByXPath(answerCandidateXPath);
                answerHTML = StringHelper.trim(pa.getHTMLTextByXPath(answerCandidateXPath));

                // use as negative training example
                fo = qae.getAnswerFeatures(question, answerHTML).getAsFeatureObject(0);
                featureObjects.add(fo);
                System.out.println("answer: " + answerHTML.substring(0, Math.min(120, answerHTML.length())) + "\n" + fo);
            }
        }

        setTrainingObjects(featureObjects);
        super.trainClassifier();

        // serialize model
        try {
            weka.core.SerializationHelper.write("data/learnedClassifiers/" + getChosenClassifierName() + ".model", getClassifier());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testClassifier(String dirPath) {

        // ArrayList<FeatureObject> featureObjects = new ArrayList<FeatureObject>();
        QAExtractor qae = QAExtractor.getInstance();
        qae.setAnswerClassifier(getChosenClassifier());
        PageAnalyzer pa = new PageAnalyzer();

        // load the questions to the web pages in an array
        if (dirPath.endsWith("/")) {
            dirPath = dirPath.substring(0, dirPath.length() - 1);
        }
        List<String> questions = FileHelper.readFileToArray(dirPath + "/questions.txt");

        // iterate through all web pages of the qa test set
        File folder = new File(dirPath);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (!listOfFiles[i].isFile()) {
                continue;
            }
            if (!listOfFiles[i].getName().endsWith("html")) {
                continue;
            }

            int index = Integer.valueOf(listOfFiles[i].getName().replaceAll("\\.html", "").replaceAll("webpage", "")) - 1;

            pa.setDocument(listOfFiles[i].getAbsolutePath());
            qae.setPa(pa);

            // FeatureObject fo = null;

            // get the question that appears on the page
            String question = questions.get(index);
            LinkedHashSet<String> questionXPaths = pa.constructAllXPaths(question);

            String[] answerInformation = qae.detectAnswer(question, questionXPaths);

            System.out.println("Question: " + question);
            System.out.println("Answer: " + answerInformation[0]);
            System.out.println("Answer xPath: " + answerInformation[1] + "\n\n");
        }

    }

    public double rankAnswer(AnswerFeatures af) {
        double[] distribution = classifySoft(af.getAsFeatureObject(-1));
        // System.out.println("distribution: " + distribution[0] + "," + distribution[1]);
        return distribution[0];
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        AnswerClassifier ac = new AnswerClassifier(Classifier.BAYES_NET);
        // ac.trainClassifier("data/benchmarkSelection/qa/training");
        ac.testClassifier("data/benchmarkSelection/qa/testing");
    }

}