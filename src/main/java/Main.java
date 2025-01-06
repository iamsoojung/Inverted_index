import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private static final Map<String, Map<Integer, Integer>> invertedIndex = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("명령어 틀림;; java Main <index|search> [추가 인자들]");
            return;
        }

        String mode = args[0];

        if ("index".equalsIgnoreCase(mode)) {   // 색인 모드
            if (args.length != 3) {
                System.out.println("땡~! java Main index <inputFile> <outputFile>");
                return;
            }
            String inputFile = args[1];
            String outputFile = args[2];
            runIndexing(inputFile, outputFile);
        } else if ("search".equalsIgnoreCase(mode)) {   // 검색 모드
            if (args.length < 3) {
                System.out.println("땡~! java Main search <indexFile> <query>");
                return;
            }
            String indexFile = args[1];
            String query = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            runSearch(indexFile, query);
        } else {
            System.out.println("이게 무슨 모드에요? " + mode + "?? 뭐임?");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void runIndexing(String inputFile, String outputFile) throws IOException {
        // 프로그램 실행 시간 측정 시작
        long programStartTime = System.currentTimeMillis();

        // 입력 파일 읽기
        long startTime = System.currentTimeMillis();
        List<String> lines = readInputFile(inputFile);
        long readInputTime = System.currentTimeMillis() - startTime;
        System.out.println("1) 파일 읽기 시간(ms): " + readInputTime);

        // 역색인 생성
        startTime = System.currentTimeMillis();
        invertedIndex.putAll(createInvertedIndex(lines));
        long indexingTime = System.currentTimeMillis() - startTime;
        System.out.println("2) 역색인 생성 시간(ms): " + indexingTime);

        // 출력 정렬
        startTime = System.currentTimeMillis();
        List<String> sortedInvertedIndex = sortInvertedIndex(invertedIndex);
        long sortingTime = System.currentTimeMillis() - startTime;
        System.out.println("3) 정렬 시간(ms): " + sortingTime);

        // 출력 파일 쓰기
        startTime = System.currentTimeMillis();
        writeOutputFile(outputFile, sortedInvertedIndex);
        long writingTime = System.currentTimeMillis() - startTime;
        System.out.println("4) 파일 쓰기 시간(ms): " + writingTime);

        // 프로그램 실행 시간 측정 종료
        long programEndTime = System.currentTimeMillis();
        System.out.println("### 전체 실행 시간(ms): " + (programEndTime - programStartTime));
        System.out.println("### 평균 실행 시간(ms): " + (readInputTime + indexingTime + sortingTime + writingTime)/4.0);
    }

    private static void runSearch(String indexFile, String query) throws IOException {
        List<String> lines = readInputFile(indexFile);
        if (lines == null) return;

        Map<String, Map<Integer, Integer>> loadedIndex = loadInvertedIndex(lines);

        List<Integer> result = search(loadedIndex, query);

        System.out.println("검색 결과: " + (result.isEmpty() ? "결과 없음" : result));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* 색인 로직 구현 */

    /**
     * 입력 파일 읽어서, 라인 단위로 반환함
     * @param filePath
     * @return 라인 단위 텍스트
     */
    private static List<String> readInputFile(String filePath) {
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
     * 라인 별로 문서아이디와 텍스트 분리 후, 역색인 생성
     * @param lines
     * @return 역색인
     */
    private static Map<String, Map<Integer, Integer>> createInvertedIndex(List<String> lines) {
        Map<String, Map<Integer, Integer>> invertedIndex = new ConcurrentHashMap<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (String line : lines) {
                futures.add(executor.submit(() -> {
                    String[] parts = line.split(" ", 2);
                    if (parts.length != 2) return;

                    int docId = Integer.parseInt(parts[0]);
                    List<String> words = processTextByString(parts[1]);

                    for (String word : words) {
                        invertedIndex.computeIfAbsent(word, k -> new ConcurrentHashMap<>())
                                .merge(docId, 1, Integer::sum);
                    }
                }));
            }

            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
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

        Pattern pattern = Pattern.compile("\\b[a-zA-Z0-9]+\\b");   // 정규식 패턴 생성
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
    private static List<String> processTextByString(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        String[] splitWords = text.toLowerCase().split("[^a-zA-Z0-9]+");

        List<String> words = new ArrayList<>();
        for (String word : splitWords) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

    /**
     * 생성된 역색인을 조건에 맞게 정렬
     * @param invertedIndex
     * @return 정렬된 역색인
     */
    private static List<String> sortInvertedIndex(Map<String, Map<Integer, Integer>> invertedIndex) {
        List<String> sortedInvertedIndex = new ArrayList<>();

        invertedIndex.entrySet().parallelStream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(entry -> {
                    String word = entry.getKey();
                    Map<Integer, Integer> docFreqMap = entry.getValue();

                    StringBuilder line = new StringBuilder(word);
                    docFreqMap.entrySet().stream()
                            .sorted((e1, e2) -> {
                                int freqCompare = Integer.compare(e2.getValue(), e1.getValue());
                                return freqCompare != 0 ? freqCompare : Integer.compare(e1.getKey(), e2.getKey());
                            })
                            .forEach(e -> line.append(" ").append(e.getKey()).append(" ").append(e.getValue()));
                    sortedInvertedIndex.add(line.toString());
                });

        return sortedInvertedIndex;
    }

    /**
     * 정렬된 역색인 결과값을, 결과 파일로 쓰기
     * @param filePath
     * @param sortedInvertedIndex
     */
    private static void writeOutputFile(String filePath, List<String> sortedInvertedIndex) throws IOException {
        Files.write(Paths.get(filePath), sortedInvertedIndex);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* 검색 로직 구현 */

    /**
     * 지정한 inverted index 불러오기
     * @param lines
     * @return inverted_index
     */
    private static Map<String, Map<Integer, Integer>> loadInvertedIndex(List<String> lines) {
        Map<String, Map<Integer, Integer>> index = new HashMap<>();

        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts.length < 2) continue;

            String word = parts[0];
            Map<Integer, Integer> docFreqMap = new HashMap<>();

            for (int i = 1; i < parts.length; i += 2) {
                int docId = Integer.parseInt(parts[i]);
                int freq = Integer.parseInt(parts[i + 1]);
                docFreqMap.put(docId, freq);
            }
            index.put(word, docFreqMap);
        }
        return index;
    }

    /**
     * 지정된 inverted index 로부터 지정한 단어를 케이스별로 검색
     * @param index
     * @param query
     * @return 각 케이스에 적절한 메서드 호출 (AND 연산, OR 연산, 단일 연산)
     */
    private static List<Integer> search(Map<String, Map<Integer, Integer>> index, String query) {
        if (query.contains(" AND ")) {  // AND 연산 처리
            String[] words = query.split(" AND ");
            return processAndQuery(index, words[0].trim(), words[1].trim());
        } else if (query.contains(" OR ")) {    // OR 연산 처리
            String[] words = query.split(" OR ");
            return processOrQuery(index, words[0].trim(), words[1].trim());
        } else {    // 단일 연산
            return processSingleQuery(index, query.trim());
        }
    }

    /**
     * AND 키워드에 따른 단어 검색 모드
     * @param index
     * @param word1
     * @param word2
     * @return
     */
    private static List<Integer> processAndQuery(Map<String, Map<Integer, Integer>> index, String word1, String word2) {
        List<Integer> result1 = processSingleQuery(index, word1);
        List<Integer> result2 = processSingleQuery(index, word2);

        result1.retainAll(result2);     // 교집합
        return result1;
    }

    /**
     * OR 키워드에 따른 단어 검색 모드
     * @param index
     * @param word1
     * @param word2
     * @return
     */
    private static List<Integer> processOrQuery(Map<String, Map<Integer, Integer>> index, String word1, String word2) {
        List<Integer> result1 = processSingleQuery(index, word1);
        List<Integer> result2 = processSingleQuery(index, word2);

        Set<Integer> resultSet = new HashSet<>(result1);    // 합집합
        resultSet.addAll(result2);

        List<Integer> result = new ArrayList<>(resultSet);      // 재정렬
        Collections.sort(result);
        return result;
    }

    /**
     * 단일 단어 검색 모드
     * @param index
     * @param word
     * @return
     */
    private static List<Integer> processSingleQuery(Map<String, Map<Integer, Integer>> index, String word) {
        Map<Integer, Integer> docFreqMap = index.get(word);
        if (docFreqMap == null)     return Collections.emptyList();
        
        List<Integer> result = new ArrayList<>(docFreqMap.keySet());    // 정렬
        Collections.sort(result);
        return result;
    }
}
