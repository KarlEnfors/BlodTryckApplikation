package exjobb.bloodpressuremeasurement;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Karl Enfors on 2017-02-24.
 *
 * Class ment for handling arraylist-format data.
 * Includes a Smoothening filter. (can compromise data)
 * Delta filter can remove sudden abnormal changes.
 *
 */

public class FrameDataHandler {

    ArrayList<Double> dataSourceA;
    ArrayList<Double> dataSourceB;

    public FrameDataHandler(){


    }

    public ArrayList<Double> planeOutData(ArrayList<Double> data){
        int startPoint = 0;
        for (int i = 0; i < 30; i++){
            startPoint += data.get(i);
        }
        startPoint /= 30;

        int endPoint = 0;
        for (int i = 599; i > 569; i--){
            endPoint += data.get(i);
        }
        endPoint /= 30;

        double gradient = (endPoint - startPoint)/540.f;
        //Log.e("GRADIENT:", " " + gradient);
        //Log.e("STARTPOINT:", " " + startPoint);
        //Log.e("ENDPOINT:", " " + endPoint);



        for(int i = 0; i < data.size(); i++){
            data.set(i, data.get(i) - (gradient * i));
        }

        return data;
    }

    public double getFrameDiffFromData(ArrayList<Double> dataA, ArrayList<Double> dataB){
        ArrayList<Double> areaA = (ArrayList<Double>) dataA.clone();
        ArrayList<Double> areaB = (ArrayList<Double>) dataB.clone();

        areaA = this.filterAmp(areaA, 80);
        areaB = this.filterAmp(areaB, 80);

        this.planeOutData(areaA);
        this.planeOutData(areaB);

        areaA = this.filterSmoothing(areaA, 2, 10);
        areaA = this.deltaData(areaA);
        areaA = this.filterSmoothing(areaA, 2, 5);
        areaA = this.drawFromDelta(areaA);
        areaA = this.filterSmoothing(areaA, 2, 10);

        areaB = this.filterSmoothing(areaB, 2, 10);
        areaB = this.deltaData(areaB);
        areaB = this.filterSmoothing(areaB, 2, 5);
        areaB = this.drawFromDelta(areaB);
        areaB = this.filterSmoothing(areaB, 2, 10);

        double frameDiff = this.getAvgPeakFrameDifference(this.peakDetection(areaA), this.peakDetection(areaB));
        return frameDiff;
    }

    public void setSource(ArrayList<Double> inDataSourceA, ArrayList<Double> inDataSourceB){
        dataSourceA = inDataSourceA;
        dataSourceB = inDataSourceB;
    }

    public ArrayList<Double> deltaFilter(ArrayList<Double> indata, int filterStrength){

        ArrayList<Double> data = (ArrayList<Double>) indata.clone();


        double prevValue;
        double thisValue;

        double deltaPrev;
        double avg;

        for (int c = 0; c < filterStrength; c++){
            prevValue = indata.get(0);
            for (int i = 1; i < indata.size() -1; i++){
                thisValue = indata.get(i);

                deltaPrev = thisValue - prevValue;

                if (Math.abs(deltaPrev) > 2){ //was 20
                    //Log.e("NOICE", "REDUCED");
                    avg = prevValue + indata.get(i+1);
                    avg /= 2.d;
                    data.set(i, avg);
                }

                prevValue = thisValue;
            }
        }

        return data;
    }

    public ArrayList<Double> filterSmoothing(ArrayList<Double> indata, int smoothStrength, int smoothFieldEffect){

        ArrayList<Double> data = (ArrayList<Double>) indata.clone();

        double avg = 0;
        for (int laps = 0; laps < smoothStrength; laps++){
            for (int i = 0; i < data.size() - smoothFieldEffect; i++){
                for (int j = 0; j < smoothFieldEffect; j++){
                    avg += indata.get(i + j);
                }
                avg /= smoothFieldEffect;
                data.set(i, avg);
                avg = 0;
            }

            for (int i = data.size() -1; i > smoothFieldEffect; i--){
                for (int j = 0; j < smoothFieldEffect; j++){
                    avg += indata.get(i - j);
                }
                avg /= smoothFieldEffect;
                data.set(i, avg);
                avg = 0;
            }
        }
        return data;
    }

