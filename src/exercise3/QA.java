package exercise3;

import java.io.Serializable;

public class QA implements Serializable {
    private String question;
    private String answer;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String q) {
        question = q;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String q) {
        answer = q;
    }
}