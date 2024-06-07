package img

import korlibs.korge.view.property.*
import korlibs.math.geom.*

interface PixelAnchorable {
    @ViewProperty(name = "anchorPixel")
    var anchorPixel: Point
}

fun <T : PixelAnchorable> T.anchorPixel(point: Point): T {
    this.anchorPixel = point
    return this
}
