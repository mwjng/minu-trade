package com.minupay.trade.stock.infrastructure.search;

public final class ChosungConverter {

    private static final char HANGUL_BASE = 0xAC00;
    private static final char HANGUL_END = 0xD7A3;
    private static final int JUNGSUNG_COUNT = 21;
    private static final int JONGSUNG_COUNT = 28;
    private static final char[] CHOSUNG = {
            'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ',
            'ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
    };

    private ChosungConverter() {}

    public static String toChosung(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (c >= HANGUL_BASE && c <= HANGUL_END) {
                int index = (c - HANGUL_BASE) / (JUNGSUNG_COUNT * JONGSUNG_COUNT);
                sb.append(CHOSUNG[index]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isChosungOnly(String input) {
        if (input == null || input.isBlank()) return false;
        for (char c : input.toCharArray()) {
            if (c == ' ') continue;
            boolean isChosung = false;
            for (char ch : CHOSUNG) {
                if (ch == c) { isChosung = true; break; }
            }
            if (!isChosung) return false;
        }
        return true;
    }
}
