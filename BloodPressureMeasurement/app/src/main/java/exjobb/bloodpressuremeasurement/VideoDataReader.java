package exjobb.bloodpressuremeasurement;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Karl Enfors January 2017
 *
 * Class for fetching picture data in chosen areas.
 * Picure data represents the G value in the pixels ARGB vector
 *
 */

public class VideoDataReader implements Runnable {

    ArrayList<Double> avaragesA;
    ArrayList<Double> avaragesB;
    Bitmap bmap;
    File video;
    ProgressBar loaderbar;
    int currentIndex;
    int part;
    MediaMetadataRetriever mediaRetriever;
    ProgressDialog loader;
    int ax;
    int ay;
    int bx;
    int by;


    public VideoDataReader(File vid, ProgressDialog loaderBar, int partOfVideo){
        avaragesA = new ArrayList<>();
        avaragesB = new ArrayList<>();
        video = vid;
        part = partOfVideo * 100;
        loader = loaderBar;
        currentIndex = 0;
        mediaRetriever = new MediaMetadataRetriever();
        mediaRetriever.setDataSource(video.getAbsolutePath());
    }

    public ArrayList<Double> getDataA(){
        return avaragesA;
    }
    public ArrayList<Double> getDataB(){
        return avaragesB;
    }


    public void setupCoordinates(int inax, int inay, int inbx, int inby){
        ax = inax;
        ay = inay;
        bx = inbx;
        by = inby;
    }

    public void setDataA(ArrayList<Double> data){
        avaragesA = data;
    }
    public void setDataB(ArrayList<Double> data){
        avaragesB = data;
    }

    private void extractValue(int index, Bitmap bitmap){
        //Log.e("DEBUG","True index:" + index + "...");

        if (bitmap == null) {
            Log.e("\t...FAILURE:","BITMAP IS NULL");
            return;
        }

        double AvgA = 0;
        double AvgB = 0;
        double noOfValues = 0;
        //A
        for (int x  = ax - 50; x < ax + 50; x++) {
            for (int y  = ay - 50; y < ay + 50; y++){
                bmap.setPixel(x, y, (bmap.getPixel(x, y) >> 8) & 0xff);
                bmap.setPixel(x, y, (bmap.getPixel(x, y) << 8));
                AvgA += Color.green(bmap.getPixel(x, y));
                noOfValues++;
            }
        }
        AvgA /= noOfValues;
        avaragesA.add(AvgA);

        noOfValues = 0;
        //B
        for (int x  = bx - 50; x < bx + 50; x++){
            for (int y  = by - 50; y < by + 50; y++){
                bmap.setPixel(x, y, (bmap.getPixel(x, y) >> 8) & 0xff);
                bmap.setPixel(x, y, (bmap.getPixel(x, y) << 8));
                AvgB += Color.green(bmap.getPixel(x, y));
                noOfValues++;
            }
        }
        AvgB /= noOfValues;
        avaragesB.add(AvgB);
    }



    @Override
    public void run() {

        long time = (currentIndex + part)*33333;
        bmap = mediaRetriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST);
        extractValue(currentIndex, bmap);
        loader.incrementProgressBy(1);
        currentIndex++;

        if (currentIndex < 100){
            this.run();
        }
        else {
            mediaRetriever.release();
        }
    }
}