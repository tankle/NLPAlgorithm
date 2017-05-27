package io.github.tankle.datastructs;

import java.util.Arrays;

public class DoubleArray {
    private int max_use_pos = -1;
    private int[] base=new int[2];
    private int[] check=new int[2];
    private int[] used;

    public boolean isEmpty(){
    	return base.length<=2;
    }
    
    /**
     * 初始化双数组
     * @param new_base
     * @param new_check
     * @param new_max_use_pos
     */
    public void init(
        int[] new_base,
        int[] new_check,
        int new_max_use_pos){
        base = new_base;
        check = new_check;
        max_use_pos = new_max_use_pos;
    }
    
    /**
     * 将used辅助数组清空
     */
    public void clean_used(){
        used = null;
    }
    
    public int[] getBaseArray(){
        return base;
    }
    public int[] getCheckArray(){
        return check;
    }
    public boolean in_range(int pos){
        return (pos>=0 && pos<base.length);
    }
    public void setBase(int pos, int value){
        base[pos] = value;
    }
    public int getBase(int pos){
        return base[pos];
    }
    public void setCheck(int pos, int value){
        check[pos] = value;
    }
    public int getCheck(int pos){
        return check[pos];
    }
    public void setUsed(int pos, int value){
        if( used == null ){
            used = new int[base.length];
        }
        used[pos] = value;
    }
    public int getUsed(int pos){
        if( used == null ){
            used = new int[base.length];
        }
        return used[pos];
    }
    public void setMaxUsePos(int new_max_use_pos){
        max_use_pos = new_max_use_pos;
    }
    public int getUnitSize(){
        return base.length;
    }
    
    public int getMaxUsePos(){
        return max_use_pos;
    }
    
    public void compress(){
        resize(max_use_pos+1);   
    }
    
    /**
     * 调整数组大小
     * @param new_size
     * @return
     */
    public int resize( int new_size ){
        base = resize_int_array(base,base.length,new_size,0);
        check = resize_int_array(check,check.length,new_size,0);
        if(used != null){
            used = resize_int_array(used,used.length,new_size,0);
        }
        return new_size;
    }
    
    private int[] resize_int_array(int[] ptr, int n, int l, int v){
        int[] tmp = Arrays.copyOf(ptr, l);
        if( l>n ){
            for( int i=n; i<l; ++i ){
                tmp[i] = v;
            }
        }
        return tmp;
    }
}
