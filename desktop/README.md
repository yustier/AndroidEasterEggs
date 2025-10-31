# Landroid Desktop - Android 16 Baklava スクリーンセーバー/ゲーム デスクトップ移植

## 概要

Android 16の「Landroid」スクリーンセーバーをWindows/Linux/macOSで動作するデスクトップアプリケーションに移植したプロジェクトです。
Compose Multiplatformを使用し、通常のゲームモードとスクリーンセーバーモードの両方に対応しています。

## 完成した機能

### 基本機能
- ✅ デスクトップモジュールの作成とビルド設定
- ✅ Compose Multiplatform 1.7.1の設定
- ✅ すべての必要なKotlinソースファイルの移植（14+ファイル）
- ✅ Android依存関係の完全な除去
- ✅ コンパイル成功（警告のみ）

### 移植したファイル
- ✅ Vec2.kt - 2Dベクトル演算
- ✅ Colors.kt - カラー定義
- ✅ Maths.kt - 数学ユーティリティ
- ✅ Physics.kt - 物理シミュレーション
- ✅ Randomness.kt - ランダムユーティリティ（Bag, RandomTable）
- ✅ Namer.kt - 惑星/星の命名システム
- ✅ Universe.kt - 宇宙シミュレーション
- ✅ VisibleUniverse.kt - 描画機能
- ✅ ComposeTools.kt - Compose拡張
- ✅ Autopilot.kt - 自動操縦システム
- ✅ PathTools.kt - SVGパス解析
- ✅ DesktopUI.kt - UIコンポーネント

### ゲーム機能
- ✅ フルスクリーンモード対応
- ✅ F11キーでフルスクリーン切り替え
- ✅ Escキーで終了
- ✅ マウス/タッチ操作による宇宙船制御
- ✅ オートパイロットモード
- ✅ 惑星探索とランディング
- ✅ リアルタイム物理シミュレーション

### スクリーンセーバー機能
- ✅ コマンドライン引数対応（`/s`, `/c`, `--help`）
- ✅ スクリーンセーバーモード（自動フルスクリーン + オートパイロット）
- ✅ マウス/キーボード入力による自動終了
- ✅ 設定ダイアログ表示

### パッケージング
- ✅ Windows .exeファイル生成成功
- ✅ インストーラー形式で配布
- ✅ JVMランタイム埋め込み（ポータブル動作）

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

# ヘルプ表示
.\gradlew.bat run --args="--help"

# ポータブル版の生成（推奨）- フォルダ形式
.\gradlew.bat createDistributable

# Windows用.exeインストーラーの生成
.\gradlew.bat packageExe

# Windows用MSIインストーラーの生成
.\gradlew.bat packageMsi
```

生成されたファイルは以下に配置されます：
- **ポータブル版（ZIP）**: `build\compose\binaries\main\app\Landroid\`
  - このフォルダをそのままコピーして使用可能
  - `Landroid.exe`を実行（JVMランタイム埋め込み済み）
- **インストーラー版(.exe)**: `build\compose\binaries\main\exe\Landroid-1.0.0.exe`
- **インストーラー版(.msi)**: `build\compose\binaries\main\msi\Landroid-1.0.0.msi`

### コマンドライン引数

```powershell
# 通常ゲームモード
Landroid.exe

# スクリーンセーバーモード（フルスクリーン + オートパイロット）
Landroid.exe /s

# 設定ダイアログ表示
Landroid.exe /c

# ヘルプメッセージ表示
Landroid.exe --help
```

### キーボード操作

**通常ゲームモード:**
- `F11` - フルスクリーン切り替え
- `ESC` - 終了
- `AUTO`ボタン - オートパイロットON/OFF
- マウス操作 - 宇宙船の制御

**スクリーンセーバーモード:**
- 任意のキー/マウス移動 - 終了（標準的なスクリーンセーバー動作）

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

## 既知の問題

- ⚠️ `forEachGesture()`の非推奨警告（機能には影響なし）
- 今後`awaitEachGesture()`への移行を推奨

## 今後の改善案

1. アイコンファイルの追加（現在コメントアウト済み）
2. プレビューモード(`/p`)の実装
3. より詳細な設定ダイアログ
4. マルチモニター対応の改善
5. 非推奨APIの更新

## ライセンス

元のAndroid Easter Eggsプロジェクトのライセンスに準拠します。

## クレジット

- 元コード: Android Open Source Project
- デスクトップ移植: このプロジェクト

