package com.smarcum720.idmaker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.example.idmaker.R;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class MainActivity extends Activity
{
    private static final String TAG = "IDMaker";
    private static final int ID_WIDTH = 800;
    private static final int ID_HEIGHT = 510;
    private static final int FRONT_CAMERA = 1;
    private static final int BACK_CAMERA = 0;

    private static String _path = "";

    private Camera _camera;
    private Button _shoot;
    private Button _reset;
    private Button _share;
    private Button _shareBack;
    private SurfaceView _surfaceView;
    private SurfaceHolder _surfaceHolder;
    private SurfaceHolder.Callback _callback;
    MediaPlayer _shootMP = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Full screen and landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        setContentView(R.layout.activity_main);

        _shoot = (Button) findViewById(R.id.shoot);
        _reset = (Button) findViewById(R.id.reset);
        _share = (Button) findViewById(R.id.share);
        _shareBack = (Button) findViewById(R.id.shareBack);

        // Turn off default button sounds
        _shoot.setSoundEffectsEnabled(false);

        // Hide/Show buttons as needed
        _reset.setVisibility(View.INVISIBLE);
        _share.setVisibility(View.INVISIBLE);
        _shareBack.setVisibility(View.INVISIBLE);
        _shoot.setVisibility(View.VISIBLE);

        // Setup path
        _path = Environment.getExternalStorageDirectory().toString() + "/id.png";

        _surfaceView = (SurfaceView) findViewById(R.id.preview);
        _surfaceHolder = _surfaceView.getHolder();

        _callback = cameraCallback();
        _surfaceHolder.addCallback(_callback);
    }

    public void onResume()
    {
        super.onResume();

        // Hide/Show buttons as needed
        _reset.setVisibility(View.INVISIBLE);
        _share.setVisibility(View.INVISIBLE);
        _shareBack.setVisibility(View.INVISIBLE);
        _shoot.setVisibility(View.VISIBLE);
    }
    public void createID(View view)
    {
        Log.i(TAG, "createID");

        Camera.ShutterCallback shutter = shutterCallback();
        Camera.PictureCallback jpeg = jpegCallback();

        _camera.takePicture(shutter, null, jpeg);
    }

    public void restartCam(View view)
    {
        Log.i(TAG, "restartCam");

        _reset.setVisibility(View.INVISIBLE);
        _share.setVisibility(View.INVISIBLE);
        _shareBack.setVisibility(View.INVISIBLE);
        _shoot.setVisibility(View.VISIBLE);

        _camera.startPreview();
    }

    public void sharePic(View view)
    {
        Log.i(TAG, "sharePic");

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/png");
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + _path));
        share.putExtra(Intent.EXTRA_TEXT, "My Image");
        startActivity(Intent.createChooser(share, "Share Image"));
    }

    SurfaceHolder.Callback cameraCallback()
    {
        SurfaceHolder.Callback callback = new SurfaceHolder.Callback()
        {
            @Override
            public void surfaceDestroyed(SurfaceHolder holder)
            {
                  _camera.stopPreview();
                  _camera.release();
                  _camera = null;
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder)
            {
                _camera = Camera.open();

                try
                {
                     _camera.setPreviewDisplay(holder);
                }
                catch(Exception exception)
                {
                    _camera.release();
                    _camera = null;
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder,
                                       int format,
                                       int width,
                                       int height)
            {
                _camera.startPreview();
            }
        };

        return callback;
    }

    Camera.ShutterCallback shutterCallback()
    {
        Camera.ShutterCallback callback = new Camera.ShutterCallback()
        {
            @Override
            public void onShutter()
            {
                AudioManager meng = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
                int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

                if (volume != 0)
                {
                    if (_shootMP == null)
                        _shootMP = MediaPlayer.create(getBaseContext(), Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
                    if (_shootMP != null)
                        _shootMP.start();
                }
            }
        };

        return callback;
    }

    Camera.PictureCallback jpegCallback()
    {
        Camera.PictureCallback callback = new Camera.PictureCallback()
        {
            @Override
            public void onPictureTaken(byte[] data, Camera camera)
            {
                File imageFile = new File(_path);

                Log.i(TAG, "Saving in path: " + _path);

                Bitmap id = Bitmap.createBitmap(ID_WIDTH, ID_HEIGHT, Bitmap.Config.ARGB_8888);

                // Decode images to bitmaps
                Bitmap pic = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap template = BitmapFactory.decodeResource(getResources(), R.drawable.mclovin_template);

                // Scale bitmaps to correct size
                Bitmap picScaled = Bitmap.createScaledBitmap(pic, ID_WIDTH, ID_HEIGHT, false);
                Bitmap templateScaled = Bitmap.createScaledBitmap(template, ID_WIDTH, ID_HEIGHT, false);

                Canvas canvas = new Canvas(id);
                canvas.drawBitmap(picScaled, 0, 0, null);
                canvas.drawBitmap(templateScaled, 0, 0, null);

                try
                {
                    OutputStream fout = new FileOutputStream(imageFile);
                    id.compress(CompressFormat.PNG, 95, fout);
                    fout.flush();
                    fout.close();

                    _shoot.setVisibility(View.INVISIBLE);
                    _reset.setVisibility(View.VISIBLE);
                    _share.setVisibility(View.VISIBLE);
                    _shareBack.setVisibility(View.VISIBLE);
                }
                catch(FileNotFoundException e)
                {
                    Log.e(TAG, "FileNotFoundExceptionError: " + e.toString());
                }
                catch(IOException e)
                {
                    Log.e(TAG, "IOExceptionError: " + e.toString());
                }
            }
        };

        return callback;
    }
}
