package com.lyc.wangzhan.service.video.impl;

import com.lyc.wangzhan.utils.*;
import com.lyc.wangzhan.service.video.VideoServiceNew;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VideoServiceNewImpl implements VideoServiceNew {
    @Value("${cookie}")
    private String cookie;

    private static String cookie2;

    @Autowired
    private RestTemplate   restTemplate;
    @Autowired
    private HttpRequestUtil httpRequestUtil;

    int highQuality = 112;

    //"80:7, 12, 13| 64:7, 12, 13"
    public static HashMap<Integer, int[]> videoCodecPriorityMap = new HashMap<>();


    // 3-2  建立URL连接请求
    private static InputStream createInputStream(String movieUrl, String avid) {
        InputStream inputStream = null;
        try {
            URL url = new URL(movieUrl);
            URLConnection urlConnection = url.openConnection();
            String refererUrl = "https://www.bilibili.com/video/av" + avid;
            urlConnection.setRequestProperty("Referer",refererUrl );
            urlConnection.setRequestProperty("Sec-Fetch-Mode", "no-cors");
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
            urlConnection.setConnectTimeout(10 * 1000);

            inputStream = urlConnection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("获取inputStream失败");
        }
        return inputStream;
    }
    public static List<HttpCookie> convertCookies(String cookie) {
        String lines[] = cookie.split("\n");
        List<HttpCookie> iCookies = new ArrayList<HttpCookie>();
        String[] cookieStrs = lines[0].replaceAll("\\||\r|\n| |\\[|\\]|\"", "").split(",|;|&");
        for (String cookieStr : cookieStrs) {
            String entry[] = cookieStr.split("=");
            HttpCookie cCookie = new HttpCookie(entry[0], entry[1]);
            iCookies.add(cCookie);
        }
        return iCookies;
    }
    //check login
    public void checkLogin() {

        String url = "https://api.bilibili.com/x/web-interface/nav?build=0&mobi_app=web";
        String json = createConnection(url);
        // System.out.println(json);

        JSONObject jObj = new JSONObject(json).getJSONObject("data");
    }

    /**
     * 从指定的 URL 下载图片并保存到本地路径
     *
     * @param imageUrl      图片的网络地址
     * @param destinationPath 本地保存的文件路径（包括文件名和扩展名）
     * @throws IOException 如果下载或保存过程中发生错误
     */
    public void downloadImage(String imageUrl, String destinationPath) throws IOException {
        // 发送 GET 请求获取图片数据
        ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);

        // 检查响应状态
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            // 将字节数组写入到本地文件
            try (FileOutputStream fos = new FileOutputStream(destinationPath)) {
                fos.write(response.getBody());
                System.out.println("图片已成功下载到: " + destinationPath);
            } catch (IOException e) {
                System.err.println("保存图片时出错: " + e.getMessage());
                throw e;
            }
        } else {
            throw new IOException("无法下载图片，服务器响应状态: " + response.getStatusCode());
        }
    }


    public  void downloadMovie(String avid, String cid,String title,String bv) {
        videoCodecPriorityMap.put(80, new int[]{7, 12, 13});
        videoCodecPriorityMap.put(64, new int[]{7, 12, 13});
        //checkLogin();
        //qn ： 视频质量         112 -> 高清 1080P+,   80 -> 高清 1080P,   64 -> 高清 720P,  32  -> 清晰 480P,  16 -> 流畅 360P
        // 最高支持 1080p,  1080P+是不支持的
        Integer qn = 80;
        //調用https://api.bilibili.com/x/player/playurl?cid=${cid}&bvid=${bvid}&qn=127&type=&otype=json&fourk=1&fnver=0&fnval=80&session=68191c1dc3c75042c6f35fba895d65b0
        String url = "https://api.bilibili.com/x/player/playurl?avid=" + avid + "&cid=" + cid + "&type=&otype=json&fourk=1&fnver=0&fnval=80&session=68191c1dc3c75042c6f35fba895d65b0";
        String jsonText1 = createConnection(url);
        JSONObject jsonObject1 = new JSONObject(jsonText1);
        //accept_quality -> {JSONArray@8058} "[120,116,80,64,32,16]"
        //从accept_quality中选择最大的一个
        JSONArray jsonArray1 = jsonObject1.getJSONObject("data").getJSONArray("accept_quality");
        highQuality = (Integer) jsonArray1.get(0);
        String paraUrl = "https://api.bilibili.com/x/player/playurl?avid=" + avid + "&cid=" + cid + "&qn="+highQuality+"";
        System.out.println("构建的url为：" + paraUrl);
        // 获取到的是json，然后筛选出里面的视频资源：url
        String movieUrl = getVideoLinkByFormat(bv, cid, highQuality, 1);
        //String jsonText = createConnection(paraUrl);

/*        JSONObject jsonObject = new JSONObject(jsonText);
        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("durl");

        //JsonObject转map
        Map<String, String> dUrlMap = jsonToMap((JSONObject) jsonArray.get(0));
        String movieUrl = dUrlMap.get("url");*/

        System.out.println("视频资源url为：" + movieUrl);
        // 根据获取的title 创建文件
        String moviePath = PathUtil.createMoviePath(title);
        //建立连接
        InputStream inputStream = createInputStream(movieUrl,avid);
        //开始流转换
        IOTransUtil.inputStreamToFile(inputStream, moviePath);
        try {
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // 2. 获取到的json选择出cid，只能选择出一个cid，还有标题

        public  Map<String, String> jsonToMap(JSONObject jsonObject) {
            Map<String, String> map = new HashMap<>();
            for (String key : jsonObject.keySet()) {
                map.put(key, String.valueOf(jsonObject.get(key)));
            }
            return map;
        }

    // 1. 建立连接拿到 json 数据
    public  String createConnectionToJson(Integer avid) {
        String cidUrl = "https://api.bilibili.com/x/web-interface/view?aid=" + avid;
        //放完 movie地址
        String cidJson = createConnection(cidUrl);
        return cidJson;
    }

    //0. 建立连接,返回页面中的json
    public  String createConnection(String url) {
        String jsonText = null;
        Connection connection = Jsoup.connect(url).timeout(3000).ignoreContentType(true);
        HashMap<String, String> heads = new HashMap<>();
        String cookieConfig = "sid=oepgnjf8, DedeUserID__ckMd5=e5218b17dbdb0f5d, DedeUserID=315275999, bili_jct=5881ec5cfb37d96b7751987fccaabd49, SESSDATA=413dd822%2C1746551129%2Cbd1ba%2Ab2CjA4bLJEEitVhNneEMbCUU6wIn0Dk7TppClXqHm59yj16Zg4C8jbfLyQ9ayvhIXyj5ISVl9uUXBEeXFPcFp3YkVXc09IUF9ZZDFlcWpiVV9DRWlEbTVwOGZpZktaVVNwT3FmNWE0M0hEQzIzSVI2cnFtWkExV1hIcklKc0JvNVFSWHVkRWJ1amZRIIEC\n22fe8792c176db0b11611e3b1f65ccb2";
        List<HttpCookie> cookies = HttpCookies.convertCookies(cookieConfig);
        //设置cookie
        Map<String, String> cookieMap = new HashMap<>();
        for (HttpCookie cookie : cookies) {
            cookieMap.put(cookie.getName(), cookie.getValue());
        }
//设置cookie
        heads.put("Cookie", HttpCookies.map2CookieStr(cookieMap));
        heads.put("Accept", "application/json, text/plain, */*");
        heads.put("Accept-Encoding", "gzip, deflate");
        heads.put("Accept-Language", "zh-CN,zh;q=0.8");
        heads.put("Connection", "keep-alive");
        heads.put("Origin", "https://message.bilibili.com");
        heads.put("Host", "api.bilibili.com");
        heads.put("Referer", "https://message.bilibili.com/pages/nav/index_new_pc_sync");
        heads.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:93.0) Gecko/20100101 Firefox/93.0");
        connection.headers(heads);
        String res = httpRequestUtil.getContent(url, heads, cookies);
        return res;
    }

    String getVideoLinkByFormat(String bvId, String cid, int qn, int downloadFormat) {
        System.out.println("正在查询MP4/FLV链接...");
        HttpHeaders headers = new HttpHeaders();
        JSONObject jObj = null;
        // 根据downloadFormat确定fnval
        String fnval = "4048";
        // 先判断类型
        String url = "https://api.bilibili.com/x/web-interface/view/detail?aid=&jsonp=jsonp&callback=__jp0&bvid="
                + bvId;
        HashMap<String, String> header = getBiliJsonAPIHeaders(bvId);
        String callBack = httpRequestUtil.getContent(url, header);
        JSONObject infoObj = new JSONObject(callBack.substring(6, callBack.length() - 1)).getJSONObject("data")
                .getJSONObject("View");
        Long aid = infoObj.optLong("aid");

        if (infoObj.optString("redirect_url").isEmpty()) {
            String trylookTail = "";
            // 普通类型
            url = downloadFormat == 2 ?
                    // 下面这个API清晰度没法选择，编码方式没法选择，固定返回1080P? mp4
                    // https://api.bilibili.com/x/player/wbi/playurl?avid=857672756&bvid=BV1HV4y1p7ce&cid=1084157816&qn=80&fnver=0&fnval=4048&fourk=1
                    "https://api.bilibili.com/x/player/wbi/playurl?cid=%s&bvid=%s&qn=%d":
                    "https://api.bilibili.com/x/player/wbi/playurl?cid=%s&bvid=%s&qn=%d&type=&otype=json&fnver=0&fnval=%s&fourk=1";
            url += trylookTail;
            url = String.format(url, cid, bvId, qn, fnval);
            url = API.encWbi(url);
//			List cookie = downloadFormat == 2 ? null : HttpCookies.globalCookiesWithFingerprint();
            String cookieConfig = "sid=oepgnjf8, DedeUserID__ckMd5=e5218b17dbdb0f5d, DedeUserID=315275999, bili_jct=5881ec5cfb37d96b7751987fccaabd49, SESSDATA=413dd822%2C1746551129%2Cbd1ba%2Ab2CjA4bLJEEitVhNneEMbCUU6wIn0Dk7TppClXqHm59yj16Zg4C8jbfLyQ9ayvhIXyj5ISVl9uUXBEeXFPcFp3YkVXc09IUF9ZZDFlcWpiVV9DRWlEbTVwOGZpZktaVVNwT3FmNWE0M0hEQzIzSVI2cnFtWkExV1hIcklKc0JvNVFSWHVkRWJ1amZRIIEC\n22fe8792c176db0b11611e3b1f65ccb2";
            List<HttpCookie> cookie = HttpCookies.convertCookies(cookieConfig);
            String json = httpRequestUtil.getContent(url, getBiliJsonAPIHeaders(bvId), cookie);
            System.out.println(json);
            jObj = new JSONObject(json).getJSONObject("data");
        } else {
            // 非普通类型
            url = "https://api.bilibili.com/pgc/player/web/playurl?fnver=0&fourk=1&otype=json&avid=%s&cid=%s&qn=%s&fnval=%s";
            url = String.format(url, aid, cid, qn, fnval);
            String json = httpRequestUtil.getContent(url, getBiliJsonAPIHeaders("av" + aid),
                    HttpCookies.convertCookies(cookie));
            jObj = new JSONObject(json).getJSONObject("result");
        }
        int linkQN = jObj.getInt("quality");
        if(qn != linkQN) { // 只有和预期不符才会去判断
            // 有时候，api返回的列表中含有比指定清晰度更高的内容
            JSONObject dash = jObj.optJSONObject("dash");
            if(dash != null) {
                JSONArray videos = dash.getJSONArray("video");
                int firstLinkQN = videos.getJSONObject(0).getInt("id");
                if(linkQN < firstLinkQN) {
                    linkQN = firstLinkQN > qn? qn: firstLinkQN;
                }
            }
        }
        String tips = String.format("%s:%s - 查询质量为: %d的链接, 得到质量为: %d的链接", bvId, cid, qn, linkQN);
        try {
            return parseType1(jObj, linkQN, getBiliWwwM4sHeaders(bvId));
        } catch (Exception e) {
             e.printStackTrace();
        }
        return null;
    }

    public HashMap<String, String> getBiliWwwM4sHeaders(String avId) {
        HashMap<String, String> headerMap = new HashMap<String, String>();
        headerMap.remove("X-Requested-With");
        headerMap.put("Referer", "https://www.bilibili.com/video/" + avId);// need addavId
        headerMap.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:93.0) Gecko/20100101 Firefox/93.0");
        return headerMap;
    }

    public HashMap<String, String> getBiliJsonAPIHeaders(String avId) {
        HashMap<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("Accept", "*/*");
        headerMap.put("Accept-Encoding", "gzip, deflate");
        headerMap.put("Accept-Language", "zh-CN,zh;q=0.8");
        headerMap.put("Connection", "keep-alive");
        headerMap.put("Host", "api.bilibili.com");
        headerMap.put("Referer", "https://www.bilibili.com/video/" + avId);// need addavId
        headerMap.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:93.0) Gecko/20100101 Firefox/93.0");
        headerMap.put("X-Requested-With", "ShockwaveFlash/28.0.0.137");
        return headerMap;
    }

    protected String parseType1(JSONObject jObj, int linkQN, HashMap<String, String> headerForValidCheck) {
        JSONObject dash = jObj.getJSONObject("dash");
        StringBuilder link = new StringBuilder();
        // 获取视频链接
            JSONArray videos = dash.getJSONArray("video");
            // 获取所有符合清晰度要求的视频
            List<JSONObject> qnVideos = new ArrayList<>(3);
            for (int i = 0; i < videos.length(); i++) {
                JSONObject video = videos.getJSONObject(i);
                if (video.getInt("id") == linkQN) {
                    qnVideos.add(video);
                }
            }
            // 如果没有找到对应的清晰度 eg. BV1K14y1g7iU 无cookie
            //（API 返回的链接里并没有对应实际清晰度的链接）
            // 那么只能随便加一个了
            if(qnVideos.size() == 0) {
                JSONObject v = videos.getJSONObject(0);
                qnVideos.add(v);
            }
            // 根据清晰度选择合适的编码优先级
            Integer rQN = highQuality;
            //{7, 12, 13}
            int[] videoCodecPriority = videoCodecPriorityMap.getOrDefault(rQN, videoCodecPriorityMap.get(80));
            // 根据需求选择编码合适的视频
            JSONObject video = findMediaByPriList(qnVideos, videoCodecPriority, 0);
            // 选择可以连通的链接
            String videoLink = getUrlOfMedia(video, false, headerForValidCheck);
            link.append(videoLink).append("#");


        // 获取音频链接
            // 获取所有音频
            List<JSONObject> listAudios = new ArrayList<>(5);
            JSONArray audios = dash.optJSONArray("audio");// 普通
            if (audios != null) {
                for (int i = 0; i < audios.length(); i++) {
                    listAudios.add(audios.getJSONObject(i));
                }
            }
            JSONObject dolby = dash.optJSONObject("dolby");// 杜比
            if (dolby != null && linkQN == 126) {
                audios = dolby.optJSONArray("audio");
                if (audios != null) {
                    for (int i = 0; i < audios.length(); i++) {
                        listAudios.add(audios.getJSONObject(i));
                    }
                }
            }
            JSONObject flac = dash.optJSONObject("flac");// flac
            JSONObject flacAudio = flac == null? null: flac.optJSONObject("audio");// audio
            if (flacAudio != null) {
                listAudios.add(flacAudio);
            }
        int[] audioQualityPriority = {30280, 30232, 30216, -1};
            if(listAudios.size() > 0) { // 存在没有音频的投稿
                JSONObject audio = findMediaByPriList(listAudios, audioQualityPriority, 1);
                String audioLink = getUrlOfMedia(audio, false, headerForValidCheck);
                link.append(audioLink);
            }
//		Logger.println(link);
        return link.toString();
    }
    JSONObject findMediaByPriList(List<JSONObject> medias, int[] priorities, int mediaType) {
        for(int priority: priorities) {
            JSONObject media = findMediaByPri(medias, priority, mediaType);
            if(media != null)
                return media;
        }
        return medias.get(0);
    }
    JSONObject findMediaByPri(List<JSONObject> medias, int priority, int mediaType) {
        for(JSONObject media: medias) {
            if(-1 == priority || (mediaType == 0 &&media.getInt("codecid") == priority)
                    || (mediaType == 1 &&media.getInt("id") == priority))
                return media;
        }
        return null;
    }


    String getUrlOfMedia(JSONObject media, boolean checkValid, HashMap<String, String> headerForValidCheck) {
        String baseUrl = media.getString("base_url");
        if(!checkValid) {
            return baseUrl;
        }else {
            if (httpRequestUtil.checkValid(baseUrl, headerForValidCheck, null)) {
                return baseUrl;
            } else {
                JSONArray backup_urls = media.getJSONArray("backup_url");
                for (int j = 0; j < backup_urls.length(); j++) {
                    String backup_url = backup_urls.getString(j);
                    if (httpRequestUtil.checkValid(backup_url, headerForValidCheck, null)) {
                        return backup_url;
                    }
                }
                return null;
            }
        }
    }

    protected String getAudioLink(String auId, String _auId, int qn) {
        String auIdNum = auId.substring(2);
//		String url = String.format("https://www.bilibili.com/audio/music-service-c/web/url?sid=%s&privilege=2&quality=2", auIdNum);
        String url = String.format("https://www.bilibili.com/audio/music-service-c/url?songid=%s&privilege=2&quality=%d&mid=&platform=web", auIdNum, qn);
        HashMap<String, String> headers = getCommonHeaders();
        String cookieConfig = "sid=oepgnjf8, DedeUserID__ckMd5=e5218b17dbdb0f5d, DedeUserID=315275999, bili_jct=5881ec5cfb37d96b7751987fccaabd49, SESSDATA=413dd822%2C1746551129%2Cbd1ba%2Ab2CjA4bLJEEitVhNneEMbCUU6wIn0Dk7TppClXqHm59yj16Zg4C8jbfLyQ9ayvhIXyj5ISVl9uUXBEeXFPcFp3YkVXc09IUF9ZZDFlcWpiVV9DRWlEbTVwOGZpZktaVVNwT3FmNWE0M0hEQzIzSVI2cnFtWkExV1hIcklKc0JvNVFSWHVkRWJ1amZRIIEC\n22fe8792c176db0b11611e3b1f65ccb2";
        List<HttpCookie> cookies = HttpCookies.convertCookies(cookieConfig);
        //设置cookie
        Map<String, String> cookieMap = new HashMap<>();
        for (HttpCookie cookie : cookies) {
            cookieMap.put(cookie.getName(), cookie.getValue());
        }
        String r = httpRequestUtil.getContent(url, headers, cookies);
        JSONObject data = new JSONObject(r).getJSONObject("data");
        int realQn = data.optInt("type");
        String link = data.getJSONArray("cdns").getString(0);
        return link;
    }

    public HashMap<String, String> getCommonHeaders() {
        HashMap<String, String>
        headerMap = new HashMap<String, String>();
        headerMap.put("Accept", "text/html,application/xhtml+xml;q=0.9,image/webp,*/*;q=0.8");
        headerMap.put("Accept-Encoding", "gzip, deflate");
        headerMap.put("Accept-Language", "zh-CN,zh;q=0.8");
        headerMap.put("Cache-Control", "max-age=0");
        headerMap.put("Connection", "keep-alive");
        headerMap.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:93.0) Gecko/20100101 Firefox/93.0");
        return headerMap;
    }

}
