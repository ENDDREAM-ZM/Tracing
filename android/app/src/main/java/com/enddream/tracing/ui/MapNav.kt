package com.enddream.tracing.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

object MapNav {

    private val schemes = listOf(
        "androidamap://route/plan/?sourceApplication=enddream&dlat=%1\$s&dlon=%2\$s&dname=%3\$s&dev=0&t=2",
        "baidumap://map/direction?destination=latlng:%1\$s,%2\$s|name:%3\$s&coord_type=gcj02&mode=walking&src=android.enddream.xingji",
        "qqmap://map/routeplan?type=walk&to=%3\$s&tocoord=%1\$s,%2\$s&referer=enddream",
        "google.navigation:q=%1\$s,%2\$s&mode=w"
    )

    private data class Target(val pkg: String, val scheme: String)

    private val targets = listOf(
        Target("com.autonavi.minimap", "androidamap://route/plan/?sourceApplication=enddream&dlat=%1\$s&dlon=%2\$s&dname=%3\$s&dev=0&t=2"),
        Target("com.baidu.BaiduMap", "baidumap://map/direction?destination=latlng:%1\$s,%2\$s|name:%3\$s&coord_type=gcj02&mode=walking&src=android.enddream.xingji"),
        Target("com.tencent.map", "qqmap://map/routeplan?type=walk&to=%3\$s&tocoord=%1\$s,%2\$s&referer=enddream"),
        Target("com.google.android.apps.maps", "google.navigation:q=%1\$s,%2\$s&mode=w")
    )

    private fun installed(ctx: Context): List<Target> = targets.filter { t ->
        runCatching {
            ctx.packageManager.getPackageInfo(t.pkg, 0)
            true
        }.getOrDefault(false)
    }

    fun openSystem(ctx: Context, lat: Double, lng: Double, name: String): Boolean {
        if (lat == 0.0 || lng == 0.0) return false
        val safeName = Uri.encode(name)
        val apps = installed(ctx)
        if (apps.isEmpty()) return false
        val intents = apps.map { t ->
            val uri = String.format(t.scheme, lat.toString(), lng.toString(), safeName)
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage(t.pkg)
            }
        }
        val chooser = Intent.createChooser(intents.first(), "导航到 $name").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intents.size > 1) {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.drop(1).toTypedArray())
            }
        }
        return runCatching { ctx.startActivity(chooser) }.isSuccess
    }

    @Suppress("unused")
    private fun legacy(ctx: Context, lat: Double, lng: Double, name: String): Boolean {
        val safeName = Uri.encode(name)
        val extra = schemes.map { s ->
            val uri = String.format(s, lat.toString(), lng.toString(), safeName)
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        val chooser = Intent.createChooser(extra.first(), "导航到 $name").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_INITIAL_INTENTS, extra.drop(1).toTypedArray())
        }
        return runCatching { ctx.startActivity(chooser) }.isSuccess
    }
}
