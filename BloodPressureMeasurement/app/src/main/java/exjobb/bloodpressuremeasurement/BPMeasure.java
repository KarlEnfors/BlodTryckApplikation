package exjobb.bloodpressuremeasurement;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Karl Enfors on 2017-04-19.
 *
 *
 */

public class BPMeasure{

    private double coefSystolic;
    private double coefDiastolic;

    private double BPcalibratedSys;
    private double BPcalibratedDia;

    public BPMeasure(){



    }



    public void calibrate(double BPsys, double BPdia, int frameDiff, String patientName){

        coefSystolic  = getCoefficients(BPsys, getVelocityFromFrameDiff(frameDiff, 33333));
        coefDiastolic = getCoefficients(BPdia, getVelocityFromFrameDiff(frameDiff, 33333));

        savePatientCalibration(patientName);
    }
    public void setCalibrationParam(double coefSys, double coefDia){
        this.coefDiastolic = coefDia;
        this.coefSystolic = coefSys;
    }

    private double getCoefficients(double BP, double pulseWaveVelocity){
        return (BP / pulseWaveVelocity);
    }

    private double getVelocityFromFrameDiff(int frameDiff, double frameTime_us){
        return frameDiff * frameTime_us;
    }

    private boolean savePatientCalibration(String name){

        String filename = name + new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());
        Log.e("Kalibrering fil: "," " + filename);

        return false;
    }

    public String getBloodPressureAsString(int frameDiff){
        if (coefSystolic <= -1 || coefDiastolic <= -1){
            //Toast.makeText(, "Calibrate First Please...", Toast.LENGTH_SHORT).show();
            return "ERROR";
        }


    return "" + (getVelocityFromFrameDiff(frameDiff, 33333) * coefSystolic) + (getVelocityFromFrameDiff(frameDiff, 33333) * coefDiastolic);

    }
    private boolean canMeasure(){

        return false;
    }

}
