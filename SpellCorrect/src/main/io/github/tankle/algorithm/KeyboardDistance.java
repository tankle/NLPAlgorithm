package io.github.tankle.algorithm;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IDEA
 * User: hztancong
 * Date: 2017/4/20
 * Time: 10:42
 */
public class KeyboardDistance extends LevensteinDistance {

    private static final double SCORE_MIS_HIT = 0.1;
    private static final Map<Character, String> charSiblings;
    static {
        charSiblings = new HashMap<>();
        charSiblings.put('q', "was");
        charSiblings.put('w', "qsead");
        charSiblings.put('e', "wsdfra");
        charSiblings.put('r', "edfgt");
        charSiblings.put('t', "rfghy");
        charSiblings.put('y', "tghju");
        charSiblings.put('u', "yhjki");
        charSiblings.put('i', "ujkla");
        charSiblings.put('o', "iklpu");
        charSiblings.put('p', "ol");
        charSiblings.put('a', "qwsxzei");
        charSiblings.put('s', "qazxcdew");
        charSiblings.put('d', "wsxcvfre");
        charSiblings.put('f', "edcvbgtr");
        charSiblings.put('g', "rfvbnhytj");
        charSiblings.put('h', "tgbnmjuy");
        charSiblings.put('j', "yhnmkiu");
        charSiblings.put('k', "ujmloi");
        charSiblings.put('l', "ikpon");
        charSiblings.put('z', "asx");
        charSiblings.put('x', "zasdc");
        charSiblings.put('c', "xsdfv");
        charSiblings.put('v', "cdfgb");
        charSiblings.put('b', "vfghn");
        charSiblings.put('n', "bghjml");
        charSiblings.put('m', "nhjk");
    }

    private double keyboardDistance(char c1, char c2) {
        if (c1 == c2) {
            return 0;
        }
        String s = charSiblings.get(c1);
        if (s != null && s.indexOf(c2) > -1) {
            return SCORE_MIS_HIT;
        }
        return 1;
    }

    @Override
    public double substitudeCost(char c1, char c2) {
        return keyboardDistance(c1, c2);
    }
}
