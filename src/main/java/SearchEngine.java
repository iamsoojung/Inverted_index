import java.io.IOException;
import java.util.*;

public class SearchEngine {

    public static void runSearch(String indexFile, String query) throws IOException {
        // 지정한 inverted index 읽기
        List<String> lines = FileUtils.readInputFile(indexFile);
        if (lines == null) return;

        Map<String, Map<Integer, Integer>> loadedIndex = loadInvertedIndex(lines);

        // 해당 inverted index 에서 단어 찾기 (boolean 연산 처리 가능)
        Map<String, List<Integer>> result = search(loadedIndex, query);

        if (result.isEmpty()) {
            System.out.println("검색 결과: 결과 없음");
        } else {
            for (Map.Entry<String, List<Integer>> entry : result.entrySet()) {
                System.out.println(entry.getKey() + " : " + entry.getValue());
            }
        }
    }

    /**
     * 지정한 inverted index 불러오기
     * @param lines
     * @return inverted index
     */
    private static Map<String, Map<Integer, Integer>> loadInvertedIndex(List<String> lines) {
        Map<String, Map<Integer, Integer>> index = new HashMap<>();

        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts.length < 2) continue;

            String word = parts[0];
            Map<Integer, Integer> docFreqMap = new HashMap<>();

            for (int i = 1; i < parts.length; i += 2) {
                try {
                    int docId = Integer.parseInt(parts[i]);
                    int freq = Integer.parseInt(parts[i + 1]);
                    docFreqMap.put(docId, freq);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.err.println("잘못된 라인 형식: " + line);
                    break;
                }
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
    private static Map<String, List<Integer>> search(Map<String, Map<Integer, Integer>> index, String query) {
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
     * AND 포함된 단어 검색 쿼리 처리
     * @param index
     * @param word1
     * @param word2
     * @return
     */
    private static Map<String, List<Integer>> processAndQuery(Map<String, Map<Integer, Integer>> index, String word1, String word2) {
        Map<String, List<Integer>> result = new HashMap<>();

        List<Integer> result1 = extractDocList(index, word1);
        List<Integer> result2 = extractDocList(index, word2);

        result.put(word1, result1);
        result.put(word2, result2);

        if (result1.size() < result2.size()) {
            result1.retainAll(result2); // 교집합 계산
            result.put(word1 + " AND " + word2, result1);
        } else {
            result2.retainAll(result1);
            result.put(word1 + " AND " + word2, result2);
        }

        return result;
    }

    /**
     * OR 포함된 단어 검색 쿼리 처리
     * @param index
     * @param word1
     * @param word2
     * @return
     */
    private static Map<String, List<Integer>> processOrQuery(Map<String, Map<Integer, Integer>> index, String word1, String word2) {
        Map<String, List<Integer>> result = new HashMap<>();

        List<Integer> result1 = extractDocList(index, word1);
        List<Integer> result2 = extractDocList(index, word2);

        result.put(word1, result1);
        result.put(word2, result2);

        Set<Integer> resultSet = new HashSet<>(result1); // 합집합 계산
        resultSet.addAll(result2);

        List<Integer> resultList = new ArrayList<>(resultSet);
        Collections.sort(resultList);
        result.put(word1 + " OR " + word2, resultList);

        return result;
    }

    /**
     * 단일 단어 검색 쿼리 처리
     * @param index
     * @param word
     * @return
     */
    private static Map<String, List<Integer>> processSingleQuery(Map<String, Map<Integer, Integer>> index, String word) {
        Map<String, List<Integer>> result = new HashMap<>();
        List<Integer> resultList = extractDocList(index, word);
        result.put(word, resultList);
        return result;
    }

    /**
     * 특정 단어에 해당하는 문서 ID 리스트 추출
     * @param index
     * @param word
     * @return
     */
    private static List<Integer> extractDocList(Map<String, Map<Integer, Integer>> index, String word) {
        Map<Integer, Integer> docFreqMap = index.get(word);
        if (docFreqMap == null) {
            return Collections.emptyList();
        }

        List<Integer> resultList = new ArrayList<>(docFreqMap.keySet());
        Collections.sort(resultList); // 정렬
        return resultList;
    }
}
