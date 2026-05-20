package fr.olegueyan.algomix.infrastructure.rendering.rubik

import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import fr.olegueyan.algomix.application.rubik.scene.RubikSceneState
import fr.olegueyan.algomix.domain.cube.CubeFace
import fr.olegueyan.algomix.domain.cube.Cubie
import fr.olegueyan.algomix.domain.cube.MoveAxis
import fr.olegueyan.algomix.ui.components.rubik.RubikCubeAppearance
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class RubikRenderer(
    private val sceneState: RubikSceneState,
) : GLSurfaceView.Renderer {
    @Volatile
    var appearance = RubikCubeAppearance()

    private var program = 0
    private var mvpUniform = 0
    private var colorUniform = 0
    private var positionAttribute = 0

    private val geometry = RubikCubeGeometry()

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)
    private val model = FloatArray(16)
    private val modelViewProjection = FloatArray(16)
    private val temp = FloatArray(16)
    private val moveRotation = FloatArray(16)
    private val composedModel = FloatArray(16)
    private val cameraRotation = FloatArray(16)
    private val backgroundColor = FloatArray(4)
    private val bodyColor = FloatArray(4)

    companion object {
        const val VERTICAL_FOV_DEGREES = 45f
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        compileProgram()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val safeHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, width, safeHeight)
        Matrix.perspectiveM(projection, 0, VERTICAL_FOV_DEGREES, width.toFloat() / safeHeight, 0.1f, 50f)
        sceneState.updateViewport(width, safeHeight, VERTICAL_FOV_DEGREES)
    }

    override fun onDrawFrame(gl: GL10?) {
        val currentAppearance = appearance
        fillColor(currentAppearance.backgroundColor, backgroundColor)
        fillColor(currentAppearance.bodyColor, bodyColor)
        GLES20.glClearColor(
            backgroundColor[0],
            backgroundColor[1],
            backgroundColor[2],
            backgroundColor[3],
        )
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        sceneState.animateFrame()
        buildViewProjection()

        GLES20.glUseProgram(program)
        geometry.bindPositionAttribute(positionAttribute)

        drawCore()

        for (cubie in sceneState.cubeState.cubies) {
            drawCubieBody(cubie)
        }
        for (cubie in sceneState.cubeState.cubies) {
            drawCubieStickers(cubie)
        }
    }

    private fun buildViewProjection() {
        sceneState.camera.toMatrix(cameraRotation)
        Matrix.setIdentityM(view, 0)
        Matrix.translateM(view, 0, 0f, 0f, -sceneState.camera.zoom)
        Matrix.multiplyMM(temp, 0, view, 0, cameraRotation, 0)
        System.arraycopy(temp, 0, view, 0, temp.size)
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
    }

    private fun drawCore() {
        RubikCubeTransforms.setCore(model)
        drawModel(model, bodyColor, CubeFace.entries)
    }

    private fun drawCubieBody(cubie: Cubie) {
        RubikCubeTransforms.setCubieBody(model, cubie)
        applyMoveAnimation(cubie)
        drawModel(model, bodyColor, CubeFace.entries)
    }

    private fun drawCubieStickers(cubie: Cubie) {
        for (face in CubeFace.entries) {
            val stickerColor = cubie.faces[face.ordinal] ?: continue
            RubikCubeTransforms.setSticker(model, cubie, face)
            applyMoveAnimation(cubie)
            drawModel(model, stickerColor.rgba, listOf(face))
        }
    }

    private fun applyMoveAnimation(cubie: Cubie) {
        val rotation = sceneState.currentMoveRotation(cubie) ?: return
        val (axis, angleDeg) = rotation
        Matrix.setIdentityM(moveRotation, 0)
        when (axis) {
            MoveAxis.X -> Matrix.rotateM(moveRotation, 0, angleDeg, 1f, 0f, 0f)
            MoveAxis.Y -> Matrix.rotateM(moveRotation, 0, angleDeg, 0f, 1f, 0f)
            MoveAxis.Z -> Matrix.rotateM(moveRotation, 0, angleDeg, 0f, 0f, 1f)
        }
        Matrix.multiplyMM(composedModel, 0, moveRotation, 0, model, 0)
        System.arraycopy(composedModel, 0, model, 0, model.size)
    }

    private fun drawModel(modelMatrix: FloatArray, color: FloatArray, faces: Iterable<CubeFace>) {
        Matrix.multiplyMM(modelViewProjection, 0, viewProjection, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpUniform, 1, false, modelViewProjection, 0)
        GLES20.glUniform4fv(colorUniform, 1, color, 0)
        for (face in faces) {
            geometry.drawFace(face.ordinal)
        }
    }

    private fun compileProgram() {
        val vertexShader = loadShader(
            GLES20.GL_VERTEX_SHADER,
            """
                uniform mat4 uMVP;
                attribute vec4 aPos;
                void main() {
                    gl_Position = uMVP * aPos;
                }
            """.trimIndent(),
        )
        val fragmentShader = loadShader(
            GLES20.GL_FRAGMENT_SHADER,
            """
                precision mediump float;
                uniform vec4 uColor;
                void main() {
                    gl_FragColor = uColor;
                }
            """.trimIndent(),
        )

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        mvpUniform = GLES20.glGetUniformLocation(program, "uMVP")
        colorUniform = GLES20.glGetUniformLocation(program, "uColor")
        positionAttribute = GLES20.glGetAttribLocation(program, "aPos")
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun fillColor(color: Int, destination: FloatArray) {
        destination[0] = Color.red(color) / 255f
        destination[1] = Color.green(color) / 255f
        destination[2] = Color.blue(color) / 255f
        destination[3] = Color.alpha(color) / 255f
    }
}
