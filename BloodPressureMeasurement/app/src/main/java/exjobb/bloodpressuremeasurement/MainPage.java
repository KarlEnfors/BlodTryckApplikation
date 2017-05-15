package exjobb.bloodpressuremeasurement;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MainPage extends Activity {

    ImageButton Calibrate;
    ImageButton runTestButton;
    ImageButton showGraphButton;
    ImageButton drawSampleButton;
    VideoLoader videoLoader;
    Handler handler;
    ProgressDialog loader;
    FrameDataHandler dataHandler;

    Patient patient;
    TextView patientDisplay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);


        handler = new Handler();
        videoLoader = new VideoLoader(this);
        dataHandler = new FrameDataHandler();

        patient = new Patient("DEBUG");
        patientDisplay = (TextView) findViewById(R.id.patient_display);

        loader = new ProgressDialog(MainPage.this);
        Calibrate = (ImageButton) findViewById(R.id.load_data_button);
        runTestButton   = (ImageButton) findViewById(R.id.run_test_button);
        showGraphButton = (ImageButton) findViewById(R.id.showGraphButton);
        drawSampleButton = (ImageButton) findViewById(R.id.drawSampleButton);

        //LOAD VIDEO BUTTON.
        Calibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrate();

                Calibrate.setImageAlpha(70);
                Calibrate.setScaleX(1.3f);
                Calibrate.setScaleY(1.3f);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Calibrate.setImageAlpha(255);
                        Calibrate.setScaleX(1);
                        Calibrate.setScaleY(1);
                    }
                }, 300);
            }
        });

        runTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runTestButton.setImageAlpha(70);
                runTestButton.setScaleX(1.3f);
                runTestButton.setScaleY(1.3f);
                MainPage.this.DEBUG();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        runTestButton.setImageAlpha(255);
                        runTestButton.setScaleX(1);
                        runTestButton.setScaleY(1);

                    }
                }, 300);
            }
        });

        showGraphButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGraphButton.setImageAlpha(70);
                showGraphButton.setScaleX(1.3f);
                showGraphButton.setScaleY(1.3f);
                drawGraph();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showGraphButton.setImageAlpha(255);
                        showGraphButton.setScaleX(1);
                        showGraphButton.setScaleY(1);
                    }
                }, 300);
            }
        });

        drawSampleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawSampleButton.setImageAlpha(70);
                drawSampleButton.setScaleX(1.3f);
                drawSampleButton.setScaleY(1.3f);

                MainPage.this.runTest();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        drawSampleButton.setImageAlpha(255);
                        drawSampleButton.setScaleX(1);
                        drawSampleButton.setScaleY(1);

                    }
                }, 300);
            }
        });


        displayData();
        setupPatient();

    }

    public void drawGraph() {
        GraphView graph = new GraphView(this);
        ArrayList<Double> pictureDataA;
        ArrayList<Double> pictureDataB;

        if (videoLoader.getDataSourceA() == null){
            pictureDataA = (ArrayList<Double>) patient.debugDataA.clone();
            pictureDataB = (ArrayList<Double>) patient.debugDataB.clone();

        }else {
            pictureDataA = (ArrayList<Double>) videoLoader.getDataSourceA().clone();
            pictureDataB = (ArrayList<Double>) videoLoader.getDataSourceB().clone();
        }

        pictureDataA = dataHandler.filterAmp(pictureDataA, 80);
        pictureDataB = dataHandler.filterAmp(pictureDataB, 80);

        dataHandler.planeOutData(pictureDataA);
        dataHandler.planeOutData(pictureDataB);

        pictureDataA = dataHandler.filterSmoothing(pictureDataA, 2, 10);
        pictureDataA = dataHandler.deltaData(pictureDataA);
        pictureDataA = dataHandler.filterSmoothing(pictureDataA, 2, 5);
        pictureDataA = dataHandler.drawFromDelta(pictureDataA);
        pictureDataA = dataHandler.filterSmoothing(pictureDataA, 2, 10);

        pictureDataB = dataHandler.filterSmoothing(pictureDataB, 2, 10);
        pictureDataB = dataHandler.deltaData(pictureDataB);
        pictureDataB = dataHandler.filterSmoothing(pictureDataB, 2, 5);
        pictureDataB = dataHandler.drawFromDelta(pictureDataB);
        pictureDataB = dataHandler.filterSmoothing(pictureDataB, 2, 10);

        //For Peak DEBUG
        //dataHandler.getAvgPeakFrameDifference(dataHandler.peakDetection(pictureDataA), dataHandler.peakDetection(pictureDataB));

        //graph.drawPeakGraph(dataHandler.peakDetection(pictureDataA), dataHandler.peakDetection(pictureDataB), Color.RED, Color.GREEN);

        graph.drawGraph(pictureDataA, Color.BLACK);
        graph.show();

        graph.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Dialog diaA = new Dialog(this);
        diaA.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        diaA.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        diaA.setContentView(graph);
        diaA.show();
    }

    private void setupPatient(){
        final Dialog d = new Dialog(this);
        final LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        final Button newPatientB = new Button(this);
        newPatientB.setText("New Patient");
        Button oldPatientB = new Button(this);
        oldPatientB.setText("Load Patient");

        newPatientB.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final EditText textField = new EditText(MainPage.this);
                textField.setHint("Name");
                container.removeAllViews();
                container.addView(textField);
                newPatientB.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.e("Patient name: ", "" + textField.getText());
                        patient = new Patient(textField.getText().toString());
                        d.dismiss();
                    }
                });

                newPatientB.setText("Ok");
                container.addView(newPatientB);
            }
        });

        oldPatientB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("Old patient", "Load file patient data");


            }
        });

        container.addView(newPatientB);
        container.addView(oldPatientB);

        d.setContentView(container);
        d.show();
    }


    private void runTest(){

        videoLoader.loadFiles(new Runnable() {
            @Override
            public void run() {
                displayTestResult();
            }
        });

    }


    public void displayTestResult(){
        Dialog testResult = new Dialog(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("BP result:");
        title.setTextSize(32.f);


        double result = patient.getTestResult(dataHandler.getFrameDiffFromData(videoLoader.getDataSourceA(), videoLoader.getDataSourceB()), true);
        title.append("\n Systolic : " + result);
        Log.e("Measurement...","Systolic  : " + result);
        result = patient.getTestResult(dataHandler.getFrameDiffFromData(videoLoader.getDataSourceA(), videoLoader.getDataSourceB()), false);
        title.append("\n Diastolic: " + result);
        Log.e("Measurement...","Diastolic : " + result);


        container.addView(title);

        testResult.setContentView(container);
        testResult.show();

    }

    private void calibrate(){
        if (patient == null){
            setupPatient();
            return;
        }

        final Dialog d = new Dialog(this);
        d.setTitle("Calibration");
        final LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        final Button confirmB = new Button(this);
        confirmB.setText("Ok");

        TextView info = new TextView(this);
        info.setText("Type calibration BP:");

        final EditText sysBPinput = new EditText(this);
        sysBPinput.setInputType(InputType.TYPE_CLASS_NUMBER);
        sysBPinput.setHint("Systolic");
        final EditText diaBPinput = new EditText(this);
        diaBPinput.setInputType(InputType.TYPE_CLASS_NUMBER);
        diaBPinput.setHint("Diastolic");

        container.addView(info);
        container.addView(sysBPinput);
        container.addView(diaBPinput);
        container.addView(confirmB);
        d.setContentView(container);
        d.show();

        confirmB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patient.setCaliBP(Integer.valueOf(sysBPinput.getText().toString()), Integer.valueOf(diaBPinput.getText().toString()), 0.2d);
                patient.savePatient(MainPage.this);
                videoLoader.loadFiles(new Runnable() {
                    @Override
                    public void run() {
                        setupCalibration();
                    }
                });
                d.dismiss();
            }
        });
    }

    public void setupCalibration(){
        //dataHandler.setSource();

        /*
        patient.debugDataA = (ArrayList<Double>) videoLoader.getDataSourceA().clone();
        patient.debugDataB = (ArrayList<Double>) videoLoader.getDataSourceB().clone();

        saveDebug();
        ArrayList<Double> areaA = (ArrayList<Double>) videoLoader.getDataSourceA().clone();
        ArrayList<Double> areaB = (ArrayList<Double>) videoLoader.getDataSourceB().clone();

        areaA = dataHandler.filterAmp(areaA, 80);
        areaB = dataHandler.filterAmp(areaB, 80);

        dataHandler.planeOutData(areaA);
        dataHandler.planeOutData(areaB);

        areaA = dataHandler.filterSmoothing(areaA, 2, 10);
        areaA = dataHandler.deltaData(areaA);
        areaA = dataHandler.filterSmoothing(areaA, 2, 5);
        areaA = dataHandler.drawFromDelta(areaA);
        areaA = dataHandler.filterSmoothing(areaA, 2, 10);

        areaB = dataHandler.filterSmoothing(areaB, 2, 10);
        areaB = dataHandler.deltaData(areaB);
        areaB = dataHandler.filterSmoothing(areaB, 2, 5);
        areaB = dataHandler.drawFromDelta(areaB);
        areaB = dataHandler.filterSmoothing(areaB, 2, 10);


        double frameDiff = dataHandler.getAvgPeakFrameDifference(dataHandler.peakDetection(areaA), dataHandler.peakDetection(areaB));
*/
        double frameDiff = dataHandler.getFrameDiffFromData(videoLoader.getDataSourceA(), videoLoader.getDataSourceB());
        double PWV = patient.patientData.get(Patient.LENGTH_PARAM);
        double denom = (frameDiff * 8333.d);

        Log.e("Cali. Setup:", " frameDiff: " + frameDiff);

        if (denom != 0){
            PWV /= denom;
            patient.setCaliCoefficients(frameDiff, true);
            patient.setCaliCoefficients(frameDiff, false);
        }


        //SHOULD SAVE HERE :) idiot
        drawGraph();

    }

    public void saveDebug(){

        try{

            FileOutputStream FOS = openFileOutput("DEBUGA", Context.MODE_PRIVATE);
            ObjectOutputStream OOS = new ObjectOutputStream(FOS);
            OOS.writeObject(patient.debugDataA);
            FOS.close();
            OOS.close();

            FOS = openFileOutput("DEBUGB", Context.MODE_PRIVATE);
            OOS = new ObjectOutputStream(FOS);
            OOS.writeObject(patient.debugDataB);
            FOS.close();
            OOS.close();

        }catch (Exception e){

        }finally {

        }
    }

    public void loadDebug(String fileName){
        try{
            FileInputStream FIS = openFileInput("DEBUGA");
            ObjectInputStream OIS = new ObjectInputStream(FIS);
            patient.debugDataA = (ArrayList<Double>) OIS.readObject();
            FIS.close();
            OIS.close();

            FIS = openFileInput("DEBUGB");
            OIS = new ObjectInputStream(FIS);
            patient.debugDataB = (ArrayList<Double>) OIS.readObject();
            FIS.close();
            OIS.close();

        }catch (Exception e){
            Log.e("LOAD ERROR","" + e.toString());

        }finally {

        }
    }

    private void DEBUG(){

        patient.setCaliBP(120, 80, 0.2);
        patient.setCaliCoefficients(5, true);
        patient.setCaliCoefficients(5, false);


        Dialog testResult = new Dialog(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("BP result:");
        title.setTextSize(32.f);

        double result = patient.getTestResult(5, true);
        title.append("\n Systolic : " + Math.round(result));
        Log.e("Measurement...","Systolic  : " + Math.round(result));
        result = patient.getTestResult(5, false);
        title.append("\n Diastolic: " + Math.round(result));
        Log.e("Measurement...","Diastolic : " + Math.round(result));

        container.addView(title);
        testResult.setContentView(container);
        testResult.show();






    }


    private void displayData(){
        patientDisplay.setTextSize(24.f);
        handler.post(new Runnable() {
            @Override
            public void run() {
                try{
                    patientDisplay.setText(patient.getName());
                    ArrayList<Double> p = patient.getData();
                    patientDisplay.append("\nSys.  Calibration BP: " + p.get(Patient.BP_SYS_CALIBRATED));
                    patientDisplay.append("\nDias. Calibration BP: " + p.get(Patient.BP_DIA_CALIBRATED));
                    patientDisplay.append("\nSys.  Coefficients:   " + p.get(Patient.BP_SYS_COEF));
                    patientDisplay.append("\nDias. Coefficients:   " + p.get(Patient.BP_DIA_COEF));
                    patientDisplay.append("\nPTTD:                 " + p.get(Patient.FRAMETIME_US));
                    patientDisplay.append("\nDistance:             " + p.get(Patient.LENGTH_PARAM));

                }catch (Exception e){

                }finally {
                    handler.postDelayed(this, 100);
                }
            }
        });
    }
}
