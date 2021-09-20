package com.example.coffeenet_v1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ClassifierActivity extends AppCompatActivity {

    private String[] labels;
    private Mat inputImage;
    private  Bitmap bmp;
    private List<Rect> coordinates;
    private HashMap<String,Scalar> symptomToColorMap = new HashMap<>();
    public Object res;
    private int ACTION_KEY;
    private String PHOTO_URI;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifier);
        labels = new String[]{"CERCOSPORA", "HEALTH", "MINER", "PHOMA", "RUST"};

        symptomToColorMap.put("CERCOSPORA",new Scalar(255,0,0)); // RED
        symptomToColorMap.put("MINER",new Scalar(0,255,0)); // GREEN
        symptomToColorMap.put("PHOMA",new Scalar(0,0,255)); // BLUE
        symptomToColorMap.put("RUST",new Scalar(255,255,255)); // WHITE

        OpenCVLoader.initDebug();


        ACTION_KEY = getIntent().getIntExtra("ACTION_KEY", 0);
        PHOTO_URI = getIntent().getStringExtra("CAPTURED_IMAGE_PATH");
        Log.d("Photo URI: ", PHOTO_URI);
        Log.d("Action Key: ", String.valueOf(ACTION_KEY));

        Uri filePath = Uri.parse(PHOTO_URI);
        if (ACTION_KEY == 0) {
            try {
                FileInputStream is = new FileInputStream(this.getContentResolver().openFileDescriptor(filePath, "r").getFileDescriptor());
                bmp = BitmapFactory.decodeStream(is);
                is.close();
            } catch (IOException e) {
                Log.getStackTraceString(e);
            }

        }
        if(ACTION_KEY == 1){
            Log.d("ACTION_KEY","if loop");
          //  ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),Uri.parse(PHOTO_URI));
            try {
                bmp = BitmapFactory.decodeFile(PHOTO_URI);
                //bmp = ImageDecoder.decodeBitmap(source);

            } catch (Exception e) {
                Log.getStackTraceString(e);
            }
        }
        Log.d("bmp Height: ",String.valueOf(bmp.getHeight()));
        ImageView imageView = (ImageView) findViewById(R.id.leafView);

        imageView.setImageBitmap(bmp);
    }

    public void onbuttonClassify(View view){
        view.setEnabled(false);
        Toast.makeText(this,"Processing ...",Toast.LENGTH_SHORT).show();
        inputImage = new Mat();
        Utils.bitmapToMat(bmp,inputImage);
        Log.i("Mat shape",String.valueOf(inputImage.size()));
        Imgproc.cvtColor(inputImage,inputImage,Imgproc.COLOR_RGB2BGR);
        // get the segmented image
        SegmentImage segmentImage = new SegmentImage(inputImage);
        List<Mat> regionOfInterest = segmentImage.getSegmentedImage();
        coordinates = segmentImage.getRectCoordinate();
        Imgproc.cvtColor(inputImage,inputImage,Imgproc.COLOR_BGR2RGB);
        // Get Classification probablities
        Classifier classifier = new Classifier(this,regionOfInterest);
        List<float[][]> outputProbablity = classifier.getListOfOutputProbablity();
        // Draw bounding box
       drawRectangel(outputProbablity);

    }
    public void drawRectangel(List<float[][]> probalities){
        int loopCount = 0;
        List<Float>listOfMaxProb = new ArrayList<>(5);

        for(float[][] prob : probalities){
            for(int i = 0; i< 5;i++){
                listOfMaxProb.add(prob[0][i]);
              //  System.out.println("Prob: "+prob[0][i]);

            }

            float maxProb = Collections.max(listOfMaxProb);
            //System.out.println("MaxProb: "+maxProb);

            int indexOfMaxProb = listOfMaxProb.indexOf(maxProb);
            listOfMaxProb.clear();
            System.out.println("indexMax: "+indexOfMaxProb);
            // Draw rect
            if(indexOfMaxProb != 1) {
                Rect r = coordinates.get(loopCount);
                Imgproc.rectangle(inputImage, r, symptomToColorMap.get(labels[indexOfMaxProb]), 5);
                Imgproc.putText(inputImage, labels[indexOfMaxProb], new Point(r.x, r.y - 10), Imgproc.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(0, 255, 0), 3);
            }
            loopCount ++;
        }

        // Display the image into the ImageView object
        Bitmap img_bitmap = Bitmap.createBitmap(inputImage.cols(),inputImage.rows(),Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(inputImage,img_bitmap);

        // Get Image View Object
        ImageView imageView = (ImageView) findViewById(R.id.leafView);

        imageView.setImageBitmap(img_bitmap);

    }
}