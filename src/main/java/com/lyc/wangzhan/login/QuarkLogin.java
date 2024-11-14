package com.lyc.wangzhan.login;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyc.wangzhan.entity.Video;
import com.lyc.wangzhan.mapper.VideoMapper;
import com.microsoft.playwright.*;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


import com.lyc.wangzhan.constant.CommonConstant;

@Service
public class QuarkLogin implements LoginService{

    //Logger
    private static final Logger logger = LoggerFactory.getLogger(QuarkLogin.class);

    private BrowserContext context;

    @Value("${quark.cookies}")
    private String cookies;
    private HttpHeaders headers;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private VideoMapper videoMapper;


    private void setHeaders(){
        this.headers =  new HttpHeaders();
        headers.set("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Safari/537.36 Core/1.94.225.400 QQBrowser/12.2.5544.400");
        headers.set("origin", "https://pan.quark.cn");
        headers.set("referer", "https://pan.quark.cn/");
        headers.set("accept-language", "zh-CN,zh;q=0.9");
        headers.set("cookie", this.cookies);
    }



    private Map<String, String> cookiesStrToDict(String cookiesStr) {
        Map<String, String> cookiesDict = new HashMap<>();
        String[] cookiesList = cookiesStr.split("; ");
        for (String cookie : cookiesList) {
            String[] parts = cookie.split("=", 2);
            if (parts.length == 2) {
                cookiesDict.put(parts[0], parts[1]);
            }
        }
        return cookiesDict;
    }

    private Map<String, String> transferCookies(List<Map<String, Object>> cookiesList) {
        Map<String, String> cookiesDict = new HashMap<>();
        for (Map<String, Object> cookie : cookiesList) {
            String domain = (String) cookie.get("domain");
            if (domain != null && domain.contains("quark")) {
                cookiesDict.put((String) cookie.get("name"), (String) cookie.get("value"));
            }
        }
        return cookiesDict;
    }

