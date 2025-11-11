# NFS File Distributor

NFSで共有されているディレクトリからファイルを読み込んで、複数の宛先に分散するJavaプログラムです。
以下の機能を提供します：
- ファイル分散処理（FileDistributor / FileReader）
- **複数台サーバーでのXML分散処理（MasterCoordinator / WorkerServer）**

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

---

## 複数台サーバーでのXML分散処理

### 概要

複数台のサーバー間でXMLファイルの処理を分散実行する機能です。
マスターサーバー（サーバー1）から処理を起動し、ワーカーサーバー（サーバー2、3など）にタスクを配分して並列処理を行います。
すべての処理が完了すると、マスターサーバーで結果を集約して確認できます。

### アーキテクチャ

```
┌─────────────────────────────────────────────────────────┐
│  サーバー1 (マスター)                                      │
│  ┌──────────────────────────────────────────────┐       │
│  │  MasterCoordinator                            │       │
│  │  - XMLファイルのスキャン                        │       │
│  │  - タスクの配分                                 │       │
│  │  - 結果の集約・レポート                          │       │
│  │  - ローカル処理も実行                            │       │
│  └──────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────┘
           │                │
           │ HTTP/REST      │ HTTP/REST
           ▼                ▼
    ┌─────────┐      ┌─────────┐
    │サーバー2 │      │サーバー3 │
    │  (ワーカー)│      │  (ワーカー)│
    └─────────┘      └─────────┘
     WorkerServer     WorkerServer
```

### コンポーネント

#### MasterCoordinator (マスターサーバー)
- 指定されたディレクトリからXMLファイルをスキャン
- 各XMLファイルを処理タスクとして生成
- ラウンドロビン方式でワーカーサーバーにタスクを配分
- マスター自身も処理に参加（効率的なリソース活用）
- すべてのタスクの完了を待機
- 処理結果を集約してレポート出力

#### WorkerServer (ワーカーサーバー)
- REST APIエンドポイントを提供
- マスターからタスクを受信
- XMLファイルの処理を実行
- 処理結果をマスターに返却

#### XMLProcessor
- 実際のXML処理ロジック
- XMLファイルのパース
- 要素数のカウント
- ビジネスロジックの実行

### 設定方法

#### マスターサーバーの設定

`config-master.properties`ファイルを作成:

```properties
# XMLファイルが格納されているディレクトリ
source.directory=/mnt/nfs/xml-files

# ワーカーサーバーのURLリスト（カンマ区切り）
worker.urls=http://server2:8080,http://server3:8080
```

サンプルファイルをコピーして編集:
```bash
cp config-master.properties.example config-master.properties
# config-master.propertiesを環境に合わせて編集
```

#### ワーカーサーバーの設定

各ワーカーサーバーで`config-worker.properties`ファイルを作成:

```properties
# ワーカーサーバーのポート番号
worker.port=8080
```

サンプルファイルをコピーして編集:
```bash
cp config-worker.properties.example config-worker.properties
# config-worker.propertiesを環境に合わせて編集
```

### 実行方法

#### 1. ビルド

まず、すべてのサーバーでプロジェクトをビルドします:

```bash
mvn clean package
```

#### 2. ワーカーサーバーの起動

サーバー2とサーバー3で、それぞれワーカーサーバーを起動します:

```bash
# サーバー2で実行
java -cp target/nfs-file-distributor-1.0.0-jar-with-dependencies.jar com.example.nfs.WorkerServer

# サーバー3で実行
java -cp target/nfs-file-distributor-1.0.0-jar-with-dependencies.jar com.example.nfs.WorkerServer
```

ワーカーサーバーが起動すると、以下のようなログが表示されます:
```
情報: ワーカーサーバーを起動しました: ポート 8080
情報: ワーカーサーバーが稼働中です。終了するには Ctrl+C を押してください。
```

#### 3. マスターサーバーから処理を実行

サーバー1（マスター）で分散処理を起動します:

```bash
# サーバー1で実行
java -cp target/nfs-file-distributor-1.0.0-jar-with-dependencies.jar com.example.nfs.MasterCoordinator
```

処理が完了すると、以下のようなレポートが表示されます:

```
情報: ========================================
情報: 分散処理が完了しました
情報: ========================================
情報: 処理ファイル数: 100
情報: 成功: 98
情報: 失敗: 2
情報: 合計XML要素数: 15420
情報: 合計処理時間: 3540 ms
情報: 全体経過時間: 1250 ms
情報: 使用ワーカー数: 2 (リモート) + 1 (ローカル)
情報: ========================================
```

