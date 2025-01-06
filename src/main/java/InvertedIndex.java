import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class InvertedIndex {
    private static final Map<String, Map<Integer, Integer>> invertedIndex = new ConcurrentHashMap<>();

    public static void runIndexing(String inputFile, String outputFile) throws IOException {
        // 프로그램 실행 시간 측정 시작
        long programStartTime = System.currentTimeMillis();

        // 입력 파일 읽기
        long startTime = System.currentTimeMillis();
        List<String> lines = FileUtils.readInputFile(inputFile);
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
        FileUtils.writeOutputFile(outputFile, sortedInvertedIndex);
        long writingTime = System.currentTimeMillis() - startTime;
        System.out.println("4) 파일 쓰기 시간(ms): " + writingTime);

        // 프로그램 실행 시간 측정 종료
        long programEndTime = System.currentTimeMillis();
        System.out.println("### 전체 실행 시간(ms): " + (programEndTime - programStartTime));
        System.out.println("### 평균 실행 시간(ms): " + (readInputTime + indexingTime + sortingTime + writingTime)/4.0);
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
                    List<String> words = TextProcessor.processTextByString(parts[1]);

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
}
