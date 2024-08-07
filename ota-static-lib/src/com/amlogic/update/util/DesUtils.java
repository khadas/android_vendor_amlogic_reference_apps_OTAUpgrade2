/******************************************************************
*
*Copyright (C) 2012 Amlogic, Inc.
*
*Licensed under the Apache License, Version 2.0 (the "License");
*you may not use this file except in compliance with the License.
*You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing, software
*distributed under the License is distributed on an "AS IS" BASIS,
*WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*See the License for the specific language governing permissions and
*limitations under the License.
******************************************************************/
package com.amlogic.update.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;

import javax.crypto.Cipher;

import android.util.Log;

public class DesUtils {
	private static final String STRING_KEY = "gjaoun";
	private static final String LOG_TAG = "ChipCheck";

	private Cipher encryptCipher = null;
	private Cipher decryptCipher = null;

	//amlogic encrypt is 7c0f13b6d5986e65
	//AMLOGIC eccrypt is a6f8b4b74ed1ed75
	public static boolean isAmlogicChip() {
		// following code check if chip is amlogic
		String cupinfo = "7c0f13b6d5986e65";
		try {
			DesUtils des = new DesUtils(STRING_KEY);
			//Log.i(LOG_TAG, "encrypt amlogic: " + des.encrypt("AMLOGIC"));
			if (des.decrypt(GetCpuInfo(des)).indexOf(des.decrypt(cupinfo)) != -1) {
				Log.i(LOG_TAG, "matched cpu ");
				return true;
			} else {
				Log.e(LOG_TAG, " Sorry! Your cpu is not Amlogic,u can't run");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	static String byteArr2HexStr(byte[] arrB) throws Exception {
		int iLen = arrB.length;
		StringBuffer sb = new StringBuffer(iLen * 2);

		for (int i = 0; i < iLen; i++) {
			int intTmp = arrB[i];
			while (intTmp < 0) {
				intTmp = intTmp + 256;
			}
			if (intTmp < 16) {
				sb.append("0");
			}

			sb.append(Integer.toString(intTmp, 16));
		}
		return sb.toString();
	}

	static byte[] hexStr2ByteArr(String strIn) throws Exception {
		byte[] arrB = strIn.getBytes();
		int iLen = arrB.length;
		byte[] arrOut = new byte[iLen / 2];

		for (int i = 0; i < iLen; i = i + 2) {
			String strTmp = new String(arrB, i, 2);
			arrOut[i / 2] = (byte) Integer.parseInt(strTmp, 16);
		}
		return arrOut;
	}

	DesUtils() throws Exception {
		this(STRING_KEY);
	}

	DesUtils(String strKey) throws Exception {
		// Security.addProvider(new com.sun.crypto.provider.SunJCE());
		Key key = getKey(strKey.getBytes());
		encryptCipher = Cipher.getInstance("DES");
		encryptCipher.init(Cipher.ENCRYPT_MODE, key);

		decryptCipher = Cipher.getInstance("DES");
		decryptCipher.init(Cipher.DECRYPT_MODE, key);
	}

	byte[] encrypt(byte[] arrB) throws Exception {
		return encryptCipher.doFinal(arrB);
	}

	String encrypt(String strIn) throws Exception {
		return byteArr2HexStr(encrypt(strIn.getBytes()));
	}

	byte[] decrypt(byte[] arrB) throws Exception {
		return decryptCipher.doFinal(arrB);
	}

	String decrypt(String strIn) throws Exception {
		return new String(decrypt(hexStr2ByteArr(strIn)));
	}

	private Key getKey(byte[] arrBTmp) throws Exception {
		byte[] arrB = new byte[8];
		for (int i = 0; i < arrBTmp.length && i < arrB.length; i++) {
			arrB[i] = arrBTmp[i];
		}

		Key key = new javax.crypto.spec.SecretKeySpec(arrB, "DES");
		return key;
	}

	// get cpu info
	static String GetCpuInfo(DesUtils des) {
		String result = null;
		CommandRun cmdexe = new CommandRun();
		try {
			String[] args = { "/system/bin/cat", "/proc/cpuinfo" };
			result = cmdexe.run(args, "/system/bin/");
			result = result.toLowerCase();
			try {
				result = des.encrypt(result);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Log.i(LOG_TAG, result);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return result;
	}

	static class CommandRun {
		public synchronized String run(String[] cmd, String workdirectory)
				throws IOException {
			String result = "";
			try {
				ProcessBuilder builder = new ProcessBuilder(cmd);
				InputStream in = null;

				if (workdirectory != null) {
					builder.directory(new File(workdirectory));
					builder.redirectErrorStream(true);
					Process process = builder.start();
					in = process.getInputStream();
					byte[] re = new byte[1024];
					while (in.read(re) != -1)
						// Log.i(LOG_TAG, new String(re));
						result = result + new String(re);
				}

				if (in != null) {
					in.close();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return result;
		}
	}
}
