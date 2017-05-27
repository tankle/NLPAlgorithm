package io.github.tankle.algorithm;


import io.github.tankle.TxtDictDB;
import io.github.tankle.datastructs.BoundInfo;
import io.github.tankle.datastructs.DoubleArrayMaster;
import io.github.tankle.utils.StrHelper;
import io.github.tankle.utils.UtilHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.*;


/**
 * Created by IDEA
 * User: hztancong
 * Date: 2017/4/18
 * Time: 13:58
 */
public class DTSpellCorrectFactory {
    public static final String SPELL_CORRECT_COMMAND_NAME = "spell_correct";
    public static final String K_ACCEPT_CORRECT_TYPE = "accept_correct_type"; // 可接受的纠错类型

    public static final String V_ALL = "all";
    private static final String WORD_SPLIT_SIGN = "Ｕ";
    private static final String LINE_BEGIN = "<B>";

    private static KeyboardDistance keyboardDistance = new KeyboardDistance();
    private static Map<StrategyType, String> strategy_map = new HashMap<>();


    /**
     * 可以接受的纠错类型
     */
    public enum ACCEPT_CORRECT_TYPE{
        pinyin, // 拼音列表
        single_word, // 单个拼音,
        mix
    }

    private enum StrategyType{
        dict,
        single_word,
        pinyin,
        mix
    }
    static {
        strategy_map.put(StrategyType.single_word, "单个字母策略");
        strategy_map.put(StrategyType.pinyin, "拼音组合策略");
        strategy_map.put(StrategyType.dict, "词典纠错策略");
        strategy_map.put(StrategyType.mix, "中英文混合纠错");
    }



