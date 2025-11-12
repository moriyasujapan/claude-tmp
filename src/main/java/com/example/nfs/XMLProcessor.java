package com.example.nfs;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XMLファイルを処理するクラス
 * 実際のXML処理ロジックを実装
 * HikariCPを使用してDB接続を行う
 */
public class XMLProcessor {
    private static final Logger logger = Logger.getLogger(XMLProcessor.class.getName());

    private final DatabaseConnectionPool connectionPool;
    private final boolean enableDatabase;

    /**
     * コンストラクタ（DB接続あり）
     */
    public XMLProcessor(DatabaseConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        this.enableDatabase = true;
    }

    /**
     * コンストラクタ（DB接続なし）
     */
    public XMLProcessor() {
        this.connectionPool = null;
        this.enableDatabase = false;
    }

    /**
     * XMLファイルを処理する
     *
     * @param task 処理タスク
     * @return 処理結果
     */
    public ProcessingResult processXMLFile(ProcessingTask task) {
        ProcessingResult result = new ProcessingResult(task.getTaskId(), task.getFilePath());
        long startTime = System.currentTimeMillis();

        try {
            File xmlFile = new File(task.getFilePath());

            if (!xmlFile.exists()) {
                result.setSuccess(false);
                result.setErrorMessage("ファイルが存在しません: " + task.getFilePath());
                return result;
            }

            if (!xmlFile.canRead()) {
                result.setSuccess(false);
                result.setErrorMessage("ファイルの読み込み権限がありません: " + task.getFilePath());
                return result;
            }

            // XMLファイルをパース
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);
            document.getDocumentElement().normalize();

            // 全要素数をカウント
            NodeList allElements = document.getElementsByTagName("*");
            int elementCount = allElements.getLength();

            // ルート要素を取得
            Element rootElement = document.getDocumentElement();
            String rootTagName = rootElement.getTagName();

            // ここに実際のビジネスロジックを実装
            // 例：特定の要素を抽出、データを変換、検証など
            logger.info(String.format("XMLファイルを処理しました: %s (要素数: %d, ルート: %s)",
                    xmlFile.getName(), elementCount, rootTagName));

            // データベースに保存（有効な場合のみ）
            if (enableDatabase && connectionPool != null) {
                saveToDatabase(task.getTaskId(), xmlFile.getName(),
                              task.getFilePath(), elementCount, rootTagName);
            }

            result.setSuccess(true);
            result.setElementCount(elementCount);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "XMLファイルの処理に失敗しました: " + task.getFilePath(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        } finally {
            long endTime = System.currentTimeMillis();
            result.setProcessingTimeMs(endTime - startTime);
        }

        return result;
    }

    /**
     * 処理結果をデータベースに保存
     * サンプル実装：xml_processing_historyテーブルに保存
     */
    private void saveToDatabase(String taskId, String fileName, String filePath,
                                int elementCount, String rootTagName) {
        String sql = "INSERT INTO xml_processing_history " +
                    "(task_id, file_name, file_path, element_count, root_tag_name, processed_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionPool.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, taskId);
            pstmt.setString(2, fileName);
            pstmt.setString(3, filePath);
            pstmt.setInt(4, elementCount);
            pstmt.setString(5, rootTagName);
            pstmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));

            int rowsAffected = pstmt.executeUpdate();
            logger.fine(String.format("データベースに保存しました: %s (%d行)", fileName, rowsAffected));

        } catch (SQLException e) {
            logger.log(Level.WARNING, "データベースへの保存に失敗しました: " + fileName, e);
            // DB保存失敗はXML処理全体の失敗とはしない（ログ出力のみ）
        }
    }

    /**
     * サンプル: XMLデータを別のテーブルに保存する例
     * 実際のビジネスロジックに応じてカスタマイズしてください
     */
    private void saveXMLDataToDatabase(Document document, String taskId) throws SQLException {
        // 例：bookタグのデータをxml_booksテーブルに保存
        NodeList books = document.getElementsByTagName("book");

        String sql = "INSERT INTO xml_books (task_id, book_id, author, title, genre, price) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionPool.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < books.getLength(); i++) {
                Element book = (Element) books.item(i);

                String bookId = book.getAttribute("id");
                String author = getElementTextContent(book, "author");
                String title = getElementTextContent(book, "title");
                String genre = getElementTextContent(book, "genre");
                String priceStr = getElementTextContent(book, "price");

                pstmt.setString(1, taskId);
                pstmt.setString(2, bookId);
                pstmt.setString(3, author);
                pstmt.setString(4, title);
                pstmt.setString(5, genre);
                pstmt.setDouble(6, priceStr != null ? Double.parseDouble(priceStr) : 0.0);

                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();
            logger.info(String.format("バッチ挿入完了: %d件", results.length));
        }
    }

    /**
     * 要素のテキストコンテンツを取得
     */
    private String getElementTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }
}
