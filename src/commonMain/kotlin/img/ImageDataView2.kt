package img

import korlibs.image.format.*
import korlibs.korge.view.*
import korlibs.math.geom.*

open class ImageDataView2(
    data: ImageData? = null,
    animation: String? = null,
    playing: Boolean = false,
    smoothing: Boolean = true,
) : Container(), PixelAnchorable, Anchorable {
    // Here we can create repeated in korge-parallax if required
    open fun createAnimationView(): ImageAnimationView2<out SmoothedBmpSlice> {
        return imageAnimationView2()
    }

    open val animationView: ImageAnimationView2<out SmoothedBmpSlice> = createAnimationView()

    override var anchorPixel: Point by animationView::anchorPixel
    override var anchor: Anchor by animationView::anchor

    var smoothing: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                animationView.smoothing = value
            }
        }

    var data: ImageData? = data
        set(value) {
            if (field !== value) {
                field = value
                updatedDataAnimation()
            }
        }

    var animation: String? = animation
        set(value) {
            if (field !== value) {
                field = value
                updatedDataAnimation()
            }
        }

    init {
        updatedDataAnimation()
        if (playing) play() else stop()
        this.smoothing = smoothing
    }

    fun play() {
        animationView.play()
    }

    fun stop() {
        animationView.stop()
    }

    private fun updatedDataAnimation() {
        animationView.animation =
            if (animation != null) data?.animationsByName?.get(animation) else data?.defaultAnimation
    }
}

