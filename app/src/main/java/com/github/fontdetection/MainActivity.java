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
import android.support.v7.app.AppCompatActivity;
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
    private Uri imageUri; //TODO: this is ugly. Find a way to pass it even if intent becomes null

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
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                File photoFile = null;
                try {
                    //we need this to get the full-sized picture
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    //TODO: We should add some logging
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    imageUri = Uri.fromFile(photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,
                            imageUri);
                    startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_LOAD_IMAGE: setPictureAfterLoad(data);
                break;
            case REQUEST_CAPTURE_IMAGE: setPictureAfterCapture(data);
                break;
        }
    }

    private void setPictureAfterLoad(Intent data) {
        if (data == null)
            return;
        Uri selectedImage = data.getData();
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        String picturePath = null;
        try (Cursor cursor = getContentResolver().query(selectedImage, filePathColumn,
                null, null, null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                picturePath = cursor.getString(columnIndex);
            }
        }

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
    }

    private void setPictureAfterCapture(Intent data) {
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        ContentResolver cr = getContentResolver();
        Uri pictureUri = imageUri;
        cr.notifyChange(pictureUri, null);
        Bitmap picture;
        try {
            picture = android.provider.MediaStore.Images.Media.getBitmap(cr, pictureUri);
            imageView.setImageBitmap(picture);
        } catch (Exception e) {
            //TODO: logging
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
