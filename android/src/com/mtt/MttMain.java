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

        int curPlayerHack = Common.TYPE_X;
        @Override
        public boolean onTouchEvent(MotionEvent e)
        {

            System.out.printf("MotionEvent e.x %f e.y %f type %d\n",
                              e.getX(), e.getY(),e.getAction());

            // play in a space
            if (e.getAction() == e.ACTION_UP) {

                // convert to openGL coordinate frame
                float y = renderer.height - e.getY();
                float x = e.getX();

                // approximate lookup for which cell to look in:
                int boardRow = (int)(3 *y / renderer.height);
                int boardCol = (int)(3 *x / renderer.width);


                int subBoardRow = (int)(((3 *y)  % renderer.height) / (renderer.height/3));
                int subBoardCol = (int)(((3 *x)  % renderer.width)/ (renderer.width/3));

                System.out.printf("boardRow %d boardCol %d subBoardRow %d subBoardCol %d\n",
                                  boardRow, boardCol, subBoardRow, subBoardCol);

                // XX player management. Should do this in another class?
                int status = game.takeAction(boardRow,boardCol,subBoardRow,subBoardCol, curPlayerHack);
                curPlayerHack = (curPlayerHack == Common.TYPE_X ? Common.TYPE_O : Common.TYPE_X);



                game.current.checkWon();
                System.out.printf("Status: %d\n", status);
                requestRender();
            }

            return true;
        }

    }


    MttGame game;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        game = new MttGame();
        game.current = new TopBoard();

        if (true) { // load some random game state
            game.current.board[1][0].playCell(0,0, Common.TYPE_X);
            game.current.board[1][0].playCell(1,1, Common.TYPE_X);
            game.current.board[1][0].playCell(2,2, Common.TYPE_X);

            game.current.board[1][0].playCell(0,1, Common.TYPE_O);
            game.current.board[1][0].playCell(0,2, Common.TYPE_O);
            game.current.checkWon();

            System.out.println("Who won bottom left: "+game.current.board[0][0].whoWon());
            game.current.checkWon();
        }


        // Apparently it is not possible to disable the status bar
        // on tablets
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
