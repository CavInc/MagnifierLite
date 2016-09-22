package cav.magnifierlite;

import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MAGNIFER";

    private SurfaceView sv;
    private SurfaceHolder holder;
    private Camera camera;
    private TextView zoomText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        zoomText = (TextView) findViewById(R.id.zoom_text);

        sv = (SurfaceView) findViewById(R.id.surfaceView);

    }
}
