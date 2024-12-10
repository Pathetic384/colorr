package com.example.colorr;

import static android.view.View.INVISIBLE;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class TestView extends AppCompatActivity {

    //1: test
    //2-13: red and green
    //14-17:  red or green
    //18-23: vague tritan
    //24,25: clear tritan

    ImageButton back;
    Button  enter;
    EditText box;
    ImageView image;
    TextView textView;

    String currentTest = "p1";
    List<String> red_and_green;
    List<String> red_or_green;
    List<String> vague_tritan;
    List<String> clear_tritan;

    private int current = 1;
    private TextView process;


    int total = 1, both = 0, red = 0, green = 0, tri = 0, error = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_view);


        process = findViewById(R.id.process);
        back = findViewById(R.id.back);
        box = findViewById(R.id.box);
        enter = findViewById(R.id.enter);
        image = findViewById(R.id.imageView2);
        textView = findViewById(R.id.textView2);

        process.setText(current + " / " + 10);

        back.setOnClickListener(v -> {
            startActivity(new Intent(TestView.this, MainActivity.class));
        });

        enter.setOnClickListener(v -> {
            checkAnswer();
        });

        red_and_green = new ArrayList<>(Arrays.asList("p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10", "p11", "p12", "p13"));
        red_or_green = new ArrayList<>(Arrays.asList("p14", "p15", "p16", "p17"));
        vague_tritan = new ArrayList<>(Arrays.asList("p18", "p19", "p20", "p21", "p22"));
        clear_tritan = new ArrayList<>(Arrays.asList("p24", "p25"));

        View rootView = findViewById(android.R.id.content);

        Button startTutorialButton = findViewById(R.id.tutorial);
        startTutorialButton.setOnClickListener(v -> startTutorial(rootView, image, box, enter, back));
    }

    private void startTutorial(View rootView, View imageView, View box, View enterButton, View backButton) {
        new TapTargetSequence(this)
                .targets(
                        // First: Highlight the whole screen and introduce the activity

                        // Second: Highlight the image for testing colorblindness
                        TapTarget.forView(imageView, "Test Image", "This image is an Ishihara plate for testing color blindness. Observe the number visible in the image.")
                                .outerCircleColor(R.color.blue_200) // Custom color for this target
                                .outerCircleAlpha(0.96f)
                                .transparentTarget(true)
                                .targetRadius(140)
                                .titleTextSize(20)
                                .descriptionTextSize(16)
                                .cancelable(true),

                        // Third: Highlight the EditText (box) for number entry
                        TapTarget.forView(box, "Enter the Number", "Input the number you see in the Ishihara plate into this box.")
                                .outerCircleColor(R.color.green_200) // Custom color for this target
                                .outerCircleAlpha(0.96f)
                                .targetCircleColor(android.R.color.white)
                                .transparentTarget(true)
                                .targetRadius(80)
                                .titleTextSize(20)
                                .descriptionTextSize(16)
                                .cancelable(true),

                        // Fourth: Highlight the enter button
                        TapTarget.forView(enterButton, "Next Picture", "Click this button to proceed to the next Ishihara plate. At the end, you will see your result.")
                                .outerCircleColor(R.color.purple_200) // Custom color for this target
                                .outerCircleAlpha(0.96f)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(20)
                                .descriptionTextSize(16)
                                .cancelable(true),

                        // Fifth: Highlight the back button
                        TapTarget.forView(backButton, "Back to Main Menu", "Click this button to return to the main activity at any time.")
                                .outerCircleColor(R.color.red_200) // Custom color for this target
                                .outerCircleAlpha(0.96f)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(20)
                                .descriptionTextSize(16)
                                .cancelable(true)
                )
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        // Tutorial finished
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                        // Each step of the tutorial
                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                        // Tutorial canceled
                    }
                })
                .start();
    }



    void switchTest() {
//        Toast.makeText(this, "total=" + total  + "both=" + both + " red="
//                + red + " green=" + green
//                + "error=" + error +  " tri=" + tri, Toast.LENGTH_SHORT).show();

        box.setText("");
        box.setHint("Enter number");
        if(total > 10) {
            return;
        }
        if(total == 10) {
            String result = "You do not have color blindness :D";
            if(error >= 7) result = "You might have total color blindness :(";
            else if(both >=3 && green >= 2) result = "You have Deuteranopia (green deficiency)";
            else if(both >=3 && red >= 2) result = "You have Protanopia (red deficiency)";
            else if(tri >=2 ) result = "You have Tritanopia (blue deficiency)";
            textView.setText(result);

            Resources res = getResources();
            int resID = res.getIdentifier("face" , "drawable", getPackageName());
            image.setImageResource(resID);
            box.setVisibility(INVISIBLE);
            enter.setVisibility(INVISIBLE);
            return;
        }
        if(total<6) {
            setImg(1);
        }
        if(total >= 6) {
            if(both >=2) {
                setImg(2);
            }
            else {
                if(tri <= 2) setImg(3);
                else setImg(4);
            }
        }

        current++;
        total++;
        process.setText(current + " / " + 10); // Update progress
    }

    public void setImg(int i) {
        String mDrawableName = "p1";
        if(i==1) {
            mDrawableName = red_and_green.get(new Random().nextInt(red_and_green.size()));
            red_and_green.remove(mDrawableName);
        }
        if(i==2) {
            Log.d("test", red_or_green.size() + "");
            mDrawableName = red_or_green.get(new Random().nextInt(red_or_green.size()));
            red_or_green.remove(mDrawableName);
        }
        if(i==3) {
            mDrawableName = vague_tritan.get(new Random().nextInt(vague_tritan.size()));
            vague_tritan.remove(mDrawableName);
        }
        if(i==4) {
            mDrawableName = clear_tritan.get(new Random().nextInt(clear_tritan.size()));
            clear_tritan.remove(mDrawableName);
        }
        currentTest = mDrawableName;
        Resources res = getResources();
        int resID = res.getIdentifier(mDrawableName , "drawable", getPackageName());
        image.setImageResource(resID);
    }

    void checkAnswer() {
        if(Objects.equals(currentTest, "p1")) {
            //right
            if(box.getText().toString().trim().equals("12")) {
                switchTest();
                return;
            }
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p2")) {
            //right
            if(box.getText().toString().trim().equals("8")) {
                switchTest();
                return;
            }
            //red-green
            if(box.getText().toString().trim().equals("3")) {
                both++;
                switchTest();
                return;
            }
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p3")) {
            if(box.getText().toString().trim().equals("29")) {
                switchTest();
                return;
            }
            //red green
            if(box.getText().toString().trim().equals("70")) {
                both++;
                switchTest();
                return;
            }
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p4")) {
            //right
            if(box.getText().toString().trim().equals("5")) {
                switchTest();
                return;
            }
            //red-green
            if(box.getText().toString().trim().equals("2")) {
                both++;
                switchTest();
                return;
            }
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p5")) {
            //right
            if(box.getText().toString().trim().equals("3")) {
                switchTest();
                return;
            }
            //red-green
            if(box.getText().toString().trim().equals("5")) {
                both++;
                switchTest();
                return;
            }
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p6")) {
            if(box.getText().toString().trim().equals("15")) {
                switchTest();
                return;
            }
            //red green
            if(box.getText().toString().trim().equals("17")) {
                both++;
                switchTest();
                return;
            }
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p7")) {
            if(box.getText().toString().trim().equals("74")) {
                switchTest();
                return;
            }
            //red green
            if(box.getText().toString().trim().equals("21")) {
                both++;
                switchTest();
                return;
            }
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p8")) {
            if(box.getText().toString().trim().equals("6")) {
                switchTest();
                return;
            }
            //else color blind
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p9")) {
            if(box.getText().toString().trim().equals("45")) {
                switchTest();
                return;
            }
            //else...
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p10")) {
            if(box.getText().toString().trim().equals("5")) {
                switchTest();
                return;
            }
            //else...
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p11")) {
            if(box.getText().toString().trim().equals("7")) {
                switchTest();
                return;
            }
            //else...
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p12")) {
            if(box.getText().toString().trim().equals("16")) {
                switchTest();
                return;
            }
            //else...
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p13")) {
            if(box.getText().toString().trim().equals("73")) {
                switchTest();
                return;
            }
            //else...
            error++;
            both++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p14")) {
            if(box.getText().toString().trim().equals("26")) {
                switchTest();
                return;
            }
            //no red
            if(box.getText().toString().trim().equals("6")) {
                red++;
                switchTest();
                return;
            }
            //no green
            if(box.getText().toString().trim().equals("2")) {
                green++;
                switchTest();
                return;
            }
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p15")) {
            if(box.getText().toString().trim().equals("42")) {
                switchTest();
                return;
            }
            //no red
            if(box.getText().toString().trim().equals("2")) {
                switchTest();
                red++;
                return;
            }
            //no green
            if(box.getText().toString().trim().equals("4")) {
                switchTest();
                green++;
                return;
            }
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p16")) {
            if(box.getText().toString().trim().equals("35")) {
                switchTest();
                return;
            }
            //no red
            if(box.getText().toString().trim().equals("5")) {
                switchTest();
                red++;
                return;
            }
            //no green
            if(box.getText().toString().trim().equals("3")) {
                switchTest();
                green++;
                return;
            }
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p17")) {
            if(box.getText().toString().trim().equals("96")) {
                switchTest();
                return;
            }
            //no red
            if(box.getText().toString().trim().equals("6")) {
                switchTest();
                red++;
                return;
            }
            //no green
            if(box.getText().toString().trim().equals("9")) {
                switchTest();
                green++;
                return;
            }
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p18")) {
            if(box.getText().toString().trim().equals("97")) {
                switchTest();
                return;
            }
            //else tritan
            tri++;
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p19")) {
            if(box.getText().toString().trim().equals("45")) {
                switchTest();
                return;
            }
            //else tritan
            tri++;
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p20")) {
            if(box.getText().toString().trim().equals("16")) {
                switchTest();
                return;
            }
            //else tritan
            tri++;
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p21")) {
            if(box.getText().toString().trim().equals("73")) {
                switchTest();
                return;
            }
            //else tritan
            tri++;
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p22")) {
            if(box.getText().toString().trim().equals("29")) {
                switchTest();
                return;
            }
            //tritan
            if(box.getText().toString().trim().equals("70")) {
                switchTest();
                tri++;
                return;
            }
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p23")) {
            if(box.getText().toString().trim().equals("57")) {
                switchTest();
                return;
            }
            //tritan
            if(box.getText().toString().trim().equals("55")) {
                switchTest();
                tri++;
                return;
            }
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p24")) {
            if(box.getText().toString().trim().equals("15")) {
                switchTest();
                return;
            }
            //tritan
            if(box.getText().toString().trim().equals("17")) {
                switchTest();
                tri++;
                return;
            }
            error++;
            switchTest();
        }


        else if(Objects.equals(currentTest, "p25")) {
            if(box.getText().toString().trim().equals("74")) {
                switchTest();
                return;
            }
            //tritan
            if(box.getText().toString().trim().equals("21")) {
                switchTest();
                tri++;
                return;
            }
            error++;
            switchTest();
        }
    }
}