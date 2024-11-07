package com.lyc.wangzhan.video.impl;

import com.lyc.wangzhan.utils.HttpRequestUtil;
import com.lyc.wangzhan.utils.IOTransUtil;
import com.lyc.wangzhan.utils.PathUtil;
import com.lyc.wangzhan.video.VideoServiceNew;
import org.json.JSONArray;
import org.json.JSONObject;
import cn.hutool.http.HttpUtil;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VideoServiceNewImpl implements VideoServiceNew {
    @Value("${cookie}")
    private String cookie;

    @Autowired
    private RestTemplate   restTemplate;
    @Autowired
    private HttpRequestUtil httpRequestUtil;


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


    public  void downloadMovie(String avid, String cid,String title) {
        Map<String, String> heads = new HashMap<>();
        heads.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        heads.put("Accept-Encoding", "gzip, deflate, br");
        heads.put("Accept-Language", "en,zh-CN;q=0.9,zh;q=0.8");
        heads.put("Cache-Control", "max-age=0");
        heads.put("Connection", "keep-alive");

        heads.put("Cookie", cookie);

        heads.put("Host", "api.bilibili.com");
        heads.put("Sec-Fetch-Mode", "navigate");
        heads.put("Sec-Fetch-Site", "none");
        heads.put("Sec-Fetch-User", "?1");
        heads.put("Upgrade-Insecure-Requests", "1");
        heads.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        //qn ： 视频质量         112 -> 高清 1080P+,   80 -> 高清 1080P,   64 -> 高清 720P,  32  -> 清晰 480P,  16 -> 流畅 360P
        // 最高支持 1080p,  1080P+是不支持的
        Integer qn = 80;
        String paraUrl = "https://api.bilibili.com/x/player/playurl?avid=" + avid + "&cid=" + cid + "&fnver=0&qn=80&type=mp4&platform=html5&high_quality=1";
        System.out.println("构建的url为：" + paraUrl);
        // 获取到的是json，然后筛选出里面的视频资源：url
        String jsonText = createConnection(paraUrl);

        JSONObject jsonObject = new JSONObject(jsonText);
        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("durl");

        //JsonObject转map
        Map<String, String> dUrlMap = jsonToMap((JSONObject) jsonArray.get(0));
        String movieUrl = dUrlMap.get("url");

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
        Map<String, String> heads = new HashMap<>();
        heads.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        heads.put("Accept-Encoding", "gzip, deflate, br");
        heads.put("Accept-Language", "en,zh-CN;q=0.9,zh;q=0.8");
        heads.put("Cache-Control", "max-age=0");
        heads.put("Connection", "keep-alive");

        heads.put("Cookie", cookie);

        heads.put("Host", "api.bilibili.com");
        heads.put("Sec-Fetch-Mode", "navigate");
        heads.put("Sec-Fetch-Site", "none");
        heads.put("Sec-Fetch-User", "?1");
        heads.put("Upgrade-Insecure-Requests", "1");
        heads.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        connection.headers(heads);
        try {
            jsonText = connection.get().text();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("建立获取cid连接失败");
        }
        return jsonText;
    }
}
