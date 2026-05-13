package fr.olegueyan.algomix.ui.components.rubik

import android.content.Context
import android.util.AttributeSet
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.application.rubik.scene.RubikSceneConfiguration
import fr.olegueyan.algomix.application.rubik.scene.RubikZoomSettings

/** Internal style bundle extracted from XML attributes for [RubikCubeView]. */
internal data class RubikCubeViewStyle(
    val appearance: RubikCubeAppearance = RubikCubeAppearance(),
    val doubleTapResetEnabled: Boolean = true,
    val zoomSettings: RubikZoomSettings = RubikZoomSettings(),
) {
    /** Groups the XML parser used by the Rubik view style. */
    companion object {
        /** Reads the supported Rubik view attributes from the provided Android context. */
        fun from(context: Context, attrs: AttributeSet?): RubikCubeViewStyle {
            if (attrs == null) {
                return RubikCubeViewStyle()
            }

            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RubikCubeView)
            try {
                return RubikCubeViewStyle(
                    appearance = RubikCubeAppearance(
                        backgroundColor = typedArray.getColor(
                            R.styleable.RubikCubeView_rubikBackgroundColor,
                            RubikCubeAppearance.DEFAULT_BACKGROUND_COLOR,
                        ),
                        bodyColor = typedArray.getColor(
                            R.styleable.RubikCubeView_rubikBodyColor,
                            RubikCubeAppearance.DEFAULT_BODY_COLOR,
                        ),
                    ),
                    doubleTapResetEnabled = typedArray.getBoolean(
                        R.styleable.RubikCubeView_rubikEnableDoubleTapReset,
                        true,
                    ),
                    zoomSettings = RubikZoomSettings(
                        minZoomFactor = typedArray.getFloat(
                            R.styleable.RubikCubeView_rubikMinZoomFactor,
                            RubikSceneConfiguration.MIN_ZOOM_FACTOR,
                        ),
                        initialZoomFactor = typedArray.getFloat(
                            R.styleable.RubikCubeView_rubikInitialZoomFactor,
                            RubikSceneConfiguration.INITIAL_ZOOM_FACTOR,
                        ),
                        maxZoomFactor = typedArray.getFloat(
                            R.styleable.RubikCubeView_rubikMaxZoomFactor,
                            RubikSceneConfiguration.MAX_ZOOM_FACTOR,
                        ),
                    ),
                )
            } finally {
                typedArray.recycle()
            }
        }
    }
}
