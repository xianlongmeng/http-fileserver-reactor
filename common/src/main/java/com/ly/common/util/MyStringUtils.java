package com.ly.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MyStringUtils {

    private MyStringUtils() {
    }

    /**
     * 求MD5值，返回16进制字符串
     * 
     * @param str
     * @return
     */
    public static String md5DigestAsHexString(String str) {
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("MD5");
            m.update(str.getBytes(StandardCharsets.UTF_8));
            return bytesToHexStr(m.digest(), "");
        } catch (NoSuchAlgorithmException e) {
            return str;
        }
    }

    /**
     * 字节转十六进制
     * 
     * @param b
     *        需要进行转换的byte字节
     * @return 转换后的Hex字符串
     */
    public static String byteToHex(byte b) {
        String hex = Integer.toHexString(b & 0xFF);
        if (hex.length() < 2) {
            hex = "0" + hex;
        }
        return hex;
    }

    public static String bytesToHexStr(byte[] bytes) {
        return bytesToHexStr(bytes, null);
    }

    /**
     * byte转为16进制字符串
     * 
     * @param bytes
     * @param splitStr
     * @return
     */
    public static String bytesToHexStr(byte[] bytes, String splitStr) {
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String strInt = "";
        for (int i = 0; i < bytes.length; i++) {
            if (splitStr != null && !splitStr.isEmpty() && i != 0) {
                sb.append(splitStr);
            }
            strInt = Integer.toHexString(bytes[i] & 0xFF);
            if (strInt.length() < 2) {
                sb.append(0);
            }
            sb.append(strInt.toUpperCase());

        }
        return sb.toString();
    }

    /**
     * Hex字符串转byte
     * 
     * @param inHex
     *        待转换的Hex字符串
     * @return 转换后的byte
     */
    public static byte hexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

    /**
     * hex字符串转byte数组
     * 
     * @param inHex
     *        待转换的Hex字符串
     * @return 转换后的byte数组结果
     */
    public static byte[] hexToByteArray(String inHex) {
        int hexlen = inHex.length();
        byte[] result;
        if (hexlen % 2 == 1) {
            // 奇数
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {
            // 偶数
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = hexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }

}
