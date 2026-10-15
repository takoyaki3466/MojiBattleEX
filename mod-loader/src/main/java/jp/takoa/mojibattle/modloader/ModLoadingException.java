package jp.takoa.mojibattle.modloader;

/** MODの発見、検証、依存解決、エントリーポイント生成失敗を表す。 */
public final class ModLoadingException extends RuntimeException {
    public ModLoadingException(String message) {
        super(message);
    }

    public ModLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
