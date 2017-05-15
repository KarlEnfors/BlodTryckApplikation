package exjobb.bloodpressuremeasurement;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by Karl Enfors on 2017-04-25.
 */

public class Patient {

    ArrayList<Double> patientData;
    ArrayList<Double> debugDataA;
    ArrayList<Double> debugDataB;


    private String patientName;
    public static final int PULSE_WAVE_VELOCITY = 0;
    public static final int BP_SYS_CALIBRATED = 1;
    public static final int BP_DIA_CALIBRATED = 2;
    public static final int BP_SYS_COEF = 3;
    public static final int BP_DIA_COEF = 4;
    public static final int LENGTH_PARAM = 5;
    public static final int PEAK_FRAME_DIFFERENTIAL = 6;
    public static final int FRAMETIME_US = 8333;


    public Patient(String name){
        this.patientData = new ArrayList<>(6);
        this.patientName = name;
    }

    public ArrayList<Double> getData(){
        return patientData;
    }
    public String getName(){
        return patientName;
    }

    public void setCaliBP(double BPsys, double BPdia, double length_m){

        this.patientData = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            patientData.add(-1.d);

        this.patientData.set(BP_SYS_CALIBRATED, BPsys);
        this.patientData.set(BP_DIA_CALIBRATED, BPdia);
        this.patientData.set(LENGTH_PARAM, length_m);

    }

    public void setCaliCoefficients(double frameDiff, boolean isSystolic){
        double coef = -1;
        double PTTD = (frameDiff * 8333) / 1000000;
        Log.e("PTTD", "" + PTTD);
        double bp;
        if (isSystolic){
            bp = patientData.get(BP_SYS_CALIBRATED);
        }
        else {
            bp = patientData.get(BP_DIA_CALIBRATED);
        }

        double tmp;
        for (double b = 0.02; b <= 0.15; b+= 0.0002){
            tmp = pressureEquation(b, PTTD);
            if  (tmp < bp && tmp > bp - 0.8){
                coef = b;
                Log.e("Match", "Systolic:" + isSystolic + " coef: " + coef);
                break;
            }
        }

        if (isSystolic){
            patientData.set(BP_SYS_COEF, coef);
            Log.e("Coef sys"," " + coef);
        }
        else {
            patientData.set(BP_DIA_COEF, coef);
            Log.e("Coef dia"," " + coef);
        }
    }


    private double pressureEquation(double b, double PTTD){
        double result = 0;
        double density = 1061; // density for blood
        double l = patientData.get(LENGTH_PARAM);

        double temp = (l * l * b * density);
        result = (1/b);

        //Log.e("Temp1: "," " + temp);
        temp /= (PTTD*PTTD);
        //Log.e("Temp2: "," " + temp);
        temp--;
        double log = Math.log(temp);
        //Log.e("Temp3: "," " + temp);

        //Log.e("Logaritm: "," " + log);

        return result * log;
    }


    public double getTestResult(double measurementFrameDiff, boolean isSystolic){
        double BPresult = 0;
        double PTTD = (measurementFrameDiff * 8333) / 1000000;

        if (isSystolic){
            BPresult = pressureEquation(patientData.get(BP_SYS_COEF), PTTD);
        }else {
            BPresult = pressureEquation(patientData.get(BP_DIA_COEF), PTTD);
        }
        return BPresult;
    }



    public void savePatient(Context c){
        String fileName = this.patientName;
        try{

            FileOutputStream FOS = c.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream OOS = new ObjectOutputStream(FOS);
            OOS.writeObject(this.patientData);
            FOS.close();
            OOS.close();

            //Log.e("Save..."," Did what i did");

        }catch (Exception e){

        }finally {

        }
    }

    public void loadPatient(Context c, String fileName){
        try{
            FileInputStream FIS = c.openFileInput(fileName);
            ObjectInputStream OIS = new ObjectInputStream(FIS);
            this.patientData = (ArrayList<Double>) OIS.readObject();
            FIS.close();
            OIS.close();

            //Log.e("Load..."," Did what i did");

        }catch (Exception e){

            Log.e("LOAD ERROR","" + e.toString());

        }finally {

        }
    }
}