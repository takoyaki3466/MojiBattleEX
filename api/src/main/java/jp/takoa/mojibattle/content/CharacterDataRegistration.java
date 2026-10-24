package jp.takoa.mojibattle.content;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.stream.Stream;

/**
 * 1キャラクターと装備Abilityを構成するデータリソース一式。
 *
 * @param character            キャラクター定義JSONのリソース参照ID（Registry登録IDではない）
 * @param skill                スキル定義JSONのリソース参照ID（Registry登録IDではない）
 * @param ultimate             必殺技定義JSONのリソース参照ID（Registry登録IDではない）
 * @param passive              パッシブ定義JSONのリソース参照ID（Registry登録IDではない）
 * @param translationDirectory 翻訳JSONを置くASSET相対ディレクトリ
 */
public record CharacterDataRegistration(ResourceId character, ResourceId skill, ResourceId ultimate, ResourceId passive,
                                        String translationDirectory) {

    public CharacterDataRegistration {
        if (character == null || skill == null || ultimate == null || passive == null) {
            throw new IllegalArgumentException("Character data resources are required");
        }

        String namespace = character.namespace();

        if (Stream.of(skill, ultimate, passive).anyMatch(id -> !namespace.equals(id.namespace()))) {
            throw new IllegalArgumentException("Character data resources must share one namespace");
        }

        if (translationDirectory == null || translationDirectory.isBlank()) {
            throw new IllegalArgumentException("Translation directory is required");
        }

        new ResourceId(namespace, translationDirectory + "/validation.json");
    }

    public String namespace() {
        return character.namespace();
    }

    /**
     * 指定言語に対応する翻訳JSONのResource IDを返す。
     */
    public ResourceId translation(String language) {
        return new ResourceId(namespace(), translationDirectory + "/" + language + ".json");
    }
}
