package com.example.nfs;

/**
 * 処理結果を表すクラス
 */
public class ProcessingResult {
    private String taskId;
    private String filePath;
    private boolean success;
    private String errorMessage;
    private long processingTimeMs;
    private int elementCount;

    public ProcessingResult() {
    }

    public ProcessingResult(String taskId, String filePath) {
        this.taskId = taskId;
        this.filePath = filePath;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public int getElementCount() {
        return elementCount;
    }

    public void setElementCount(int elementCount) {
        this.elementCount = elementCount;
    }
}
