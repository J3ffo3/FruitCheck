package com.example.fruitchecker;

import static org.opencv.core.Core.add;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProcessor;
import androidx.viewpager.widget.ViewPager;

import com.example.fruitchecker.ml.ModelTest;
import com.google.android.material.tabs.TabLayout;

import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UploadActivity extends AppCompatActivity {
    Paint paint = new Paint();
    ImageView imageView;
    Bitmap bitmap;
    List<Integer> colors = Arrays.asList(Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
            Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED);
    List<String> labels;

    ModelTest model;

    Bitmap bitmapScale;


    ViewPager viewPagerF;
    TabLayout tabLayout;
    FruitsPagerAdapter fruitsPagerAdapter;

    ImageButton goBack;
    ImageButton refreshButton;

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
        refreshButton.setOnClickListener(v -> selectNewImage());

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

        selectNewImage();

        //Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        //startActivityForResult(cameraIntent, 1);
    }

    private void selectNewImage() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                bitmapScale = Bitmap.createScaledBitmap(bitmap, 300, 300, false);
                get_predictions();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.close();
    }

    private void get_predictions() {

        if (bitmapScale == null) {
            // Añade un manejo para cuando bitmapScale sea nulo
            return;
        }

        // Creates inputs for reference.
        TensorImage image = TensorImage.fromBitmap(bitmapScale);


        // Runs model inference and gets result.

        ModelTest.Outputs outputs = model.process(image);
        float[] locations = outputs.getLocationsAsTensorBuffer().getFloatArray();
        float[] classes = outputs.getClassesAsTensorBuffer().getFloatArray();
        float[] scores = outputs.getScoresAsTensorBuffer().getFloatArray();
        float[] numberOfDetections = outputs.getNumberOfDetectionsAsTensorBuffer().getFloatArray();

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

        Bitmap mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutable);
        int h = mutable.getHeight();
        int w = mutable.getWidth();

        paint.setTextSize(h / 15f);
        paint.setStrokeWidth(h / 85f);

        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > 0.5) {
                int x = i * 4;
                paint.setColor(colors.get(i));
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(new RectF(locations[x + 1] * w, locations[x] * h, locations[x + 3] * w, locations[x + 2] * h), paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawText(labels.get((int) classes[i]) + " " + scores[i], locations[x + 1] * w, locations[x] * h, paint);
            }
        }

        imageView.setImageBitmap(mutable);
    }
}
