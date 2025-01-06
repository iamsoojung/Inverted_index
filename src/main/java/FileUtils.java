import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {
    /**
     * 입력 파일 읽어서, 라인 단위로 반환함
     * @param filePath
     * @return 라인 단위 텍스트
     */
    public static List<String> readInputFile(String filePath) {
        try {
            try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
                return lines.collect(Collectors.toList());
            }
        } catch (IOException e) {
            System.err.println("파일이 없거나 읽을 수 없네요...ㅠ : " + e.getMessage());
        }
        return null;
    }

    /**
     * 정렬된 역색인 결과값을, 결과 파일로 쓰기
     * @param filePath
     * @param sortedInvertedIndex
     */
    public static void writeOutputFile(String filePath, List<String> sortedInvertedIndex) throws IOException {
        Files.write(Paths.get(filePath), sortedInvertedIndex);
    }
}
