package com.github.fontdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LabelActivity extends AppCompatActivity {
    private static final String TAG = LabelActivity.class.getSimpleName();
    private static final String IMAGE_PATH_KEY = "image_path";

    private List<Rect> segments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label);
        Bundle extras = getIntent().getExtras();

        String imagePath = extras.getString(IMAGE_PATH_KEY);
        final Bitmap image = BitmapFactory.decodeFile(imagePath);

        ImageView imageView = (ImageView) findViewById(R.id.imageView2);
        imageView.setImageBitmap(image);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return segments != null;
            }
        });

        Button segment = (Button) findViewById(R.id.buttonBrowse);
        segment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityManager connMgr = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    new FetchSegmentsTask().execute(image);
                } else {
                    // TODO: notify user through TextView
                    Log.i(TAG, "No network connection available");
                }
            }
        });
    }

    private class FetchSegmentsTask extends AsyncTask<Bitmap, Void, List<Rect>> {
        private static final String BACKEND_ENDPOINT = "localhost";
        private static final int CONNECT_TIMEOUT = 5000;
        private static final int READ_TIMEOUT = 5000;

        private Bitmap image;

        @Override
        protected List<Rect> doInBackground(Bitmap... params) {
            image = params[0];

            URL segmentUrl = null;
            try {
                segmentUrl = new URL(BACKEND_ENDPOINT);
            } catch (MalformedURLException e) {
                Log.e(TAG, "Unable to parse backend url", e);
            }

            List<Rect> rects = new LinkedList<>();
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) segmentUrl.openConnection();
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(image.getByteCount());

                // Send the compressed image to the backend for segmentation
                try (OutputStream outputStream = connection.getOutputStream()) {
                    image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    String responseMessage = connection.getResponseMessage();
                    Log.e(TAG, "Could not upload image. Response message: " + responseMessage);
                    return rects;
                }

                try (InputStream inputStream = connection.getInputStream()) {
                    rects = readRectsFromStream(inputStream);
                }
            } catch (IOException e) {
                Log.e(TAG, "Unable to fetch segments", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return rects;
        }

        @Override
        protected void onPostExecute(List<Rect> rects) {
            if (rects.isEmpty()) {
                return;
            }

            Bitmap segmentedImage = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(segmentedImage);
            canvas.drawBitmap(image, 0, 0, null);

            Paint drawPaint = new Paint();
            drawPaint.setStyle(Paint.Style.STROKE);
            drawPaint.setStrokeWidth(5);
            drawPaint.setColor(Color.RED);
            for (Rect rect: rects) {
                canvas.drawRect(rect, drawPaint);
            }

            // Update image view to use the segmented image
            ImageView imageView = (ImageView) findViewById(R.id.imageView2);
            imageView.setImageDrawable(new BitmapDrawable(getResources(), segmentedImage));
            segments = rects;
        }

        private List<Rect> readRectsFromStream(InputStream inputStream) throws IOException {
            List<Rect> rects = new LinkedList<>();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                Rect rect = readRectFromLine(line);
                if (rect != null) {
                    rects.add(rect);
                }
            }

            return rects;
        }

        private Rect readRectFromLine(String line) {
            String[] sidesAsStr = line.split(",");
            if (sidesAsStr.length != 4) {
                return null;
            }

            int[] sides = new int[sidesAsStr.length];
            for (int i = 0; i < sidesAsStr.length; i++) {
                try {
                    sides[i] = Integer.parseInt(sidesAsStr[i]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            return new Rect(sides[0], sides[1], sides[2], sides[3]);
        }
    }
}
