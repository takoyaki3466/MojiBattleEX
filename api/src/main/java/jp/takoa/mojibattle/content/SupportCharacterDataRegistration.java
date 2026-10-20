package jp.takoa.mojibattle.content;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 補助キャラクターJSONと翻訳辞書の登録単位。
 * @param support MOD自身の名前空間にある補助DATAリソース参照ID（Registry登録IDではない）
 * @param translationDirectory 言語名JSONを置くASSET相対ディレクトリ
 */
public record SupportCharacterDataRegistration(ResourceId support, String translationDirectory) {
    public SupportCharacterDataRegistration {
        Objects.requireNonNull(support, "support");
        if (translationDirectory == null || translationDirectory.isBlank()) {
            throw new IllegalArgumentException("translationDirectory is required");
        }
    }

    public String namespace() {
        return support.namespace();
    }

    public ResourceId translation(String language) {
        return new ResourceId(namespace(), translationDirectory + "/" + language + ".json");
    }
}
