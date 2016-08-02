package org.voovan.tools;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * 类文字命名
 *
 * @author helyho
 *
 * Java Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TByteBuffer {
    /**
     * 将ByteBuffer转换成 byte 数组
     * @param bytebuffer ByteBuffer 对象
     * @return byte 数组
     */
    public static byte[] toArray(ByteBuffer bytebuffer){
        if(!bytebuffer.hasArray()) {
            int oldPosition = bytebuffer.position();
            bytebuffer.position(0);
            int size = bytebuffer.limit();
            byte[] buffers = new byte[size];
            bytebuffer.get(buffers);
            bytebuffer.position(oldPosition);
            return buffers;
        }else{
            return bytebuffer.array();
        }
    }

    /**
     * 将 Bytebuffer 转换成 字符串
     * @param bytebuffer Bytebuffer 对象
     * @param charset 字符集
     * @return 字符串对象
     * @throws UnsupportedEncodingException 不支持的字符集对象
     */
    public static String toString(ByteBuffer bytebuffer,String charset) throws UnsupportedEncodingException {
        return new String(toArray(bytebuffer), charset);
    }
}
