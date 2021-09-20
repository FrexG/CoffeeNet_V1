package com.example.coffeenet_v1;

import com.google.android.material.animation.ImageMatrixProperty;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SegmentImage {
    private Mat img = null;
    private List<Mat> SegmentedImage = new ArrayList<>();
    private Mat leafMask = null;
    private List<Rect> rectCoordinate = new ArrayList<>();

    SegmentImage(Mat im){
        this.img = im;
        createMask();

    }
    private void createMask(){
        Mat bgr_image = this.img;
        // Filter kernel size

        Size kernel = new Size(3,3);

        Imgproc.GaussianBlur(bgr_image,bgr_image,kernel,0);
        // Convert to HSV color space

        Mat hsv_image = new Mat();

        Imgproc.cvtColor(bgr_image,hsv_image,Imgproc.COLOR_BGR2HSV);

        List<Mat> hsv_channels = new ArrayList<>(3);

        Core.split(hsv_image,hsv_channels);
        Mat h_channel = hsv_channels.get(0);

        h_channel = Mat.zeros(hsv_image.rows(),hsv_image.cols(), CvType.CV_8UC1);

        hsv_channels.set(0,h_channel);

        // Merge Channels
        Core.merge(hsv_channels,hsv_image);

        Mat mask = new Mat();

        Core.inRange(hsv_image,new Scalar(0,0,80),new Scalar(0,90,255),mask);
        Core.bitwise_not(mask,mask);
        leafMask = mask;
        removeBackground(mask,hsv_image);

    }

    private void removeBackground(Mat m,Mat hsv_image){

        Mat mask = m;
        Mat bg_removedHSV = hsv_image;
        Mat result_image = new Mat();

        Core.bitwise_and(img,img,result_image,mask);
        equalizeHistogram(result_image);
        //diseaseSegmentByColor(result_image);
    }
     private void equalizeHistogram(Mat resultImage){
        Mat hsv_image = new Mat();
        Imgproc.cvtColor(resultImage,hsv_image,Imgproc.COLOR_BGR2HSV);

        List<Mat> hsv_channels = new ArrayList<>(3);
        Core.split(hsv_image,hsv_channels);

        Mat V_Channel = hsv_channels.get(2);

        // Histogram Equalize V Channel
        Imgproc.equalizeHist(V_Channel,V_Channel);
        hsv_channels.set(2,V_Channel);

        Core.merge(hsv_channels,hsv_image);

        diseaseSegmentByColor(hsv_image);

    }
    private void diseaseSegmentByColor(Mat resultImage){
        Mat bg_removed = resultImage;
       // Mat segmentedImg = new Mat();
        //Imgproc.cvtColor(bg_removed,segmentedImg,Imgproc.COLOR_BGR2HSV);
        Mat mask = new Mat();

        Core.inRange(bg_removed,new Scalar(30,0,0),new Scalar(65,255,255),mask);
        Core.bitwise_not(mask,mask);
        Mat spots = new Mat();
        Core.bitwise_and(bg_removed,bg_removed,spots,mask);

        thresholdMask(spots);
    }
    private void thresholdMask(Mat spots){
        Mat mask = new Mat();
        Imgproc.cvtColor(spots,mask,Imgproc.COLOR_BGR2GRAY);

        Mat thresh = new Mat();

        Imgproc.threshold(mask,thresh,0,255,Imgproc.THRESH_OTSU);

        Mat kernel = Mat.ones(new Size(9,9),CvType.CV_32F);

        Imgproc.morphologyEx(thresh,thresh,Imgproc.MORPH_CLOSE,kernel);

        findContours(thresh);
    }
    private void findContours(Mat thresh){
        Mat binary = thresh;
        Mat Spots = new Mat();
        double largestArea = getleafArea();
        Imgproc.cvtColor(img,Spots, Imgproc.COLOR_BGR2RGB);
        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary,contours,hierarchy,Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);

        Rect  r;
        for(int i = 0; i< contours.size();i++){
            r = Imgproc.boundingRect(contours.get(i));
            if((r.width * r.height) >= largestArea * 0.001){
               // Imgproc.rectangle(Spots,r,new Scalar(0,255,0),5);
                Mat imageROI = new Mat(img,r);
                SegmentedImage.add(imageROI);
                rectCoordinate.add(r);

            }

        }

    }
    public List<Mat> getSegmentedImage(){
        return SegmentedImage;
    }

    public List<Rect> getRectCoordinate(){
        return rectCoordinate;
    }

    private double getleafArea(){
        // Find the Area of the largest countour
        double leaf_area;
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(leafMask,contours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

        List<Double> areas = new ArrayList<>();

        for(int i = 0;i<contours.size();i++){
            areas.add(Imgproc.contourArea(contours.get(i)));
        }
        leaf_area = Collections.max(areas);

        return leaf_area;

    }
}
