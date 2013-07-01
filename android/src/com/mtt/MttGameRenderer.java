package com.mtt;

import java.util.*;
import java.nio.*;

import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.*;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class MttGameRenderer implements GLSurfaceView.Renderer
{
    private static final String TAG = "MyGLRenderer";

    private final String vertexShaderCode =
        "attribute vec4 vPosition;" +
        "void main() {" +
        "  gl_Position = vPosition;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}";

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

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

    ArrayList<BoardView> boards = new ArrayList();

    public MttGameRenderer(MttGame game)
    {
        this.game = game;

    }


    class BoardView
    {
        // RGBA format (either 130 across the board or 0)
        private final String xTex =
            "XXXX\0\0\0\0\0\0\0\0\0\0\0\0XXXX" +
            "\0\0\0\0XXXX\0\0\0\0XXXX\0\0\0\0" +
            "\0\0\0\0\0\0\0\0XXXX\0\0\0\0\0\0\0\0" +
            "\0\0\0\0XXXX\0\0\0\0XXXX\0\0\0\0"+
            "XXXX\0\0\0\0\0\0\0\0\0\0\0\0XXXX";

        private final String oTex =
            "\0\0\0\0\0\0\0\0XXXX\0\0\0\0\0\0\0\0" +
            "\0\0\0\0XXXX\0\0\0\0XXXX\0\0\0\0" +
            "XXXX\0\0\0\0\0\0\0\0\0\0\0\0XXXX" +
            "\0\0\0\0XXXX\0\0\0\0XXXX\0\0\0\0" +
            "\0\0\0\0\0\0\0\0XXXX\0\0\0\0\0\0\0\0";

        private final String zTex =
            "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0" +
            "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0" +
            "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0" +
            "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0" +
            "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0";

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
            "uniform mat4 PM;"+
            "attribute vec4 position;"+
            "attribute vec2 texIn;"+
            "varying vec2 texOut;"+
            "void main()"+
            "{"+
            "    gl_Position = PM*position;"+
            "    texOut = texIn;"+
            "}";

        private final String texFrag =
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

        int xTexID;
        int linesProgramID, texProgramID;
        FloatBuffer vertexBuffer;

        FloatBuffer fbPM;

        public void print(float v[])
        {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    System.out.printf("%14f ", v[i*4 + j]);
                }
                System.out.printf("\n");
            }
        }

        final int sizeoffloat = 4;

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

            xTexID = makeTexture(5, 5, GLES20.GL_RGBA, xTex);
        }

        int makeTexture(int width, int height, int format, String values)
        {
            byte rawbytes[] = values.getBytes(); // no trailing \0
            ByteBuffer bb = ByteBuffer.allocateDirect(rawbytes.length);

            int texid_arr[] = {0};
            GLES20.glGenTextures(1, texid_arr, 0);
            int texid = texid_arr[0];

            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, bb);

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
            return programID;
        }

        void draw()
        {
            drawBoard();
            drawXOs();
        }

        void drawXOs()
        {

        }

        void drawBoard()
        {
            GLES20.glUseProgram(linesProgramID);

            int vertexCount = pts.length/2;
            int vertexStride = 2 * 4; // bytes per vertex

            int posAttr = GLES20.glGetAttribLocation(linesProgramID, "position");
            System.out.println("posAttr "+ posAttr);

            GLES20.glEnableVertexAttribArray(posAttr);
            GLES20.glVertexAttribPointer(posAttr, 2, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);



            int pmUnif = GLES20.glGetUniformLocation(linesProgramID, "PM");
            GLES20.glUniformMatrix4fv(pmUnif, 1, false, PM, 0);
            //checkGlError("glUniformMatrix4fv");

            int colorUnif = GLES20.glGetUniformLocation(linesProgramID, "color");
            GLES20.glUniform4fv(colorUnif, 1, color, 0);

            GLES20.glLineWidth(linewd);

            System.out.println("vertexCount "+ vertexCount);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);

            GLES20.glDisableVertexAttribArray(posAttr);
        }


    }


    void drawTriangle()
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(3*3*4);
        bb.order(ByteOrder.nativeOrder());

        FloatBuffer vertexBuffer = bb.asFloatBuffer();

        // number of coordinates per vertex in this array
        int COORDS_PER_VERTEX = 3;
        float triangleCoords[] = { // in counterclockwise order:
            0.0f,  0.622008459f, 0.0f,   // top
            -0.5f, -0.311004243f, 0.0f,   // bottom left
            0.5f, -0.311004243f, 0.0f    // bottom right
        };

        // Set color with red, green, blue and alpha (opacity) values
        float color[] = { 0.5f, 0.3f, 0.1f, 1.0f };

        vertexBuffer.put(triangleCoords);
        vertexBuffer.position(0);

        int vertID = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragID = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        int programID = GLES20.glCreateProgram();
        GLES20.glAttachShader(programID, vertID);
        GLES20.glAttachShader(programID, fragID);
        GLES20.glLinkProgram(programID);


        GLES20.glUseProgram(programID);

        int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
        int vertexStride = COORDS_PER_VERTEX * 4; // bytes per vertex

        int posAttr = GLES20.glGetAttribLocation(programID, "vPosition");
        GLES20.glEnableVertexAttribArray(posAttr);
        GLES20.glVertexAttribPointer(posAttr, 3, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        int colorUnif = GLES20.glGetUniformLocation(programID, "vColor");
        GLES20.glUniform4fv(colorUnif, 1, color, 0);

        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(posAttr);

    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);


        boards.add(new BoardView(-1,-1,.66666f, 10, new float[]{.6f,.6f,.6f,1.0f}));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                float pad = .8f;
                float scale  = (.666f / 3) * pad;
                float offX = -1 + i * .6666f  + .1f*.6666f;
                float offY = -1 + j * .6666f  + .1f*.6666f;
                boards.add(new BoardView(offX, offY, scale, 3, new float[]{1.0f,1.0f,1.0f,1.0f}));
            }
    }

    @Override
    public void onDrawFrame(GL10 unused) {

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        for (BoardView bv : boards)
            bv.draw();


        System.out.println("onDrawFrame()" + boards.size());

    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);
    }
}