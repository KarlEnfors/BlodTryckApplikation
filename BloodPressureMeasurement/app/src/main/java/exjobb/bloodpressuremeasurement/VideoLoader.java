package exjobb.bloodpressuremeasurement;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Karl Enfors in January 2017.
 *
 */

public class VideoLoader extends Dialog{

    File lastFileFound;
    File videoFile;
    Context context;
    Runnable callback;

    VideoDataReader[] videoReader;
    ArrayList<Double> dataSourceA;
    ArrayList<Double> dataSourceB;



    public VideoLoader(@NonNull Context c){
        super(c);
        lastFileFound = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        context = c;
        this.setCancelable(false);

    }

    public void loadFiles(Runnable r){

        this.setTitle("Choose file...");
        this.show();
        loadVideo();

        callback = r;

    }
    private void loadVideo(){
        lastFileFound = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.toString().contains("mp4") || pathname.isDirectory()){
                    return true;
                }else
                    return false;
            }
        };

        listFiles(lastFileFound, filter);


    }


    private void setVideoReadingCoordinates(){

        final ArrayList<Integer> coords = new ArrayList<>();
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        final Bitmap originalBM;


        TextView title = new TextView(context);
        title.setTextSize(24.f);
        title.setText("Choose coordinates:");
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        retr.setDataSource(videoFile.getAbsolutePath());
        originalBM = retr.getFrameAtTime(5 * 33333, MediaMetadataRetriever.OPTION_CLOSEST);
        final Paint pen = new Paint();
        pen.setStrokeWidth(8.f);
        pen.setColor(Color.GREEN);
        Button confirmB = new Button(context);
        confirmB.setText("Ok");
        final TextView text = new TextView(context);
        text.setTextSize(18.f);

        final ImageView image = new ImageView(context);
        image.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (coords.size()>=4){
                    coords.clear();
                }

                Bitmap bm = originalBM.copy(Bitmap.Config.ARGB_8888, true);

                Canvas canvas = new Canvas(bm);
                float x = event.getX() * 1.2f;
                float y = event.getY();

                coords.add((int) x);
                coords.add((int) y);

                canvas.drawLine(x - 50, y, x + 50, y, pen);
                canvas.drawLine(x, y - 50, x, y + 50, pen);

                if (coords.size() == 4){
                    String s = " AX:" + coords.get(0);
                    s += " AY:" + coords.get(1);
                    s += " BX:" + coords.get(2);
                    s += " BY:" + coords.get(3);
                    text.setText(s);
                }
                image.setImageBitmap(bm);

                return false;
            }
        });

        image.setImageBitmap(originalBM);
        image.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        confirmB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (coords.size() < 4)
                    return;
                int AX = Integer.valueOf(coords.get(0));
                int AY = Integer.valueOf(coords.get(1));
                int BX = Integer.valueOf(coords.get(2));
                int BY = Integer.valueOf(coords.get(3));

                fetchData(AX, AY, BX, BY);
            }
        });

        container.addView(title);
        container.addView(image);
        container.addView(text);
        container.addView(confirmB);
        this.setContentView(container);
    }

    private void listFiles(final File f, final FileFilter filter){


        ArrayList<String> listStrings = new ArrayList<>();
        if (f.canRead() && f.isDirectory()){
            for ( File file : f.listFiles(filter)) {
                listStrings.add(file.getAbsolutePath());
            }
        }else {
            Log.e("CHOSEN FILE: ", "" + f.getAbsolutePath());
            lastFileFound = f;
            if (f.getAbsolutePath().endsWith(".mp4")){
                videoFile = f;
                setVideoReadingCoordinates();
            }
            return;

        }

        ArrayAdapter<String> listItems = new ArrayAdapter<String>(context, R.layout.simplelistitem, listStrings);
        ListView list = new ListView(context);
        list.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        list.setAdapter(listItems);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView current = (TextView) view;

                //(String) current.getText();

                if (f.isDirectory()) {
                    listFiles(new File((String) current.getText()), filter);
                }
            }

        });

        this.setContentView(list);
    }

    public File getVideo(){
        return videoFile;
    }
    public File getLastFile(){
        return lastFileFound;
    }


    public void fetchData(int ax, int ay, int bx, int by){

        ThreadPoolExecutor executor = new ThreadPoolExecutor(12, 12, 30, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(2));
        final ProgressDialog loader = new ProgressDialog(this.getContext());
        final Handler handler = new Handler();
        videoReader = new VideoDataReader[12];

        loader.setMax(600);
        loader.setProgress(0);
        loader.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        loader.setMessage("Extracting frame data...");
        loader.setCancelable(false);
        loader.show();

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (loader.getProgress() < 600){
                    handler.postDelayed(this, 5000);
                    return;
                }else{
                    gatherDataSummary();
                    loader.hide();
                    Toast.makeText(context, "Extraction Done", Toast.LENGTH_SHORT).show();
                }
            }
        });

        for(int i = 0; i < 6; i++){
            if(executor.getCorePoolSize() < 6){
                Log.e("DEBUG:","Less than 6 threads in pool");

            }
            videoReader[i] = new VideoDataReader(videoFile, loader, i);
            videoReader[i].setupCoordinates(ax, ay, bx, by);
            executor.execute(videoReader[i]);
        }
    }

    private void gatherDataSummary() {
        dataSourceA = new ArrayList<>();
        dataSourceB = new ArrayList<>();

        for(int n = 0; n < 6; n++){
            dataSourceA.addAll(videoReader[n].getDataA());
            dataSourceB.addAll(videoReader[n].getDataB());

            try {
                Log.e("Datavalues: ","A:" + dataSourceA.get(53*n));
                Log.e("Datavalues: ","B:" + dataSourceB.get(53*n));

            }catch (Exception e){
                Log.e("error","" +e.toString());
            }

        }
        this.dismiss();
        callback.run();
    }


    public ArrayList<Double> getDataSourceA(){return dataSourceA;}
    public ArrayList<Double> getDataSourceB(){return dataSourceB;}

}
