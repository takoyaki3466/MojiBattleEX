package jp.takoa.mojibattle.crash;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * レポート本文を {@code yyyy-MMdd-HHmmss.txt} 形式のファイルとして書き出す。
 *
 * <p>同じ秒に複数のレポートが作られた場合は {@code -2}, {@code -3} … を付けて既存ファイルを上書きしない。
 * 保存先へ書き込めない場合は次の候補へ順に切り替える。</p>
 */
public final class CrashReportWriter {
    static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MMdd-HHmmss");
    private static final int MAX_SAME_SECOND_FILES = 1000;

    private CrashReportWriter() {
    }

    /**
     * 発生日時から拡張子付きの基本ファイル名を返す。
     */
    public static String fileName(ZonedDateTime timestamp) {
        return FILE_NAME_FORMAT.format(timestamp) + ".txt";
    }

    /**
     * 候補ディレクトリへ順に書き込みを試みる。例外は送出しない。
     *
     * @param candidates 試行順の保存先。先頭が通常の保存先
     * @param timestamp  ファイル名に使うローカル日時
     * @param content    レポート本文
     * @return 書き込み結果
     */
    public static Result write(List<Path> candidates, ZonedDateTime timestamp, String content) {
        return write(candidates, timestamp, (directory, fallback, failures) -> content);
    }

    /**
     * 保存先ごとに本文を生成しながら候補ディレクトリへ順に書き込みを試みる。例外は送出しない。
     * 本文へ実際の保存先やフォールバックの理由を記録するために使う。
     */
    public static Result write(List<Path> candidates, ZonedDateTime timestamp, ContentSource contentSource) {
        List<String> failures = new ArrayList<>();
        String baseName = FILE_NAME_FORMAT.format(timestamp);
        for (int index = 0; index < candidates.size(); index++) {
            Path directory = candidates.get(index);
            try {
                Files.createDirectories(directory);
                String content = contentSource.content(directory, index > 0, List.copyOf(failures));
                Path written = createUnique(directory, baseName, content);
                return new Result(written, index > 0, List.copyOf(failures));
            } catch (IOException | RuntimeException error) {
                failures.add(directory + " -> " + error.getClass().getSimpleName() + ": " + error.getMessage());
            }
        }
        return new Result(null, true, List.copyOf(failures));
    }

    private static Path createUnique(Path directory, String baseName, String content) throws IOException {
        for (int suffix = 1; suffix <= MAX_SAME_SECOND_FILES; suffix++) {
            String name = suffix == 1 ? baseName + ".txt" : baseName + "-" + suffix + ".txt";
            Path target = directory.resolve(name);
            try {
                // CREATE_NEWにより、存在確認と作成を不可分に行い、既存ファイルを上書きしない。
                Files.writeString(target, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return target;
            } catch (FileAlreadyExistsException exists) {
                // 同じ秒のレポートが既にあるため、次の番号を試す。
            }
        }
        throw new IOException("Too many crash reports for " + baseName + " in " + directory);
    }

    /**
     * 保存先に応じたレポート本文を返す。
     */
    @FunctionalInterface
    public interface ContentSource {
        String content(Path directory, boolean fallback, List<String> previousFailures);
    }

    /**
     * @param file         書き込んだファイル。全候補で失敗した場合はnull
     * @param usedFallback 通常の保存先以外へ書き込んだ、または全て失敗した場合true
     * @param failures     失敗した保存先と理由
     */
    public record Result(Path file, boolean usedFallback, List<String> failures) {
        public boolean succeeded() {
            return file != null;
        }
    }
}
