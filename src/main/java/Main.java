import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("명령어 틀림;; java Main <index|search> [추가 인자들]");
            return;
        }

        String mode = args[0];

        if ("index".equalsIgnoreCase(mode)) {
            if (args.length != 3) {
                System.out.println("땡~! java Main index <inputFile> <outputFile>");
                return;
            }
            String inputFile = "../input/" + args[1];
            String outputDir = "../output/";
            String outputFile = outputDir + args[2];

            File dir = new File(outputDir);     // output 디렉토리 없으면 만들도록
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    System.out.println("output 디렉토리가 생성되었습니다: " + outputDir);
                } else {
                    System.out.println("output 디렉토리를 생성할 수 없습니다. 권한을 확인하세요.");
                    return;
                }
            }
            InvertedIndex.runIndexing(inputFile, outputFile);
        } else if ("search".equalsIgnoreCase(mode)) {
            if (args.length < 3) {
                System.out.println("땡~! java Main search <indexFile> <query>");
                return;
            }
            String indexFile = "../output/" + args[1];
            String query = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            SearchEngine.runSearch(indexFile, query);
        } else {
            System.out.println("이게 무슨 모드에요? " + mode + "?? 뭐임?");
        }
    }
}
