package io.github.tankle.suggest;

import java.io.DataInputStream;
import java.io.FileInputStream;

/**
 * Created by IDEA
 * User: hztancong
 * Date: 2017/6/27
 * Time: 16:21
 */
public class DTSuggestTreeFactory {
    /**
     * 加载suggest tree 文件
     * @param fn
     * @return
     * @throws Exception
     */
    public static SuggestTree loadSuggestTree(String fn) throws Exception {
        FileInputStream fis = new FileInputStream(fn);
        DataInputStream dis = new DataInputStream(fis);
        int byte_size = dis.readInt();
        byte[] bytes = new byte[byte_size];
        dis.readFully(bytes);
        SuggestTree suggestTree = (SuggestTree) Serializer.deserialize(bytes);
        dis.close();
        fis.close();

        return suggestTree;
    }
}
