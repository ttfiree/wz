package com.lyc.wangzhan.video.impl;

import com.lyc.wangzhan.utils.*;
import com.lyc.wangzhan.video.TotalService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.lyc.wangzhan.utils.API.encodeURL;
@Service
public class TotalServiceImpl implements TotalService {

    @Autowired
    private HttpRequestUtil httpRequestUtil;

    @Autowired
    private HttpHeaders httpHeader;

@Autowired
    private API api;
    private static String wbiImg = null;
    public List<String> query(String spaceID) {
        String urlFormat = "https://api.bilibili.com/x/space/wbi/arc/search?mid=%s&ps=%d&tid=%s&special_type=charging&pn=%dorder=pubdate&platform=web"; // &web_location=1550101&order_avoided=true
        String url = String.format(urlFormat, spaceID, 50, 1, 0);
        url += api.genDmImgParams();
        url += encWbi(url);
        HashMap<String, String> headersRefer = new HashMap<>();
            headersRefer = new HashMap<String, String>();
            headersRefer.put("Accept", "text/html,application/xhtml+xml;q=0.9,image/webp,*/*;q=0.8");
            headersRefer.put("Accept-Encoding", "gzip, deflate");
            headersRefer.put("Accept-Language", "zh-CN,zh;q=0.8");
            headersRefer.put("Cache-Control", "max-age=0");
            headersRefer.put("Connection", "keep-alive");
            headersRefer.put("Host", "api.bilibili.com");
        headersRefer.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:93.0) Gecko/20100101 Firefox/93.0");;
        headersRefer.put("Referer", "https://space.bilibili.com/");
        headersRefer.put("Origin", "https://space.bilibili.com/");
        String json = httpRequestUtil.getContent(url, headersRefer, HttpCookies.globalCookiesWithFingerprint());
        JSONObject jobj = new JSONObject(json);
        JSONArray arr = jobj.getJSONObject("data").getJSONObject("list").getJSONArray("vlist");
        return arr.toList().stream().map(o -> {
            JSONObject obj = new JSONObject((HashMap) o);
            return obj.getString("bvid");
        }).collect(Collectors.toList());
    }

    static Random random = new Random();

    static String f114(int a, int b) {
        int t = random.nextInt(114);
        return String.format("[%d,%d,%d]", 2 * a + 2 * b + 3 * t, 4 * a - b + t, t);
    }

    static String f514(int a, int b) {
        int t = random.nextInt(514);
        return String.format("[%d,%d,%d]", 3 * a + 2 * b + t, 4 * a - 4 * b + 2 * t, t);
    }


    public  String encWbi(String url) {
        // Logger.println(url);
        // 获取 mixinKey
        getWbiUrl();
        String mixinKey = api.getMixinKey(wbiImg);
        // url 参数URLEncode并排序
        String paramEncodedSorted, sep, wts = "wts=" + (System.currentTimeMillis() / 1000);
        int questionMarkIdx = url.indexOf("?");
        if (questionMarkIdx >= 0) {
            String paramRaw = (url).substring(questionMarkIdx + 1);
            if (paramRaw.isEmpty()) {
                sep = "";
            } else {
                sep = "&";
            }
            paramRaw += sep + wts;
            String[] params = paramRaw.split("&");
            List<String> paramList = Arrays.stream(params).map((aEqB) -> {
                try {
                    // Logger.println(aEqB);
                    String[] keyValue = aEqB.split("=", 2);
                    String key = URLEncoder.encode(keyValue[0], "UTF-8");
                    String value = keyValue.length >= 2 ? encodeURL(keyValue[1]) : "";
                    return key + "=" + value;
                } catch (UnsupportedEncodingException e) {
                    return aEqB;
                }
            }).collect(Collectors.toList());
            Collections.sort(paramList);
            paramEncodedSorted = String.join("&", paramList);
            // Logger.println(paramRaw);
            // Logger.println(paramSorted);
        } else {
            sep = "?";
            paramEncodedSorted = wts;
        }
        String md5 = Encrypt.MD5(paramEncodedSorted + mixinKey);
        String encUrl = String.format("%s%sw_rid=%s&%s", url, sep, md5, wts);
        // Logger.println(encUrl);
        return encUrl;
    }

    private  void getWbiUrl() {
        if (wbiImg == null) {
                if (wbiImg == null) {
                    HttpRequestUtil util = new HttpRequestUtil();
                    String content = util.getContent("https://api.bilibili.com/x/web-interface/nav",
                            httpHeader.getCommonHeaders(), HttpCookies.getGlobalCookies());
                    JSONObject obj = new JSONObject(content).getJSONObject("data").getJSONObject("wbi_img");
                    String wbiImgUrl = obj.getString("img_url");
                    int is = wbiImgUrl.lastIndexOf("/");
                    int ie = wbiImgUrl.indexOf(".", is);
                    String wbiSubUrl = obj.getString("sub_url");
                    int ss = wbiSubUrl.lastIndexOf("/");
                    int se = wbiSubUrl.indexOf(".", ss);
                    wbiImg = wbiImgUrl.substring(is + 1, ie) + wbiSubUrl.substring(ss + 1, se);
                }
        }
    }




}
