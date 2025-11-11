package com.example.nfs;

import java.nio.file.Path;
import java.util.List;

/**
 * ラウンドロビン方式でファイルを分散する戦略
 * ファイルを順番に各宛先に割り当てる
 */
public class RoundRobinDistributionStrategy implements DistributionStrategy {

    @Override
    public Path selectDestination(Path file, List<Path> destinations, int index) {
        if (destinations == null || destinations.isEmpty()) {
            throw new IllegalArgumentException("分散先のリストが空です");
        }

        // インデックスを使用してラウンドロビンで分散先を選択
        int destinationIndex = index % destinations.size();
        return destinations.get(destinationIndex);
    }
}
