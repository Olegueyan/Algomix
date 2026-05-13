package fr.olegueyan.algomix.ui.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import fr.olegueyan.algomix.databinding.ActivityMainBinding

/** Hosts the main screen and forwards lifecycle events to the cube view. */
class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding

    /** Initializes the edge-to-edge layout and inflates the main binding. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    /** Resumes the OpenGL surface when the activity enters the foreground. */
    override fun onResume() {
        super.onResume()
        binding.cubeView.onResume()
    }

    /** Pauses the OpenGL surface when the activity leaves the foreground. */
    override fun onPause() {
        super.onPause()
        binding.cubeView.onPause()
    }
}
