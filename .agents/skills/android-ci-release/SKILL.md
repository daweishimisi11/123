---
name: android-ci-release
description: >-
  Run local Android Gradle verification on Windows PowerShell, verify repo-local
  git identity and push over HTTP/1.1, and cut semantic v* release tags without
  polling CI. 适用于本地构建与测试校验（本地校验）、提交并推送到 origin（提交推送）以及创建并推送新版本标签发版（打标签发版）。
---

# Android 本地校验与发版流程

## 执行边界与安全护栏

按用户意图严格区分三类任务，禁止越权自动推送或发版：

- **仅校验**：只执行第 1 节。
- **提交推送**：执行第 1–2 节。
- **发版打标**：仅当用户明确要求发版或打 tag 时才执行第 3 节。
- 校验失败必须先修复，禁止跳过校验提交；严禁使用 `git push --force`。

## 1. 本地全量校验（Windows PowerShell 5.1）

动态定位当前用户的 JDK 17 与 Android SDK，并一次性运行 Lint、单元测试、Debug 构建及 `androidTest` Kotlin 编译检查：

```powershell
$jdkDir = Get-ChildItem -Path (Join-Path $env:USERPROFILE '.jdks') -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending |
    Select-Object -First 1
if (-not $jdkDir) {
    throw "未在 $env:USERPROFILE\.jdks 下找到 jdk-17*，请先安装 JDK 17。"
}

$sdkDir = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
if (-not (Test-Path $sdkDir)) {
    throw "未找到 Android SDK：$sdkDir"
}

$env:JAVA_HOME = $jdkDir.FullName
$env:ANDROID_HOME = $sdkDir
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

.\gradlew.bat lintDebug testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin --console=plain
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
```

> 说明：CI 的 `ci.yml` 运行 `lintDebug testDebugUnitTest assembleDebug`，此处额外保留 `compileDebugAndroidTestKotlin` 以提前拦截 Compose UI 测试编译错误；发版时的 R8 混淆压缩与签名校验（`assembleRelease`）由 `release.yml` 在远端统一执行。

## 2. 提交与推送

1. **核对当前仓库局部作者身份**（仅检查 `--local`，不匹配时只调整 repo 级配置或提醒用户，禁止修改全局配置或编造邮箱）：

   ```powershell
   $gitName = git config --local user.name
   $gitEmail = git config --local user.email
   if ($gitName -ne 'daweishimisi11' -or $gitEmail -ne '360553218+daweishimisi11@users.noreply.github.com') {
       throw "仓库局部作者配置不匹配（当前：$gitName <$gitEmail>），请确认或仅使用 git config --local 修正。"
   }
   ```

2. **核对分支与工作区状态**（确认当前分支为 `main`，避免误推其他分支或漏文件）：

   ```powershell
   $branch = git branch --show-current
   if ($branch -ne 'main') {
       throw "当前分支为 '$branch'，请切换或确认后再推送到 main。"
   }
   git status --short --branch
   ```

3. 使用简体中文 Conventional Commits 提交变更（`release.yml` 会按提交类型自动归类生成 GitHub Release 与 Telegram 更新日志），随后强制使用 `HTTP/1.1` 推送，规避 Windows 下 HTTP/2 / `SSL_ERROR_SYSCALL` 断连：

   ```powershell
   git -c http.version=HTTP/1.1 push origin main
   if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
   ```

## 3. 打标签发版（无需轮询等待）

1. **拉取远端标签并确定版本号**：默认按语义化版本递增最后一位修订号（如 `v0.2.5` → `v0.2.6`），若用户已指定版本号则以其为准。`versionName` 与 `versionCode` 由 `.github/workflows/release.yml` 通过 `-PversionName` 和 `-PversionCode=${GITHUB_RUN_NUMBER}` 自动注入 `app/build.gradle.kts`，**发版时不要手动修改 `app/build.gradle.kts` 中的版本号**。

   ```powershell
   git -c http.version=HTTP/1.1 fetch --tags
   if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
   git tag --sort=-v:refname | Select-Object -First 5
   ```

2. **更新应用内更新日志**：在 `app/src/main/java/com/av123/video/ui/screens/about/AboutScreen.kt` 顶部 `APP_CHANGELOG` 列表最前方新增一条 `ChangelogEntry`（简体中文）：

   ```kotlin
   ChangelogEntry(
       version = "vX.Y.Z",
       highlights = listOf(
           "本版本主要改动说明"
       )
   ),
   ```

3. **校验、提交并推送主分支**：执行第 1 节与第 2 节，完成后确认工作区已 clean（`git status --short` 无输出）。
4. **创建并推送标签**（不使用 `&&`，逐行检查 `$LASTEXITCODE` 以兼容 PowerShell 5.1）：

   ```powershell
   $tag = 'vX.Y.Z'
   git tag $tag
   if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
   git -c http.version=HTTP/1.1 push origin $tag
   if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
   ```

5. **禁止挂起等待或轮询 CI**：推送标签后即可结束任务；`.github/workflows/release.yml` 会在签名发布 `123-vX.Y.Z.apk` 前自动执行 `./gradlew lintDebug testDebugUnitTest assembleRelease`，并通过 `Notify Telegram on Release` / `Notify Telegram on Failure` 自动将结果发送到 Telegram。
