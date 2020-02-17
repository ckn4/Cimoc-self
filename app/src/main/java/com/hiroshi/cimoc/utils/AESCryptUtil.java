package com.hiroshi.cimoc.utils;

import android.util.Base64;

import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESCryptUtil {

    /** 算法/模式/填充 **/
    private static final String CipherMode = "AES/CBC/PKCS7Padding";

    // 创建密钥, 长度为128位(16bytes), 且转成字节格式
    private static SecretKeySpec createKey(String key) {

        byte[] data = null;

        if (key == null) { key = ""; }
        StringBuffer sb = new StringBuffer(16);
        sb.append(key);
        while (sb.length() < 16) { sb.append("0"); }
        if (sb.length() > 16) { sb.setLength(16); }

        try { data = sb.toString().getBytes("UTF-8");}
        catch (UnsupportedEncodingException e) { e.printStackTrace(); }

        return new SecretKeySpec(data, "AES");
    }

    // 创建初始化向量, 长度为16bytes, 向量的作用其实就是salt
    private static IvParameterSpec createIV(String iv) {

        byte[] data = null;

        if (iv == null) { iv = ""; }
        StringBuffer sb = new StringBuffer(16);
        sb.append(iv);
        while (sb.length() < 16) { sb.append("0"); }
        if (sb.length() > 16) { sb.setLength(16); }

        try { data = sb.toString().getBytes("UTF-8"); }
        catch (UnsupportedEncodingException e) { e.printStackTrace(); }

        return new IvParameterSpec(data);
    }

    // 解密字节数组
    public static String decrypt(String content, String key, String iv) {

        try {
            SecretKeySpec secretKeySpec = createKey(key);
            IvParameterSpec ivParameterSpec = createIV(iv);
            Cipher cipher = Cipher.getInstance(CipherMode);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return new String(cipher.doFinal(Base64.decode(content,Base64.DEFAULT)), "UTF-8");
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

}

