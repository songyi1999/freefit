package com.freefit.app.run

import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 高德栅格瓦片底图（GCJ-02 坐标系，国内访问快、中文标注）。
 * 免 key 的公开栅格端点，个人小规模使用。
 */
private object AmapTileSource : OnlineTileSourceBase(
    "AmapRoad", 3, 18, 256, ".png",
    arrayOf(
        "https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8",
        "https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8",
        "https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8",
        "https://webrd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8",
    ),
    "© 高德地图",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl + "&x=" + MapTileIndex.getX(pMapTileIndex) +
            "&y=" + MapTileIndex.getY(pMapTileIndex) +
            "&z=" + MapTileIndex.getZoom(pMapTileIndex)
}

/** WGS-84(GPS) → GCJ-02(高德/火星坐标) 标准转换 */
object Gcj02 {
    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323

    fun fromWgs84(lat: Double, lng: Double): Pair<Double, Double> {
        if (outOfChina(lat, lng)) return lat to lng
        var dLat = transformLat(lng - 105.0, lat - 35.0)
        var dLng = transformLng(lng - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        dLng = (dLng * 180.0) / (A / sqrtMagic * cos(radLat) * PI)
        return (lat + dLat) to (lng + dLng)
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    private fun outOfChina(lat: Double, lng: Double) =
        lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

/**
 * 轨迹地图：高德底图 + 黄色轨迹线 + 起终点标记，自动缩放至轨迹范围。
 * 离线/瓦片加载失败时底图空白但轨迹线仍显示。
 */
@Composable
fun TrackMapView(
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(AmapTileSource)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                isTilesScaledToDpi = true
                isVerticalMapRepetitionEnabled = false
            }
        },
        update = { mv ->
            if (points.size < 2) return@AndroidView

            // 重建轨迹覆盖物（点数变化不频繁，全量重建足够）
            mv.overlays.removeAll { it is Polyline || it is Marker }

            val geo = points.map { (lat, lng) ->
                val g = Gcj02.fromWgs84(lat, lng)
                GeoPoint(g.first, g.second)
            }

            val line = Polyline(mv).apply {
                setPoints(geo)
                outlinePaint.color = 0xFFFACC15.toInt()
                outlinePaint.strokeWidth = 12f
                outlinePaint.strokeCap = Paint.Cap.ROUND
            }
            mv.overlays.add(line)

            fun marker(p: GeoPoint, title: String) = Marker(mv).apply {
                position = p
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                this.title = title
            }
            mv.overlays.add(marker(geo.first(), "Start"))
            mv.overlays.add(marker(geo.last(), "End"))

            if (mv.tag != true) {
                mv.tag = true
                mv.post {
                    mv.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(geo).increaseByScale(1.35f),
                        false,
                    )
                }
            }
            mv.invalidate()
        },
        onRelease = { it.onPause() },
    )
}
