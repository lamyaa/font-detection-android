package com.github.fontdetection;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_LOAD_IMAGE = 1;
    private static final int REQUEST_CAPTURE_IMAGE = 2;
    private static final String TAG = MainActivity.class.getSimpleName();

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button browse = (Button) findViewById(R.id.buttonBrowse);
        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_LOAD_IMAGE);
            }
        });

        Button camera = (Button) findViewById(R.id.buttonCamera);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File photoFile = null;
                try {
                    // Need this to get the full-sized picture
                    photoFile = createImageFile();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to create image file", e);
                }

                // Continue only if the file was successfully created
                if (photoFile != null) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    imageUri = Uri.fromFile(photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_LOAD_IMAGE:
                if (resultCode == RESULT_OK) {
                    setPictureAfterLoad(data);
                }
                break;
            case REQUEST_CAPTURE_IMAGE:
                if (resultCode == RESULT_OK) {
                    setPictureAfterCapture();
                }
                break;
            default:
                Log.e(TAG, "Invalid request code: " + requestCode);
                break;
        }
    }

    private void setPictureAfterLoad(@NonNull Intent data) {
        Uri selectedImage = data.getData();
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        try (Cursor cursor = getContentResolver().query(selectedImage, filePathColumn,
                null, null, null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);

                ImageView imageView = (ImageView) findViewById(R.id.imageView);
                imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
            }
        }
    }

    private void setPictureAfterCapture() {
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        ContentResolver contentResolver = getContentResolver();
        contentResolver.notifyChange(imageUri, null);
        try {
            Bitmap picture = MediaStore.Images.Media.getBitmap(contentResolver, imageUri);
            imageView.setImageBitmap(picture);
        } catch (IOException e) {
            Log.e(TAG, "Unable to retrieve picture: " + imageUri, e);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }
}
