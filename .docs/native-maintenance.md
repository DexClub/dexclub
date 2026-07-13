# DexClub Native 维护说明

## 目标

本稿只面向下面这类工作：

- `dexkit-binding` 绑定维护
- vendor DexKit 同步或排障
- `libcxx` prefab 发布
- Android / native 构建链复现
- CI native 打包链路对齐

如果只是改：

- `.docs/`
- `domain-core`
- 一部分 `cli / mcp`
- 普通 JVM 测试

默认不需要先进入这条维护路径。

## 当前相关位置

- `dexkit-binding/`
  - 仓库内 KMP 包装层
- `dexkit-binding/vendor/DexKit/`
  - vendored 上游 DexKit
- `dexkit-binding/vendor/libcxx-prefab/`
  - 本地维护的 `libcxx` prefab 仓库
- `settings.gradle.kts`
  - 根仓通过 `includeBuild("dexkit-binding")` 接入绑定层
- `dexkit-binding/settings.gradle.kts`
  - `dexkit-binding` 自己通过 `includeBuild("vendor/DexKit")` 接入上游构建
- `dexkit-binding/settings.gradle.kts`
  - `dexkit-binding` 自己的依赖解析默认包含 `mavenLocal()`

## 当前环境前提

进入 native / Android 维护路径时，当前仓库实际依赖下面这组环境：

- JDK 21
- Android SDK
- Android NDK `26.1.10909125`
- Android NDK `28.2.13676358`
- `cmake` `3.18.1`
- `cmake` `3.31.6`
- `ninja`

当前口径和 CI 保持一致：

- `NDK 28.2.13676358`
  - 用于发布 `dexkit-binding/vendor/libcxx-prefab`
- `NDK 26.1.10909125`
  - vendored 上游 `dexkit-android` 模块当前声明使用它
- 两个 `cmake` 版本都会在 CI 中安装

## 首次准备

首次拉取后先初始化 submodule：

```bash
git submodule update --init --recursive
```

如果要复现 Android / native 构建链，本地通常还需要准备 `local.properties`：

- 仓库根目录 `local.properties`
- `dexkit-binding/vendor/DexKit/local.properties`
- 在发布 `libcxx` 时，`dexkit-binding/vendor/libcxx-prefab/local.properties`

内容只需要指向当前 `sdk.dir`。

CI 里也是按这个方式临时写入。

## `libcxx` 与 `mavenLocal()`

当前仓库不是“完全不依赖本地状态”的构建。

真实情况是：

- `dexkit-binding/vendor/libcxx-prefab` 当前会发布 `dev.rikka.ndk.thirdparty:libcxx:1.3.0`
- vendored DexKit 的 Android 侧当前实际声明依赖是 `dev.rikka.ndk.thirdparty:cxx:1.2.0`
- 两者不是同一个 Maven 坐标，因此当前不会形成直接版本冲突
- 以仓库当前代码搜索结果看，`libcxx:1.3.0` 还没有被主工程或 vendored DexKit 直接声明消费
- 现在触发 `publishToMavenLocal` 的位置主要是 CI/native 维护脚本本身，而不是 `dexkit-binding` 的正常编译类路径

如果本地还没有发布过 `libcxx`，先执行：

```bash
cd dexkit-binding/vendor/libcxx-prefab
./gradlew :cxx:publishToMavenLocal
```

这一步会把下面这个坐标发布到本机 `mavenLocal()`：

```text
dev.rikka.ndk.thirdparty:libcxx:1.3.0
```

当前更准确的理解是：

- 这一步主要服务于本地或 CI 复现 `libcxx-prefab` 维护链
- 它不等同于“vendored DexKit Android 编译当前必须依赖这个坐标”
- 如果后续继续保留这条发布步骤，最好把它视为维护链产物准备，而不是普通开发前置条件

如果后续要继续清理这条链，优先应先确认：

- `.github/workflows/verify.yml`
- `.github/workflows/build-native.yml`

里这步发布是否仍然有真实下游消费。

## 常用维护命令

### 1. 验证 KMP 包装层

```bash
./gradlew -p dexkit-binding compileKotlinJvm
./gradlew -p dexkit-binding jvmTest
```

### 2. 验证 Android 主构建链

```bash
./gradlew -p dexkit-binding assembleAndroidMain
./gradlew -p dexkit-binding testAndroidHostTest
```

如果仓库路径刚发生过重命名，或者 `./gradlew clean` / Android native clean 报出 `.cxx` 目录里的旧绝对路径，可以先执行：

```bash
./gradlew cleanVendoredDexKitAndroidCxxCache
```

这个任务会清掉：

- `dexkit-binding/vendor/DexKit/dexkit-android/.cxx`
- `dexkit-binding/vendor/DexKit/dexkit-android/build/intermediates/cxx`

然后再重新跑：

```bash
./gradlew -p dexkit-binding assembleAndroidMain
```

当前根 `clean` 也会先触发这一步，避免路径迁移后继续命中旧 `.cxx` 元数据。

### 3. 构建桌面 DexKit native library

```bash
./gradlew -p dexkit-binding prepareDexKitDesktopNative
```

### 4. 构建 Android DexKit AAR

```bash
./gradlew -p dexkit-binding assembleDexKitAndroidRelease
```

## CI 对齐点

当前 workflow 中，native 维护链路的关键动作是：

1. 安装 Android SDK、双 NDK、双 `cmake`
2. 给 `dexkit-binding/vendor/libcxx-prefab` 写 `local.properties`
3. 执行 `:cxx:publishToMavenLocal`
4. 把 `.m2/repository/dev/rikka/ndk/thirdparty/libcxx` 打包并传给后续 job
5. 后续 native job 恢复这批 `mavenLocal()` 文件
6. 给根目录和 `dexkit-binding/vendor/DexKit` 写 `local.properties`
7. 再通过 `./gradlew -p dexkit-binding ...` 或直接进入 `vendor/DexKit` 执行对应 native 构建任务

对应文件：

- `.github/workflows/build-native.yml`
- `.github/workflows/build-packages.yml`

如果本地排查结果和 CI 不一致，优先先对齐这几个点，而不是先猜 Gradle 或 NDK 版本问题。

## 不进入这条路径时

如果当前改动不涉及：

- `dexkit-binding`
- vendor DexKit
- `libcxx` 发布
- Android / native 打包

优先先走普通开发路径：

```bash
./gradlew :app-service:testStructured
./gradlew :domain-core:testWorkspace
./gradlew :cli-app:testStructured
./gradlew :mcp-app:testStructured
./gradlew :domain-core:compileKotlinJvm
```

不要把 native 维护前提扩散成所有改动的默认要求。