### 使用例

#### 例1: 3台のサーバーでXMLファイルを分散処理

**前提条件:**
- サーバー1、2、3が互いに通信可能
- `/mnt/nfs/xml-files`ディレクトリがNFSでマウント済み
- 100個のXMLファイルが格納されている

**設定 (サーバー1の config-master.properties):**
```properties
source.directory=/mnt/nfs/xml-files
worker.urls=http://server2:8080,http://server3:8080
```

**実行フロー:**
1. サーバー2、3でワーカーを起動
2. サーバー1からマスターコーディネーターを起動
3. 100個のXMLファイルがラウンドロビンで配分:
   - ファイル1 → サーバー1（ローカル処理）
   - ファイル2 → サーバー2
   - ファイル3 → サーバー3
   - ファイル4 → サーバー1
   - ...
4. すべての処理完了後、サーバー1で結果を確認

#### 例2: テスト実行（サンプルXMLファイルを使用）

プロジェクトにはサンプルXMLファイルが含まれています:

```bash
# テスト用設定
cat > config-master.properties <<EOF
source.directory=./sample-xml-files
worker.urls=http://localhost:8081,http://localhost:8082
EOF

# ターミナル1: ワーカー1を起動
cat > config-worker.properties <<EOF
worker.port=8081
EOF
java -cp target/nfs-file-distributor-1.0.0-jar-with-dependencies.jar com.example.nfs.WorkerServer

# ターミナル2: ワーカー2を起動
cat > config-worker.properties <<EOF
worker.port=8082
EOF
java -cp target/nfs-file-distributor-1.0.0-jar-with-dependencies.jar com.example.nfs.WorkerServer

# ターミナル3: マスターを実行
java -cp target/nfs-file-distributor-1.0.0-jar-with-dependencies.jar com.example.nfs.MasterCoordinator
```

### プロジェクト構造（分散処理関連）

```
.
├── src/main/java/com/example/nfs/
│   ├── MasterCoordinator.java        # マスターサーバー（タスク配分・結果集約）
│   ├── WorkerServer.java             # ワーカーサーバー（REST API）
│   ├── XMLProcessor.java             # XML処理ロジック
│   ├── ProcessingTask.java           # タスク定義
│   └── ProcessingResult.java         # 処理結果
├── config-master.properties.example   # マスター設定サンプル
├── config-worker.properties.example   # ワーカー設定サンプル
└── sample-xml-files/                  # サンプルXMLファイル
    ├── sample1.xml
    ├── sample2.xml
    └── sample3.xml
```

### カスタマイズ

#### XML処理ロジックのカスタマイズ

`XMLProcessor.java`の`processXMLFile`メソッドに独自の処理ロジックを追加できます:

```java
public ProcessingResult processXMLFile(ProcessingTask task) {
    // ... 既存のコード ...

    // カスタム処理の例:
    // - 特定の要素を抽出
    NodeList items = document.getElementsByTagName("item");

    // - データの変換
    // - 検証ルールの適用
    // - データベースへの保存

    // ... 結果を返す ...
}
```

### トラブルシューティング

#### ワーカーサーバーに接続できない

- ファイアウォール設定を確認
- ワーカーサーバーが起動しているか確認
- URLとポート番号が正しいか確認

```bash
# ワーカーのヘルスチェック
curl http://server2:8080/health
# 期待される応答: {"status": "ok"}
```

#### XMLファイルが見つからない

- `source.directory`のパスが正しいか確認
- NFSマウントが正常か確認
- ファイルの読み込み権限があるか確認

```bash
# ディレクトリの確認
ls -la /mnt/nfs/xml-files
```

### パフォーマンスチューニング

#### スレッドプールのサイズ調整

デフォルトではCPUコア数に応じたスレッドプールを使用します。
調整する場合は、以下のコードを変更:

```java
// MasterCoordinator.java または WorkerServer.java
this.executorService = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() * 2  // 2倍に増やす例
);
```

#### タイムアウトの調整

HTTP通信のタイムアウトを調整:

```java
// MasterCoordinator.java
this.httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(30))  // 接続タイムアウト
    .build();

// リクエストタイムアウト
HttpRequest request = HttpRequest.newBuilder()
    .timeout(Duration.ofMinutes(10))  // リクエストタイムアウト
    .build();
```

---

## ライセンス

このプロジェクトはサンプルコードです。自由に使用・改変してください。
