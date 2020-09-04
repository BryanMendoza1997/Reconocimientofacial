package com.example.reconocimiento;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;

import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

public class MainActivity extends AppCompatActivity {

    Vision vision;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(),  null);
        visionBuilder.setVisionRequestInitializer(new
                VisionRequestInitializer("AIzaSyB5MkIB5lNnQH1kC1tZ3ATeEsv7z66moKs"));
        vision = visionBuilder.build();
        Paint recpaint=new Paint();
        recpaint.setStrokeWidth(5);
        recpaint.setColor(Color.GREEN);
        recpaint.setStyle(Paint.Style.STROKE);

    }
    public Image getImageToProcess(){
        ImageView imagen = (ImageView)findViewById(R.id.imgFamilia);
        BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        bitmap = scaleBitmapDown(bitmap, 1200);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);

        byte[] imageInByte = stream.toByteArray();

        Image inputImage = new Image();
        inputImage.encodeContent(imageInByte);
        return inputImage;
    }
    public BatchAnnotateImagesRequest setBatchRequest(String TipoSolic, Image inputImage){
        Feature desiredFeature = new Feature();
        desiredFeature.setType(TipoSolic);

        AnnotateImageRequest request = new AnnotateImageRequest();
        request.setImage(inputImage);
        request.setFeatures(Arrays.asList(desiredFeature));


        BatchAnnotateImagesRequest batchRequest =  new BatchAnnotateImagesRequest();
        batchRequest.setRequests(Arrays.asList(request));
        return batchRequest;
    }
    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }
    public void ProcesarTexto(View View){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest = setBatchRequest("FACE_DETECTION",
                        getImageToProcess());
                try {

                    Vision.Images.Annotate  annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response  = annotateRequest.execute();

                    List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();

                    int numberOfFaces = faces.size();
                    String likelihoods = "";
                    for(int i=0; i<numberOfFaces; i++)
                        likelihoods += "\n Rostro " + i + "  "  +
                                faces.get(i).getJoyLikelihood();

                    final String message =   "Esta imagen tiene " + numberOfFaces + " rostros " + likelihoods;

                    ///dibujar recuadro
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    Bitmap myBitmap = BitmapFactory.decodeResource(
                            getApplicationContext().getResources(),
                            R.drawable.familiados,
                            options
                    );

                    Paint myRectPaint = new Paint();
                    myRectPaint.setStrokeWidth(8);
                    myRectPaint.setColor(Color.GREEN);
                    myRectPaint.setStyle(Paint.Style.STROKE);

                    final Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(),
                            Bitmap.Config.RGB_565);
                    Canvas tempCanvas = new Canvas(tempBitmap);
                    tempCanvas.drawBitmap(myBitmap, 0, 0, null);

                    FaceDetector faceDetector = new
                            FaceDetector.Builder(getApplicationContext())
                            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                            .setTrackingEnabled(false)
                            .build();

                    if(!faceDetector.isOperational()) {
                        new AlertDialog.Builder(getApplicationContext()).setMessage("NO EXISTEN CARAS DETECTADA!").show();
                        return;
                    }

                    Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
                    SparseArray<Face> facess = faceDetector.detect(frame);

                    for (int i = 0; i < facess.size(); i++) {
                        Face thisFace = facess.valueAt(i);
                        float x1 = thisFace.getPosition().x;
                        float y1 = thisFace.getPosition().y;
                        float x2 = x1 + thisFace.getWidth();
                        float y2 = y1 + thisFace.getHeight();
                        tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);

                    }
                    //FIN



                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.txtTexto);
                            imageDetail.setText(message.toString());
                            ImageView imagen = (ImageView)findViewById(R.id.imgFamilia);
                            imagen.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


}