    private String dictToCookieStr(Map<String, String> cookiesDict) {
        return cookiesDict.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("; "));
    }


    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonCookies(String content) {
        //cookies 按照;分割
        List<String> cookies = Arrays.asList(content.split(";"));
        List<Map<String, Object>> cookiesList = new ArrayList<>();
        for (int i = 0; i < cookies.size(); i++) {
            String cookie = cookies.get(i);
            //cookie 按照=分割
            Map<String, Object> cookieMap = new HashMap<>();
            String[] cookieParts = cookie.split("=");
            cookieMap.put(cookieParts[0], cookieParts[1]);
            cookiesList.add(cookieMap);
        }
        return cookiesList;
    }

    public String getCookies() {
                List<Map<String, Object>> savedCookies = parseJsonCookies(cookies);
                Map<String, String> cookiesDict = transferCookies(savedCookies);
                return dictToCookieStr(cookiesDict);

    }




    private String getPwdId(String shareUrl) {
        return shareUrl.split("\\?")[0].split("/s/")[1];
    }

    private String getStoken(String pwdId) {
        String url = "https://drive-pc.quark.cn/1/clouddrive/share/sharepage/token";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("pwd_id", pwdId);
        requestBody.put("passcode", "");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        // Parse the response to get stoken
        return ""; // Return the parsed stoken
    }

    private List<Map<String, Object>> getDetail(String pwdId, String stoken, String pdirFid) {
        String url = "https://drive-pc.quark.cn/1/clouddrive/share/sharepage/detail";

        // Build URL with query parameters
        String fullUrl = String.format("%s?pwd_id=%s&stoken=%s&pdir_fid=%s&_page=1&_size=50&_sort=file_type:asc,updated_at:desc",
                url, pwdId, stoken, pdirFid);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                requestEntity,
                Map.class
        );

        // Parse the response to get the file details
        return new ArrayList<>(); // Return the parsed file details
    }

    public Map<String, Object> getSortedFileList(String pdirFid, String page, String size, String fetchTotal, String sort) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("pr", "ucpro");
        params.put("fr", "pc");
        params.put("uc_param_str", "");
        params.put("pdir_fid", pdirFid);
        params.put("_page", page);
        params.put("_size", size);
        params.put("_fetch_total", fetchTotal);
        params.put("_fetch_sub_dirs", "1");
        params.put("_sort", sort);
        params.put("__dt", String.valueOf(new Random().nextInt(9900) + 100));
        params.put("__t", String.valueOf(System.currentTimeMillis()));
        // 构建带参数的URL
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://drive-pc.quark.cn/1/clouddrive/file/sort");
        params.forEach(builder::queryParam);

        // 创建带header的请求实体
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                requestEntity,
                String.class
        );
        //使用fastjson解析响应转换为Map<String, Map<String, Object>>
        JSONObject jsonObject = JSON.parseObject(response.getBody());
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public String generateRandomCode(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            sb.append(characters.charAt(randomIndex));
        }

        return sb.toString();
    }

    public String getShareTaskId(String fid, String fileName, int urlType, int expiredType, String password) throws IOException {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("fid_list", Collections.singletonList(fid));
        jsonData.put("title", fileName);
        jsonData.put("url_type", urlType);
        jsonData.put("expired_type", expiredType);

        if (urlType == 2) {
            jsonData.put("passcode", password.isEmpty() ? generateRandomCode(4) : password);
        }

        Map<String, String> params = new HashMap<>();
        params.put("pr", "ucpro");
        params.put("fr", "pc");
        params.put("uc_param_str", "");
        //将params转换为URL参数
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://drive-pc.quark.cn/1/clouddrive/share");
        params.forEach(builder::queryParam);

        RequestEntity<Map<String, Object>> requestEntity = new RequestEntity<>(jsonData, headers, HttpMethod.POST, URI.create(builder.toUriString()));
        ResponseEntity<String> exchange = restTemplate.exchange(requestEntity, String.class);


        Map<String, Object> responseData = new ObjectMapper().readValue(exchange.getBody(), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> data = (Map<String, Object>) responseData.get("data");
        return (String) data.get("task_id");
    }

    public String getShareId(String taskId) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("pr", "ucpro");
        params.put("fr", "pc");
        params.put("uc_param_str", "");
        params.put("task_id", taskId);
        params.put("retry_index", "0");


        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://drive-pc.quark.cn/1/clouddrive/task?pr=ucpro&fr=pc&uc_param_str=&task_id="+taskId+"");
        params.forEach(builder::queryParam);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                requestEntity,
                String.class
        );


        Map<String, Object> responseData = new ObjectMapper().readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> data = (Map<String, Object>) responseData.get("data");
        return (String) data.get("share_id");
    }

    public String submitShare(String shareId) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("pr", "ucpro");
        params.put("fr", "pc");
        params.put("uc_param_str", "");

        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("share_id", shareId);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://drive-pc.quark.cn/1/clouddrive/share/password");
        params.forEach(builder::queryParam);
        // 设置请求体
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(jsonData, headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                requestEntity,
                String.class);

        JSONObject responseData = JSON.parseObject(responseEntity.getBody());
        JSONObject data = responseData.getJSONObject("data");
        String shareUrl = (String) data.get("share_url");
        if (data.containsKey("passcode")) {
            shareUrl += "?pwd=" + data.get("passcode");
        }
        return shareUrl;
    }

    public void shareRun(String shareUrl ) {
        setHeaders();
        int urlType = 1;
        int expiredType=4;
        String firstDir = "";
        String secondDir = "";
        try {
            logger.info("文件夹网页地址：" + shareUrl);
            String pwdId = shareUrl.split("/")[shareUrl.split("/").length - 1].split("-")[0];

            int firstPage = 1;
            int n = 0;
            int error = 0;
            new File("share").mkdirs();
            String saveSharePath = "share/share_url.txt";
                Map<String,Object> jsonData = getSortedFileList(pwdId, String.valueOf(firstPage), "50", "1", "file_type:asc,file_name:asc");
                JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(jsonData.get("data")));
                List<Map<String, Object>> list = (List<Map<String, Object>>) jsonObject.get("list");
                for (Map<String, Object> i1 : list) {
                    secondDir = (String) i1.get("file_name");
                    //secondDir截取.前面的字符串
                    String secondDir1 = secondDir.substring(0, secondDir.lastIndexOf("."));
                                    boolean shareSuccess = false;
                                    String shareErrorMsg = "";
                                    String fid = "";
                    Video video = new Video();
                    video.setName(secondDir1);
                    video.setIs_del(0);
                    video = videoMapper.selectOne(new QueryWrapper(video));
                    if(video == null){
                        video = new Video();
                        video.setName(secondDir1);
                        video.setIs_del(0);
                        video.setCreateTime(new Date());
                        videoMapper.insert(video);
                    }
                                        try {
                                            logger.info(n + ".开始分享 " + secondDir + " 文件夹");
                                            Thread.sleep((long) (Math.random() * 1500 + 500));
                                            fid = (String) i1.get("fid");
                                            String taskId = getShareTaskId(fid, secondDir, urlType, expiredType, null);
                                            String shareId = getShareId(taskId);
                                            String sharedUrl = submitShare(shareId);
                                            logger.info(n + ".分享成功 " +  secondDir + " 文件夹"+sharedUrl);
                                            video.setUrl(sharedUrl);
                                            video.setProcess(CommonConstant.DOWNLOAD_STATUS_UPLOAD);
                                            videoMapper.updateById(video);
                                        } catch (Exception e) {
                                            logger.error("分享失败：",e);
                                            video.setProcess(CommonConstant.DOWNLOAD_STATUS_UPLOAD_ERROR);
                                            videoMapper.updateById(video);
                                            shareErrorMsg = e.getMessage();
                                            error++;
                                        }
                                    if (!shareSuccess) {
                                       logger.info("分享失败：" + shareErrorMsg);
                                    }
                }
        } catch (Exception e) {
            logger.error("分享失败：",e);
           logger.info("分享失败：" + e.getMessage());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("./share/share_error.txt", true))) {
                writer.write(firstDir + "/" + secondDir + " 文件夹");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }


}
