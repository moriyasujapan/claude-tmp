# NFS File Distributor

NFSで共有されているディレクトリからファイルを読み込んで、複数の宛先に分散するJavaプログラムです。

## 機能

- NFSマウントされたディレクトリから複数のファイルを一括読み込み
- 複数の宛先ディレクトリへの並列分散処理
- 2つの分散戦略をサポート
  - **ラウンドロビン方式**: ファイルを順番に各宛先に割り当て
  - **ハッシュベース方式**: ファイル名のハッシュ値を使用して分散先を決定
- マルチスレッド処理による高速化
- サブディレクトリの再帰的処理に対応

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

## 実行方法

### 方法1: Mavenを使用

```bash
mvn exec:java -Dexec.mainClass="com.example.nfs.FileDistributor"
```

### 方法2: JARファイルを直接実行

```bash
java -jar target/nfs-file-distributor-1.0.0-jar-with-dependencies.jar
```

## 使用例

### 例1: ラウンドロビン方式での分散

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

### 例2: ハッシュベース方式での分散

```properties
source.directory=/mnt/nfs/data
destination.directories=/mnt/server1/data,/mnt/server2/data,/mnt/server3/data
distribution.strategy=hash
```

この設定では、ファイル名のハッシュ値を使用して分散先が決定されます。
同じファイル名は常に同じ宛先に振り分けられます。

## プロジェクト構造

```
.
├── pom.xml                          # Maven設定ファイル
├── config.properties.example        # 設定ファイルのサンプル
├── README.md                        # このファイル
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── nfs/
                        ├── FileDistributor.java                  # メインクラス
                        ├── DistributionStrategy.java            # 分散戦略インターフェース
                        ├── RoundRobinDistributionStrategy.java  # ラウンドロビン実装
                        └── HashDistributionStrategy.java        # ハッシュベース実装
```

## 動作の仕組み

1. **ファイルスキャン**: ソースディレクトリを再帰的にスキャンして、すべてのファイルをリストアップ
2. **分散先の決定**: 選択された戦略に基づいて各ファイルの分散先を決定
3. **並列コピー**: マルチスレッドを使用して複数のファイルを並列にコピー
4. **ディレクトリ構造の保持**: ソースディレクトリの構造を宛先でも維持
5. **結果レポート**: 成功・失敗したファイル数をログに出力

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
