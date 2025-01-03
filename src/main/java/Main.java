import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 3 || !"index".equalsIgnoreCase(args[0])) {
            System.out.println("Command 틀림 ;; java <className> index <inputFile> <outputFile> !!! ");
        }

        String inputFile = args[1];
        String outputFile = args[2];

        // 입력 파일을 라인 단위로 읽기
        List<String> lines = readInputFile(inputFile);

        // 역색인 생성
        Map<String, Map<Integer, Integer>> originInvertedIndex = createInvertedIndex(lines);

        // 출력 정렬
        List<String> sortedInvertedIndex = sortInvertedIndex(originInvertedIndex);
        System.out.println(sortedInvertedIndex);

        // 출력 파일 쓰기

    }

    /**
     * 입력 파일 읽어서, 라인 단위로 반환함
     * @param filePath
     * @return
     * @throws IOException
     */
    private static List<String> readInputFile(String filePath) throws IOException {
        return Files.readAllLines(Paths.get(filePath));
    }

    /**
     * 라인 별로 문서아이디와 텍스트 분리 후, 역색인 생성
     * @param lines
     * @return
     */
    private static Map<String, Map<Integer, Integer>> createInvertedIndex (List<String> lines) {
        Map<String, Map<Integer, Integer>> invertedIndex = new HashMap<>();     // 역색인 맵

        for (String line : lines) {
            String[] parts = line.split(" ", 2);    //공백 기준으로 ID, 텍스트 분리
            if (parts.length != 2)  continue;   // 포맷 이상하면 continue

            int docId = Integer.parseInt(parts[0]);     // 문서 ID
            List<String> words = processTextByPattern(parts[1]);    // 텍스트 (단어 추출하여 분리)

            for (String word : words) {
                Map<Integer, Integer> docMap = invertedIndex.getOrDefault(word, new HashMap<>());   // 역색인에 없는 단어면 새 맵 추가
                docMap.put(docId, docMap.getOrDefault(docId, 0) + 1);   // 문서 ID의 빈도 업데이트
                invertedIndex.put(word, docMap);    // 역색인에 저장
            }
        }

        return invertedIndex;
    }

    /**
     * 입력된 텍스트를 전처리하여 단어 추출
     * (특수문자 제거 & 알파벳으로만 이루어진 단어 추출 & 소문자 변환)
     * @param text 입력된 원본 텍스트
     * @return 전처리된 단어 리스트
     */
    private static List<String> processTextByPattern(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        Pattern pattern = Pattern.compile("\\b[a-zA-Z]+\\b");   // 정규식 패턴 생성
        Matcher matcher = pattern.matcher(text.toLowerCase());  // 소문자 변환 후 정규식 매칭

        List<String> words = new ArrayList<>();
        while (matcher.find()) {    // 정규식에 일치하는 단어 찾기
            words.add(matcher.group()); // 찾은 단어 추가
        }
        return words;
    }

    /**
     * Pattern 클래스 대신 String 클래스로 전처리
     */
    private static List<String> processText(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        String[] splitWords = text.toLowerCase().split("[^a-zA-Z]+");

        List<String> words = new ArrayList<>();
        for (String word : splitWords) {
            if (!words.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

    private static List<String> sortInvertedIndex(Map<String, Map<Integer, Integer>> invertedIndex) {
        List<String> sortedInvertedIndex = new ArrayList<>();

        invertedIndex.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())     // 1) 단어 ASC
                .forEach(entry -> {
                    String word = entry.getKey();
                    Map<Integer, Integer> docFreqMap = entry.getValue();

                    StringBuilder line = new StringBuilder(word);
                    docFreqMap.entrySet().stream()
                            .sorted((e1, e2) -> {
                                int freqCompare = Integer.compare(e2.getValue(), e1.getValue());    // 2) 단어 빈도수 DESC
                                return freqCompare != 0 ? freqCompare : Integer.compare(e1.getKey(), e2.getKey());  // 3) 문서 ID ASC
                            })
                            .forEach(e -> line.append(" ").append(e.getKey()).append(" ").append(e.getValue()));
                    sortedInvertedIndex.add(line.toString());
                });

        return sortedInvertedIndex;
    }
}