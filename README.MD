# DexClub

## 初始化

首次克隆后请初始化子模块：

```bash
git submodule update --init --recursive
```

## 构建与运行

### Android

- 在 Android Studio 中运行导入的 Android 运行配置
- 构建 Debug APK：

```bash
./gradlew :androidApp:assembleDebug
```

APK 输出路径：

`androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### Desktop

- 运行桌面版：

```bash
./gradlew :desktopApp:run
```

- 热重载运行：

```bash
./gradlew :desktopApp:hotRunJvm --auto
```

### 其他常用命令

```bash
./gradlew build
./gradlew clean
```

### Dex Engine CLI

- 构建 fat jar：

```bash
./gradlew :dex-engine:cli:shadowJar
./gradlew :dex-engine:cli:fatJar
```

说明：

- `shadowJar` 是当前正式的 fat jar 构建任务
- `fatJar` 保留为兼容别名

- 产物路径：

`dex-engine/cli/build/libs/dex-engine-cli-all.jar`

- 运行要求：

`dex-engine-cli-all.jar` 需要 **Java 21** 运行。

- 常用示例：

```bash
java -jar dex-engine/cli/build/libs/dex-engine-cli-all.jar --help
java -jar dex-engine/cli/build/libs/dex-engine-cli-all.jar inspect --input path/to/app.apk
java -jar dex-engine/cli/build/libs/dex-engine-cli-all.jar search-class --input path/to/app.apk --keyword Main --limit 20
java -jar dex-engine/cli/build/libs/dex-engine-cli-all.jar export-smali --input path/to/classes.dex --class com.example.Main --output out/Main.smali
java -jar dex-engine/cli/build/libs/dex-engine-cli-all.jar export-java --input path/to/classes.dex --class com.example.Main --output out/Main.java
```

## 测试

- 运行所有测试：

```bash
./gradlew allTests
```

- 运行 sharedUI 单个测试：

```bash
./gradlew :sharedUI:jvmTest --tests 类名.方法名
```

