package com.lyc.wangzhan.utils;

import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HttpCookies {
	static List<HttpCookie> globalCookies;
	static List<HttpCookie> globalCookiesWithFingerprint;
	static String csrf;
	static String refreshToken;

	public static List<HttpCookie> convertCookies(String cookie) {
		String lines[] = cookie.split("\n");
		if (lines.length >= 2) {
			refreshToken = lines[1].trim();
		}
		List<HttpCookie> iCookies = new ArrayList<HttpCookie>();
		String[] cookieStrs = lines[0].replaceAll("\\||\r|\n| |\\[|\\]|\"", "").split(",|;|&");
		for (String cookieStr : cookieStrs) {
			String entry[] = cookieStr.split("=");
			HttpCookie cCookie = new HttpCookie(entry[0], entry[1]);
			iCookies.add(cCookie);
		}
		return iCookies;
	}

	/**
	 * @deprecated	推荐使用HttpCookies.globalCookiesWithFingerprint()
	 * @return	不带指纹的全局cookie
	 */
	public static List<HttpCookie> getGlobalCookies() {
		return globalCookies;
	}

	public static List<HttpCookie> globalCookiesWithFingerprint() {
		String cookieConfig = "sid=oepgnjf8, DedeUserID__ckMd5=e5218b17dbdb0f5d, DedeUserID=315275999, bili_jct=5881ec5cfb37d96b7751987fccaabd49, SESSDATA=413dd822%2C1746551129%2Cbd1ba%2Ab2CjA4bLJEEitVhNneEMbCUU6wIn0Dk7TppClXqHm59yj16Zg4C8jbfLyQ9ayvhIXyj5ISVl9uUXBEeXFPcFp3YkVXc09IUF9ZZDFlcWpiVV9DRWlEbTVwOGZpZktaVVNwT3FmNWE0M0hEQzIzSVI2cnFtWkExV1hIcklKc0JvNVFSWHVkRWJ1amZRIIEC\n22fe8792c176db0b11611e3b1f65ccb2";
		List<HttpCookie> cookies = HttpCookies.convertCookies(cookieConfig);
		return cookies;
	}

	public static void setGlobalCookies(List<HttpCookie> globalCookies) {
		HttpCookies.globalCookies = globalCookies;
		csrf = null;
		globalCookiesWithFingerprint = null;
	}

	public static String getCsrf() {
		if (csrf == null && globalCookies != null) {
			for (HttpCookie cookie : globalCookies) {
				if ("bili_jct".equals(cookie.getName())) {
					csrf = cookie.getValue();
				}
			}
		}
		return csrf;
	}

	public static String getRefreshToken() {
		return refreshToken;
	}

	public static void setRefreshToken(String refreshToken) {
		HttpCookies.refreshToken = refreshToken;
	}

	public static String map2CookieStr(Map<String, String> kvMap) {
		if (kvMap.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : kvMap.entrySet()) {
			sb.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
		}
		return sb.substring(0, sb.length() - 2);
	}

	public static String get(String key) {
		for (HttpCookie cookie : globalCookiesWithFingerprint()) {
			if (key.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public static boolean set(String key, String value) {
		for (HttpCookie cookie : globalCookiesWithFingerprint()) {
			if (key.equals(cookie.getName())) {
				cookie.setValue(value);
				return true;
			}
		}
		HttpCookie cCookie = new HttpCookie(key, value);
		globalCookiesWithFingerprint().add(cCookie);
		return false;
	}
	static Pattern pResolution = Pattern.compile("\"6e7c\":\"(\\d+)x(\\d+)\"");
	public static String genNewFingerprint() {
		try {
			Map<String, String> kvMap = new HashMap<>();
			long currentTime = System.currentTimeMillis();
			// 获取 buvid3 b_nut i-wanna-go-back b_ut 有效期一年
			HttpURLConnection conn = (HttpURLConnection) new URL("https://www.bilibili.com/").openConnection();
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.1 Safari/605.1.15");
			//host www.bilibili.com
			conn.setRequestProperty("Host", "www.bilibili.com");
			conn.connect();
			List<String> setCookie = conn.getHeaderFields().get("Set-Cookie");
			setCookie = setCookie != null ? setCookie : conn.getHeaderFields().get("set-cookie");
			for (String c : setCookie) {
				String[] kv = c.split(";", 2)[0].split("=", 2);
				kvMap.put(kv[0].trim(), kv[1].trim());
			}
			kvMap.put("i-wanna-go-back", "-1");
			// 生成 b_lsid
			String b_lsid = ResourcesUtil.randomHex(8) + "_" + Long.toHexString(currentTime).toUpperCase();
			kvMap.put("b_lsid", b_lsid);
			// 生成 _uuid
			String _uuid = new StringBuilder().append(ResourcesUtil.randomHex(8)).append("-")
					.append(ResourcesUtil.randomHex(4)).append("-").append(ResourcesUtil.randomHex(4)).append("-")
					.append(ResourcesUtil.randomHex(4)).append("-").append(ResourcesUtil.randomHex(18)).append("infoc")
					.toString();
			kvMap.put("_uuid", _uuid);
			// 获取 buvid4
			HttpRequestUtil util = new HttpRequestUtil();
			HttpHeaders headers = new HttpHeaders();
			String tempCookie = HttpCookies.map2CookieStr(kvMap);
			String r = util.getContent("https://api.bilibili.com/x/frontend/finger/spi", headers.getCommonHeaders(),
					HttpCookies.convertCookies(tempCookie));
			String buvid4 = new JSONObject(r).getJSONObject("data").getString("b_4");
			kvMap.put("buvid4", buvid4);
			kvMap.put("SESSDATA","54107de1%2C1746361438%2C6ac51%2Ab2CjC3HIdqj_UlsQsSQtgoJyCXUfjMYdvIcRgpAYjVMtwMtMOehkmAc3PSlyWJG-ooqe8SVkg1TGF3SDJ3NlhzNTM5dllIb00tcEg3ZGR6NUFKTzNQeWxzcmZ0bVA3a2NaejBObDh0Y1VWbGg1a21sdTlrMGI1U2FZYkFTcVlBbjc0c2owdDFRODFRIIEC");
			return HttpCookies.map2CookieStr(kvMap);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}


}
