# 追迹

公交与地铁查询 App。公交数据来自掌上公交，地铁线网与首末班来自高德，界面用 Compose 实现。

## Introduction

追迹把公交实时到站、地铁线网与首末班、线路收藏、步行导航整合在一个 App 里。

- 公交：附近站点、经过线路、实时车辆与路段畅通度、到站提醒、时刻表
- 地铁：城市线网、方向切换、每站两班到达时间、换乘线路、时刻表、运营公告
- 收藏：公交与地铁按方向分别收藏
- 导航：调用已安装的高德地图、百度地图、腾讯地图、Google地图来进行导航

包名 :  `com.enddream.tracing`

## Supported Versions

- Android 9 及以上（minSdk 28）
- targetSdk 36

## Install

下载 `Tracing-x.y.z.apk` 直接安装。

## Download

见 Releases。

## Build

```bash
cd android
gradle :app:assembleRelease
```

需要 Android SDK 36 与 JDK 17。签名信息放在 `android/keystore.properties` 与 `android/keystore/`，两者不纳入版本库。

## Credits

数据来源：

- 掌上公交 ：公交线路、站点、实时车辆
- 高德地图 ：地铁线网与首末班

开源库：

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin](https://kotlinlang.org)
- [OkHttp](https://square.github.io/okhttp)
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)

## License

[MIT](LICENSE)
