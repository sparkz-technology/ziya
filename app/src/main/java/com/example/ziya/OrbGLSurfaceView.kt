import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.example.ziya.OrbRenderer

class OrbGLSurfaceView(context: Context, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    private val renderer: OrbRenderer

    init {
        setEGLContextClientVersion(2) // Use OpenGL ES 2.0
        setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Configure for transparency
        holder.setFormat(PixelFormat.TRANSLUCENT)

        renderer = OrbRenderer(context)
        setRenderer(renderer)

        renderMode = RENDERMODE_CONTINUOUSLY
        setZOrderOnTop(true) // Ensure it's drawn on top of other views
    }

    fun updateAudioLevels(intensity: Float) {
        renderer.updateAudioLevels(intensity)
    }

    fun setOrbState(state: Int) {
        renderer.setOrbState(state)
    }
}