package com.readcamp.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 英文句子切分器（上传文章时服务端调用，设计见 doc/00-design.md §1）
 * 规则：
 *  - 段落：按空行（\n\n 或 \r?\n\s*\r?\n）分段，段落号 0 起；段内单个换行折叠为空格
 *  - 句界：. ! ?（含句尾引号归属：句界后紧跟的 " ' 归当前句）
 *  - 缩写保护：白名单（Mr. Dr. e.g. U.S. 等）+ 数字小数点不切
 */
public final class SentenceSplitter {

    private SentenceSplitter() {
    }

    /** 切分结果：句子文本 + 段落号 */
    public record SentencePart(String text, int para) {
    }

    /** 常见缩写白名单（不含末尾句点，比较时小写） */
    private static final Set<String> ABBREVIATIONS = Set.of(
            "mr", "mrs", "ms", "dr", "prof", "st", "vs", "etc", "inc", "ltd",
            "jr", "sr", "no", "dept", "univ", "assn", "mt", "ft", "capt", "gen",
            "col", "sgt", "adm", "rev", "hon", "sen", "rep", "gov", "pres",
            "eg", "ie", "cf", "al", "approx", "est", "min", "max", "avg", "hrs"
    );

    /** 多段缩写：如 U.S. U.K. e.g. i.e. */
    private static final Set<String> ABBREVIATIONS_MULTI = Set.of(
            "u.s", "u.k", "e.g", "i.e", "u.n", "e.u", "a.m", "p.m"
    );

    /**
     * 切分全文为句子列表（带段落号，trim 后非空）。
     * 空行分段：正则匹配 \n + 任意空白 + \n（含 \r\n 平台差异）。
     */
    public static List<SentencePart> split(String text) {
        List<SentencePart> parts = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return parts;
        }
        String[] paragraphs = text.split("\\r?\\n\\s*\\r?\\n");
        int para = 0;
        for (String paragraph : paragraphs) {
            for (String sentence : splitParagraph(paragraph)) {
                parts.add(new SentencePart(sentence, para));
            }
            para++;
        }
        return parts;
    }

    /** 段内切句：空白折叠为单空格，按 . ! ? 切分（缩写/数字/引号保护） */
    private static List<String> splitParagraph(String paragraph) {
        List<String> sentences = new ArrayList<>();
        String normalized = paragraph.replace("\r\n", "\n").replaceAll("\\s+", " ");

        StringBuilder current = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            current.append(c);

            if (isSentenceEnd(normalized, i)) {
                // 句尾引号归属
                while (i + 1 < normalized.length()
                        && (normalized.charAt(i + 1) == '"' || normalized.charAt(i + 1) == '\'')) {
                    current.append(normalized.charAt(i + 1));
                    i++;
                }
                String sentence = current.toString().trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
                current.setLength(0);
            }
        }

        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            sentences.add(tail);
        }
        return sentences;
    }

    /** 判断位置 i 的字符（. ! ?）是否为句界 */
    private static boolean isSentenceEnd(String text, int i) {
        char c = text.charAt(i);
        if (c == '!' || c == '?') {
            return true;
        }
        if (c != '.') {
            return false;
        }
        // 数字小数点：前后都是数字 → 不切（3.14）
        if (i > 0 && i + 1 < text.length()
                && Character.isDigit(text.charAt(i - 1))
                && Character.isDigit(text.charAt(i + 1))) {
            return false;
        }
        // 取点前完整 token（允许内部点，如 u.s / e.g），白名单内 → 不切
        int start = i - 1;
        while (start >= 0 && (Character.isLetter(text.charAt(start))
                || text.charAt(start) == '\''
                || text.charAt(start) == '-'
                || text.charAt(start) == '.')) {
            start--;
        }
        String word = text.substring(start + 1, i).toLowerCase(Locale.ROOT);
        if (word.endsWith(".")) {
            word = word.substring(0, word.length() - 1);
        }
        if (ABBREVIATIONS.contains(word) || ABBREVIATIONS_MULTI.contains(word)) {
            return false;
        }
        // 缩写中间点：点后紧跟字母（无空格）视为缩写内部（如 U.S.、e.g.、A.B.C.）
        // 正常句界点后是空格或句尾引号，不受影响
        if (i + 1 < text.length() && Character.isLetter(text.charAt(i + 1))) {
            return false;
        }
        return true;
    }
}
