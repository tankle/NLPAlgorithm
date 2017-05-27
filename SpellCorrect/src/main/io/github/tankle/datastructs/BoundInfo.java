package io.github.tankle.datastructs;

import java.util.HashMap;
import java.util.Map;

public class BoundInfo {
    public static final int NULL_ID = -1;
    private int begin;//起始位置
    private int end;//结束位置
    private int id=NULL_ID;//对应的doc id，默认为NULL_ID
    private Map<String,Double> scores = new HashMap<String,Double>();//打分
    
    public BoundInfo(){
    }
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Begin:[").append(begin).append("] ");
        sb.append("End:[").append(end).append("]");
        sb.append("ID:[").append(id).append("]");
        sb.append("Scores:[");
        for(String score_name : scores.keySet()){
        	sb.append("(").append(score_name).append("|").append(scores.get(score_name)).append(")");
        }
        sb.append("]");
        return sb.toString();
    }
    
    public boolean isNull(){
        return NULL_ID == id;
    }
    
    /**
     * 获取本BoundInfo所获取的范围大小
     * @return
     */
    public int getCover(){
        return end-begin+1;
    }
    
    public boolean isEmpty(){
        return id == NULL_ID;
    }
    
    /**
     * 测试一个位置是否在本bound的begin和end之间
     * @param pos
     * @return
     */
    public boolean inRange(int pos){
    	return (pos>=begin&&pos<=end);
    }
    
    /**
     * 生成空的BoundInfo
     * @param abegin
     * @param aend
     * @return
     */
    public static BoundInfo createEmptyBoundInfo(int abegin, int aend){
        return new BoundInfo(abegin, aend, NULL_ID);
    }
    
    public BoundInfo(int abegin, int aend, int aid){
        init(abegin, aend, aid);
    }
    
    public void init(int a_begin, int a_end, int a_id){
        begin = a_begin;
        end = a_end;
        id = a_id;
    }
    
    public void setBegin(int a_begin){
        begin = a_begin;
    }
    public void setEnd(int a_end){
        end = a_end;
    }
    public void setID(int a_id){
        id = a_id;
    }
    public void setNullID(){
    	id = NULL_ID;
    }
    public int getBegin(){
        return begin;
    }
    public int getEnd(){
        return end;
    }
    public int getID(){
        return id;
    }
    public void setScore(String key, Double value){
    	scores.put(key, value);
    }
    public Double getScore(String key){
    	return scores.get(key);
    }
    /**
     * 返回所有的score
     * @return
     */
    public Map<String,Double> getScores(){
    	return scores;
    }
}
