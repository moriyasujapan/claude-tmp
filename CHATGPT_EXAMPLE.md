# ChatGPT API サンプル

JavaでOpenAI ChatGPT APIを使用して質問と回答を取得するサンプルコードです。

## 必要なもの

- Java 11以上
- Maven
- OpenAI APIキー（https://platform.openai.com/api-keys で取得）

## ファイル構成

```
src/main/java/com/example/chatgpt/
├── ChatGPTClient.java    # ChatGPT APIクライアント
└── ChatGPTExample.java   # 使用例
```

## 主な機能

### ChatGPTClient

OpenAI Chat Completions APIを使用してChatGPTと対話するクライアントクラス。

**主なメソッド:**

- `sendMessage(String message)` - シンプルな質問を送信
- `sendMessageWithSystem(String systemPrompt, String userMessage)` - システムプロンプト付きで質問を送信

**コンストラクタ:**

- `ChatGPTClient(String apiKey)` - デフォルトモデル（gpt-3.5-turbo）を使用
- `ChatGPTClient(String apiKey, String model)` - モデルを指定（例: "gpt-4", "gpt-3.5-turbo"）

## 使用方法

### 1. APIキーの設定

```bash
export OPENAI_API_KEY="your-api-key-here"
```

### 2. プロジェクトのビルド

```bash
mvn clean compile
```

### 3. サンプルの実行

```bash
mvn exec:java -Dexec.mainClass="com.example.chatgpt.ChatGPTExample"
```

## サンプルコード

### 基本的な使用例

```java
import com.example.chatgpt.ChatGPTClient;

public class SimpleExample {
    public static void main(String[] args) throws Exception {
        // APIキーを指定してクライアントを作成
        String apiKey = System.getenv("OPENAI_API_KEY");
        ChatGPTClient client = new ChatGPTClient(apiKey);

        // 質問を送信
        String answer = client.sendMessage("Javaとは何ですか？");
        System.out.println(answer);
    }
}
```

### システムプロンプトを使用する例

```java
import com.example.chatgpt.ChatGPTClient;

public class SystemPromptExample {
    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        ChatGPTClient client = new ChatGPTClient(apiKey);

        // システムプロンプトで役割を指定
        String systemPrompt = "あなたは親切なプログラミングの先生です。";
        String answer = client.sendMessageWithSystem(
            systemPrompt,
            "例外処理について教えてください。"
        );
        System.out.println(answer);
    }
}
```

### GPT-4を使用する例

```java
import com.example.chatgpt.ChatGPTClient;

public class GPT4Example {
    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");

        // GPT-4を指定（APIキーがGPT-4アクセス権を持つ場合）
        ChatGPTClient client = new ChatGPTClient(apiKey, "gpt-4");

        String answer = client.sendMessage("再帰関数とは何ですか？");
        System.out.println(answer);
    }
}
```

## 利用可能なモデル

- `gpt-3.5-turbo` - 高速で低コスト（デフォルト）
- `gpt-4` - より高精度（APIキーに権限が必要）
- `gpt-4-turbo` - GPT-4の高速版
- その他、OpenAIが提供する最新モデル

詳細は [OpenAI API documentation](https://platform.openai.com/docs/models) を参照してください。

## エラーハンドリング

```java
try {
    String answer = client.sendMessage("質問内容");
    System.out.println(answer);
} catch (Exception e) {
    System.err.println("エラー: " + e.getMessage());
    e.printStackTrace();
}
```

## 注意事項

- APIキーは環境変数やセキュアな設定ファイルで管理してください
- APIの使用には料金が発生します（従量課金制）
- レート制限に注意してください
- 本番環境では適切なエラーハンドリングとリトライ処理を実装してください

## 料金について

OpenAI APIは使用したトークン数に応じて課金されます。最新の料金情報は [OpenAI Pricing](https://openai.com/pricing) を参照してください。

## 参考リンク

- [OpenAI API Documentation](https://platform.openai.com/docs/)
- [Chat Completions API Guide](https://platform.openai.com/docs/guides/chat)
- [OpenAI API Keys](https://platform.openai.com/api-keys)
