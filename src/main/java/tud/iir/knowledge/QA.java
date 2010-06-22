package tud.iir.knowledge;

import java.util.ArrayList;

import tud.iir.extraction.qa.QASite;
import tud.iir.helper.StringHelper;

public class QA extends Extractable {

    private static final long serialVersionUID = 5928350250594103674L;

    private String question = "";
    private ArrayList<String> answers = null;
    private QAXPathInformation qaInformation = null;
    private QASite qaSite = null;

    public QA(QASite qaSite) {
        this.qaSite = qaSite;
        answers = new ArrayList<String>();
        qaInformation = new QAXPathInformation();
        sources = new Sources<Source>();
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question, String url, String xPath) {
        this.question = StringHelper.trim(question, "?");
        qaInformation.addQuestionInformation(url, xPath);
    }

    public ArrayList<String> getAnswers() {
        return answers;
    }

    public boolean addAnswer(String answer, String url, String xPath) {

        answer = StringHelper.trim(answer, "?!.");

        // only take text around pre- and suffix if available
        int index0 = 0;
        int index1 = answer.length();
        if (qaSite.getAnswerPrefix().length() > 0) {
            index0 = answer.indexOf(qaSite.getAnswerPrefix());
        }

        if (qaSite.getAnswerSuffix().length() > 0) {
            index1 = answer.lastIndexOf(qaSite.getAnswerSuffix());
        }

        if (index0 != -1 && index0 + qaSite.getAnswerPrefix().length() < index1) {
            answer = answer.substring(index0 + qaSite.getAnswerPrefix().length(), index1);
        } else if (qaSite.getAnswerPrefix().length() > 0 && qaSite.getAnswerSuffix().length() > 0) {
            return false;
        }

        boolean newCheck = answers.add(answer);

        if (!newCheck)
            return false;

        qaInformation.addAnswerInformation(answers.size() - 1, url, xPath);

        return true;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(getQuestion()).append("\n");

        for (String answer : answers) {
            stringBuilder.append("\t").append(answer).append("\n");
        }

        return stringBuilder.toString();
    }
}