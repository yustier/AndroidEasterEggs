# Landroid Desktop - Android 16 Baklava スクリーンセーバー/ゲーム デスクトップ移植

## 概要

Android 16の「Landroid」スクリーンセーバーをWindows/Linux/macOSで動作するデスクトップアプリケーションに移植したプロジェクトです。
Compose Multiplatformを使用し、通常のゲームモードとスクリーンセーバーモードの両方に対応しています。

## 使用方法

### ビルドと実行

**重要: すべてのコマンドは`desktop`ディレクトリ内で実行してください**

```powershell
# プロジェクトのディレクトリに移動（必須）
cd desktop

# コンパイル
.\gradlew.bat compileKotlin

# 実行（通常モード）
.\gradlew.bat run

# スクリーンセーバーモードでテスト
.\gradlew.bat run --args="/s"

# 設定/使い方ダイアログ表示
.\gradlew.bat run --args="/c"

# ポータブル版の生成（推奨）- フォルダ形式
.\gradlew.bat createDistributable

# Windows用.exeインストーラーの生成
.\gradlew.bat packageExe

# Windows用MSIインストーラーの生成
.\gradlew.bat packageMsi
```

生成されたファイルは以下に配置されます：
- **ポータブル版**: `build\compose\binaries\main\app\Landroid\`
  - このフォルダをそのままコピーして使用可能
  - `Landroid.exe`を実行（JVMランタイム埋め込み済み）
- **インストーラー版(.exe)**: `build\compose\binaries\main\exe\Landroid-1.1.1.exe`
- **インストーラー版(.msi)**: `build\compose\binaries\main\msi\Landroid-1.1.1.msi`

### コマンドライン引数

```powershell
# 通常ゲームモード
Landroid.exe

# スクリーンセーバーモード（フルスクリーン + オートパイロット）
Landroid.exe /s

# 設定/使い方ダイアログ表示（日本語）
Landroid.exe /c
```

**注意:** `/c`ダイアログには、起動方法、操作方法、スクリーンセーバーモードの説明が日本語で表示されます。

### キーボード操作

**通常ゲームモード:**
- `F11` - フルスクリーン切り替え
- `ESC` - 終了
- `AUTO`ボタン - オートパイロットON/OFF
- マウス操作 - 宇宙船の制御

**スクリーンセーバーモード:**
- マウスポインタ - 自動的に非表示
- マウス移動 - 終了（1.5秒後から検知開始）
- 任意のキー - 即座に終了
- マルチモニター - 全画面黒塗り対応

## スクリーンセーバー(.scr)への変換

Windowsスクリーンセーバー(.scr)は実際には実行可能ファイル(.exe)をリネームしたものです。

### 手動インストール方法

1. `Landroid.exe`を`Landroid.scr`にリネーム
2. `app/`および`runtime/`フォルダと一緒に保持（相対パス依存あり）
3. `.scr`ファイルを右クリック → 「インストール」でスクリーンセーバーとして登録

**注意:** 
- フォルダ構造を維持することで、埋め込みJVMが正常に動作します
- `/s`引数が自動的に渡され、スクリーンセーバーモードで起動します

## 技術仕様

### 開発環境
- **Gradle**: 9.0.0
- **Kotlin**: 2.1.0
- **Compose Multiplatform**: 1.7.1
- **Java**: 21 (JVM toolchain)
- **Target**: Windows, Linux, macOS

### 主要な依存関係
- `androidx.collection:collection:1.4.0` - ArraySet等のコレクション
- `kotlinx-coroutines-core:1.8.0` - コルーチン
- `kotlinx-coroutines-swing:1.8.0` - Swing統合

### Android依存の除去内容
- `android.util.Log` → `println()`
- `android.os.Build.VERSION.SDK_INT` → 定数 `16`
- `android.content.res.Resources` → ハードコード文字列配列
- `com.android_baklava.egg.R` → リソース参照削除
- `com.android_baklava.egg.flags.Flags` → `true`固定
- `androidx.core.math.MathUtils.clamp` → カスタム実装
- `DreamService` - 削除（Android専用）

## プロジェクト構造

```
desktop/
├── build.gradle.kts           # ビルド設定
├── settings.gradle.kts        # プロジェクト設定
├── gradle.properties          # Gradle設定
├── README.md                  # このファイル
└── src/main/kotlin/com/android_baklava/desktop/
    ├── Main.kt                # エントリーポイント
    └── landroid/              # 移植されたLandroidコード
        ├── Autopilot.kt
        ├── Colors.kt
        ├── ComposeTools.kt
        ├── DesktopUI.kt
        ├── Maths.kt
        ├── Namer.kt
        ├── PathTools.kt
        ├── Physics.kt
        ├── Randomness.kt
        ├── Universe.kt
        ├── Vec2.kt
        └── VisibleUniverse.kt
```

## バージョン履歴

### v1.1.1 (2025-11-01)
- ✨ スクリーンセーバーモードでマウスポインタを自動非表示
- 🗑️ `--help`オプションを削除（`/c`に統合）
- 🌐 `/c`ダイアログを日本語化し、詳細な使い方を表示
- 📝 プロポーショナルフォントでも見やすいダイアログデザイン

### v1.1.0 (2025-11-01)
- ✨ Android 16公式ロゴをアイコンとして追加
- 🐛 フルスクリーン切り替え時のウィンドウサイズ保持を修正
- 🐛 スクリーンセーバー起動時の即座終了問題を修正（1.5秒安定化期間）
- ✨ マルチモニター対応（全画面黒塗り）
- ✨ マウス移動検知の改善

### v1.0.0 (2025-11-01)
- 🎉 Android 16 LandroidのデスクトップPCへの移植完了
- ✨ 通常ゲームモードとスクリーンセーバーモード対応
- 📦 ポータブル版とインストーラー版の配布

## 既知の問題

- ⚠️ `forEachGesture()`の非推奨警告（機能には影響なし）
- ⚠️ スクリーンセーバーモードでのマウスクリック検知は動作しない（オートパイロットのため）
  - マウス移動検知で十分に機能するため、実用上の問題なし

## 今後の改善案

1. プレビューモード(`/p`)の実装
2. 非推奨APIの更新（`awaitEachGesture()`への移行）
3. 英語版ダイアログの追加オプション

## ライセンス

(c) MMXXV Airoku / Claude Sonnet 4.5 All rights reserved for modified part.

This software includes code licensed under the Apache License 2.0.
This software includes modified portions of code originally licensed under the Apache License, Version 2.0.
You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
Copyright (C) 2024 The Android Open Source Project
