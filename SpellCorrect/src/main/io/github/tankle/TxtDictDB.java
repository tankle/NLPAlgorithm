package io.github.tankle;

import io.github.tankle.algorithm.BKTree;
import io.github.tankle.algorithm.LevensteinDistance;
import io.github.tankle.algorithm.MetricSpace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by IDEA
 * User: hztancong
 * Date: 2017/5/27
 * Time: 16:32
 */
public class TxtDictDB {

    public HashSet<String> getPinyinSet() {
        return pinyin_set;
    }

    public void setPinyinSet(HashSet<String> pinyin_set) {
        this.pinyin_set = pinyin_set;
    }

    private HashSet<String> pinyin_set = new HashSet<String>();

    private Map<String, Double> alphaCountInfo = new HashMap<>();
    private Map<String, Set<String>> alpha2word = new HashMap<>();
    private Map<String, Double> word_count = new HashMap<>();
    private Map<String, Double> bi_gram_lan_model = new HashMap<>();

    public BKTree<String> getBk() {
        return bk;
    }

    public void setBk(BKTree<String> bk) {
        this.bk = bk;
    }

    MetricSpace<String> ms = new LevensteinDistance();
    BKTree<String> bk = new BKTree<String>(ms);


    public Map<String, Double> getAlphaCountInfo() {
        return alphaCountInfo;
    }

    public void setAlphaCountInfo(Map<String, Double> alphaCountInfo) {
        this.alphaCountInfo = alphaCountInfo;
    }

    public Map<String, Set<String>> getAlpha2word() {
        return alpha2word;
    }

    public void setAlpha2word(Map<String, Set<String>> alpha2word) {
        this.alpha2word = alpha2word;
    }

    public Map<String, Double> getWord_count() {
        return word_count;
    }

    public void setWord_count(Map<String, Double> word_count) {
        this.word_count = word_count;
    }

    public Map<String, Double> getBi_gram_lan_model() {
        return bi_gram_lan_model;
    }

    public void setBi_gram_lan_model(Map<String, Double> bi_gram_lan_model) {
        this.bi_gram_lan_model = bi_gram_lan_model;
    }
}