    /**
     *
     * 纠错，当没有纠错结果的时候返回一个<tt>null</tt>
     * @param q
     * @param accept_correct_type   可以接受的纠错类型
     * @throws Exception
     * */
    @SuppressWarnings("unchecked")
    public static String querySpellCorrect(String q, List<String> accept_correct_type) throws Exception {

        String final_query = null;

        Set<String> accpetCorrectSet = new HashSet<>();

        //默认只对拼音纠错
        if(accept_correct_type.isEmpty()){
            accept_correct_type.add(ACCEPT_CORRECT_TYPE.pinyin.name());
        }
        /**
         * 根据配置读取需要纠错的类型
         */
        if(accept_correct_type.contains(V_ALL)){
            for (ACCEPT_CORRECT_TYPE acceptCorrectType : ACCEPT_CORRECT_TYPE.values()) {
                accpetCorrectSet.add(acceptCorrectType.name());
            }
        }else{
            accpetCorrectSet.addAll(accept_correct_type);
        }

        String norm_query = StrHelper.nlpNorm(q);

        boolean is_eng = StrHelper.queryWholeIsEnglishStrict(norm_query, "'");

        int ch_count = StrHelper.chineseCount(norm_query);
        int eng_count = StrHelper.alphabetCount(norm_query);

        boolean is_mix = false;
        if(ch_count > 0 && eng_count > 0 && ch_count < norm_query.length() && eng_count < norm_query.length()){
            is_mix = true;
        }
        boolean is_meet = false;
        /**
         * 不是纯字母，也不是中英文混合，或者长度小于3
         */
        if(is_eng && norm_query.length() > 3) {
            is_meet = true;
        }

        if( !is_meet && is_mix){
            if(ch_count >= 1 && eng_count >= 3){
                is_meet = true;
            }
        }

        if( ! is_meet){
            return null;
        }


//        Map<DTDictFactory.DictType, TxtDictDB> dict_db_info = tddb_master.getDbs();
//        TxtDictDB phrase = dict_db_info.get(DTDictFactory.DictType.phrase);
        TxtDictDB phrase = new TxtDictDB();
        Map<String, Double> alphaCountInfo = phrase.getAlphaCountInfo();

        // 1. 先从词典判断是否是rel_words。
//        TxtDictDB tddb = tddb_master.getTxtDictDB(DTDictFactory.DictType.phrase);
//        String info_str = tddb.getInfoStr(norm_query);
//        if(info_str!=null) {
//            //先判断是否是直接转换的，即query_rewrite
//            boolean is_tr_word = judgeTrWord(info_str);
//            if (is_tr_word) {
////                final_query = DTQueryRewriteFactory.getSpeWord(info_str, DTQueryRewriteFactory.TR_WORDS);
////                logger.info(String.format("Query Rewrite [%s] to [%s] by strategy [%s]",
////                        norm_query, final_query, strategy_map.get(StrategyType.dict)));
//                return final_query;
//            }
//        }

        /**
         * 是中英文混合
         */
        if(is_mix && accpetCorrectSet.contains(ACCEPT_CORRECT_TYPE.mix.name())){
//			Map<String, List<String>> unicode2pinyins = (Map<String, List<String>>) md.get(EnumHelper.CustomDataType.unicode_to_hanyu_pinyin);
			Map<String, List<String>> unicode2pinyins = new HashMap<>();
            final_query = QueryRewriteByMixWords(norm_query, phrase, unicode2pinyins);
        }
        else if(is_eng) {
            // 有多个拼音
            if (norm_query.contains(" ") || norm_query.contains("'")) {
                String[] words;
                if (norm_query.contains(" ")) {
                    words = norm_query.split(" ");
                } else {
                    words = norm_query.split("'");
                }

                List<String> pinyin_list = new ArrayList<>();
                for (String word : words) {
                    if (alphaCountInfo.containsKey(word)) {
                        pinyin_list.add(word);
                    }
                }
                // 每个单词都是一个拼音或者英文字母
                if (accpetCorrectSet.contains(ACCEPT_CORRECT_TYPE.pinyin.name()) &&
                        pinyin_list.size() == words.length && pinyin_list.size() > 1) {
                    final_query = QueryRewriteByPinyinList(norm_query, phrase, pinyin_list);
                }
            } else {
                /**
                 * TODO 词频统计优化
                 */
                List<String> pinyin_list = tryPySpl(norm_query);
                // 单个拼音 或者拼音有错的： zhsngbichen
                if (accpetCorrectSet.contains(ACCEPT_CORRECT_TYPE.single_word.name()) &&
                        pinyin_list.size() == 1) {
                    final_query = QueryRewriteByOnePinyin(norm_query, phrase, alphaCountInfo);

                    // 对纠错后的拼音尝试切分，如果能切分则进行中文解码
                    List<String> tmp_pinyin_list = tryPySpl(norm_query);
                    if(tmp_pinyin_list.size() > 1 && accept_correct_type.contains(ACCEPT_CORRECT_TYPE.pinyin.name())){
                        String new_final_query = QueryRewriteByPinyinList(norm_query, phrase, tmp_pinyin_list);
                        if(new_final_query != null && !new_final_query.isEmpty()){
                            final_query = new_final_query;
                        }
                    }
                }

                // 有多个拼音
                else if (accpetCorrectSet.contains(ACCEPT_CORRECT_TYPE.pinyin.name()) &&
                        pinyin_list.size() > 1) {
                    final_query = QueryRewriteByPinyinList(norm_query, phrase, pinyin_list);
                }
            }
        }
        return final_query;
    }


    //单字分词结果转为拼音单字分词结果
    public static List<String> singleWordsToPyWords(
            List<String> single_words,
            Map<String,List<String>> unicode2pinyins,
            boolean spl_py
    ){
        List<String> rlt = new ArrayList<String>(single_words.size());
        List<String> pys = null;
        List<String> py_spls = null;
        for( String word : single_words ){
            if( unicode2pinyins.containsKey(word) ){
                pys = unicode2pinyins.get(word);
                rlt.add(pys.get(0));
            }else{
                if( spl_py ){
                    py_spls = tryPySpl(word);
                    rlt.addAll(py_spls);
                }else{
                    rlt.add(word);
                }
            }
        }
        return rlt;
    }

