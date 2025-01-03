import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 3 || !"index".equalsIgnoreCase(args[0])) {
            System.out.println("Command 틀림 ;; java <className> index <inputFile> <outputFile> !!! ");
            return;
        }

        String inputFile = args[1];
        String outputFile = args[2];

        // 프로그램 실행 시간 측정 시작
        long programStartTime = System.currentTimeMillis();

        // 입력 파일 읽기
        long startTime = System.currentTimeMillis();
        List<String> lines = readInputFile(inputFile);
        long readInputTime = System.currentTimeMillis() - startTime;
        System.out.println("1) 파일 읽기 시간(ms): " + readInputTime);

        // 역색인 생성
        startTime = System.currentTimeMillis();
        Map<String, Map<Integer, Integer>> originInvertedIndex = createInvertedIndex(lines);
        long indexingTime = System.currentTimeMillis() - startTime;
        System.out.println("2) 역색인 생성 시간(ms): " + indexingTime);

        // 출력 정렬
        startTime = System.currentTimeMillis();
        List<String> sortedInvertedIndex = sortInvertedIndex(originInvertedIndex);
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

    private static List<String> processTextByString(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        String[] splitWords = text.toLowerCase().split("[^a-zA-Z]+");

        List<String> words = new ArrayList<>();
        for (String word : splitWords) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

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

    private static void writeOutputFile(String filePath, List<String> sortedInvertedIndex) throws IOException {
        Files.write(Paths.get(filePath), sortedInvertedIndex);
    }
}
