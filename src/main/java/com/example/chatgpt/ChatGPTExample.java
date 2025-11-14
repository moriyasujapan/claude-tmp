package com.example.chatgpt;

/**
 * ChatGPT APIの使用例
 *
 * 使用方法:
 * 1. OpenAI APIキーを環境変数 OPENAI_API_KEY に設定
 * 2. このプログラムを実行
 *
 * 環境変数の設定例:
 *   export OPENAI_API_KEY="your-api-key-here"
 *   mvn compile exec:java -Dexec.mainClass="com.example.chatgpt.ChatGPTExample"
 */
public class ChatGPTExample {

    public static void main(String[] args) {
        // 環境変数からAPIキーを取得
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("エラー: OPENAI_API_KEY 環境変数が設定されていません");
            System.err.println("使用方法: export OPENAI_API_KEY=\"your-api-key\"");
            System.exit(1);
        }

        try {
            // ChatGPTクライアントの作成
            ChatGPTClient client = new ChatGPTClient(apiKey);

            // 例1: シンプルな質問
            System.out.println("=== 例1: シンプルな質問 ===");
            String question1 = "Javaとは何ですか？簡潔に説明してください。";
            System.out.println("質問: " + question1);
            String answer1 = client.sendMessage(question1);
            System.out.println("回答: " + answer1);
            System.out.println();

            // 例2: プログラミングに関する質問
            System.out.println("=== 例2: プログラミングに関する質問 ===");
            String question2 = "JavaでHTTP通信を行う方法を教えてください。";
            System.out.println("質問: " + question2);
            String answer2 = client.sendMessage(question2);
            System.out.println("回答: " + answer2);
            System.out.println();

            // 例3: システムプロンプトを使用
            System.out.println("=== 例3: システムプロンプトを使用 ===");
            String systemPrompt = "あなたは親切なJavaプログラミングの先生です。初心者にもわかりやすく説明してください。";
            String question3 = "例外処理について教えてください。";
            System.out.println("システムプロンプト: " + systemPrompt);
            System.out.println("質問: " + question3);
            String answer3 = client.sendMessageWithSystem(systemPrompt, question3);
            System.out.println("回答: " + answer3);
            System.out.println();

            // 例4: GPT-4を使用（APIキーがGPT-4アクセス権を持つ場合）
            System.out.println("=== 例4: 特定のモデルを指定 ===");
            ChatGPTClient gpt4Client = new ChatGPTClient(apiKey, "gpt-4");
            String question4 = "再帰関数とは何ですか？";
            System.out.println("質問 (using GPT-4): " + question4);
            try {
                String answer4 = gpt4Client.sendMessage(question4);
                System.out.println("回答: " + answer4);
            } catch (Exception e) {
                System.err.println("注意: GPT-4へのアクセスにはAPIキーが対応している必要があります");
                System.err.println("エラー: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("エラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