    /**
     * 用来尝试将输入的字符串按照拼音去切分，但是需要检验是否能够切出拼音，如果不能切成拼音的话，就保留原来的字符串
     * @return
     */
    public static List<String> tryPySpl( String str ){
        try{
            if( str.length() <= 4 ){
                List<String> rlt = new ArrayList<String>();
                rlt.add(str);
                return rlt;
            }
//            TxtDictDB pinyin_unit_tdb = tddb_master.getTxtDictDB(DictType.pinyin_unit);
//            DoubleArrayMaster dam = pinyin_unit_tdb.getDoubleArrayMaster();
            DoubleArrayMaster dam = new DoubleArrayMaster();
            char[] char_array = str.toCharArray();
            List<List<BoundInfo>> seg_list = dam.seg(char_array, true, null);
            List<BoundInfo> best_bounds = seg_list.get(0);
            int py_unit_count = 0;
            int total_count = best_bounds.size();
            for( BoundInfo bound_info : best_bounds ){
                if(bound_info.isNull()){
                    ;
                }else{
                    py_unit_count++;
                }
            }
            double ratio = (double)py_unit_count/(double)total_count;
            if( ratio < 0.75 ){
                List<String> rlt = new ArrayList<String>();
                rlt.add(str);
                return rlt;
            }
            List<String> rlt = StrHelper.boundsToStrs(str,best_bounds);
            boolean py_spl_check = checkPySplRlt(rlt);
            if( py_spl_check ){
                return rlt;
            }
            rlt = new ArrayList<String>();
            rlt.add(str);
            return rlt;
        }catch( Exception e ){
            List<String> rlt = new ArrayList<String>();
            rlt.add(str);
            return rlt;
        }
    }

    private static boolean checkPySplRlt( List<String> rlt ){
        for( String str : rlt ){
            if( str.length() == 1 ){
                return false;
            }
        }
        return true;
    }


    /**
     * 混合词拼写纠错
     * @param norm_query
     * @param phrase
     *@param unicode2pinyins  @return
     */
    private static String QueryRewriteByMixWords(String norm_query,
                                                 TxtDictDB phrase, Map<String, List<String>> unicode2pinyins) {

        List<String> words = StrHelper.singleWordTokenize(norm_query);

        List<String> pinyin_list = singleWordsToPyWords(words, unicode2pinyins, true);

        HashSet<String> allPinYin = phrase.getPinyinSet();
        // 所有拼音需要是合法拼音
        for (String pinyin : pinyin_list) {
            // 拼音的长度需要大于等于2
            if( pinyin.length() < 2 ||!allPinYin.contains(pinyin) ){
                return null;
            }
        }

        Set<String> chineseWords = new HashSet<>();
        for (String word : words) {
            if(StrHelper.isChinese(word)){
                chineseWords.add(word);
            }
        }

        return QueryRewriteByPinyinList(norm_query, phrase, pinyin_list, chineseWords, StrategyType.mix);
    }

    /**
     *
     * TODO 建立前缀树，用于判断是否要纠错
     * 对一个拼音进行重写
     *  @param norm_query
     * @param phrase
     * @param alphaCountInfoMap
     */
    private static String QueryRewriteByOnePinyin(String norm_query, TxtDictDB phrase, Map<String, Double> alphaCountInfoMap) {
        String best_words = null;
        if(! alphaCountInfoMap.containsKey(norm_query)){
            BKTree bk = phrase.getBk();
            Set<String> words = bk.query(norm_query, 1.0);

            Double max_count = 0.0;
            Double min_distance = 9999.0;
            Double distance = 0.0;

            for (String word : words) {
                if(word.length() != norm_query.length()){
                    continue;
                }
                distance = keyboardDistance.distance(norm_query, word);
                if(alphaCountInfoMap.containsKey(word) && distance <= min_distance){
                    // 1. 选取距离最小
                    // 2. 距离相等，选择出现次数最多
                    if(distance < min_distance
                            || (distance.equals(min_distance) && alphaCountInfoMap.get(word) > max_count)) {
                        best_words = word;
                        max_count = alphaCountInfoMap.get(word);
                        min_distance = distance;
                    }
                }
            }

            if(best_words!= null && !best_words.isEmpty()) {
//                logger.info(String.format("Query Rewrite [%s] to [%s] by strategy [%s]. 候选词:[%s]",
//                        norm_query, best_words, strategy_map.get(StrategyType.single_word), StringUtils.join(words, ";")));
            }
        }
        return best_words;
    }



