# NFS File Distributor

NFSで共有されているディレクトリからファイルを読み込んで、複数の宛先に分散するJavaプログラムです。
書き込み側（FileDistributor）と読み込み側（FileReader）の両方を提供します。

## 機能

### FileDistributor（書き込み側）
- NFSマウントされたディレクトリから複数のファイルを一括読み込み
- 複数の宛先ディレクトリへの並列分散処理
- 2つの分散戦略をサポート
  - **ラウンドロビン方式**: ファイルを順番に各宛先に割り当て
  - **ハッシュベース方式**: ファイル名のハッシュ値を使用して分散先を決定
- マルチスレッド処理による高速化
- サブディレクトリの再帰的処理に対応

### FileReader（読み込み側）
- 分散された複数のディレクトリからファイルを読み込み
- 各ファイルをOpen/Closeする処理を並列実行
- オプションでファイル内容の検証が可能
- 処理時間とバイト数の統計情報を出力

## 必要要件

- Java 11以上
- Maven 3.6以上（ビルド時）

## ビルド方法

```bash
mvn clean package
```

このコマンドで、`target`ディレクトリに以下のJARファイルが生成されます:
- `nfs-file-distributor-1.0.0.jar` - 通常のJAR
- `nfs-file-distributor-1.0.0-jar-with-dependencies.jar` - 依存関係を含むJAR

## 設定

### FileDistributor（書き込み側）の設定

`config.properties`ファイルを作成して、以下のパラメータを設定します:

```properties
# NFSマウントされたソースディレクトリのパス
source.directory=/mnt/nfs/source

# 分散先ディレクトリのリスト（カンマ区切り）
destination.directories=/mnt/dest1,/mnt/dest2,/mnt/dest3

# 分散戦略（roundrobin または hash）
distribution.strategy=roundrobin
```

サンプル設定ファイル `config.properties.example` を参考にしてください。

```bash
cp config.properties.example config.properties
# config.propertiesを環境に合わせて編集
```

### FileReader（読み込み側）の設定

`config-reader.properties`ファイルを作成して、以下のパラメータを設定します:

```properties
# 読み込み元ディレクトリのリスト（カンマ区切り）
source.directories=/mnt/dest1,/mnt/dest2,/mnt/dest3

# ファイル内容の検証を行うかどうか
# true: ファイルの全内容を読み込んで検証
# false: ファイルをオープン・クローズのみ（高速）
validate.files=false
```

サンプル設定ファイル `config-reader.properties.example` を参考にしてください。

```bash
cp config-reader.properties.example config-reader.properties
# config-reader.propertiesを環境に合わせて編集
```

## 実行方法

### FileDistributor（書き込み側）の実行

#### 方法1: Mavenを使用

```bash
mvn exec:java -Dexec.mainClass="com.example.nfs.FileDistributor"
```

#### 方法2: JARファイルを直接実行

```bash
java -cp target/nfs-file-distributor-1.0.0-jar-with-dependencies.jar com.example.nfs.FileDistributor
```

### FileReader（読み込み側）の実行

#### 方法1: Mavenを使用

```bash
mvn exec:java -Dexec.mainClass="com.example.nfs.FileReader"
```

#### 方法2: JARファイルを直接実行

```bash
java -cp target/nfs-file-distributor-1.0.0-jar-with-dependencies.jar com.example.nfs.FileReader
```

## 使用例

### FileDistributor（書き込み側）の使用例

#### 例1: ラウンドロビン方式での分散

```properties
source.directory=/mnt/nfs/data
destination.directories=/mnt/server1/data,/mnt/server2/data,/mnt/server3/data
distribution.strategy=roundrobin
```

この設定では、ソースディレクトリ内のファイルが順番に3つの宛先サーバーに分散されます:
- ファイル1 → /mnt/server1/data
- ファイル2 → /mnt/server2/data
- ファイル3 → /mnt/server3/data
- ファイル4 → /mnt/server1/data
- ...

#### 例2: ハッシュベース方式での分散

```properties
source.directory=/mnt/nfs/data
destination.directories=/mnt/server1/data,/mnt/server2/data,/mnt/server3/data
distribution.strategy=hash
```

この設定では、ファイル名のハッシュ値を使用して分散先が決定されます。
同じファイル名は常に同じ宛先に振り分けられます。

### FileReader（読み込み側）の使用例

#### 例1: 分散されたファイルの読み込み（検証なし）

```properties
source.directories=/mnt/server1/data,/mnt/server2/data,/mnt/server3/data
validate.files=false
```

この設定では、3つのディレクトリからすべてのファイルをスキャンし、各ファイルをOpen/Closeします。
ファイル内容の読み込みは行わないため、高速に処理できます。

#### 例2: ファイル内容の検証付き読み込み

```properties
source.directories=/mnt/server1/data,/mnt/server2/data,/mnt/server3/data
validate.files=true
```

この設定では、各ファイルの全内容を読み込んで検証します。
ファイルが正しく読み込めるか、破損していないかを確認できます。

## プロジェクト構造

```
.
├── pom.xml                               # Maven設定ファイル
├── config.properties.example             # 書き込み側設定ファイルのサンプル
├── config-reader.properties.example      # 読み込み側設定ファイルのサンプル
├── README.md                             # このファイル
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── nfs/
                        ├── FileDistributor.java                  # 書き込み側メインクラス
                        ├── FileReader.java                       # 読み込み側メインクラス
                        ├── DistributionStrategy.java            # 分散戦略インターフェース
                        ├── RoundRobinDistributionStrategy.java  # ラウンドロビン実装
                        └── HashDistributionStrategy.java        # ハッシュベース実装
```

## 動作の仕組み

### FileDistributor（書き込み側）

1. **ファイルスキャン**: ソースディレクトリを再帰的にスキャンして、すべてのファイルをリストアップ
2. **分散先の決定**: 選択された戦略に基づいて各ファイルの分散先を決定
3. **並列コピー**: マルチスレッドを使用して複数のファイルを並列にコピー
4. **ディレクトリ構造の保持**: ソースディレクトリの構造を宛先でも維持
5. **結果レポート**: 成功・失敗したファイル数をログに出力

### FileReader（読み込み側）

1. **ディレクトリスキャン**: 複数の分散先ディレクトリを再帰的にスキャン
2. **ファイルリストの作成**: すべてのディレクトリからファイルリストを集約
3. **並列読み込み**: マルチスレッドを使用して複数のファイルを並列にOpen/Close
4. **オプション検証**: validate.files=trueの場合、ファイル内容を全て読み込んで検証
5. **統計レポート**: 処理ファイル数、総バイト数、処理時間をログに出力

## ログ

処理の進捗状況はJava標準のロギング機能を使用して出力されます。
ログレベルを変更する場合は、JVMの起動オプションで設定してください:

```bash
java -Djava.util.logging.config.file=logging.properties -jar target/nfs-file-distributor-1.0.0-jar-with-dependencies.jar
```

## 注意事項

- NFSディレクトリは事前にマウントしておく必要があります
- 宛先ディレクトリへの書き込み権限が必要です
- 大量のファイルを処理する場合、十分なディスク容量を確保してください
- 同名ファイルが既に存在する場合は上書きされます

## カスタマイズ

新しい分散戦略を追加するには:

1. `DistributionStrategy`インターフェースを実装した新しいクラスを作成
2. `FileDistributor.java`のmainメソッド内で新しい戦略を追加

```java
case "custom":
    strategy = new CustomDistributionStrategy();
    break;
```

## ライセンス

このプロジェクトはサンプルコードです。自由に使用・改変してください。
