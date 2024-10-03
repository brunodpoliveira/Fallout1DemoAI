package utils

import korlibs.korge.view.*
import korlibs.math.raycasting.*
import korlibs.datastructure.Extra

var RayResult.view: View? by Extra.Property { null }
var RayResult.blockedResults: List<RayResult>? by Extra.Property { null }
