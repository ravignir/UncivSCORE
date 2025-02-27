package com.unciv.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.unciv.Constants
import com.unciv.ui.images.ImageWithCustomSize
import com.unciv.ui.popup.Popup
import com.unciv.ui.popup.popups
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.toLabel

/** A loading screen that creates a screenshot of the current screen and adds a "Loading..." popup on top of that */
class LoadingScreen(
    previousScreen: BaseScreen? = null
) : BaseScreen() {
    val screenshot: Texture
    init {
        screenshot = takeScreenshot(previousScreen)
        val image = ImageWithCustomSize(
            TextureRegion(
                screenshot,
                0,
                screenshot.height,
                screenshot.width,
                -screenshot.height
            )
        )
        image.width = stage.width
        image.height = stage.height
        stage.addActor(image)
        stage.addAction(Actions.sequence(
            Actions.delay(1000f),
            Actions.run {
                val popup = Popup(stage)
                popup.add(Constants.loading.toLabel())
                popup.open()
            }
        ))
    }

    private fun takeScreenshot(previousScreen: BaseScreen?): Texture {
        if (previousScreen != null) {
            for (popup in previousScreen.popups) popup.isVisible = false
            previousScreen.render(Gdx.graphics.getDeltaTime())
        }
        val pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
        val screenshot = Texture(pixmap)
        pixmap.dispose()

        if (previousScreen != null) {
            for (popup in previousScreen.popups) popup.isVisible = true
        }
        return screenshot
    }


    override fun dispose() {
        screenshot.dispose()
        super.dispose()
    }
}
