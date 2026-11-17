# 追迹

[![Release](https://img.shields.io/github/v/release/ENDDREAM-ZM/Tracing?style=flat-square&label=release)](https://github.com/ENDDREAM-ZM/Tracing/releases)
[![License](https://img.shields.io/github/license/ENDDREAM-ZM/Tracing?style=flat-square)](LICENSE)

## Introduction

追迹是公交与地铁查询 App。公交数据取自掌上公交的接口，地铁线网与首末班取自高德地图，界面用 Compose 实现。

- 公交：附近站点、经过线路、实时车辆与路段畅通度、到站提醒、时刻表
- 地铁：城市线网、方向切换、每站两班到达时间、换乘线路、时刻表、运营公告
- 收藏：公交与地铁按方向分别收藏
- 导航：调用已安装的高德、百度、腾讯、Google 地图

包名 `com.enddream.tracing`。

## Supported Versions

- Android 9 及以上（minSdk 28）
- targetSdk 36

## Install

下载 Release 里的 APK 直接安装，覆盖安装需要签名一致。

## Download

[Releases](https://github.com/ENDDREAM-ZM/Tracing/releases)

## Build

```bash
cd android
gradle :app:assembleRelease
```

需要 Android SDK 36 与 JDK 17。签名配置放在 `android/keystore.properties` 与 `android/keystore/`，两者不纳入版本库。

## Credits

- [掌上公交](https://h5.mygolbs.com)：公交线路、站点、实时车辆
- [高德地图](https://webapi.amap.com)：地铁线网与首末班
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin](https://kotlinlang.org)
- [OkHttp](https://square.github.io/okhttp)
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)

## License

[MIT](LICENSE)