    /**
     * 按照拼音列表进行query重写
     * @param norm_query
     * @param phrase
     * @param pinyin_list
     */
    private static String QueryRewriteByPinyinList(String norm_query, TxtDictDB phrase, List<String> pinyin_list) {
        Set<String> chineseWords = new HashSet<>();
        return QueryRewriteByPinyinList(norm_query, phrase, pinyin_list, chineseWords, StrategyType.pinyin);
    }

    /**
     * 按照拼音列表进行query重写
     * @param norm_query
     * @param phrase
     * @param pinyin_list
     * @param chineseWords
     */
    private static String QueryRewriteByPinyinList(String norm_query, TxtDictDB phrase,
                                                   List<String> pinyin_list, Set<String> chineseWords, StrategyType stragetyType) {
        Map<String, Set<String>> pinyin2word = new HashMap<>();
        Map<String, Set<String>> alpha2word = phrase.getAlpha2word();
        Map<String, Double> bi_gram_lan_model = phrase.getBi_gram_lan_model();
        Map<String, Double> wordCount = phrase.getWord_count();

        String best_word = null;

        for (String pinyin : pinyin_list) {
            if(! alpha2word.containsKey(pinyin)){
                break;
            }
            pinyin2word.put(pinyin, alpha2word.get(pinyin));
        }

        // 对于过长的拼音也不走拼音识别模块
        if(pinyin2word.size() != pinyin_list.size() || pinyin_list.size() > 20){
            return null;
        }

        Map<String, Double> wordInfo = decodePinyin2WordByHMM(pinyin_list, chineseWords, pinyin2word, bi_gram_lan_model);

        if(wordInfo.size() > 0 ){
            Double max_count = 0.0;
            Double now_count;
            int word_count;
            for (String word : wordInfo.keySet()) {
                // 纠错词必须要含有原有中文词的一半以上
                if(! chineseWords.isEmpty()){
                    word_count = StrHelper.wordContainsCount(word, chineseWords);
                    if(word_count < ((chineseWords.size()+1)/2)){
                        continue;
                    }
                }

                if (wordCount.containsKey(word)) {
                    now_count = wordCount.get(word);
                    if (max_count < now_count) {
                        max_count = now_count;
                        best_word = word;
                    }
                }

            }
            if(best_word != null) {
//                logger.info(String.format("Query Rewrite [%s] to [%s] by strategy [%s]; 候选词:[%s]",
//                        norm_query, best_word, strategy_map.get(stragetyType), StringUtils.join(wordInfo.keySet(), ";")));
            }
        }
        return best_word;
    }


