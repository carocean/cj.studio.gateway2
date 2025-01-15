//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cj.studio.gateway.mic;

import java.security.MessageDigest;

public class Encript {
    private static final String[] hexDigits = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};

    public Encript() {
    }

    public static String md5(String inputStr) {
        return encodeByMD5(inputStr);
    }

    public static boolean authenticatePassword(String password, String inputString) {
        return password.equals(encodeByMD5(inputString));
    }

    private static String encodeByMD5(String originString) {
        if (originString != null) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] results = md5.digest(originString.getBytes());
                String result = byteArrayToHexString(results);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();

        for(int i = 0; i < b.length; ++i) {
            resultSb.append(byteToHexString(b[i]));
        }

        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (b < 0) {
            n = 256 + b;
        }

        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    public static String sha1(String str) {
        char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        try {
            MessageDigest mdTemp = MessageDigest.getInstance("SHA1");
            mdTemp.update(str.getBytes("UTF-8"));
            byte[] md = mdTemp.digest();
            int j = md.length;
            char[] buf = new char[j * 2];
            int k = 0;

            for(int i = 0; i < j; ++i) {
                byte byte0 = md[i];
                buf[k++] = hexDigits[byte0 >>> 4 & 15];
                buf[k++] = hexDigits[byte0 & 15];
            }

            return new String(buf);
        } catch (Exception var9) {
            return null;
        }
    }

    public static void main(String... args) {
        String appKey = "995C2A861BE8064A1F8A022B5C0D2E36";
        String appSecret = "6EA4774EE78DCDF0768CA18ECF3AD1DB";
        String nonce = "7696317939417951096";
        String text = String.format("%s%s%s", appKey, nonce, appSecret);
        String sign = md5(text);
        System.out.println(String.format("appKey=%s", appKey));
        System.out.println(String.format("appSecret=%s", appSecret));
        System.out.println(String.format("nonce=%s", nonce));
        System.out.println(String.format("sign=%s", sign));
    }
}
