package fr.olegueyan.algomix.infrastructure.rendering.rubik

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

internal class RubikCubeGeometry {
    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    init {
        val vertices = floatArrayOf(
            1f, -1f, 1f, 1f, -1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f,
            -1f, -1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f,
            -1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, -1f, -1f, 1f, -1f,
            -1f, -1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, 1f,
            -1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f, 1f,
            1f, -1f, -1f, -1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f,
        )
        val faceTriangles = intArrayOf(0, 1, 2, 0, 2, 3)
        val indices = ShortArray(36) { index ->
            val base = (index / 6) * 4
            (base + faceTriangles[index % 6]).toShort()
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
            .also { it.position(0) }

        indexBuffer = ByteBuffer.allocateDirect(indices.size * Short.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(indices)
            .also { it.position(0) }
    }

    fun bindPositionAttribute(attributeLocation: Int) {
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(attributeLocation, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(attributeLocation)
    }

    fun drawFace(faceIndex: Int) {
        indexBuffer.position(faceIndex * 6)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }
}