    public ArrayList<Double> peakIdentifier(ArrayList<Double> rawDataA, ArrayList<Double> rawDataB){
        ArrayList<Double> lowA  = (ArrayList<Double>) rawDataA.clone();
        ArrayList<Double> medA  = (ArrayList<Double>) rawDataA.clone();
        ArrayList<Double> highA = (ArrayList<Double>) rawDataA.clone();
        ArrayList<Double> lowB  = (ArrayList<Double>) rawDataB.clone();
        ArrayList<Double> medB  = (ArrayList<Double>) rawDataB.clone();
        ArrayList<Double> highB = (ArrayList<Double>) rawDataB.clone();

        //Low filtering of A area.
        lowA = this.planeOutData(lowA);
        lowA = filterAmp(lowA,40);
        lowA = deltaFilter(lowA, 5);
        lowA = filterSmoothing(lowA, 2, 2);

        //Medium filtering of A area.
        medA = this.planeOutData(medA);
        medA = filterAmp(medA,40);
        medA = deltaFilter(medA, 5);
        medA = filterSmoothing(medA, 5, 5);

        //High filtering of A area.
        highA = this.planeOutData(highA);
        highA = filterAmp(highA,40);
        highA = deltaFilter(highA, 5);
        highA = filterSmoothing(highA, 3, 20);

        //Low filtering of B area.
        lowB = this.planeOutData(lowB);
        lowB = filterAmp(lowB,40);
        lowB = deltaFilter(lowB, 5);
        lowB = filterSmoothing(lowB, 2, 2);

        //Medium filtering of B area.
        medB = this.planeOutData(medB);
        medB = filterAmp(medB,40);
        medB = deltaFilter(medB, 5);
        medB = filterSmoothing(medB, 5, 5);

        //High filtering of B area.
        highB = this.planeOutData(highB);
        highB = filterAmp(highB,40);
        highB = deltaFilter(highB, 5);
        highB = filterSmoothing(highB, 3, 20);

        return lowA;
    }



    public ArrayList<Double> peakDetection(ArrayList<Double> data){

        ArrayList<Double> peakData = new ArrayList<>();

        double delta;
        double oldDelta = data.get(2);
        for (int i = 2; i < data.size(); i++){
            peakData.add(0.d);
            delta = data.get(i) - data.get(i-1);
            if (oldDelta < 0 && delta > 0){
                //Log.e("PEAK @", " " + i);
                //Log.e("OLD DELTA: ", " " + oldDelta);
                //Log.e("NEW DELTA: ", " " + delta);
                peakData.set(i-2, 20.d);

                //FOR VISUAL DEBUGGING IN GRAPH, ITS NOT VERY NICE
                //data.set(i -3, data.get(i) + 20);
            }
            oldDelta = delta;
        }
        return peakData;
    }

    public ArrayList<Double> deltaData(ArrayList<Double> data){
        ArrayList<Double> deltas = new ArrayList<>();
        double old = data.get(0);


        for (Double current : data){
            deltas.add(old - current);
            old = current;
        }

        double current;
        double next;

        for (int i = 1; i < 200; i++){
            old = deltas.get(i -1);
            current = deltas.get(i);
            next = deltas.get(i+1);

            if (current < 0) {
                //Log.e("E","WHEN THE TRUTH IS FOUND");
                if ((old > 0) == (next > 0)){
                    //deltas.set(i, 0.1d);
                }

            }else {
                //if ((old < 0) == (next < 0)){
                //    deltas.set(i, -0.1d);
                //}
            }
        }

        return deltas;
    }

    public ArrayList<Double> drawFromDelta(ArrayList<Double> deltas){
        ArrayList<Double> data = new ArrayList<>();

        double value = 0;
        for (Double current : deltas){
            value+= current;
            data.add(value);
        }

        return data;
    }


    public ArrayList<Double> filterAmp(ArrayList<Double> data, double ampFactor){

        for(int i = 0; i < data.size(); i++){
            data.set(i, data.get(i) * ampFactor);
        }
        return data;
    }

    public double getAvgPeakFrameDifference(ArrayList<Double> peaksA, ArrayList<Double> peaksB){
        double result = 0;
        int matches = 0;

        for (int aTime = 30; aTime<570; aTime++){
            if (peaksA.get(aTime) > 0){
                for (int bTime = aTime; bTime < 570; bTime++){
                    if (peaksB.get(bTime) > 0){
                        if (bTime - aTime < 11 && bTime - aTime > 1){
                            result += bTime - aTime;
                            matches++;
                            Log.e("Delta Frames:", " " + (bTime - aTime));
                        }
                    }
                }
            }
        }

        if (matches >0)
            result /= matches;
        Log.e("Delta Frames...", "Final: "  + result);

        return result;
    }
}
