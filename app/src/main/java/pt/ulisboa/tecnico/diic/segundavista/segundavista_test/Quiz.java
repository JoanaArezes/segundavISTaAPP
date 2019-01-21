package pt.ulisboa.tecnico.diic.segundavista.segundavista_test;

import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;

public class Quiz {
    private Beacon beacon;

    private ArrayList<String> questions = new ArrayList<>();
    private ArrayList<String> answers = new ArrayList<>();
    private ArrayList<String> extras = new ArrayList<>();

    private int currentQuestion = 0;
    private boolean isPlaying = false;

    public Quiz(Beacon beacon) {
        this.beacon = beacon;

        questions.add(beacon.getQuestion1());
        questions.add(beacon.getQuestion2());
        questions.add(beacon.getQuestion3());

        answers.add(beacon.getAnswer1());
        answers.add(beacon.getAnswer2());
        answers.add(beacon.getAnswer3());

        extras.add(beacon.getExtraAnswer1());
        extras.add(beacon.getExtraAnswer2());
        extras.add(beacon.getExtraAnswer3());

        this.isPlaying = true;
        MainActivity.mTTs.speak("You started the quiz. When you hear a question, press one time for True, or two times for False. After you hear the answer, press one time to hear the next question. Here's the first." + questions.get(currentQuestion), TextToSpeech.QUEUE_FLUSH, null);
        //this.playCurrentQuestion();
    }

    public void playCurrentQuestion() {
        MainActivity.data.setText(this.questions.get(currentQuestion));
        MainActivity.mTTs.speak(this.questions.get(currentQuestion), TextToSpeech.QUEUE_FLUSH, null);
    }

    public String checkAnswer(String clickType) {

        if (clickType.equals("SINGLE")) {
            if (this.answers.get(currentQuestion).equals("True")) {
                return "Correct!";
            } else {
                return "Incorrect.";
            }
        } else if (clickType.equals("DOUBLE")) {
            if (this.answers.get(currentQuestion).equals("False")) {
                return "Correct!";
            } else {
                return "Incorrect.";
            }
        }
        return "";
    }

    public void play(String clickType) {
        if (isPlaying) {
            String answer = checkAnswer(clickType);

            if (!answer.equals("")) {
                if (extras.get(currentQuestion) != null) {
                    answer = answer + "\n" + extras.get(currentQuestion);
                }
                MainActivity.data.setText(answer);
                MainActivity.mTTs.speak(answer, TextToSpeech.QUEUE_FLUSH, null);
            } else {
                Log.d("play", "invalid type of click");
            }
            isPlaying = false;
        }
        else {
            currentQuestion++;
            if (currentQuestion > 2) {
                MainActivity.data.setText("This quiz has ended");
                MainActivity.mTTs.speak("This quiz has ended.", TextToSpeech.QUEUE_FLUSH, null);
                MainActivity.state = 1;
            } else {
                playCurrentQuestion();
                isPlaying = true;
            }
        }
    }
}
