package tud.iir.knowledge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

class QAXPathInformation implements Serializable {

    private static final long serialVersionUID = 7840532658220421123L;

    private ArrayList<String[]> questionSourcesPaths = null;
    private HashMap<Integer, String[]> answerSourcesPaths = null;

    public QAXPathInformation() {
        questionSourcesPaths = new ArrayList<String[]>();
        answerSourcesPaths = new HashMap<Integer, String[]>();
    }

    public void addQuestionInformation(String url, String xPath) {
        String[] questionInformation = { url, xPath };
        questionSourcesPaths.add(questionInformation);
    }

    public void addAnswerInformation(int index, String url, String xPath) {
        String[] answerInformation = { url, xPath };
        answerSourcesPaths.put(index, answerInformation);
    }

}