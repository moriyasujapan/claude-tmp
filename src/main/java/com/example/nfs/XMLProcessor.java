package com.example.nfs;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XMLファイルを処理するクラス
 * 実際のXML処理ロジックを実装
 */
public class XMLProcessor {
    private static final Logger logger = Logger.getLogger(XMLProcessor.class.getName());

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

            // ここに実際のビジネスロジックを実装
            // 例：特定の要素を抽出、データを変換、検証など
            logger.info(String.format("XMLファイルを処理しました: %s (要素数: %d)",
                    xmlFile.getName(), elementCount));

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
}
