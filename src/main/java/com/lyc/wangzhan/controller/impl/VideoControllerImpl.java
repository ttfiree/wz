package com.lyc.wangzhan.controller.impl;

import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyc.wangzhan.constant.CommonConstant;
import com.lyc.wangzhan.controller.VideoController;
import com.lyc.wangzhan.entity.Video;
import com.lyc.wangzhan.service.login.LoginService;
import com.lyc.wangzhan.mapper.VideoMapper;
import com.lyc.wangzhan.service.wordpress.WordpressService;
import com.lyc.wangzhan.utils.*;
import com.lyc.wangzhan.service.video.TotalService;
import com.lyc.wangzhan.service.video.VideoServiceNew;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.lyc.wangzhan.utils.PathUtil.outPath;


@RestController( "/video")
@RequestMapping("/video")
public class VideoControllerImpl implements VideoController {
    @Autowired
    private VideoServiceNew videoService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private TotalService totalService;
    @Autowired
    private HttpHeaders httpHeaders;

    @Value("${cookie}")
    private String cookie;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private LoginService loginService;
    @Autowired
    private WordpressService wordpressService;
    @Override
    public void downloadImg(List<String> urls) throws IOException {

        //https://www.bilibili.com/video/BV1X4DpYnExZ/?spm_id_from=333.1007.tianma.2-3-6.click 提取1X4DpYnExZ
        for (String url : urls) {
            String bv = url;
            //如果末尾是/，去掉/
            if (bv.endsWith("/")) {
                bv = bv.substring(0, bv.length() - 1);
            }
            try{
                download(bv);
            }catch (Exception e){
                //重试
                download(bv);
            }

        }


    }

    private void download(String bv) throws IOException {
        long start = System.currentTimeMillis();
        //get请求https://api.bilibili.com/x/web-interface/wbi/view 参数bvid
        String videoUrl = "https://api.bilibili.com/x/web-interface/view?bvid=" + bv;
        String videoJson1 = videoService.createConnection(videoUrl);
        JSONObject videoJson = new JSONObject(videoJson1);
        //获取data节点的aid
        String avid = String.valueOf(videoJson.getJSONObject("data").getLong("aid"));
        String cid = String.valueOf(videoJson.getJSONObject("data").getLong("cid"));
        String title = videoJson.getJSONObject("data").getString("title");
        String name = videoJson.getJSONObject("data").getJSONObject("owner").getString("name");
        title = title + "-" + name;
        String picurl = videoJson.getJSONObject("data").getString("pic");
        // 根据cid拼接成完整的请求参数,并执行下载操作
        videoService.downloadMovie(avid, cid,title,bv);
        //urls = https://www.bilibili.com/video/BV1X4DpYnExZ/?spm_id_from=333.1007.tianma.2-3-6.click 使用正则提取BV1X4DpYnExZ
        videoService.downloadImage(picurl, PathUtil.createImagePath(picurl,title));
        //PathUtil.compressTo7z(title);
        long end = System.currentTimeMillis();
        System.out.println("下载完成，耗时：" + (end - start) / 1000 + "秒");
    }

    @Override
    public void compressTo7z(String title) {
       // PathUtil.copyFile(title);
        PathUtil.moveFile(title);
        PathUtil.compressFolder(title);
    }

    @Override
    public void list(String spaceId) throws IOException {
        Connection connection = Jsoup.connect(spaceId)
                .headers(httpHeaders.getCommonHeaders("space.bilibili.com"))
                .timeout(10000)
                .followRedirects(true);

        // 添加cookie（如果需要）
        // 执行请求
            Document document = connection.get();

        List<String> avids = new ArrayList<>();
        // 转换XPath为CSS选择器
        // xpath: /html/body/div[2]/div[4]/div/div/div[2]/div[4]/div/div/ul[2]/li
        Elements liElements = document.select("#submit-video-list > ul.clearfix.cube-list > li");

        for (Element li : liElements) {
            String avid = li.attr("data-avid"); // 假设avid存储在data-avid属性中
            // 或者其他属性
            // String avid = li.id();  // 如果存储在id中
            // String avid = li.attr("aid");  // 如果存储在aid属性中

            if (avid != null && !avid.isEmpty()) {
                avids.add(avid);
            }
        }
        System.out.println(avids);
    }

    @Override
    public void test(String spaceId) throws IOException {
        //loginService.shareRun(spaceId);
        wordpressService.uploadMedia(spaceId);
    }

    @Override
    public void deal(String title) throws IOException {
        Video video = new Video();
        video.setName(title);
        video.setIs_del(0);
        video = videoMapper.selectOne(new QueryWrapper(video));
        if(video == null){
            video = new Video();
            video.setName(title);
            video.setIs_del(0);
            video.setCreateTime(new Date());
            videoMapper.insert(video);
            video = videoMapper.selectOne(new QueryWrapper(video));
        }
        PathUtil.compressFolderDownload(title);
        video.setPic_path(outPath + File.separator + title);
        PathUtil.moveFileQuark(title);
        video.setProcess(CommonConstant.DOWNLOAD_STATUS_COMPRESS);
        videoMapper.updateById(video);
    }

    public void getAid(String url) {
        String html = HttpUtil.get(url);
        Document document = Jsoup.parse(html);
        //获取data-aid属性值
        document.select("//*[@id=\"submit-video-list\"]/ul[2]/li[]").forEach(element -> {
            System.out.println(element.attr("data-aid"));
        });
    }


    public static void main(String[] args) {
        String cookieConfig = "sid=oepgnjf8, DedeUserID__ckMd5=e5218b17dbdb0f5d, DedeUserID=315275999, bili_jct=5881ec5cfb37d96b7751987fccaabd49, SESSDATA=413dd822%2C1746551129%2Cbd1ba%2Ab2CjA4bLJEEitVhNneEMbCUU6wIn0Dk7TppClXqHm59yj16Zg4C8jbfLyQ9ayvhIXyj5ISVl9uUXBEeXFPcFp3YkVXc09IUF9ZZDFlcWpiVV9DRWlEbTVwOGZpZktaVVNwT3FmNWE0M0hEQzIzSVI2cnFtWkExV1hIcklKc0JvNVFSWHVkRWJ1amZRIIEC\n22fe8792c176db0b11611e3b1f65ccb2";
        List<HttpCookie> cookies = HttpCookies.convertCookies(cookieConfig);
        System.out.println(cookies);
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
