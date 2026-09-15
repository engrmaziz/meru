package app.meru.android.feature.drive

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.meru.android.engine.telemetry.RoutePoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val STYLE_URL = "https://demotiles.maplibre.org/style.json"
private const val SOURCE_ID = "meru-route"
private const val LAYER_ID = "meru-route-line"

@Composable
fun DriveMap(
    route: List<RoutePoint>,
    latitude: Double?,
    longitude: Double?,
    bearing: Float?,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(180.dp),
    expanded: Boolean = false,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).also { it.onCreate(null) }
    }

    DisposableEffect(Unit) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = if (expanded) modifier.fillMaxSize() else modifier,
        factory = { mapView },
        update = { view ->
            view.getMapAsync { map ->
                if (map.style == null) {
                    map.setStyle(STYLE_URL) { style ->
                        ensureRouteLayer(style)
                        updateRoute(style, route)
                    }
                } else {
                    map.style?.let {
                        ensureRouteLayer(it)
                        updateRoute(it, route)
                    }
                }
            }
        },
    )

    LaunchedEffect(route.size, latitude, longitude, bearing) {
        mapView.getMapAsync { map ->
            map.style?.let { style ->
                ensureRouteLayer(style)
                updateRoute(style, route)
            }
            if (latitude != null && longitude != null) {
                val target = LatLng(latitude, longitude)
                map.easeCamera(CameraUpdateFactory.newLatLngZoom(target, if (expanded) 16.0 else 14.5), 450)
                bearing?.let { map.easeCamera(CameraUpdateFactory.bearingTo(it.toDouble()), 300) }
            }
        }
    }
}

private fun ensureRouteLayer(style: Style) {
    if (style.getSource(SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
    }
    if (style.getLayer(LAYER_ID) == null) {
        style.addLayer(
            LineLayer(LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.lineColor(AndroidColor.parseColor("#2EE6A6")),
                PropertyFactory.lineWidth(4f),
            ),
        )
    }
}

private fun updateRoute(style: Style, route: List<RoutePoint>) {
    val source = style.getSource(SOURCE_ID) as? GeoJsonSource ?: return
    if (route.size < 2) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
        return
    }
    val line = LineString.fromLngLats(
        route.map { Point.fromLngLat(it.longitude, it.latitude) },
    )
    source.setGeoJson(Feature.fromGeometry(line))
}
