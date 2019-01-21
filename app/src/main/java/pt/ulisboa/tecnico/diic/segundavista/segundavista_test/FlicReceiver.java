package pt.ulisboa.tecnico.diic.segundavista.segundavista_test;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import io.flic.lib.FlicBroadcastReceiver;
import io.flic.lib.FlicButton;
import io.flic.lib.FlicManager;
import android.speech.tts.TextToSpeech;

public class FlicReceiver extends FlicBroadcastReceiver {

    @Override
    protected void onRequestAppCredentials(Context context) {
        FlicManager.setAppCredentials("99df8ff9-e927-4bf2-bec1-9bc1d982dee9", "498f136f-ce96-4e43-9929-1b2484218c83", "segunda vISTa");
    }

    @Override
    public void onButtonUpOrDown(final Context context, final FlicButton button, boolean wasQueued, int timeDiff, boolean isUp, boolean isDown) {
        if (isUp) {
            if (MainActivity.clicks == 0) {
                CountDownTimer timer = new CountDownTimer(1500, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        // do nothing :D
                    }

                    @Override
                    public void onFinish() {
                        if (MainActivity.clicks == 1) {
                            onButtonSingleClick();
                        } else if (MainActivity.clicks == 2) {
                            onButtonDoubleClick();
                        } else {
                            onButtonTripleClick();
                        }
                        MainActivity.clicks = 0;
                    }
                }.start();
            }
            MainActivity.clicks += 1;
        } else {
            // Code for button down event here
        }
    }

    public void onButtonSingleClick() {
        Log.d("singleClick", ":)");
            if (MainActivity.state == 0) {
                // do nothing :D
            } else if (MainActivity.state == 1) {
                // if it's still speaking, stop audio
                if(MainActivity.mTTs.isSpeaking())
                    MainActivity.mTTs.stop();
                else {
                    // beacon's extra info
                    MainActivity.data.setText(MainActivity.currentBeacon.getExtraInfo());
                    MainActivity.mTTs.speak("Painting Description. " + MainActivity.currentBeacon.getExtraInfo(), TextToSpeech.QUEUE_FLUSH, null);
                }
            } else {
                MainActivity.quiz.play("SINGLE");
            }
    }

    public void onButtonDoubleClick() {
        Log.d("doubleClick", ":)");
        if (MainActivity.state == 0) {
            // do nothing :D
        }
        else if (MainActivity.state == 1) {
            // beacon's fun facts
            MainActivity.data.setText(MainActivity.currentBeacon.getFunFacts());
            MainActivity.mTTs.speak("Fun Facts. " + MainActivity.currentBeacon.getFunFacts(), TextToSpeech.QUEUE_FLUSH, null);
        }
        else {
            MainActivity.quiz.play("DOUBLE");
        }
    }

    public void onButtonTripleClick() {
        Log.d("tripleClick", ":)");
        if (MainActivity.state == 0) {
            // do nothing :)
        }
        else if (MainActivity.state == 1) {
            // start beacon's quiz
            MainActivity.state = 2;
            MainActivity.quiz = new Quiz(MainActivity.currentBeacon);
        }
        else {
            // stop playing quiz
            MainActivity.data.setText("You left the quiz.");
            MainActivity.mTTs.speak("You left the quiz.", TextToSpeech.QUEUE_FLUSH, null);
            MainActivity.state = 1;
        }
    }

    @Override
    public void onButtonRemoved(Context context, FlicButton button) {
        // Button was removed
        MainActivity.data.setText("Connection to Flic button was lost.");
        MainActivity.mTTs.speak("Connection to Flic button was lost.", TextToSpeech.QUEUE_FLUSH, null);
    }
}
