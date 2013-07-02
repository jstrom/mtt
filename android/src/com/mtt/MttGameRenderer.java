package com.mtt;

import java.util.*;
import java.nio.*;

import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.*;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.*;

public class MttGameRenderer implements GLSurfaceView.Renderer
{
    private static final String TAG = "MyGLRenderer";

    static final int sizeoffloat = 4;
    static final int sizeofshort = 2;

    int width, height; // viewport dims

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Get the link status.
        final int[] linkStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, linkStatus, 0);

        String error = GLES20.glGetShaderInfoLog(shader);
        // If the link failed, delete the program.
        // if (error.size() != 0)
        if (linkStatus[0] != GLES20.GL_TRUE)
        {
            Log.e(TAG, "Error compiling shader: " + error);
        }

        return shader;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    MttGame game;
    GLSurfaceView view;

    ArrayList<BoardView> boards = new ArrayList();

    public MttGameRenderer(MttGame game, GLSurfaceView view)
    {
        this.game = game;
        this.view = view;
    }



    static ByteBuffer wrapBuffer(float data[])
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length*sizeoffloat);
        bb.order(ByteOrder.nativeOrder());

        FloatBuffer fb = bb.asFloatBuffer();

        fb.put(data);
        fb.position(0);
        return bb;
    }

    static ByteBuffer wrapBuffer(short data[])
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length*sizeofshort);
        bb.order(ByteOrder.nativeOrder());

        ShortBuffer fb = bb.asShortBuffer();

        fb.put(data);
        fb.position(0);
        return bb;
    }


    class BoardView
    {
        byte[] makeX()
        {
            int width = 5; int height = 5;

            byte out[] = new byte[width*height*4];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = y*width + x;

                    if (y == x || (width -1 -x) == y) {
                        out[idx*4 + 0] = (byte) 0xff;
                        out[idx*4 + 1] = (byte) 0x00;
                        out[idx*4 + 2] = (byte) 0x00;
                        out[idx*4 + 3] = (byte) 0xff;
                    }
                }
            }

            return out;
        }

        byte[] makeO()
        {
            int width = 5; int height = 5;

            byte out[] = new byte[width*height*4];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = y*width + x;

                    int manhattan_dist = Math.abs(2 - x) + Math.abs(2 - y);
                    if (manhattan_dist == 2) {
                    // if (y == x || (width -1 -x) == y) {
                        out[idx*4 + 0] = (byte) 0x00;
                        out[idx*4 + 1] = (byte) 0x00;
                        out[idx*4 + 2] = (byte) 0xff;
                        out[idx*4 + 3] = (byte) 0xff;
                    }
                }
            }

            return out;
        }

        byte[] makeZ()
        {
            int width = 5; int height = 5;

            byte out[] = new byte[width*height*4];

            return out;
        }

        private final String singleColorVert =
            "uniform mat4 PM;"+
            "attribute vec4 position;" +
            "void main() {" +
            "  gl_Position =  position * PM;"+ // Post multiply because opengl es only allows column major matrices
            "}";

        private final String singleColorFrag =
            "precision mediump float;" +
            "uniform vec4 color;" +
            "void main() {" +
            "  gl_FragColor = color;" +
            "}";

        private final String texVert =
            "precision mediump float;"+
            "uniform mat4 PM;"+
            "attribute vec4 position;"+
            "attribute vec2 texIn;"+
            "varying vec2 texOut;"+
            "void main()"+
            "{"+
            "    gl_Position = position*PM;"+ // post mult
            "    texOut = texIn;"+
            "}";

        private final String texFrag =
            "precision mediump float;"+
            "uniform sampler2D texture;"+
            "varying vec2 texOut;"+
            "void main()"+
            "{"+
            "    gl_FragColor = texture2D(texture, texOut);"+
            "}";

        float pts[] = {0,1,3,1,
                       0,2,3,2,
                       1,0,1,3,
                       2,0,2,3};


        private final float PM[] = {1,0,0,0,
                                    0,1,0,0,
                                    0,0,1,0,
                                    0,0,0,1};

        float color[];// = {1.0f,1.0f,1.0f,1.0f};
        float linewd = 10.0f;

        int xTexID,oTexID,zTexID;
        int linesProgramID, texProgramID;
        FloatBuffer vertexBuffer;

        FloatBuffer fbPM;

        Board model;
        public void print(float v[])
        {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    System.out.printf("%14f ", v[i*4 + j]);
                }
                System.out.printf("\n");
            }
        }

        public BoardView(float offX, float offY, float scale, float linewd, float[]  color)
        {
            this.color = color;
            this.linewd = linewd;

            // make projection matrix. Row major here, hack by transposing mult in shader
            PM[0*4 + 3] = offX;
            PM[1*4 + 3] = offY;
            PM[0*4 + 0] = PM[1*4 + 1] = scale;
            {
                ByteBuffer bb = ByteBuffer.allocateDirect(PM.length*sizeoffloat);
                bb.order(ByteOrder.nativeOrder());

                fbPM = bb.asFloatBuffer();

                fbPM.put(PM);
                fbPM.position(0);
            }

            // vertex buffer for the board
            {
                ByteBuffer bb = ByteBuffer.allocateDirect(pts.length*sizeoffloat);
                bb.order(ByteOrder.nativeOrder());

                vertexBuffer = bb.asFloatBuffer();

                vertexBuffer.put(pts);
                vertexBuffer.position(0);
            }

            linesProgramID = makeProgram(singleColorVert, singleColorFrag);
            texProgramID = makeProgram(texVert, texFrag);

            if (false) {
                byte rawbytes[]= new byte[6*6*4];
                for (int x = 0; x < 6; x++)
                    for (int y = 0; y < 6; y++) {
                        int idx = y*6 + x;
                        rawbytes[idx *4 + 0] = (byte)0xff;
                        rawbytes[idx *4 + 1] = (byte)0xff;
                        rawbytes[idx *4 + 2] = (byte)0xff;
                        rawbytes[idx *4 + 3] = (byte)0xff;
                    }

                xTexID = makeTexture(5,5,GLES20.GL_RGBA, rawbytes);
            }

            if (false) {
                byte rawbytes[] = makeX();//xTex.getBytes();
                System.out.printf("xTex.getBytes().length %d\n",rawbytes.length);
                for (int x = 0; x < 5; x++) {
                    for (int y = 0; y < 5; y++) {
                        System.out.printf(" [0x%x,0x%x,0x%x,0x%x]",
                                          rawbytes[(y*5+x)*4 + 0]&0xff,
                                          rawbytes[(y*5+x)*4 + 0]&0xff,
                                          rawbytes[(y*5+x)*4 + 0]&0xff,
                                          rawbytes[(y*5+x)*4 + 0]&0xff);
                    }
                    System.out.println();
                }
            }

            xTexID = makeTexture(5, 5, GLES20.GL_RGBA, makeX());
            oTexID = makeTexture(5, 5, GLES20.GL_RGBA, makeO());
            zTexID = makeTexture(5, 5, GLES20.GL_RGBA, makeZ());
        }

        int makeTexture(int width, int height, int format, byte rawbytes[])
        {
            // byte rawbytes[] = values.getBytes(); // no trailing \0
            ByteBuffer bb = ByteBuffer.allocateDirect(rawbytes.length);
            bb.order(ByteOrder.nativeOrder());
            bb.put(rawbytes);
            bb.position(0);

            checkGlError("pre Make tex");

            //GLES20.glEnable(GLES20.GL_TEXTURE_2D);
            //checkGlError("glEnable");
            int texid_arr[] = {0};
            GLES20.glGenTextures(1, texid_arr, 0);
            int texid = texid_arr[0];
            checkGlError("glGenTextures init");

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texid);
            checkGlError("glBindTexture init");
            // Interpolate textures
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            // Clamp to edge is required to use non-power-of-2 texture sizes apparently
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, bb);
            checkGlError("glTexImage2D init");

            return texid;
        }


        int makeProgram(String vert, String frag)
        {

            int vertID = loadShader(GLES20.GL_VERTEX_SHADER, vert);
            int fragID = loadShader(GLES20.GL_FRAGMENT_SHADER, frag);
            int programID = GLES20.glCreateProgram();
            GLES20.glAttachShader(programID, vertID);
            GLES20.glAttachShader(programID, fragID);
            GLES20.glLinkProgram(programID);
            //checkGlError("glLinkProgram");

            // Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programID, GLES20.GL_LINK_STATUS, linkStatus, 0);

			// If the link failed, delete the program.
			if (linkStatus[0] == 0)
			{
				Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programID));
            }
            return programID;
        }

        void draw()
        {
            drawBoard();
            drawXOs();
        }

        void drawXOs()
        {

            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 3; i++) {
                    switch(model.whoPlayed(i,j)) {
                        case Common.TYPE_NONE:
                            drawTexture(texProgramID, zTexID, i, j); // unnecessary
                            break;
                        case Common.TYPE_X:
                            drawTexture(texProgramID, xTexID, i, j); // unnecessary
                            break;
                        case Common.TYPE_O:
                            drawTexture(texProgramID, oTexID, i, j); // unnecessary
                            break;
                    }
                }
            }
        }

        // Draw a texture inside the ith,jth box on the board
        void drawTexture(int programID, int texID, int rowIdx, int colIdx)
        {

            GLES20.glUseProgram(programID);

            int attrTexI = GLES20.glGetUniformLocation(programID, "texture");
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            checkGlError("activeTexture draw");
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texID);
            checkGlError("bindTexture");
            GLES20.glUniform1i(attrTexI, 0); // Bind the uniform to TEXTURE0
            checkGlError("textureUnif");

            int pmUnif = GLES20.glGetUniformLocation(programID, "PM");
            GLES20.glUniformMatrix4fv(pmUnif, 1, false, PM, 0);
            // System.out.println("tex PM "+ pmUnif);

            ByteBuffer fbTri = wrapBuffer(
                new short[]{0,1,2, 2,3,0});

            ByteBuffer fbPos = wrapBuffer(
                // new float[]{rowIdx + 0.0f, colIdx + 0.0f,
                //             rowIdx + 0.0f, colIdx + 1.0f,
                //             rowIdx + 1.0f, colIdx + 1.0f,
                //             rowIdx + 1.0f, colIdx + 0.0f});
                new float[]{colIdx + 0.0f, rowIdx + 0.0f,
                            colIdx + 0.0f, rowIdx + 1.0f,
                            colIdx + 1.0f, rowIdx + 1.0f,
                            colIdx + 1.0f, rowIdx + 0.0f});

            ByteBuffer fbTex = wrapBuffer(
                new float[] {0.0f,0.0f,
                             0.0f,1.0f,
                             1.0f,1.0f,
                             1.0f,0.0f}); // account for y flip?



            int posAttr = GLES20.glGetAttribLocation(programID, "position");
            GLES20.glEnableVertexAttribArray(posAttr);
            GLES20.glVertexAttribPointer(posAttr, 2, GLES20.GL_FLOAT, false, 2*sizeoffloat, fbPos);
            // System.out.println("tex Pos "+ posAttr);
            checkGlError("tex:posAttr");

            int texAttr = GLES20.glGetAttribLocation(programID, "texIn");
            GLES20.glEnableVertexAttribArray(texAttr);
            GLES20.glVertexAttribPointer(texAttr, 2, GLES20.GL_FLOAT, false, 2*sizeoffloat, fbTex);
            // System.out.println("tex Tex "+ texAttr);
            checkGlError("tex:texAttr");


            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, fbTri);
            checkGlError("tex:drawElements");

            GLES20.glDisableVertexAttribArray(posAttr);
            GLES20.glDisableVertexAttribArray(texAttr);
            checkGlError("tex:texAttr");
            checkGlError("tex:posAttr");
        }

        void drawBoard()
        {
            GLES20.glUseProgram(linesProgramID);

            int vertexCount = pts.length/2;
            int vertexStride = 2 * 4; // bytes per vertex

            int posAttr = GLES20.glGetAttribLocation(linesProgramID, "position");
            // System.out.println("board Pos "+ posAttr);

            GLES20.glEnableVertexAttribArray(posAttr);
            GLES20.glVertexAttribPointer(posAttr, 2, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

            int pmUnif = GLES20.glGetUniformLocation(linesProgramID, "PM");
            GLES20.glUniformMatrix4fv(pmUnif, 1, false, PM, 0);
            // System.out.println("board PM "+ pmUnif);

            int colorUnif = GLES20.glGetUniformLocation(linesProgramID, "color");
            GLES20.glUniform4fv(colorUnif, 1, color, 0);

            GLES20.glLineWidth(linewd);

            GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);

            GLES20.glDisableVertexAttribArray(posAttr);
        }


    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        //GLES20.glClearColor(200/255f, 0.0f, 0.0f, 70/255f);

        GLES20.glEnable(GLES20.GL_BLEND); // needed for colors alpha transparency
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        if (boards.size() == 0) {
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 3; i++) {
                    float pad = .8f;
                    float scale  = (.666f / 3) * pad;
                    float offX = -1 + i * .6666f  + .1f*.6666f;
                    float offY = -1 + j * .6666f  + .1f*.6666f;
                    boards.add(new BoardView(offX, offY, scale, 3, new float[]{1.0f,1.0f,1.0f,1.0f}));
                }
            }
            boards.add(new BoardView(-1,-1,.66666f, 10, new float[]{.6f,.6f,.6f,1.0f}));
        }

        // Doesn't seem to work unless called late/from GL thread?
        boolean res = view.isHardwareAccelerated();
        if (res)
            System.out.printf("Rend Hardware Acceleration Enabled!\n");
        else
            System.out.printf("Rend Hardware Acceleration DISABLED!\n");

    }

    @Override
    public void onDrawFrame(GL10 unused) {

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


        // update board model XXX indices are brittle
        boards.get(boards.size()-1).model = game.current;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                boards.get(i*3+j).model = game.current.board[i][j];
            }

        for (BoardView bv : boards)
            bv.draw();


        // System.out.println("onDrawFrame()" + boards.size());

    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height)
    {
        this.width = width;
        this.height = height;
        System.out.printf("onSurfaceChanged width %d height %d\n",width, height);

        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);
    }

}