package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        System.out.println(processText("apple a a what???").toString());
    }

    /**
     * 입력된 텍스트를 전처리하여 단어 추출
     * (특수문자 제거 & 알파벳으로만 이루어진 단어 추출 & 소문자 변환)
     * @param text 입력된 원본 텍스트
     * @return 전처리된 단어 리스트
     */
    public static List<String> processText(String text) {
        Pattern pattern = Pattern.compile("\\b[a-zA-Z]+\\b");   // 정규식 패턴 생성
        Matcher matcher = pattern.matcher(text.toLowerCase());  // 소문자 변환 후 정규식 매칭

        List<String> words = new ArrayList<>();
        while (matcher.find()) {    // 정규식에 일치하는 단어 찾기
            words.add(matcher.group()); // 찾은 단어 추가
        }
        return words;
    }
}