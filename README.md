# 追迹 - Tracing

公交与地铁查询 App。公交数据与时刻表来自掌上公交，地铁线网、里程与票价来自高德，界面用 Compose 实现。

## Introduction

追迹把公交实时到站、地铁线网与首末班、线路收藏、步行导航整合在一个 App 里。

- 公交：附近站点、经过线路、实时车辆与路段畅通度、到站提醒、时刻表（含高德首末班与票价）
- 地铁：城市线网、方向切换、每站两班到达时间、换乘线路、时刻表、运营公告
- 收藏：公交与地铁按方向分别收藏
- 导航：调用已安装的高德地图、百度地图、腾讯地图、Google地图来进行导航

包名 :  `com.enddream.tracing`

## Supported Versions

- Android 9 及以上（minSdk 28）
- targetSdk 36

## Download

见 [Releases](https://github.com/ENDDREAM-ZM/Tracing/releases)

## Build

```bash
cd android
gradle :app:assembleRelease
```

需要 Android SDK 36 与 JDK 17。签名信息放在 `android/keystore.properties` 与 `android/keystore/`，两者不纳入版本库。

自行构建还需要在高德开放平台申请定位 SDK 的 Key（绑定包名 `com.enddream.tracing` 与自己的签名 SHA1），并写进 `android/keystore.properties`：

```properties
amapKey=你的Key
```

没有配置时构建仍可通过，但高德定位不可用（会回退系统定位）。

## Credits

数据来源：

- 掌上公交：公交线路、站点、实时车辆、发车时刻表，地铁城市表
- 高德地图：地铁线网、换乘线路、全程里程、票价与运营公告；公交首末班、票价与所属公司
- 高德定位 SDK：定位、逆地理编码、坐标转换
- 已安装的地图应用：跳转导航

开放源代码许可：

- [高德 SDK](https://lbs.amap.com/)（高德地图开放平台授权）：定位、逆地理编码、公交线路检索
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（Apache-2.0）：液态玻璃模糊、高光与阴影
- [Jetpack Compose](https://developer.android.com/jetpack/compose)（Apache-2.0）：界面框架，含 Material 3
- [Material Icons](https://fonts.google.com/icons)（Apache-2.0）：界面图标
- [AndroidX](https://developer.android.com/jetpack/androidx)（Apache-2.0）：Core、Activity、Lifecycle、DataStore
- [Kotlin](https://kotlinlang.org)（Apache-2.0）：开发语言与标准库
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)（Apache-2.0）：协程
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)（Apache-2.0）：JSON 解析
- [OkHttp](https://square.github.io/okhttp)（Apache-2.0）：网络请求
- [Okio](https://square.github.io/okio)（Apache-2.0）：I/O 库

## Privacy

位置信息由高德定位 SDK 提供，首次启动时询问，可在设置里关闭并退回系统定位。关闭后不调用任何高德接口。

## License

[MIT](LICENSE)
