package io.github.tankle.datastructs;

public class ResultPairType {
    public int value;
    public int length;
    
    public void set_result(int r, int l){
        value = r;
        length = l;
    }
    
    public int getValue(){
        return value;
    }
    
    public int getLength(){
        return length;
    }
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Value:[").append(value).append("] Length:[").append(length).append("]");
        return sb.toString();
    }
    
    public boolean isLegal(){
    	return length > 0;
    }
}
