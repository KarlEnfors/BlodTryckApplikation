package exjobb.bloodpressuremeasurement;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by Karl Enfors on 2017-02-02.
 */

public class GraphView extends ImageView {

    Bitmap displayGraph;
    Canvas canvas;

    public GraphView(Context context) {
        super(context);
        displayGraph = null;
        canvas = null;

    }

    private int getSizeY(ArrayList<Double> data){
        return (int) (getHighest(data) - getLowest(data) +1);
    }

    private double getLowest(ArrayList<Double> data){
        double lowest = data.get(0);
        for (Double v : data){
            if (lowest > v)
                lowest = v;
        }
        return lowest;
    }

    private double getHighest(ArrayList<Double> data){
        double highest = data.get(0);
        for (Double v : data){
            if (highest < v)
                highest = v;
        }
        return highest;
    }

    public void drawGraph(ArrayList<Double> data, int color){


        //for (int i = data.size() - 20; i < data.size(); i++){
        //  data.set(i, data.get(i) -30);
        //}
        //Log.e("BM Height: ", ""+ getSizeY(data));

        displayGraph = Bitmap.createBitmap(1200, getSizeY(data)*2, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(displayGraph);
        canvas.drawColor(Color.LTGRAY);


        double lowest = getLowest(data);

        int x = 0;
        double oldValue = data.get(0);
        Paint pen = new Paint();
        int strokeWidth = (displayGraph.getHeight()/300);
        pen.setStrokeWidth(strokeWidth);
        pen.setStyle(Paint.Style.FILL_AND_STROKE);
        pen.setColor(color);

        for (Double value : data){
            canvas.drawLine(x*2, canvas.getHeight() - (float) (oldValue - lowest), x*2 +1,  canvas.getHeight() - (float)(value - lowest), pen);
            oldValue = value;
            x++;
        }
    }

    public void drawPeakGraph(ArrayList<Double> dataA, ArrayList<Double> dataB, int colorA, int colorB){


        displayGraph = Bitmap.createBitmap(1200, 400, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(displayGraph);
        canvas.drawColor(Color.LTGRAY);

        Paint penA = new Paint();
        int strokeWidth = 4;
        penA.setStrokeWidth(strokeWidth);
        penA.setStyle(Paint.Style.FILL_AND_STROKE);
        penA.setColor(colorA);
        Paint penB = new Paint();
        penB.setStrokeWidth(strokeWidth);
        penB.setStyle(Paint.Style.FILL_AND_STROKE);
        penB.setColor(colorB);


        for (int x = 30; x < 570; x++){
            canvas.drawLine(x*2, 0, x*2, dataA.get(x).floatValue() * 10, penA);
            canvas.drawLine(x*2, 0, x*2, dataB.get(x).floatValue() * 10, penB);
        }
    }


    public void show() {

        super.setImageBitmap(Bitmap.createScaledBitmap(displayGraph, 1200, 600, true));

    }
}
