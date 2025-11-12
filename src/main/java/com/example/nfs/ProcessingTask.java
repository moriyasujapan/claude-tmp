package com.example.nfs;

/**
 * 処理タスクを表すクラス
 */
public class ProcessingTask {
    private String filePath;
    private String taskId;

    public ProcessingTask() {
    }

    public ProcessingTask(String taskId, String filePath) {
        this.taskId = taskId;
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
