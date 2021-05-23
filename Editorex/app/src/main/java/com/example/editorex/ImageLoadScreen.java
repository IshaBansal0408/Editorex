package com.example.editorex;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageLoadScreen extends AppCompatActivity {

    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int GALLERY_REQUEST_CODE = 105;
    ImageView selectedImage;
    Button cameraBtn,galleryBtn,bW_button,Sk_Button,color_button;
    String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_load_screen);

        selectedImage = findViewById(R.id.displayImageView);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        bW_button = findViewById(R.id.BlackWhiteBtn);
        Sk_Button = findViewById(R.id.PencilSketch);
        color_button = findViewById(R.id.back_to_color);

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askCameraPermissions();
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
            }
        });

        bW_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);

                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                selectedImage.setColorFilter(filter);
            }
        });

        color_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(1);

                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                selectedImage.setColorFilter(filter);
            }
        });

        Sk_Button.setOnClickListener(new View.OnClickListener() {


            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View v) {
                selectedImage.setDrawingCacheEnabled(true);

                selectedImage.destroyDrawingCache();
                selectedImage.buildDrawingCache();

                Bitmap InputBitmap = selectedImage.getDrawingCache();
                Bitmap ResultBitmap = Changetosketch(InputBitmap);
                selectedImage.setImageBitmap(ResultBitmap);
            }

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            private Bitmap Changetosketch(Bitmap bmp) {
                Bitmap Copy,Invert,Result,B_invert;
                Copy =bmp;
                Copy = toGrayscale(Copy);
                Invert = createInvertedBitmap(Copy);
                B_invert = blur(ImageLoadScreen.this,Invert);
                Result = ColorDodgeBlend(B_invert, Copy);

                return Result;
            }

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            private Bitmap blur(ImageLoadScreen ctx, Bitmap image) {
                float BITMAP_SCALE = 0.4f;
                float BLUR_RADIUS = 4.5f;
                Bitmap photo = image.copy(Bitmap.Config.ARGB_8888, true);

                try {
                    final RenderScript rs = RenderScript.create( ctx );
                    final Allocation input = Allocation.createFromBitmap(rs, photo, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
                    final Allocation output = Allocation.createTyped(rs, input.getType());
                    final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
                    script.setRadius( BLUR_RADIUS ); /* e.g. 3.f */
                    script.setInput( input );
                    script.forEach( output );
                    output.copyTo( photo );
                }catch (Exception e){
                    e.printStackTrace();
                }
                return photo;
            }

            private Bitmap ColorDodgeBlend(Bitmap source, Bitmap layer) {
                Bitmap base = source.copy(Bitmap.Config.ARGB_8888, true);
                Bitmap blend = layer.copy(Bitmap.Config.ARGB_8888, false);

                IntBuffer buffBase = IntBuffer.allocate(base.getWidth() * base.getHeight());
                base.copyPixelsToBuffer(buffBase);
                buffBase.rewind();

                IntBuffer buffBlend = IntBuffer.allocate(blend.getWidth() * blend.getHeight());
                blend.copyPixelsToBuffer(buffBlend);
                buffBlend.rewind();

                IntBuffer buffOut = IntBuffer.allocate(base.getWidth() * base.getHeight());
                buffOut.rewind();

                while (buffOut.position() < buffOut.limit()) {

                    int filterInt = buffBlend.get();
                    int srcInt = buffBase.get();

                    int redValueFilter = Color.red(filterInt);
                    int greenValueFilter = Color.green(filterInt);
                    int blueValueFilter = Color.blue(filterInt);

                    int redValueSrc = Color.red(srcInt);
                    int greenValueSrc = Color.green(srcInt);
                    int blueValueSrc = Color.blue(srcInt);

                    int redValueFinal = colordodge(redValueFilter, redValueSrc);
                    int greenValueFinal = colordodge(greenValueFilter, greenValueSrc);
                    int blueValueFinal = colordodge(blueValueFilter, blueValueSrc);


                    int pixel = Color.argb(255, redValueFinal, greenValueFinal, blueValueFinal);


                    buffOut.put(pixel);
                }

                buffOut.rewind();

                base.copyPixelsFromBuffer(buffOut);
                blend.recycle();

                return base;

            }
            private int colordodge(int in1, int in2) {
                float image = (float)in2;
                float mask = (float)in1;
                return ((int) ((image == 255) ? image:Math.min(255, (((long)mask << 8 ) / (255 - image)))));
            }

            private Bitmap createInvertedBitmap(Bitmap src) {
                ColorMatrix colorMatrix_Inverted =
                        new ColorMatrix(new float[] {
                                -1,  0,  0,  0, 255,
                                0, -1,  0,  0, 255,
                                0,  0, -1,  0, 255,
                                0,  0,  0,  1,   0});

                ColorFilter ColorFilter_Sepia = new ColorMatrixColorFilter(
                        colorMatrix_Inverted);

                Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);

                Paint paint = new Paint();

                paint.setColorFilter(ColorFilter_Sepia);
                canvas.drawBitmap(src, 0, 0, paint);

                return bitmap;

            }

            private Bitmap toGrayscale(Bitmap bmpOriginal) {

                int width, height;
                height = bmpOriginal.getHeight();
                width = bmpOriginal.getWidth();

                Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas c = new Canvas(bmpGrayscale);
                Paint paint = new Paint();
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
                paint.setColorFilter(f);
                c.drawBitmap(bmpOriginal, 0, 0, paint);
                return bmpGrayscale;
            }

        });

    }

    private void askCameraPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        }else {
            dispatchTakePictureIntent();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == CAMERA_PERM_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                dispatchTakePictureIntent();
            }else {
                Toast.makeText(this, "Camera Permission is Required to Use camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                File f = new File(currentPhotoPath);
                selectedImage.setImageURI(Uri.fromFile(f));
                Log.d("tag", "ABsolute Url of Image is " + Uri.fromFile(f));

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
            }

        }

        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "." + getFileExt(contentUri);
                Log.d("tag", "onActivityResult: Gallery Image Uri:  " + imageFileName);
                selectedImage.setImageURI(contentUri);
            }

        }
    }

    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "net.smallacademy.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }


}