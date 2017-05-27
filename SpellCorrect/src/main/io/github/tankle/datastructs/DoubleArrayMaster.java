package io.github.tankle.datastructs;

import org.apache.log4j.Logger;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class DoubleArrayMaster {
	private static Logger logger = Logger.getLogger(DoubleArrayMaster.class.getName());
    private static final char ZERO_CHAR = (char)0;
    private DoubleArray array_ = new DoubleArray();
    private int key_size_;
    private char[][] key_;
    private int[] length_;
    private int progress_;
    private int next_check_pos_;
    private boolean no_delete_;
    private int error_=0;

    public boolean isEmpty(){
    	return array_.isEmpty();
    }
    
    /**
     * 生成整句只有一个边界信息的列表
     * @param chars
     * @param id
     * @return
     */
    public static List<BoundInfo> createSingleBIList(
    	char[] chars,
    	Integer id
    	){
    	List<BoundInfo> rlt = new ArrayList<BoundInfo>();
    	BoundInfo bi = new BoundInfo();
    	if( id == null ){
    		bi.setNullID();
    	}else{
    		bi.setID(id);
    	}
    	bi.setBegin(0);
    	bi.setEnd(chars.length-1);
    	rlt.add(bi);
    	return rlt;
    }

    
    /**
     * 计算一个边界列表的原始query字符覆盖率
     * @param cand
     * @return
     */
    public static int computeCover(List<BoundInfo> cand){
        int cover = 0;
        for( BoundInfo bound_info : cand ){
            if(bound_info.isEmpty())
                continue;
            cover += bound_info.getCover();
        }
        return cover;
    }
    
    private int size_of_key( char[] key ){
        int i;
        int length = key.length;
        for( i=0; i<length; ++i){
            if(key[i] == ZERO_CHAR){
                break;
            }
        }
        return i;
    }
    
    public DoubleArray getDoubleArray(){
        return array_;
    }
    
    public List<List<BoundInfo>> seg(char[] query, boolean only_best) throws Exception{
    	return seg(query, only_best, null);
    }
    
    public List<List<BoundInfo>> seg(char[] query, boolean only_best, MarkRltFilter mark_rlt_filter) throws Exception{
        List<List<BoundInfo>> rlt = new ArrayList<List<BoundInfo>>();
        if( query == null || query.length == 0 )
            return rlt;
        //先把所有可能的词都标注出来
        List<BoundInfo> raw_mark_rlt = mark(query, 0, query.length-1);
        List<BoundInfo> mark_rlt = raw_mark_rlt;
        if( mark_rlt_filter != null ){
        	mark_rlt = mark_rlt_filter.filter(raw_mark_rlt);
        }
        //然后找到覆盖率最大的切分方法
        TreeMap<Integer,TreeMap<Integer,Integer>> matrix = fillMatrix( mark_rlt );
        List<List<BoundInfo>> cands = null;
        if( mark_rlt.size() < 5 ){
        	cands = matrixToCands(query, matrix);
        }else{
        	cands = forwordMatrixToCands(query, matrix);
        }
        List<List<BoundInfo>> sorted_cands = sortCands(cands);
        if(only_best){
            rlt.add(sorted_cands.get(0));
        }else{
            rlt.addAll(sorted_cands);
        }
        return rlt;
    }
    
    //前向最大匹配生成且切分候选集
    public List<List<BoundInfo>> forwordMatrixToCands( char[] query, TreeMap<Integer,TreeMap<Integer,Integer>> matrix ){
    	int max_end = query.length-1;
    	int now_beg = 0;
    	List<BoundInfo> forword_bounds = new ArrayList<BoundInfo>();
    	TreeMap<Integer,Integer> end_to_id = null;
    	BoundInfo new_bi = null;
    	while(true){
    		if(now_beg > max_end){
    			break;
    		}
    		if( !matrix.containsKey(now_beg) ){
    			new_bi = BoundInfo.createEmptyBoundInfo(now_beg, now_beg);
    			forword_bounds.add(new_bi);
    			now_beg += 1;
    			continue;
    		}
    		end_to_id = matrix.get(now_beg);
    		if( end_to_id == null || end_to_id.isEmpty() ){
    			new_bi = BoundInfo.createEmptyBoundInfo(now_beg, now_beg);
    			forword_bounds.add(new_bi);
    			now_beg += 1;
    			continue;
    		}
    		for( Integer last_end : end_to_id.descendingKeySet() ){
    			Integer last_id = end_to_id.get(last_end);
    			new_bi = new BoundInfo(now_beg,last_end,last_id);
    			now_beg = last_end + 1;
    			forword_bounds.add(new_bi);
    			break;
    		}
    	}
    	List<List<BoundInfo>> rlt = new ArrayList<List<BoundInfo>>();
    	rlt.add(forword_bounds);
    	return rlt;
    }
    
    private List<List<BoundInfo>> sortCands( List<List<BoundInfo>> cands ){
        TreeMap<Integer,TreeMap<Integer,List<List<BoundInfo>>>> cover2count2cands = new TreeMap<Integer,TreeMap<Integer,List<List<BoundInfo>>>>();
        int cover = 0;
        int info_count = 0;
        for(List<BoundInfo> cand : cands){
            cover = computeCover(cand);
            info_count = cand.size();
            if(!cover2count2cands.containsKey(cover))
                cover2count2cands.put(cover, new TreeMap<Integer,List<List<BoundInfo>>>());
            TreeMap<Integer,List<List<BoundInfo>>> count2cands = cover2count2cands.get(cover);
            if(!count2cands.containsKey(info_count))
                count2cands.put(info_count, new ArrayList<List<BoundInfo>>());
            List<List<BoundInfo>> t_cands = count2cands.get(info_count);
            t_cands.add(cand);
        }
        List<List<BoundInfo>> rlt = new ArrayList<List<BoundInfo>>();
        TreeMap<Integer,List<List<BoundInfo>>> count2cands = null;
        List<List<BoundInfo>> t_cands = null;
        for(Integer new_cover : cover2count2cands.descendingKeySet() ){
            count2cands = cover2count2cands.get(new_cover);
            for(Integer count : count2cands.keySet()){
                t_cands = count2cands.get(count);
                rlt.addAll(t_cands);
            }
        }
        return rlt;
    }
    
    //将矩阵形式的标注结果，抓换成为列表形式的结果
    private List<List<BoundInfo>> matrixToCands(
            char[] query,
            TreeMap<Integer,TreeMap<Integer,Integer>> matrix){
        //通过begin和end来索引以前遍历已经生成了的情况，如果后续又遇到相同范围内的数据，可以直接取
        TreeMap<Integer, TreeMap<Integer, List<List<BoundInfo>>>> cand_cache = new TreeMap<Integer, TreeMap<Integer,List<List<BoundInfo>>>>();
        List<List<BoundInfo>> rlt = recMatrixToCands(matrix,0,query.length-1,cand_cache);
        return rlt;
    }
    
    private List<List<BoundInfo>> recMatrixToCands(
        TreeMap<Integer,TreeMap<Integer,Integer>> matrix,
        int begin,
        int end,
        TreeMap<Integer, TreeMap<Integer, List<List<BoundInfo>>>> cand_cache
        ){
        List<List<BoundInfo>> cands = new ArrayList<List<BoundInfo>>();
        if(begin>end)
            return cands;
        //try to find in cache
        if( cand_cache.containsKey(begin) ){
            TreeMap<Integer, List<List<BoundInfo>>> now_row = cand_cache.get(begin);
            if( now_row.containsKey(end) )
                return now_row.get(end);
        }
        
        List<BoundInfo> head_bounds = new ArrayList<BoundInfo>();
        int min_end = end;
        //can not find rlt in cache
        TreeMap<Integer,Integer> now_row = null;
        for( int i=begin; i<=end; i++ ){
            if( i>min_end )
                break;
            if( !matrix.containsKey(i) )
                continue;
            now_row = matrix.get(i);
            for( Integer now_end : now_row.keySet() ){
                if( min_end > now_end )
                    min_end = now_end;
                head_bounds.add(new BoundInfo(i,now_end,now_row.get(now_end)));
            }
        }
        if( head_bounds.isEmpty() ){
            List<BoundInfo> cand = new ArrayList<BoundInfo>();
            for(int i=begin; i<=end; i++)
                cand.add(BoundInfo.createEmptyBoundInfo(i,i));
            cands.add(cand);
        }
        List<BoundInfo> head_cand = null;
        for( BoundInfo head_bound : head_bounds ){
            head_cand = new ArrayList<BoundInfo>();
            for( int i=begin; i<head_bound.getBegin(); i++ )
                head_cand.add(BoundInfo.createEmptyBoundInfo(i,i));
            head_cand.add(head_bound);
            List<List<BoundInfo>> son_cands = 
                    recMatrixToCands(matrix, head_bound.getEnd()+1, end, cand_cache);
            if( son_cands == null || son_cands.isEmpty() ){
                cands.add(head_cand);
            }else{
                for( List<BoundInfo> son_cand : son_cands ){
                    List<BoundInfo> t_cand = new ArrayList<BoundInfo>();
                    t_cand.addAll(head_cand);
                    t_cand.addAll(son_cand);
                    cands.add(t_cand);
                }
            }
        }
        if(!cand_cache.containsKey(begin)){
            cand_cache.put(begin, new TreeMap<Integer, List<List<BoundInfo>>>());
        }
        TreeMap<Integer, List<List<BoundInfo>>> new_row = cand_cache.get(begin);
        new_row.put(end, cands);
        return cands;
    }
    /**
     * 将列表形式的BoundInfo，填充成为矩阵形式
     * @param mark_rlt
     * @return
     */
    private TreeMap<Integer,TreeMap<Integer,Integer>> fillMatrix(List<BoundInfo> mark_rlt){
        TreeMap<Integer,TreeMap<Integer,Integer>> rlt = new TreeMap<Integer,TreeMap<Integer,Integer>>();
        Integer now_begin = null;
        TreeMap<Integer,Integer> row = null;
        for(BoundInfo bound_info : mark_rlt){
            now_begin = bound_info.getBegin();
            if( !rlt.containsKey(now_begin) ){
                rlt.put(now_begin, new TreeMap<Integer,Integer>());
            }
            row = rlt.get(now_begin);
            row.put(bound_info.getEnd(), bound_info.getID());
        }
        return rlt;
    }

    public List<BoundInfo> mark(char[] query, int start, int stop){
        List<BoundInfo> rlt = new ArrayList<BoundInfo>();
        if( start > stop )
            return rlt;
        char[] sub_query = null;
        List<ResultPairType> rpt_list = null;
        for(int i=start; i<=stop; i++ ){
//            sub_query = StrHelper.arrayCopy(query, i, stop+1);
            rpt_list = new ArrayList<ResultPairType>();
            commonPrefixSearch(sub_query, rpt_list, 100);
            for(ResultPairType rpt : rpt_list){
            	if( !rpt.isLegal() ){
            		continue;
            	}
                rlt.add(resultPairTypeToBoundInfo(i, rpt));
            }
        }
        return rlt;
    }
    
    /**
     * 将前缀匹配的结果集元素ResultPairType转换成为BoundInfo
     * @param start
     * @param rpt
     * @return
     */
    private static BoundInfo resultPairTypeToBoundInfo(int start, ResultPairType rpt){
        BoundInfo rlt = new BoundInfo();
        rlt.setBegin(start);
        rlt.setEnd(start+rpt.getLength()-1);
        rlt.setID(rpt.getValue());
        return rlt;
    }
    
    /**
     * 清空词典内容，重新初始化
     */
    public void clear(){
        if( no_delete_ ){
            array_ = null;
        }
        array_ = null;
        no_delete_ = false;
    }
    
    public void set_array( DoubleArray ptr, int size ){
        clear();
        array_ = ptr;
        no_delete_ = true;
    }
    
    /**
     * 根据输入的词和词对应信息，构建词典,待构建的词典中的词必须按照词典序从小到大排序
     * @param key_size
     * @param key
     * @param length
     * @return
     */
    public int build(
        int key_size,
        char[][] key,
        int[] length
        ){
        if(key_size<=0 || key==null){
            return 0;
        }
        key_ = key;
        length_ = length;
        key_size_ = key_size;
        progress_ = 0;
                
        array_ = new DoubleArray();
        array_.resize(16);
        array_.setBase(0, 1);
        next_check_pos_ = 0;
        
        NodeT root_node = new NodeT();
        root_node.left = 0;
        root_node.right = key_size;
        root_node.depth = 0;
        
        List<NodeT> siblings = new ArrayList<NodeT>();
        fetch(root_node,siblings);
        if(error_<0){
        	logger.error(String.format("double array build failed, error:[%s]",error_));
        }
        insert(siblings);
        
        array_.clean_used();
        return error_;
    }
//
//    public int open(String fn) throws IOException{
//    	FileInputStream fis = null;
//    	DataInputStream dis = null;
//    	try{
//			fis = new FileInputStream(fn);
//			dis = new DataInputStream(fis);
//			int unit_size = dis.readInt();
//			int max_use_pos = dis.readInt();
//			int[] new_base = readIntArrayFromDataInputStream(dis, unit_size);
//			int[] new_check = readIntArrayFromDataInputStream(dis, unit_size);
//			DoubleArray t_array = new DoubleArray();
//			t_array.init(new_base, new_check, max_use_pos);
//			array_ = t_array;
////			String t = "chenyixunlizhi";
////			List<List<BoundInfo>> list = this.seg(t.toCharArray(), true);
//		    return 0;
//    	}finally{
//    		dis.close();
//    		fis.close();
//    	}
//    }
//
    /**
     * 因为文件句柄不知道何时关闭，所以不再使用
     * 加载二进制的词典文件
     * @param fn
     * @return
     * @throws IOException 
     */
    public int open_memory_map(String fn) throws IOException{
    	FileInputStream fis = null;
    	FileChannel fc = null;
    	try{
    		try{
				logger.info(String.format("before open double array file [%s], try copy it",fn));
//				FileHelper.copyFile(fn+".new", fn);
			}catch(Exception e){
				logger.error(String.format("before open double array file [%s], can not copy it",fn));
			}
			fis = new FileInputStream(fn);
			fc = fis.getChannel();
			long byte_size = fc.size();
			MappedByteBuffer in = fc.map(MapMode.READ_ONLY, 0, byte_size);
			int unit_size = in.getInt();
			int max_use_pos = in.getInt();
			int[] new_base = new int[unit_size];
			readIntArrayFromMappedBuffer(in, new_base, unit_size);
			int[] new_check = new int[unit_size];
			readIntArrayFromMappedBuffer(in, new_check, unit_size);
			DoubleArray t_array = new DoubleArray();
			t_array.init(new_base, new_check, max_use_pos);
			array_ = t_array;
		    return 0;
    	}finally{
    		logger.info("double array bin file start close");
    		fc.close();
    		fis.close();
    		fc = null;
    		fis = null;
    		try{
				logger.info(String.format("after open double array file [%s], try copy it",fn));
//				FileHelper.copyFile(fn+".new", fn);
			}catch(Exception e){
				logger.error(String.format("after open double array file [%s], can not copy it",fn));
			}
    		logger.info("double array bin file finished close");
    	}
    }
    
//    private int[] readIntArrayFromDataInputStream(
//    	DataInputStream dis,
//    	int size
//    	) throws IOException{
//    	int byte_size = size * 4;
//    	byte[] bytes = new byte[byte_size];
//    	dis.readFully(bytes);
////        int[] int_array = DataHelper.byteArray2intArray(bytes);
//        int[] int_array = DataHelper.byteArray2intArray(bytes);
//        return int_array;
//    }
    
    private void readIntArrayFromMappedBuffer(MappedByteBuffer in, int[] array, int size){
        for( int i=0; i<size; i++ ){
            array[i] = in.getInt();
        }
    }
    
    public int save(String fn){
    	FileOutputStream fos = null;
    	BufferedOutputStream bos = null;
    	DataOutputStream dos = null;
    	try {
    		fos = new FileOutputStream(fn);
    		bos = new BufferedOutputStream(fos);
    		dos = new DataOutputStream(bos);
            //将词典压缩
            array_.compress();
            dos.writeInt(array_.getUnitSize());
            dos.writeInt(array_.getMaxUsePos());
//            writeIntArrayToDataOutputStream(array_.getBaseArray(),dos);
//            writeIntArrayToDataOutputStream(array_.getCheckArray(),dos);
            return 0;
        } catch (Exception e) {
//        	logger.error(ExceptionHelper.getExceptionTraceStr(e));
            return -1;
        }finally{
        	try {
	            dos.close();
	            bos.close();
	            fos.close();
            } catch (IOException e) {
//            	logger.error(ExceptionHelper.getExceptionTraceStr(e));
            }
        }
    }
    
    /**
     * 因内存映射会导致句柄无法回收，不再使用
     * 保存二进制的词典文件
     */
    public int save_memory_map(
        String fn){
        try {
            @SuppressWarnings("resource")
            RandomAccessFile raf = new RandomAccessFile(fn,"rw");
            FileChannel fc = raf.getChannel();
            //将词典压缩
            array_.compress();
            //将节点单元的大小写入首个int，再写入最大使用位置，后续跟base的array和check的array
            int need_int_size = 4 + 4 + 2*array_.getUnitSize();
            int need_size = need_int_size * 4;
            MappedByteBuffer out = fc.map(MapMode.READ_WRITE, 0, need_size);
            out.putInt(array_.getUnitSize());
            out.putInt(array_.getMaxUsePos());
            writeIntArrayToMappedBuffer(array_.getBaseArray(),out);
            writeIntArrayToMappedBuffer(array_.getCheckArray(),out);
            fc.close();
            return 0;
        } catch (FileNotFoundException e) {
            return -1;
        } catch (IOException e) {
            return -1;
        }
    }
    
//    private void writeIntArrayToDataOutputStream(
//    	int[] array, DataOutputStream dos) throws IOException{
//    	byte[] bytes = DataHelper.intArray2byteArray(array);
//    	dos.write(bytes);
//    }
    
    private void writeIntArrayToMappedBuffer( int[] array, MappedByteBuffer out ){
        for(int i=0; i<array.length; i++){
            out.putInt(array[i]);
        }
    }
    
    /**
     * 精确匹配查找一个key所对应的value
     * @param key
     * @return
     */
    public ResultPairType exactMatchSearch(
        char[] key){
        return exactMatchSearch(key,0,0);
    }
    public ResultPairType exactMatchSearch(
        char[] key,
        int len,
        int node_pos
        ){
        if( len == 0 ){
            len = size_of_key(key);
        }
        ResultPairType result = new ResultPairType();
        result.set_result(-1, 0);
        if(!array_.in_range(node_pos)){
            return result;
        }
        int b =array_.getBase(node_pos);
        int p;
        for( int i=0; i<len; ++i ){
            p = b + (int)key[i]+1;
            if( b == array_.getCheck(p) ){
                b = array_.getBase(p);
            }else{
                return result;
            }
        }
        p = b;
        int n = array_.getBase(p);
        if( b == array_.getCheck(p) && n<0 ){
            result.set_result(-n-1, len);
        }
        return result;
    }
    
    /**
     * 寻找公共前缀匹配的结果
     * @param key
     * @param result
     * @param result_len
     * @return
     */
    public int commonPrefixSearch(
        char[] key,
        List<ResultPairType> result,
        int result_len
        ){
        return commonPrefixSearch(key, result, result_len, 0, 0);
    }
    public int commonPrefixSearch(
        char[] key,
        List<ResultPairType> result,
        int result_len,
        int len,
        int node_pos
            ){
        if(len==0){
            len = size_of_key(key);
        }
        int num = 0;
        if( !array_.in_range(node_pos) ){
            return num; 
        }
        int b = array_.getBase(node_pos);
        int n;
        int p;
        
        for( int i=0; i<len; ++i ){
            p = b;
            if(!array_.in_range(p)){
                return num;
            }
            n = array_.getBase(p);
            if( b == array_.getCheck(p) && n<0 ){
                if( num<result_len ){
                    ResultPairType new_result = new ResultPairType(); 
                    new_result.set_result(-n-1, i);
                    result.add(new_result);
                }
                ++num;
            }
            p = b + (int)key[i]+1;
            if( !array_.in_range(p) ){return num;}
            if( b == array_.getCheck(p) ){
                b = array_.getBase(p);
            }else{
                return num;
            }
        }
        
        p = b;
        if(!array_.in_range(p)){
            return num;
        }
        n = array_.getBase(p);
        if( b == array_.getCheck(p) && n<0 ){
            if( num<result_len ){
                ResultPairType new_result = new ResultPairType(); 
                new_result.set_result(-n-1, len);
                result.add(new_result);
            }
            ++num;
        }
        return num;
    }

//    public TraverseResult traverse(
//        char[] key,
//        int node_pos,
//        int key_pos){
//        return traverse(key, node_pos, key_pos, 0);
//    }
//    public TraverseResult traverse(
//        char[] key,
//        int node_pos,
//        int key_pos,
//        int len){
//        if(len == 0){
//            len = size_of_key(key);
//        }
//        int b = array_.getBase(node_pos);
//        int p;
//        for( ;key_pos < len; ++key_pos){
//            p = b + (int)key[key_pos] + 1;
//            if( b == array_.getCheck(p) ){
//                node_pos = p;
//                b = array_.getBase(p);
//            }else{
//                return new TraverseResult(-2, node_pos, key_pos);//no node
//            }
//        }
//        p=b;
//        int n = array_.getBase(p);
//        if( b == array_.getCheck(p) && n<0 ){
//            return new TraverseResult(-n-1, node_pos, key_pos);
//        }
//        return new TraverseResult(-1, node_pos, key_pos);//found, but no value
//    }
    
    public int fetch( NodeT parent, List<NodeT> siblings ){
        if( error_ < 0 ){
            return 0;
        }
        int prev = 0;
        for(int i = parent.left; i<parent.right; ++i){
            int t_depth = length_ != null ? length_[i] : size_of_key(key_[i]);
            if( t_depth < parent.depth ){
                continue;
            }
            char[] tmp = key_[i];
            int cur = 0;
            if( t_depth != parent.depth ){
                cur = (int)tmp[parent.depth] + 1;
            }
            if( prev > cur ){
                error_ = -3;
                return 0;
            }
            if(cur != prev || siblings.isEmpty()){
                NodeT tmp_node = new NodeT();
                tmp_node.depth = parent.depth + 1;
                tmp_node.code = cur;
                tmp_node.left = i;
                if( !siblings.isEmpty() ){
                    siblings.get(siblings.size()-1).right = i;
                }
                siblings.add(tmp_node);
            }
            prev = cur;
        }
        
        if( !siblings.isEmpty() ){
            siblings.get(siblings.size()-1).right = parent.right;
        }
        return siblings.size();
    }
    
    /**
     * 插入一批兄弟节点
     * @param siblings
     * @return
     */
    public int insert( List<NodeT> siblings ){
        if(error_ < 0){
            return 0;
        }
        int begin = 0;
        int pos = Math.max(siblings.get(0).code + 1, next_check_pos_)-1;
        int nonzero_num = 0;
        int first = 0;
        if(array_.getUnitSize() <= pos){array_.resize(pos+1);}
        while(true){
            ++pos;
            if(array_.getUnitSize() <= pos){array_.resize(pos+1);}
            if(array_.getCheck(pos)!=0){
                ++nonzero_num;
                continue;
            }else if(first==0){
                next_check_pos_ = pos;
                first = 1;
            }
            
            begin = pos-siblings.get(0).code;
            int new_end_pos = begin+siblings.get(siblings.size()-1).code;
            if( array_.getUnitSize() <= new_end_pos ){
                int new_size = (int)(new_end_pos * 1.25);
                array_.resize(new_size);
            }
            
            if( array_.getUsed(begin) != 0 ){
                continue;
            }
            
            boolean has_conflict = false;
            for( int i=1; i<siblings.size(); ++i ){
                int check_pos = begin + siblings.get(i).code;
                boolean now_has_conflict = array_.getCheck(check_pos) != 0;
                if( now_has_conflict ){
                    has_conflict = now_has_conflict;
                    break;
                }
            }
            //只有找到了没有冲突的位置才能进行下面的过程
            if( !has_conflict ){
                break;
            }
        }
        
        /*
         * -- Simple heuristics --
         * if the percentage of non-empty contents in check between the index
         * 'next_check_pos' and 'check' is greater than some constant
         * value(e.g. 0.95),
         * new 'next_check_pos' index is written by 'check'.
         */
        if( ((float)nonzero_num)/(pos - next_check_pos_ + 1) >= 0.95 ){
            next_check_pos_ = pos;
        }
        
        array_.setUsed(begin, 1);
        array_.setMaxUsePos(Math.max(array_.getMaxUsePos(), begin+siblings.get(siblings.size()-1).code+1));
        for( int i=0; i<siblings.size(); i++ ){
            array_.setCheck(begin+siblings.get(i).code, begin);
        }
        for( int i=0; i<siblings.size(); ++i ){
            List<NodeT> new_siblings = new ArrayList<NodeT>();
            NodeT now_sibling = siblings.get(i);
            int now_base_pos = begin+now_sibling.code;
            if( 0==fetch(now_sibling,new_siblings) ){
                int new_base_value = -now_sibling.left-1;
                array_.setBase(now_base_pos, new_base_value);
                ++progress_;
            }else{
                int h = insert(new_siblings);
                array_.setBase(now_base_pos, h);
            }
        }
        return begin;
    }
}
