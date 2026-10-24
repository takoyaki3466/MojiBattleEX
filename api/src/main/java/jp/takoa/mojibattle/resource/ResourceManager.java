package jp.takoa.mojibattle.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * MODやゲーム本体のリソースを物理ファイルへ変換せずに読み出す公開契約。
 */
public interface ResourceManager {
    /**
     * 指定IDのリソースを開く。返されたstreamの所有権は呼出し側にある。
     *
     * @throws IOException リソースが存在しない、または読込めない場合
     */
    InputStream open(ResourceKind kind, ResourceId id) throws IOException;

    boolean exists(ResourceKind kind, ResourceId id);

    /**
     * 指定名前空間・接頭辞にあるリソースIDを辞書順で返す。
     * データ駆動コンテンツの列挙に使い、返された一覧は変更できない。
     */
    List<ResourceId> list(ResourceKind kind, String namespace, String pathPrefix) throws IOException;
}
