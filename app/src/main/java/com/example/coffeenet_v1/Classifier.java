package com.example.coffeenet_v1;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.TensorFlowLite;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Classifier {
    protected static Interpreter interpreter;
    protected static ByteBuffer inputData;
    protected static TensorBuffer inputFeature0;
    protected static MappedByteBuffer TFLiteModel;
    protected static Bitmap featureBitmapImage;
    protected static List<Mat> regionOfInterest;
    protected static TensorImage tImage;
    protected static ImageProcessor imageProcessor;
    private final Context context;
    private int IMAGE_WIDTH = 224;
    private int IMAGE_HEIGHT = 224;

    private final List<float[][]> listOfOutputProbablity = new ArrayList<>();


    Classifier(Context c,List<Mat> roi){
        this.context = c;
        this.regionOfInterest = roi;
    }
    // get model name
    private String getModel(){
        return "CoffeeNet_MobileNet_Quantized.tflite";
    }

    public List<float[][]> getListOfOutputProbablity(){
        modelInference();
        return listOfOutputProbablity;
    }

    private void modelInference(){
        List<Bitmap> roiBitmap = processImage();
        inputData = ByteBuffer.allocate(IMAGE_WIDTH*IMAGE_HEIGHT*3*4);
        inputFeature0 = TensorBuffer.createFixedSize(new int[]{1,IMAGE_WIDTH,IMAGE_HEIGHT,3}, DataType.FLOAT32);
        {
            try {
                TFLiteModel = FileUtil.loadMappedFile(context,getModel());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            interpreter = new Interpreter(TFLiteModel);
        }catch (IllegalArgumentException e){
            Log.e("L88 Illegal Argument: ","Illegal Argument Exception");
        }
        for(Bitmap bm : roiBitmap){

            float[][] outputProbabilityRoi = new float[1][5];

            imageProcessor = new ImageProcessor.Builder().add(new NormalizeOp((float) 127.5, (float) 127.5)).build();

            tImage = new TensorImage(DataType.FLOAT32);
            tImage.load(bm);
            tImage = imageProcessor.process(tImage);

            //convertBitmapToByteBuffer(bm);
            //inputFeature0.loadBuffer(inputData);
            // Run Inference on inputFeature0
            interpreter.run(tImage.getBuffer(),outputProbabilityRoi);
            listOfOutputProbablity.add(outputProbabilityRoi);

        }

    }
    private List<Bitmap> processImage(){
        List<Bitmap> bitmapList = new ArrayList<>();
        //Resize each image to a fixed size of 299*299
        for(Mat r : regionOfInterest){
            // Resize image
            Mat resized = new Mat();
            Imgproc.resize(r,resized,new Size(IMAGE_WIDTH,IMAGE_HEIGHT));

            // Create bitmap from normalized mat
            featureBitmapImage = Bitmap.createBitmap(IMAGE_WIDTH,IMAGE_HEIGHT,Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(resized,featureBitmapImage);

            // Add to bitmapList

            bitmapList.add(featureBitmapImage);
        }

        return bitmapList;
    }

}