    /**
     * @param pinyin_list
     * @param chineseWords
     * @param pinyin2word
     * @param bi_gram_lan_model
     */
    private static Map<String, Double> decodePinyin2WordByHMM(List<String> pinyin_list,
                                                              Set<String> chineseWords,
                                                              Map<String, Set<String>> pinyin2word,
                                                              Map<String, Double> bi_gram_lan_model) {
        Set<String> voca = new HashSet<>();
        pinyin2word.forEach((String k, Set<String> v) -> voca.addAll(v));
        Map<String, Integer> word2idx = new HashMap<>();
        Map<Integer, String> idx2word = new HashMap<>();
        Integer idx = 0;
        for (String word : voca) {
            word2idx.put(word, idx);
            idx2word.put(idx, word);
            idx += 1;
        }

        // delta = [[0 for _ in range(s_num)] for _ in range(len(word_list))]
        Double[][] delta = new Double[pinyin_list.size()][voca.size()];
        Integer[][] psi = new Integer[pinyin_list.size()][voca.size()];

        String tmp_word;
        Set<String> words;
        double max_value;
        double tmp_value;
        Double now_prob;
        String pinyin;

        /**
         * 概率初始化
         */
        for (String word : pinyin2word.get(pinyin_list.get(0))) {
            // 如果query输入的就是第一个字符中文，则初始化概率就要大一些
            if(chineseWords.contains(word)){
                now_prob = 1.0;
            }else {
                tmp_word = LINE_BEGIN + WORD_SPLIT_SIGN + word;
                now_prob = bi_gram_lan_model.get(tmp_word);
            }
            //如果第一个字母的概率为空，则初始化位一个较小的概率值，这个跟创建转移概率矩阵的值有关系
            if(now_prob == null){
                now_prob = 0.00001;
            }
            delta[0][word2idx.get(word)] = now_prob;
            psi[0][word2idx.get(word)] = 0;
        }


        /**
         * vitebi算法，动态规划求概率
         */
        boolean flag = true;// 是否存在最优路径
        int index = 0;
        int no_prob_count;  // 转移概率个数统计
        for (int i = 1; i < pinyin_list.size(); i++) {
            pinyin = pinyin_list.get(i);
            words = pinyin2word.get(pinyin);
            no_prob_count = 0;
            for (String word : words) {
                index = -1;
                max_value = 0.0;
                Set<String> pre_words = pinyin2word.get(pinyin_list.get(i - 1));
                for (String pre_word : pre_words) {
                    tmp_word = pre_word + WORD_SPLIT_SIGN + word;
                    now_prob = bi_gram_lan_model.get(tmp_word);
                    if(now_prob == null){
                        now_prob = 0.0;
                    }
                    tmp_value = delta[i - 1][word2idx.get(pre_word)] * now_prob;
                    if(max_value < tmp_value){
                        max_value = tmp_value;
                        index = word2idx.get(pre_word);
                    }
                }

                delta[i][word2idx.get(word)] = max_value;

                if(index != -1) {
                    psi[i][word2idx.get(word)] = index;
                }
                // 前单词列表到当前的单词没有转移概率
                else{
                    no_prob_count += 1;
                }
            }
            // 前一个单词列表到当前的单词列表都没有转移概率
            if( no_prob_count == words.size()){
                flag = false;
                break;
            }
        }

        Map<String, Double> rlt = new HashMap<>();
        if(flag) {
            /**
             * 选取所有的可能的转移路径
             */
            // 从尾部开始循环
            String rlt_words = null;
            int last_idx = pinyin_list.size() - 1;
            words = pinyin2word.get(pinyin_list.get(last_idx));
            Map<Integer, Double> wordProbInfo = new HashMap<>();
            for (String word : words) {
                now_prob = delta[last_idx][word2idx.get(word)];
                if(Double.compare(0.0, now_prob) == 0){
                    continue;
                }
                wordProbInfo.put(word2idx.get(word), now_prob);
            }

            Integer[] path = new Integer[pinyin_list.size()];
            for (Integer word_id : wordProbInfo.keySet()) {
                path[last_idx] = word_id;
                for (int k = last_idx - 1; k >= 0; --k) {
                    path[k] = psi[k + 1][path[k + 1]];
                }

                List<String> final_words = new ArrayList<>();
                for (int i = 0; i < pinyin_list.size(); i++) {
                    final_words.add(idx2word.get(path[i]));
                }
                rlt_words = StringUtils.join(final_words, "");
                rlt.put(rlt_words, wordProbInfo.get(word_id));
            }
        }
        rlt = UtilHelper.sortByValue(rlt, true);
        return rlt;
    }
}
