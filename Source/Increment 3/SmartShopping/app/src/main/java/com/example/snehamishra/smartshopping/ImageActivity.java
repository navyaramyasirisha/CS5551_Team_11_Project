package com.example.snehamishra.smartshopping;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.common.base.Objects;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ImageActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int CAMERA_INTENT=1;
    private static final int GALLERY_INTENT=100;
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCRhxtGzbA_cJMcUnLP9AoliO_TkL5bg2A";
    Bitmap bitmap;
    private Feature feature;
    private Uri imageCapturedUri;

    private TextView uName, back, skip, description;
    private Button camera, gallery, analyze, shop;
    private ImageView imageDisplay;

    private final String SHOP_PREF = "PrefFile";

    //list of furniture names for the analysis search
    String[] furnitures = {"chair","table", "sofa", "love seat",
            "fan", "bed","couch","mattress","cushions",
            "recliners", "lamp", "furniture"};

    String[] furnitureColors = {"red", "blue", "pink","black"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        //assign the widgets to instances
        assignWidgets();

        SharedPreferences prefs = getSharedPreferences(SHOP_PREF, MODE_PRIVATE);
        String userName = prefs.getString("userName", null);

        //String userName = getIntent().getStringExtra("userName");

        //welcome user message
        uName.setText("Welcome "+userName +" to the Image Activity!");

        feature = new Feature();
        feature.setType("LABEL_DETECTION");
        feature.setMaxResults(10);

        // Set on click listener to buttons
        skip.setOnClickListener(this);
        back.setOnClickListener(this);
        camera.setOnClickListener(this);
        gallery.setOnClickListener(this);
        shop.setOnClickListener(this);
        analyze.setOnClickListener(this);

    }

    //assign the widgets to instances
    private void assignWidgets() {
        uName = findViewById(R.id.userNameTextView);
        back = findViewById(R.id.imageBackTV);
        skip = findViewById(R.id.imageSkipTV);
        description = findViewById(R.id.imageAnalysisDescription);
        camera = findViewById(R.id.imageCamera_btn);
        gallery = findViewById(R.id.imageGallery_btn);
        analyze = findViewById(R.id.imageAnalyze_btn);
        shop = findViewById(R.id.imageAnaShop_btn);
        imageDisplay = findViewById(R.id.imageDisplayView);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imageBackTV:
                //user navigates to the Login page
                startActivity(new Intent(ImageActivity.this, LoginActivity.class));
                finish();
                break;
            case R.id.imageSkipTV:
                //user navigates to the Shopping page
                Intent i = new Intent(ImageActivity.this, ShopActivity.class);
                i.putExtra("Analysis","");
                startActivity(i);
               // finish();
                break;
            case R.id.imageCamera_btn:
                //user navigates to Camera to click image for analysis
                openCamera();
                break;
            case R.id.imageGallery_btn:
                //user navigates to the Gallery to select image
                openGallery();
                break;
            case R.id.imageAnalyze_btn:
                //user selects to analyze the selected image
                callGoogleVision();
                break;
            case R.id.imageAnaShop_btn:
                //user selects to analysis of the image for shopping
                //Toast.makeText(getApplicationContext(),"Clicking shopping button!!",Toast.LENGTH_SHORT).show();
                shopUsingAnalysis();
                break;

        }
    }

    //function to take pics using camera
    private void openCamera(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_INTENT);
    }

    //function to load images from gallery
    private void openGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_INTENT);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
           // Toast.makeText(getApplicationContext(),"Loading Image, kindly wait !!",Toast.LENGTH_SHORT).show();
            switch (requestCode){
                case CAMERA_INTENT:
                {
                    bitmap = (Bitmap) data.getExtras().get("data");
                    imageDisplay.setImageBitmap(bitmap);
                    imageCapturedUri=null;
                    break;
                }
                case GALLERY_INTENT:
                {
                    imageCapturedUri = data.getData();
                    imageDisplay.setImageURI(imageCapturedUri);
                    bitmap=null;
                    break;
                }
            }

        }else {
            Toast.makeText(getApplicationContext(),"Unable to load Image!!",Toast.LENGTH_SHORT).show();
        }

    }


    //user can choose the image analysis for shopping
    private void shopUsingAnalysis(){
        if(description==null || description.getText()=="" ){
            Toast.makeText(getApplicationContext(),"No Analysis available to use for shopping :(",Toast.LENGTH_LONG).show();
        }else {
            String analysis = description.getText().toString();
            //Toast.makeText(getApplicationContext(),"Sending analysis text - "+analysis,Toast.LENGTH_SHORT).show();
            Intent i = new Intent(ImageActivity.this, ShopActivity.class);
            i.putExtra("Analysis",analysis);
            startActivity(i);
           // startActivity(new Intent(ImageActivity.this, ShopActivity.class));
        }
    }

    private void callGoogleVision(){
        //if(imageDisplay == null || imageDisplay.getDrawable().equals("@android:drawable/ic_menu_gallery")){
        if((bitmap == null && imageCapturedUri==null)){
            Toast.makeText(getApplicationContext(),"No image available to analyze!! try again",Toast.LENGTH_SHORT).show();

        }else {
            //Toast.makeText(getApplicationContext(),"Yuppies!!",Toast.LENGTH_LONG).show();

            final List<Feature> featureList = new ArrayList<>();
            featureList.add(feature);
            final List<AnnotateImageRequest> annotateImageRequests = new ArrayList<>();
            AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
            annotateImageReq.setFeatures(featureList);
            if(bitmap == null && imageCapturedUri!=null){
                Toast.makeText(getApplicationContext(),"Image selected from gallery!!",Toast.LENGTH_SHORT).show();
                try {
                    Bitmap bMap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageCapturedUri);
                    annotateImageReq.setImage(getImageEncodeImage(bMap));
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(),"Cannot Convert image uri to BitMap!!"+e,Toast.LENGTH_SHORT).show();
                }
            } else if(imageCapturedUri == null && bitmap!=null){
                Toast.makeText(getApplicationContext(),"Image used by Camera!!",Toast.LENGTH_SHORT).show();
                annotateImageReq.setImage(getImageEncodeImage(bitmap));
            }
            else {
                Toast.makeText(getApplicationContext(),"No Image available for Analysis!!",Toast.LENGTH_LONG).show();
                return;
            }
            annotateImageRequests.add(annotateImageReq);

            new AsyncTask<Object, Void, String>() {
                @Override
                protected String doInBackground(Object... params) {
                    try {

                        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                        VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY);

                        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                        builder.setVisionRequestInitializer(requestInitializer);

                        Vision vision = builder.build();

                        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                        batchAnnotateImagesRequest.setRequests(annotateImageRequests);

                        Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                        annotateRequest.setDisableGZipContent(true);
                        BatchAnnotateImagesResponse response = annotateRequest.execute();
                        return convertResponseToString(response);
                    } catch (GoogleJsonResponseException e) {
                        Toast.makeText(getApplicationContext(),"failed to make API request because " + e.getContent(),Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(),"failed to make API request because of other IOException " + e.getMessage(),Toast.LENGTH_LONG).show();
                    }
                    return "Cloud Vision API request failed. Check logs for details.";
                }

                protected void onPostExecute(String result) {
                    Toast.makeText(getApplicationContext(),"Analysis complete!!",Toast.LENGTH_SHORT).show();

                    String convertedResult = convertDescriptionText(result);
                    //Toast.makeText(getApplicationContext(),"Analysis text is - "+convertedResult,Toast.LENGTH_SHORT).show();
                    if(convertedResult.isEmpty() || convertedResult!=""){
                        description.setText(convertedResult);
                    }else{
                       // description.setText("No Analysis available to display!");
                        description.setText("");
                        Toast.makeText(getApplicationContext(),"No Analysis available to display!!",Toast.LENGTH_SHORT).show();
                    }


                }
            }.execute();


        }

    }

    //gets the filtered analysis description
    private String convertDescriptionText(String result){
        String desc="";
        //Toast.makeText(getApplicationContext(), "getDescriptionText is - "+result,Toast.LENGTH_SHORT).show();
        String[] words = result.split("\\s+");
        for (int i = 0; i < words.length; i++) {

            if(Arrays.asList(furnitures).contains(words[i]) || Arrays.asList(furnitureColors).contains(words[i])){
                //set the value only if it is related to furniture
                if(!desc.contains(words[i])) {
                    desc = desc + " " + words[i];
                }
            }else{
                //do noting and ignore other words not related to furniture or colors
            }

           /* if(i==0){
                //ignoring the percentage and keeping the words only for analysis
               // desc = desc + words[i] + ", ";
                if(furnitures.equals(words[i]) || furnitureColors.equals(words[i])){
                    //set the value only if it is related to furniture
                    desc = desc + words[i] +" ";
                }else{
                    //do noting and ignore other words
                }

               // Toast.makeText(getApplicationContext(), "Inside if",Toast.LENGTH_SHORT).show();
            }else if( (i%2)==0){
                //Toast.makeText(getApplicationContext(), "Inside else if",Toast.LENGTH_SHORT).show();
                //even positioned words, do nothing, ignoring them
                //these words are the percentage of the labels detected
            }else {
                //Toast.makeText(getApplicationContext(), "Inside else",Toast.LENGTH_SHORT).show();
                //desc = desc+", "+words[i];
                if(Arrays.asList(furnitures).contains(words[i]) || Arrays.asList(furnitureColors).contains(words[i])){
                    //set the value only if it is related to furniture
                    if(!desc.contains(words[i])) {
                        desc = desc + " " + words[i];
                    }
                }else{
                    //do noting and ignore other words not related to furniture or colors
                }

            }*/
        }
        return desc;
    }

    private Image getImageEncodeImage(Bitmap bitmap) {
        Image base64EncodedImage = new Image();
        // Convert the bitmap to a JPEG
        // Just in case it's a format that Android understands but Cloud Vision
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        // Base64 encode the JPEG
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }


    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        AnnotateImageResponse imageResponses = response.getResponses().get(0);
        List<EntityAnnotation> entityAnnotations;
        // entityAnnotations = imageResponses.getLandmarkAnnotations();
        // entityAnnotations = imageResponses.getLogoAnnotations();
        //SafeSearchAnnotation annotation = imageResponses.getSafeSearchAnnotation();
        // ImageProperties imageProperties = imageResponses.getImagePropertiesAnnotation();
        entityAnnotations = imageResponses.getLabelAnnotations();
        return formatAnnotation(entityAnnotations);
    }

    private String formatAnnotation(List<EntityAnnotation> entityAnnotation) {
        String message = "";

        if (entityAnnotation != null) {
            for (EntityAnnotation entity : entityAnnotation) {
                message = message + "    " + entity.getDescription() + " " + entity.getScore();
                message += "\n";
            }
        } else {
            message = "Nothing Found";
        }
        return message;
    }
}
