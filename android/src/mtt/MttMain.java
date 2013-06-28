package mtt;

import android.view.*;
import android.app.*;
import android.os.Bundle;
import android.content.pm.*; // Check EGL version
import android.content.*;

public class MttMain extends Activity
{
    /** Called when the activity is first created. */

    class MySurfaceView extends GLSurfaceView
    {
        public MySurfaceView(Context context) {
            super(context);

            // Create an OpenGL ES 2.0 context.
            setEGLContextClientVersion(2);

            setRenderer(new MttGameRenderer());

            // Render the view only when there is a change in the drawing data
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(new DrawDemoSurface(this));


        enforceVersion();
    }


    void enforceVersion()
    {
        // Check version
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (!supportsEs2) //XXX
            System.exit(0);

    }

}
