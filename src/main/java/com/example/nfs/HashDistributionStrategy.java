package com.example.nfs;

import java.nio.file.Path;
import java.util.List;

/**
 * ハッシュベースでファイルを分散する戦略
 * ファイル名のハッシュ値を使用して分散先を決定
 */
public class HashDistributionStrategy implements DistributionStrategy {

    @Override
    public Path selectDestination(Path file, List<Path> destinations, int index) {
        if (destinations == null || destinations.isEmpty()) {
            throw new IllegalArgumentException("分散先のリストが空です");
        }

        // ファイル名のハッシュ値を計算
        String fileName = file.getFileName().toString();
        int hashCode = Math.abs(fileName.hashCode());

        // ハッシュ値を使用して分散先を選択
        int destinationIndex = hashCode % destinations.size();
        return destinations.get(destinationIndex);
    }
}
