package io.github.tankle.utils;

import java.util.*;

/**
 * 一些有用的帮助方法
 *
 * Created by IDEA
 * User: hztancong
 * Date: 2017/4/26
 * Time: 11:51
 */
public class UtilHelper {

    /**
     * 对map 按照value进行排序，并返回一个map
     * @param unsortMap
     * @param reverse
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> unsortMap, boolean reverse) {

        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {

                int rlt = (o1.getValue()).compareTo(o2.getValue());
                if(reverse){
                    return  -rlt;
                }
                return rlt;
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;

    }
}
