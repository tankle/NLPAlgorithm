package io.github.tankle.utils;

import io.github.tankle.datastructs.BoundInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IDEA
 * User: hztancong
 * Date: 2017/5/27
 * Time: 16:31
 */
public class StrHelper {
    /**
     * 判断query中是包含words中字符的个数
     * @param word
     * */
    public static int wordContainsCount(String word,Set<String> words)
    {
        int count = 0;
        char now_char = ' ';
        for (int i = 0; i < word.length(); i++) {
            now_char = word.charAt(i);
            if(words.contains(String.valueOf(now_char)))
            {
                count += 1;
            }
        }
        return count;
    }

    /**
     * query 中包含中文的个数
     * @param query
     * @return
     */
    public static int chineseCount(String query) {
        if (query == null || query.isEmpty()) {
            return 0;
        }
        char nchar = ' ';
        int count = 0;
        for (int i = 0; i < query.length(); i++) {
            nchar = query.charAt(i);
            if ((nchar >= '\u4e00') && (nchar <= '\u9fa5')) {
                count += 1;
            }
        }
        return count;
    }

    /**
     * query中包含英文字母的个数
     * @param query
     * @return
     */
    public static int alphabetCount(String query) {
        if (query == null || query.isEmpty()) {
            return 0;
        }
        char now_char = ' ';
        int count = 0;
        for (int i = 0; i < query.length(); i++) {
            now_char = query.charAt(i);
            if ((now_char >= 'A' && now_char <= 'Z')||(now_char >= 'a' && now_char <= 'z')) {
                count += 1;
            }
        }
        return count;
    }


    /**
     * query是否是纯中文
     * @param aStr
     * @return
     */
    public static boolean isChinese(String aStr) {
        if (aStr == null || aStr.isEmpty()) {
            return false;
        }
        char[] charArray = aStr.toCharArray();
        int length = aStr.length();
        char nchar = ' ';
        for (int i = 0; i < length; i++) {
            nchar = charArray[i];
            if (!(nchar >= '\u4e00') && (nchar <= '\u9fa5')) {
                return false;
            }
        }
        return true;
    }


    public static boolean queryWholeIsEnglishStrict(String query, String accmatch_ignore_chars){
        char now_char = ' ';
        for (int i = 0; i < query.length(); i++) {
            now_char = query.charAt(i);
            if ((now_char == ' '||accmatch_ignore_chars.contains(String.valueOf(now_char))||now_char >= 'A' && now_char <= 'Z')||(now_char >= 'a' && now_char <= 'z')) {
                continue;
            }else{
                return false;
            }
        }
        return true;
    }


    /**
     * 全角转半角
     *
     * @return
     */
    public static String QtoB(String input) {
        char c[] = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == '\u3000') {
                c[i] = ' ';
            } else if (c[i] > '\uFF00' && c[i] < '\uFF5F') {
                c[i] = (char) (c[i] - 65248);
            }
        }
        return new String(c);
    }

    /**
     * 词典以及query共同进行的归一化
     * 大写转小写，全角转半角
     * @param raw_str
     * @return
     */
    public static String nlpNorm(String raw_str){
        String rlt = raw_str;
        rlt = StrHelper.QtoB(rlt);
        rlt = rlt.toLowerCase();
        rlt = rlt.trim();
        rlt = rlt.replaceAll(" +", " ");
        return rlt;
    }

    public static boolean isChinese(char uchar) {
        // check if this uchar is a chinese char
        if ((uchar >= '\u4e00') && (uchar <= '\u9fa5')) {
            return true;
        } else {
            return false;
        }
    }
    public static boolean isOther(char uchar) {
        // check if this uchar is not chinese or number or alphabet
        if (isChinese(uchar) || isNumber(uchar) || isAlphabet(uchar)) {
            return false;
        } else {
            return true;
        }
    }
    public static boolean isNumber(char uchar) {
        return uchar>=48&&uchar<=57;
    }
    public static boolean isAlphabet(char uchar) {
        if (((uchar >= '\u0041') && (uchar <= '\u005a'))
                || ((uchar >= '\u0061') && (uchar <= '\u007a'))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 将一个字符串按照如下规则切分
     * 汉字单独切分，连续英文联合切分，连续数字联合切分，其余单独切分
     * @param raw_str
     * @return
     */
    public static List<String> singleWordTokenize( String raw_str ){
        List<String> rlt = new ArrayList<String>();
        if (raw_str == null || raw_str.isEmpty() ){
            return rlt;
        }
        StringBuilder numtmp = new StringBuilder();
        StringBuilder alptmp = new StringBuilder();
        char uchar = ' ';
        for (int i = 0; i < raw_str.length(); i++) {
            uchar = raw_str.charAt(i);
            if (isChinese(uchar) || isOther(uchar)) {
                if (!(numtmp.length() == 0)) {
                    rlt.add(numtmp.toString());
                    numtmp = new StringBuilder();
                }
                if (!(alptmp.length() == 0)) {
                    rlt.add(alptmp.toString());
                    alptmp = new StringBuilder();
                }
                rlt.add(String.valueOf(uchar));
            } else if (isAlphabet(uchar)) {
                if (!(numtmp.length() == 0)) {
                    rlt.add(numtmp.toString());
                    numtmp = new StringBuilder();
                }
                alptmp.append(uchar);
            } else if (isNumber(uchar)) {
                if (!(alptmp.length() == 0)) {
                    rlt.add(alptmp.toString());
                    alptmp = new StringBuilder();
                }
                numtmp.append(uchar);
            }
        }
        if (!(numtmp.length() == 0)) {
            rlt.add(numtmp.toString());
            //numtmp = new StringBuilder();
        }
        if (!(alptmp.length() == 0)) {
            rlt.add(alptmp.toString());
            //alptmp = new StringBuilder();
        }
        return rlt;
    }


    /**
     * 将bound中的切分转换为对应的词语
     * @param raw
     * @param bounds
     * @return
     */
    public static List<String> boundsToStrs(String raw, List<BoundInfo> bounds ){
        try{
            List<String> rlt = new ArrayList<String>(bounds.size());
            String sub_str = null;
            for( BoundInfo bound : bounds ){
                sub_str = raw.substring(bound.getBegin(),bound.getEnd()+1);
                rlt.add(sub_str);
            }
            return rlt;
        }catch( Exception e ){
            List<String> rlt = new ArrayList<String>();
            rlt.add(raw);
            return rlt;
        }
    }
}
