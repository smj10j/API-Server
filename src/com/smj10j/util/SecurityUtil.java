package com.smj10j.util;


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.smj10j.conf.FatalException;

public class SecurityUtil {

	private static final String HMACSHA512_ALGORITHM = "HmacSHA512";
	private static final String HMACSHA1_ALGORITHM = "HmacSHA1";
	
	/**
	 * @param text
	 * @return a 32-byte hex string
	 * @throws FatalException
	 */
	public static String md5(String text) throws FatalException {
		if(text == null)
			text = "";
        MessageDigest md;
        byte[] md5hash = new byte[32];
        try {
			md = MessageDigest.getInstance("MD5");
	        md.update(text.getBytes("iso-8859-1"), 0, text.length());
	        md5hash = md.digest();
        } catch (NoSuchAlgorithmException e) {
			throw new FatalException(e);
		} catch (UnsupportedEncodingException e) {
			throw new FatalException(e);
		}	        
		return convertToHex(md5hash);
	}
	
	/**
	 * @param data
	 * @param key
	 * @return a sha512 encrypted string
	 * @throws FatalException
	 */
	public static String calculateSignature(String data, String key) throws FatalException {
		return sha512(data, key);
	}
	
	/**
	 * @param data
	 * @param key
	 * @return a sha1 encrypted string
	 * @throws FatalException
	 */
	public static String sha1(String data, String key) throws FatalException {
		String result;
		try {
			// get an hmac_sha1 key from the raw key bytes
			SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMACSHA1_ALGORITHM);
			
			// get an hmac_sha1 Mac instance and initialize with the signing key
			Mac mac = Mac.getInstance(HMACSHA1_ALGORITHM);
			mac.init(signingKey);
			
			// compute the hmac on input data bytes
			byte[] rawHmac = mac.doFinal(data.getBytes());
			
			// base64-encode the hmac
			result = Base64.encode(rawHmac);
			
			result = result.replace("==", "");
			
        } catch (Exception e) {
        	throw new FatalException("Failed to generate HMAC : " + e.getMessage());
        }
        return result;
	}	
	
	/**
	 * @param data
	 * @param key
	 * @return a sha512 encrypted string
	 * @throws FatalException
	 */
	public static String sha512(String data, String key) throws FatalException {
		String result;
		try {
			// get an hmac_sha1 key from the raw key bytes
			SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMACSHA512_ALGORITHM);
			
			// get an hmac_sha1 Mac instance and initialize with the signing key
			Mac mac = Mac.getInstance(HMACSHA512_ALGORITHM);
			mac.init(signingKey);
			
			// compute the hmac on input data bytes
			byte[] rawHmac = mac.doFinal(data.getBytes());
			
			// base64-encode the hmac
			result = Base64.encode(rawHmac);
			
			result = result.replace("==", "");
			
        } catch (Exception e) {
        	throw new FatalException("Failed to generate HMAC : " + e.getMessage());
        }
        return result;
	}	
	
	public static String cleanHash(String signature) {
		signature = signature.replace(" ", "+");
		signature = signature.replace("==", "");
		return signature;
	}
	
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }	
}
