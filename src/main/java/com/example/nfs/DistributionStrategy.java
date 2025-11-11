package com.example.nfs;

import java.nio.file.Path;
import java.util.List;

/**
 * ファイル分散戦略のインターフェース
 */
public interface DistributionStrategy {
    /**
     * 指定されたファイルの分散先を選択
     *
     * @param file ファイルのパス
     * @param destinations 分散先ディレクトリのリスト
     * @param index ファイルのインデックス
     * @return 選択された分散先ディレクトリ
     */
    Path selectDestination(Path file, List<Path> destinations, int index);
}
