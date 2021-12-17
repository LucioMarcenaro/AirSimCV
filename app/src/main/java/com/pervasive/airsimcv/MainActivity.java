package com.pervasive.airsimcv;

import android.graphics.Bitmap;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Collections;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC3;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AirSimCV::MainActivity";
    private static final int IMG_WIDTH = 640;
    private static final int IMG_HEIGHT = 480;

    private boolean m_bIsConnected = false;
    private Mat m_acquiredImage = new Mat(IMG_HEIGHT, IMG_WIDTH, CV_8UC3, new Scalar(0,0,0));
    private Bitmap m_imageBitmap = null;
    static {
        System.loadLibrary("carclient");
    }
    public native boolean CarConnect(String ipaddress);
    public native boolean CarDisconnect();
    public native void GetImage(long img);

    public native void CarForward();

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
                    if (!resultingTxt.matches ("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i=0; i<splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        EditText airsimAddress = findViewById(R.id.airsim_address);
        airsimAddress.setFilters(filters);
        ShowImageWithText("Simulator not connected");
    }

    private void ShowImageWithText(String text) {
        Scalar color = new Scalar(255, 255, 255);
        int fontType = Imgproc.FONT_HERSHEY_PLAIN;
        int fontSize = 2;
        int thickness = 3;
        Size textSize = Imgproc.getTextSize(text, fontType, fontSize, thickness, null);
        Point org = new Point((IMG_WIDTH - textSize.width)/2, (IMG_HEIGHT - textSize.height)/2);

        Imgproc.putText(m_acquiredImage, text, org, fontType, fontSize, color, thickness);
        if (m_imageBitmap == null)
            m_imageBitmap = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m_acquiredImage, m_imageBitmap);
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(m_imageBitmap);
    }
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void GetNextImage() {
        if (!m_bIsConnected)
            return;

        TaskRunner runner = new TaskRunner();
        runner.executeAsync(new CustomCallable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Mat img = new Mat();
                GetImage(img.getNativeObjAddr());
                Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2RGB);
                Imgproc.resize(img, m_acquiredImage, new Size(IMG_WIDTH, IMG_HEIGHT));
                return true;
            }
            @Override
            public void postExecute(Boolean result) {
                ImageView imageView = findViewById(R.id.imageView);
                Utils.matToBitmap(m_acquiredImage, m_imageBitmap);
                imageView.setImageBitmap(m_imageBitmap);
                GetNextImage();
            }
            @Override
            public void preExecute() {
            }
        });

    }

    public void OnButtonConnect(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        if (on == false) {
            TaskRunner runner = new TaskRunner();
            runner.executeAsync(new CustomCallable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return CarDisconnect();
                }
                @Override
                public void postExecute(Boolean result) {
                    if (result) {
                        EditText airsimAddress = findViewById(R.id.airsim_address);
                        airsimAddress.setEnabled(true);
                        m_bIsConnected = false;
                        ShowImageWithText("Simulator not connected");
                        Toast.makeText(getApplicationContext(),
                                "disconnected", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "error disconnecting", Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void preExecute() {
                }
            });

        } else {
            EditText airsimAddress = findViewById(R.id.airsim_address);
            TaskRunner runner = new TaskRunner();
            runner.executeAsync(new CustomCallable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return CarConnect(airsimAddress.getText().toString());
                }
                @Override
                public void postExecute(Boolean result) {
                    if (result) {
                        EditText airsimAddress = findViewById(R.id.airsim_address);
                        airsimAddress.setEnabled(false);
                        m_bIsConnected = true;
                        ShowImageWithText("Connecting...");
                        Toast.makeText(getApplicationContext(),
                                "connected to " + airsimAddress.getText().toString(), Toast.LENGTH_LONG).show();
                        GetNextImage();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "error connecting " + airsimAddress.getText().toString(), Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void preExecute() {
                }
            });
        }
    }
}