package com.example.fruitchecker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.example.fruitchecker.ml.ModelTest;
import com.google.android.material.tabs.TabLayout;

import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TakePictureActivity extends AppCompatActivity {

    ViewPager viewPagerF;
    TabLayout tabLayout;
    FruitsPagerAdapter fruitsPagerAdapter;

    ImageButton goBack;

    ModelTest model;
    Paint paint = new Paint();
    List<Integer> colors = Arrays.asList(Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
            Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED);
    List<String> labels;

    Bitmap bitmap;
    Bitmap bitmapScale;
    ImageView imageView;
    ImageButton refreshButton;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.take_picture);

        viewPagerF = findViewById(R.id.view_pagerFruits);
        tabLayout = findViewById(R.id.tabsFruits);
        imageView = findViewById(R.id.imageView);

        viewPagerF.setAdapter(fruitsPagerAdapter);
        tabLayout.setupWithViewPager(viewPagerF, true);

        refreshButton = findViewById(R.id.imageButtonRefresh);
        goBack = findViewById(R.id.imageButton);
        goBack.setOnClickListener(v -> finish());
        refreshButton.setOnClickListener(v -> takeNewImage());

        try {
            labels = FileUtil.loadLabels(this, "labels.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            model = ModelTest.newInstance(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);
        Log.d("labels", labels.toString());

        takeNewImage();
        //viewPagerF.setAdapter(fruitsPagerAdapter);
        //tabLayout.setupWithViewPager(viewPagerF, true);

    }

    private void takeNewImage() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, 3);
        } else {
            //Request camera permission if we don't have it.
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            bitmap = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
            imageView.setImageBitmap(bitmap);

            bitmapScale = Bitmap.createScaledBitmap(bitmap, 300, 300, false);
            getPredictionsAndDisplay();

        }
    }

    private void getPredictionsAndDisplay() {
        if (bitmapScale == null || model == null) {
            return;
        }

        // Prepare input for the model
        TensorImage image = TensorImage.fromBitmap(bitmapScale);

        // Run inference on the model
        ModelTest.Outputs outputs = model.process(image);
        float[] locations = outputs.getLocationsAsTensorBuffer().getFloatArray();
        float[] classes = outputs.getClassesAsTensorBuffer().getFloatArray();
        float[] scores = outputs.getScoresAsTensorBuffer().getFloatArray();

        // Prepare list of detected fruits
        List<String> fruitsDetected = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > 0.5) {
                fruitsDetected.add(labels.get((int) classes[i]));
            }
        }

        // Update ViewPager with detected fruits
        fruitsPagerAdapter = new FruitsPagerAdapter(this, fruitsDetected);
        viewPagerF.setAdapter(fruitsPagerAdapter);
        tabLayout.setupWithViewPager(viewPagerF, true);

        // Display detected objects on ImageView
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        int h = mutableBitmap.getHeight();
        int w = mutableBitmap.getWidth();

        paint.setTextSize(h / 15f);
        paint.setStrokeWidth(h / 85f);

        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > 0.5) {
                int x = i * 4;
                paint.setColor(colors.get(i % colors.size()));
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(new RectF(locations[x + 1] * w, locations[x] * h, locations[x + 3] * w, locations[x + 2] * h), paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawText(labels.get((int) classes[i]) + " " + scores[i], locations[x + 1] * w, locations[x] * h, paint);
            }
        }

        imageView.setImageBitmap(mutableBitmap);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (model != null) {
            model.close();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takeNewImage();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                // Manejar aquí el caso cuando el permiso de cámara es denegado
            }
        }
    }
}