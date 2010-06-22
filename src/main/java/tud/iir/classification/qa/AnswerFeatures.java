package tud.iir.classification.qa;

import tud.iir.classification.FeatureObject;

public class AnswerFeatures {

    int answerWordCount;
    float similarity1;
    float similarity2;
    float similarity3;
    float similarity4;
    float similarity5;
    float similarity6;
    float similarity7;
    float similarity8;
    int answerHintBeforeAnswer;
    int tagDistance;
    int wordDistance;
    int tagCount;
    int distinctTagCount;

    public AnswerFeatures() {
    }

    public FeatureObject getAsFeatureObject(int correct) {
        Double[] features = new Double[15];
        String[] featureNames = new String[15];

        features[0] = (double) getAnswerWordCount();
        featureNames[0] = "answer word count";

        features[1] = (double) getSimilarity1();
        featureNames[1] = "similarity1";

        features[2] = (double) getSimilarity2();
        featureNames[2] = "similarity2";

        features[3] = (double) getSimilarity3();
        featureNames[3] = "similarity3";

        features[4] = (double) getSimilarity4();
        featureNames[4] = "similarity4";

        features[5] = (double) getSimilarity5();
        featureNames[5] = "similarity5";

        features[6] = (double) getSimilarity6();
        featureNames[6] = "similarity6";

        features[7] = (double) getSimilarity7();
        featureNames[7] = "similarity7";

        features[8] = (double) getSimilarity8();
        featureNames[8] = "similarity8";

        features[9] = (double) isAnswerHintBeforeAnswer();
        featureNames[9] = "answer hint";

        features[10] = (double) getTagDistance();
        featureNames[10] = "tag distance";

        features[11] = (double) getWordDistance();
        featureNames[11] = "word distance";

        features[12] = (double) getTagCount();
        featureNames[12] = "tag count";

        features[13] = (double) getDistinctTagCount();
        featureNames[13] = "distinct tag count";

        features[14] = (double) correct;
        featureNames[14] = "class";

        FeatureObject fo = new FeatureObject(features, featureNames);
        return fo;
    }

    public int getAnswerWordCount() {
        return answerWordCount;
    }

    public void setAnswerWordCount(int answerWordCount) {
        this.answerWordCount = answerWordCount;
    }

    public float getSimilarity1() {
        return similarity1;
    }

    public void setSimilarity1(float similarity1) {
        this.similarity1 = similarity1;
    }

    public float getSimilarity2() {
        return similarity2;
    }

    public void setSimilarity2(float similarity2) {
        this.similarity2 = similarity2;
    }

    public float getSimilarity3() {
        return similarity3;
    }

    public void setSimilarity3(float similarity3) {
        this.similarity3 = similarity3;
    }

    public float getSimilarity4() {
        return similarity4;
    }

    public void setSimilarity4(float similarity4) {
        this.similarity4 = similarity4;
    }

    public float getSimilarity5() {
        return similarity5;
    }

    public void setSimilarity5(float similarity5) {
        this.similarity5 = similarity5;
    }

    public float getSimilarity6() {
        return similarity6;
    }

    public void setSimilarity6(float similarity6) {
        this.similarity6 = similarity6;
    }

    public float getSimilarity7() {
        return similarity7;
    }

    public void setSimilarity7(float similarity7) {
        this.similarity7 = similarity7;
    }

    public float getSimilarity8() {
        return similarity8;
    }

    public void setSimilarity8(float similarity8) {
        this.similarity8 = similarity8;
    }

    public int isAnswerHintBeforeAnswer() {
        return answerHintBeforeAnswer;
    }

    public void setAnswerHintBeforeAnswer(int answerHintBeforeAnswer) {
        this.answerHintBeforeAnswer = answerHintBeforeAnswer;
    }

    public int getTagDistance() {
        return tagDistance;
    }

    public void setTagDistance(int tagDistance) {
        this.tagDistance = tagDistance;
    }

    public int getWordDistance() {
        return wordDistance;
    }

    public void setWordDistance(int wordDistance) {
        this.wordDistance = wordDistance;
    }

    public int getTagCount() {
        return tagCount;
    }

    public void setTagCount(int tagCount) {
        this.tagCount = tagCount;
    }

    public int getDistinctTagCount() {
        return distinctTagCount;
    }

    public void setDistinctTagCount(int distinctTagCount) {
        this.distinctTagCount = distinctTagCount;
    }
}