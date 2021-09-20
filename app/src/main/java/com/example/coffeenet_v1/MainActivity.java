package com.example.coffeenet_v1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static final int PICK_IMAGE = 1;
    static final int REQUEST_IMAGE_CAPTURE = 0;
    String currentPhotoPath;
    String packageName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        packageName = this.getPackageName();
    }

    public void applyFilter(View view) {
        Intent intent = new Intent(this,ClassifierActivity.class);
        startActivity(intent);
    }

    public void onButtonPickImage(View view){
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent,PICK_IMAGE);
    }
    public void onButtonTakePhoto(View view) throws IOException {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Log.d("Message","onButtonTakePhoto()");
        // Ensure there's a camera activity to handle the intent
        if(true){//takePhotoIntent.resolveActivity(getPackageManager()) != null) {
            // Create a file to store the image
            File photoFile = createImageFile();
            Log.i("Abs path",photoFile.getAbsolutePath());
            // Continue only if the required File was created
            if (photoFile != null){
                Log.d("photoFile","Created!!");
                Uri photoURI = FileProvider.getUriForFile(this,packageName+".fileprovider",photoFile);
                //Uri photoURI = Uri.fromFile(photoFile);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
                startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE);
            }else{
                Log.d("photoFile","NUll, Not created!!");
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        Log.d("onActivityResult:", "CALLED");
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == 1) {
            Log.d("Camera Call", "REQUEST_PICK_IMAGE");
            if (resultCode == RESULT_OK && data != null) {
                Uri selectedImage = data.getData();
                Log.d("PATH", selectedImage.toString());

                // String stringURI = selectedImage.toString();
                Intent classifyFromLoadedImage = new Intent(this, ClassifierActivity.class);
                classifyFromLoadedImage.putExtra("ACTION_KEY",0);
                classifyFromLoadedImage.putExtra("CAPTURED_IMAGE_PATH", selectedImage.toString());

                classifyFromLoadedImage.setData(selectedImage);
                startActivity(classifyFromLoadedImage);
            }
        }else{
            Log.d("Camera Call", "REQUEST_CAPTURE_IMAGE");

                    Log.d("d:","I am here!!");
                    Intent classifyFromCaptureImage = new Intent(this,ClassifierActivity.class);
                    classifyFromCaptureImage.putExtra("ACTION_KEY",1);
                    classifyFromCaptureImage.putExtra("CAPTURED_IMAGE_PATH",currentPhotoPath);
                    startActivity(classifyFromCaptureImage);


        }

    }
    private File createImageFile() throws IOException {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss",
                        Locale.getDefault()).format(new Date());
        String imageFileName = "COFFEENET_" + timeStamp + "_";

       // File storageDir = getFilesDir();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",   /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();

        return image;
    }


}