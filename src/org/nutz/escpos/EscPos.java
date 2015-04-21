package org.nutz.escpos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * 按ESC/POS协议的要求生成数据包
 * @author wendal<wendal1985@gmail.com>
 *
 */
public class EscPos {
    
    protected ByteArrayOutputStream out;
    
    protected int pix = 384;
    protected String encode = "GB2312";
    
    protected static final byte ESC = 0x1B;
    protected static final byte GS = 0x1D;

    protected EscPos() {
        out = new ByteArrayOutputStream();
    }
    
    public static EscPos start() {
        return new EscPos();
    }
    
    // 这是测试用的方法,使用了部分android的API, TODO 删除Android依赖性.
    public EscPos simple() {
        write("北京德纳科技有限公司").nextLine();
        write("热敏打印机测试V1.0").nextLine();
        write("-----------------------------").nextLine();
        write("字体测试(默认)").nextLine();
        characterFont(1);
        write("字体测试(1)").nextLine();
        write("ABCDEFG12345678").nextLine();
        characterFont(2);
        write("字体测试(2)").nextLine();
        write("ABCDEFG12345678").nextLine();
        characterFont(0);
        write("字体测试(默认)").nextLine();
        write("-----------------------------").nextLine();
        write("对齐测试(默认)").nextLine();
        justification(1);
        write("对齐测试(居中)").nextLine();
        justification(2);
        write("对齐测试(右对齐)").nextLine();
        justification(0);
        write("-----------------------------").nextLine();
//        underline();
//        write("下划线测试").nextLine();
//        underline();
//        write("-----------------------------").nextLine();
        write("条码测试").nextLine();
        br((byte)4, "1341612");
        write("-----------------------------").nextLine();
        write("图像测试(随机点)").nextLine();
        byte[][] bitmap = new byte[200][384/8];
        for (int i = 0; i < bitmap.length; i++) {
            for (int j = 0; j < bitmap[0].length; j++) {
                bitmap[i][j] = (byte)(Math.random() * 255);
            }
        }
        bitmapV0(bitmap);
        nextLine();
        write("图像测试(PNG图片)").nextLine();
        Bitmap bp = BitmapFactory.decodeFile("/sdcard/dnet_bitmap.png");
        for (int i = 0; i < bitmap.length; i++) {
            for (int j = 0; j < bitmap[0].length; j++) {
                int z = 0;
                for (int k = 0; k < 8; k++) {
                    z = z*2 + (bp.getPixel(j*8+k, i) == -1 ? 0 : 1);
                    //System.out.println(bp.getPixel(i, j*8+k));
                }
                bitmap[i][j] = (byte)z;
//                System.out.println(z);
            }
        }
        bitmapV0(bitmap);
        nextLine();

        write("-----------------------------").nextLine();
        write("二维码测试").nextLine();
        bp = BitmapFactory.decodeFile("/sdcard/qr.png");
        bitmap = new byte[bp.getHeight()][bp.getWidth()/8];
        for (int i = 0; i < bitmap.length; i++) {
            for (int j = 0; j < bitmap[0].length; j++) {
                int z = 0;
                for (int k = 0; k < 8; k++) {
                    z = z*2 + (bp.getPixel(j*8+k, i) == -1 ? 0 : 1);
                    //System.out.println(bp.getPixel(j*8+k, i));
                }
                bitmap[i][j] = (byte)z;
            }
        }
        bitmapV0(bitmap);
        nextLine();
        write("-----------------------------").nextLine();
        feedLine(5);
        return this;
    }
    
    /**
     * 得到最终数据
     */
    public byte[] data() {
        return out.toByteArray();
    }

    public EscPos write(byte...data) {
        try {
            out.write(data);
        }
        catch (IOException e) {
        }
        return this;
    }
    
    /**
     * 按字符集配置写入字符串
     */
    public EscPos write(String str) {
        try {
            return write(str.getBytes(encode));
        }
        catch (UnsupportedEncodingException e) {
            return this;
        }
    }
    
    /**
     * 每行的像素值,在图片输出的时候需要
     */
    public EscPos _Line(int pix) {
        this.pix = pix;
        return this;
    }
    
    /**
     * 设置编码, 用于写入String时
     */
    public EscPos _Encode(String encode) {
        this.encode = encode;
        return this;
    }
    
    //----------------------------------------------------------------------------------
    // 真正的命令现在才开始
    //----------------------------------------------------------------------------------
    
    public EscPos printf(String fmt, Object ...args) {
        return write(String.format(fmt, args));
    }
    
    /**
     * 简单换行
     */
    public EscPos nextLine() {
        return write("\n");
    }
    
    public EscPos printMode(byte mode) {
        return write(ESC, (byte)0x21, mode);
    }
    
    /**
     * 重新初始化
     */
    public EscPos init() {
        return write(ESC, (byte)0x40);
    }
    
    /**
     * 写入一个条形码
     */
    public EscPos br(byte type, String value) {
        write(GS, (byte)0x6B, type);
        write(value);
        return write((byte)0x00);
    }
    
    /**
     * 字体
     */
    public EscPos characterFont(int type) {
        return write(ESC, (byte)0x4D, (byte)type);
    }
    
    /**
     * 对齐方式
     */
    public EscPos justification(int type) {
        return write(ESC, (byte)0x61, (byte)type);
    }
    
    /**
     * 填充空行
     */
    public EscPos feedLine(int n) {
        return write(ESC, (byte)0x64, (byte)n);
    }
    
    /**
     * 反向填充空行
     */
    public EscPos rfeedLine(int n) {
        return write(ESC, (byte)0x65, (byte)n);
    }
    
//    public EscPos underline() {
//        return write(ESC, (byte)'-');
//    }
    
    /**
     * 按行取模的方式输出图像数据
     */
    public EscPos bitmapV0(byte[][] data) {
        write(GS, (byte)'v', (byte)'0', (byte)0);
        write((byte)data[0].length, (byte)0, (byte)data.length, (byte)0);
        for (int i = 0; i < data.length; i++) {
            write(data[i]);
        }
        return this;
    }
}
