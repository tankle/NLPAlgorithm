package io.github.tankle.algorithm;

/**
 * Created by IDEA
 * User: hztancong
 * Date: 2017/4/5
 * Time: 19:00
 *
 * 编辑距离， 又称Levenshtein距离，是指两个字串之间，由一个转成另一个所需的最少编辑操作次数。
 * 该类中许可的编辑操作包括将一个字符替换成另一个字符，插入一个字符，删除一个字符。
 *
 * 使用动态规划算法。算法复杂度：m*n。
 *
 *
 */
public class LevensteinDistance implements MetricSpace<String> {
    private double insertCost = 1;       // 可以写成插入的函数，做更精细化处理
    private double deleteCost = 1;       // 可以写成删除的函数，做更精细化处理


    public double computeDistance(String target, String source){
        int m = source.trim().length();
        int n = target.trim().length();

        double[][] distance = new double[n+1][m+1];

        distance[0][0] = 0;
        for(int i = 1; i <= m; i++){
            distance[0][i] = i;
        }
        for(int j = 1; j <= n; j++){
            distance[j][0] = j;
        }

        double min;
        for(int i = 1; i <= n; i++){
            for(int j = 1; j <=m; j++){
                min = distance[i-1][j] + insertCost;

                if(target.charAt(i-1) == source.charAt(j-1)){
                    if(min > distance[i-1][j-1])
                        min = distance[i-1][j-1];
                }else{
                    double sub_cost = distance[i-1][j-1] + substitudeCost(target.charAt(i-1), source.charAt(j-1));
                    if(min > sub_cost){
                        min = sub_cost;
                    }
                }

                if(min > distance[i][j-1] + deleteCost){
                    min = distance[i][j-1] + deleteCost;
                }

                distance[i][j] = min;
            }
        }

        return distance[n][m];
    }

    @Override
    public double distance(String a, String b) {
        return computeDistance(a,b);
    }


    /**
     *   可以写成替换的函数，做更精细化处理。比如使用键盘距离。
     * @param c1
     * @param c2
     * @return
     */
    public double substitudeCost(char c1, char c2){
        return 1.0;
    }



    public static void main(String[] args) {
        LevensteinDistance distance = new LevensteinDistance();
        KeyboardDistance distance1 = new KeyboardDistance();

        System.out.println(distance.computeDistance("zhsng","zhang"));
        System.out.println(distance1.computeDistance("zhsng","zhang"));
        System.out.println(distance1.computeDistance("talor","talos"));
        System.out.println(distance1.computeDistance("talor","taylor"));
        System.out.println(distance1.computeDistance("blow","bolw"));
    }
}