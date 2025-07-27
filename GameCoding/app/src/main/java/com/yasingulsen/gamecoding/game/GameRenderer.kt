package com.yasingulsen.gamecoding.game

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GameRenderer(private val context: Context) : GLSurfaceView.Renderer {
    
    private val mMVPMatrix = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)
    private val mModelMatrix = FloatArray(16)
    
    private var mProgram: Int = 0
    private var ballPosition = floatArrayOf(0f, 0f, 0f)
    private var glSurfaceView: GLSurfaceView? = null
    private var targets: List<Pair<Float, Float>> = emptyList()
    
    // Simple shader for better performance
    private val vertexShaderCode = """
        attribute vec4 vPosition;
        attribute vec4 vColor;
        uniform mat4 uMVPMatrix;
        varying vec4 fragmentColor;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            fragmentColor = vColor;
        }
    """.trimIndent()
    
    private val fragmentShaderCode = """
        precision mediump float;
        varying vec4 fragmentColor;
        void main() {
            gl_FragColor = fragmentColor;
        }
    """.trimIndent()
    
    // Simple ball as a small cube
    private val ballVertices = floatArrayOf(
        // Front face
        -0.2f, -0.2f,  0.2f, 1.0f, 0.0f, 0.0f,
         0.2f, -0.2f,  0.2f, 1.0f, 0.0f, 0.0f,
         0.2f,  0.2f,  0.2f, 1.0f, 0.0f, 0.0f,
        -0.2f,  0.2f,  0.2f, 1.0f, 0.0f, 0.0f,
        // Back face
        -0.2f, -0.2f, -0.2f, 1.0f, 0.0f, 0.0f,
         0.2f, -0.2f, -0.2f, 1.0f, 0.0f, 0.0f,
         0.2f,  0.2f, -0.2f, 1.0f, 0.0f, 0.0f,
        -0.2f,  0.2f, -0.2f, 1.0f, 0.0f, 0.0f
    )
    
    private val ballIndices = shortArrayOf(
        // Front
        0, 1, 2, 0, 2, 3,
        // Back
        4, 6, 5, 4, 7, 6,
        // Left
        4, 0, 3, 4, 3, 7,
        // Right
        1, 5, 6, 1, 6, 2,
        // Top
        3, 2, 6, 3, 6, 7,
        // Bottom
        4, 5, 1, 4, 1, 0
    )
    
    // Target vertices (yellow cubes) - bigger for better visibility
    private val targetVertices = floatArrayOf(
        // Front face
        -0.4f, -0.4f,  0.4f, 1.0f, 1.0f, 0.0f,
         0.4f, -0.4f,  0.4f, 1.0f, 1.0f, 0.0f,
         0.4f,  0.4f,  0.4f, 1.0f, 1.0f, 0.0f,
        -0.4f,  0.4f,  0.4f, 1.0f, 1.0f, 0.0f,
        // Back face
        -0.4f, -0.4f, -0.4f, 1.0f, 1.0f, 0.0f,
         0.4f, -0.4f, -0.4f, 1.0f, 1.0f, 0.0f,
         0.4f,  0.4f, -0.4f, 1.0f, 1.0f, 0.0f,
        -0.4f,  0.4f, -0.4f, 1.0f, 1.0f, 0.0f
    )
    
    private val targetIndices = shortArrayOf(
        // Front
        0, 1, 2, 0, 2, 3,
        // Back
        4, 6, 5, 4, 7, 6,
        // Left
        4, 0, 3, 4, 3, 7,
        // Right
        1, 5, 6, 1, 6, 2,
        // Top
        3, 2, 6, 3, 6, 7,
        // Bottom
        4, 5, 1, 4, 1, 0
    )
    
    // Simple platform
    private val platformVertices = floatArrayOf(
        -6.0f, -3.0f, -6.0f, 0.2f, 0.8f, 0.2f,
         6.0f, -3.0f, -6.0f, 0.2f, 0.8f, 0.2f,
         6.0f, -3.0f,  6.0f, 0.2f, 0.8f, 0.2f,
        -6.0f, -3.0f,  6.0f, 0.2f, 0.8f, 0.2f
    )
    
    private val platformIndices = shortArrayOf(0, 1, 2, 0, 2, 3)
    
    // Pre-allocated buffers for better performance
    private lateinit var ballBuffer: java.nio.FloatBuffer
    private lateinit var ballIndexBuffer: java.nio.ShortBuffer
    private lateinit var targetBuffer: java.nio.FloatBuffer
    private lateinit var targetIndexBuffer: java.nio.ShortBuffer
    private lateinit var platformBuffer: java.nio.FloatBuffer
    private lateinit var indexBuffer: java.nio.ShortBuffer
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.2f, 0.3f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        
        mProgram = createProgram(vertexShaderCode, fragmentShaderCode)
        
        // Pre-allocate buffers
        ballBuffer = java.nio.ByteBuffer.allocateDirect(ballVertices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        ballBuffer.put(ballVertices)
        ballBuffer.position(0)
        
        ballIndexBuffer = java.nio.ByteBuffer.allocateDirect(ballIndices.size * 2)
            .order(java.nio.ByteOrder.nativeOrder())
            .asShortBuffer()
        ballIndexBuffer.put(ballIndices)
        ballIndexBuffer.position(0)
        
        targetBuffer = java.nio.ByteBuffer.allocateDirect(targetVertices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        targetBuffer.put(targetVertices)
        targetBuffer.position(0)
        
        targetIndexBuffer = java.nio.ByteBuffer.allocateDirect(targetIndices.size * 2)
            .order(java.nio.ByteOrder.nativeOrder())
            .asShortBuffer()
        targetIndexBuffer.put(targetIndices)
        targetIndexBuffer.position(0)
        
        platformBuffer = java.nio.ByteBuffer.allocateDirect(platformVertices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        platformBuffer.put(platformVertices)
        platformBuffer.position(0)
        
        indexBuffer = java.nio.ByteBuffer.allocateDirect(platformIndices.size * 2)
            .order(java.nio.ByteOrder.nativeOrder())
            .asShortBuffer()
        indexBuffer.put(platformIndices)
        indexBuffer.position(0)
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 100f)
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        // Set up camera for landscape
        Matrix.setLookAtM(mViewMatrix, 0,
            0f, 3f, 6f,    // eye - closer view for landscape
            0f, 0f, 0f,    // center
            0f, 1f, 0f)    // up
        
        // Draw platform first
        drawPlatform()
        
        // Draw targets
        drawTargets()
        
        // Draw ball
        drawBall()
    }
    
    fun updateBallPosition(x: Float, y: Float, z: Float) {
        ballPosition[0] = x
        ballPosition[1] = y
        ballPosition[2] = z
        // No need to request render since we're using RENDERMODE_CONTINUOUSLY
    }
    
    fun updateTargets(newTargets: List<Pair<Float, Float>>) {
        targets = newTargets
        // No need to request render since we're using RENDERMODE_CONTINUOUSLY
    }
    
    fun setGLSurfaceView(surfaceView: GLSurfaceView) {
        glSurfaceView = surfaceView
    }
    
    private fun drawBall() {
        GLES20.glUseProgram(mProgram)
        
        val mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        val mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor")
        val mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        
        // Set up the ball position
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, ballPosition[0], ballPosition[1], ballPosition[2])
        
        // Combine the matrices
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)
        
        // Apply the transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        
        // Enable and set vertex attributes
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mColorHandle)
        
        ballBuffer.position(0)
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 24, ballBuffer)
        
        ballBuffer.position(3)
        GLES20.glVertexAttribPointer(mColorHandle, 3, GLES20.GL_FLOAT, false, 24, ballBuffer)
        
        // Draw the ball as a cube
        ballIndexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, ballIndices.size, GLES20.GL_UNSIGNED_SHORT, ballIndexBuffer)
        
        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mColorHandle)
    }
    
    private fun drawTargets() {
        GLES20.glUseProgram(mProgram)
        
        val mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        val mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor")
        val mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        
        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mColorHandle)
        
        for (target in targets) {
            // Set up the target position - higher for better visibility
            Matrix.setIdentityM(mModelMatrix, 0)
            Matrix.translateM(mModelMatrix, 0, target.first, -1.5f, target.second)
            
            // Combine the matrices
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)
            
            // Apply the transformation
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)
            
            // Set vertex attributes
            targetBuffer.position(0)
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 24, targetBuffer)
            
            targetBuffer.position(3)
            GLES20.glVertexAttribPointer(mColorHandle, 3, GLES20.GL_FLOAT, false, 24, targetBuffer)
            
            // Draw the target
            targetIndexBuffer.position(0)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, targetIndices.size, GLES20.GL_UNSIGNED_SHORT, targetIndexBuffer)
        }
        
        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mColorHandle)
    }
    
    private fun drawPlatform() {
        GLES20.glUseProgram(mProgram)
        
        val mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        val mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor")
        val mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        
        // Set up the platform position
        Matrix.setIdentityM(mModelMatrix, 0)
        
        // Combine the matrices
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)
        
        // Apply the transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        
        // Enable and set vertex attributes
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mColorHandle)
        
        platformBuffer.position(0)
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 24, platformBuffer)
        
        platformBuffer.position(3)
        GLES20.glVertexAttribPointer(mColorHandle, 3, GLES20.GL_FLOAT, false, 24, platformBuffer)
        
        // Draw the platform
        indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, platformIndices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        
        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mColorHandle)
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
    
    private fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        return program
    }
} 