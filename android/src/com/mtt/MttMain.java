package com.mtt;

import android.opengl.*;
import android.content.*;

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

        MttGameRenderer renderer;

        public MySurfaceView(Context context, MttGame game) {
            super(context);
            renderer = new MttGameRenderer(game);
            // Create an OpenGL ES 2.0 context.
            setEGLContextClientVersion(2);

            setRenderer(renderer);

            // Render the view only when there is a change in the drawing data
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        }
    }


    MttGame game;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        game = new MttGame();

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(new MySurfaceView(this, game));


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
