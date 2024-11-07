package com.lyc.wangzhan.controller.impl;

import cn.hutool.http.HttpUtil;
import com.lyc.wangzhan.controller.VideoController;
import com.lyc.wangzhan.utils.HttpRequestUtil;
import com.lyc.wangzhan.utils.IOTransUtil;
import com.lyc.wangzhan.utils.PathUtil;
import com.lyc.wangzhan.video.VideoService;
import com.lyc.wangzhan.video.VideoServiceNew;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lyc.wangzhan.utils.PathUtil.compressTo7z;

@RestController( "/video")
@RequestMapping("/video")
public class VideoControllerImpl implements VideoController {
    @Autowired
    private VideoServiceNew videoService;
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void downloadImg(String urls) throws IOException {
        //https://www.bilibili.com/video/BV1X4DpYnExZ/?spm_id_from=333.1007.tianma.2-3-6.click 提取1X4DpYnExZ
        String bv = urls.substring(urls.indexOf("BV"), urls.indexOf("?"));
        //如果末尾是/，去掉/
        if (bv.endsWith("/")) {
            bv = bv.substring(0, bv.length() - 1);
        }
        long start = System.currentTimeMillis();
        //get请求https://api.bilibili.com/x/web-interface/wbi/view 参数bvid
        String videoUrl = "https://api.bilibili.com/x/web-interface/view?bvid=" + bv;
        String videoJson1 = videoService.createConnection(videoUrl);
        JSONObject videoJson = new JSONObject(videoJson1);
        //获取data节点的aid
        String avid = String.valueOf(videoJson.getJSONObject("data").getLong("aid"));
        String cid = String.valueOf(videoJson.getJSONObject("data").getLong("cid"));
        String title = videoJson.getJSONObject("data").getString("title");
        String picurl = videoJson.getJSONObject("data").getString("pic");
        // 根据cid拼接成完整的请求参数,并执行下载操作
        videoService.downloadMovie(avid, cid,title);
        //urls = https://www.bilibili.com/video/BV1X4DpYnExZ/?spm_id_from=333.1007.tianma.2-3-6.click 使用正则提取BV1X4DpYnExZ
        videoService.downloadImage(picurl, PathUtil.createImagePath(picurl,title));
        PathUtil.compressTo7z(title);
        long end = System.currentTimeMillis();
        System.err.println("总共耗时：" + (end - start) / 1000 + "s");
    }

    @Override
    public void compressTo7z(String title) {
        PathUtil.compressTo7z(title);
    }


    // 2. 获取到的json选择出cid，只能选择出一个cid，还有标题
    public static Integer JsonGetCid(String cidJson) {
        //转换成json
        JSONObject jsonObject = new JSONObject(cidJson);
        //cid
        JSONObject jsonData = jsonObject.getJSONObject("data");

        JSONArray jsonArray = jsonData.getJSONArray("pages");
        Map<String, Object> pageMap = (Map) jsonArray.get(0);
        Integer cid = (Integer) pageMap.get("cid");
        System.out.println("cid: " + cid);
        //title
        String title = jsonData.getString("title").replace("/","");
        System.out.println("title:" + title);
        return cid;
    }


    public  InputStream createUrlConnection(String cover) {
        InputStream inputStream = null;
        try {
            URL imgUrl = new URL(cover);
            URLConnection urlConnection = imgUrl.openConnection();
            urlConnection.setConnectTimeout(10 * 1000);
            inputStream = urlConnection.getInputStream();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("URL创建失败");
        }
        return inputStream;

    }




    public static List<String> JSONObjectToCoverList(JSONObject jsonObject) {
        List<String> coverList = new ArrayList<>();
        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("list");
        for (int i = 0; i < jsonArray.length(); i++) {
            Map<String, String> map = (Map) jsonArray.get(i);
            coverList.add(map.get("cover"));
        }
        return coverList;
    }
}
