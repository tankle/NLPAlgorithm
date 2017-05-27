package io.github.tankle.algorithm;

import java.util.*;

/**
 * Created by IDEA
 * User: hztancong
 * Date: 2017/4/5
 * Time: 18:59
 */

/**
 * BK树，可以用来进行拼写纠错查询
 *
 * 1.度量空间。
 * 距离度量空间满足三个条件：
 * d(x,y) = 0 <-> x = y (假如x与y的距离为0，则x=y)
 * d(x,y) = d(y,x) (x到y的距离等同于y到x的距离)
 * d(x,y) + d(y,z) >= d(x,z)  （三角不等式）
 *
 * 2、编辑距离（ Levenshtein Distance）符合基于以上三条所构造的度量空间
 *
 * 3、重要的一个结论：假设现在我们有两个参数，query表示我们搜索的字符串（以字符串为例），
 *    n为待查找的字符串与query最大距离范围，我们可以拿一个字符串A来跟query进行比较，计
 *    算距离为d。根据三角不等式是成立的，则满足与query距离在n范围内的另一个字符转B，
 *    其余与A的距离最大为d+n，最小为d-n。
 *
 *    推论如下：
 *    d(query, B) + d(B, A) >= d(query, A), 即 d(query, B) + d(A,B) >= d -->  d(A,B) >= d - d(query, B) >= d - n
 *    d(A, B) <= d(A,query) + d(query, B), 即 d(query, B) <= d + d(query, B) <= d + n
 *        其实，还可以得到  d(query, A) + d(A,B) >= d(query, B)
 *                 -->   d(A,B) >= d(query, B) - d(query, A)
 *                 -->  d(A,B) >= 1 - d >= 0 (query与B不等)  由于 A与B不是同一个字符串d(A,B)>=1
 *    所以，   min{1, d - n} <= d(A,B) <= d + n
 *
 *    利用这一特点，BK树在实现时，子节点到父节点的权值为子节点到父节点的距离（记为d1）。
 *    若查找一个元素的相似元素，计算元素与父节点的距离，记为d, 则子节点中能满足要求的
 *    相似元素，肯定是权值在d - n <= d1 <= d + n范围内，当然了，在范围内，与查找元素的距离也未必一定符合要求。
 *    这相当于在查找时进行了剪枝，然不需要遍历整个树。试验表明，距离为1范围的查询的搜索距离不会超过树的5-8%，
 *    并且距离为2的查询的搜索距离不会超过树的17-25%。

 * 参见：
 * http://blog.notdot.net/2007/4/Damn-Cool-Algorithms-Part-1-BK-Trees（原文）
 * @author yifeng
 *
 */
public class BKTree<T>{
    private final MetricSpace<T> metricSpace;

    private Node<T> root;

    public BKTree(MetricSpace<T> metricSpace) {
        this.metricSpace = metricSpace;
    }

    /**
     * 根据某一个集合元素创建BK树
     *
     * @param ms
     * @param elems
     * @return
     */
    public static <E> BKTree<E> mkBKTree(MetricSpace<E> ms, Collection<E> elems) {

        BKTree<E> bkTree = new BKTree<E>(ms);

        for (E elem : elems) {
            bkTree.put(elem);
        }

        return bkTree;
    }

    /**
     * BK树中添加元素
     *
     * @param term
     */
    public void put(T term) {
        if (root == null) {
            root = new Node<T>(term);
        } else {
            root.add(metricSpace, term);
        }
    }

    /**
     * 查询相似元素
     *
     * @param term
     *         待查询的元素
     * @param radius
     *         相似的距离范围
     * @return
     *         满足距离范围的所有元素
     */
    public Set<T> query(T term, double radius) {

        Set<T> results = new HashSet<T>();

        if (root != null) {
            root.query(metricSpace, term, radius, results);
        }

        return results;
    }

    private static final class Node<T> {

        private final T value;

        /**
         *  用一个map存储子节点
         */
        private final Map<Double, Node<T>> children;

        public Node(T term) {
            this.value = term;
            this.children = new HashMap<Double, Node<T>>();
        }

        public void add(MetricSpace<T> ms, T value) {
            // value与父节点的距离
            Double distance = ms.distance(this.value, value);

            // 距离为0，表示元素相同，返回
            if (distance == 0) {
                return;
            }

            // 从父节点的子节点中查找child，满足距离为distance
            Node<T> child = children.get(distance);


            if (child == null) {
                // 若距离父节点为distance的子节点不存在，则直接添加一个新的子节点
                children.put(distance, new Node<T>(value));
            } else {
                // 若距离父节点为distance子节点存在，则递归的将value添加到该子节点下
                child.add(ms, value);
            }
        }

        public void query(MetricSpace<T> ms, T term, double radius, Set<T> results) {

            double distance = ms.distance(this.value, term);

            // 与父节点的距离小于阈值，则添加到结果集中，并继续向下寻找
            if (distance <= radius) {
                results.add(this.value);
            }

            // 子节点的距离在最小距离和最大距离之间的。
            // 由度量空间的d(x,y) + d(y,z) >= d(x,z)这一定理，有查找的value与子节点的距离范围如下：
            // min = {1,distance -radius}, max = distance + radius
            for (double i = Math.max(distance - radius, 1); i <= distance + radius; ++i) {

                Node<T> child = children.get(i);

                // 递归调用
                if (child != null) {
                    child.query(ms, term, radius, results);
                }

            }
        }
    }
}