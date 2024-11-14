package com.lyc.wangzhan.service.wordpress;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class WordpressImpl implements WordpressService{
    //logger
    private static final Logger logger = LoggerFactory.getLogger(WordpressImpl.class);



    @Autowired
    private RestTemplate restTemplate;
    @Value("${wordpress.url}")
    private  String wordpressUrl;
    @Value("${wordpress.username}")
    private  String username;
    @Value("${wordpress.password}")
    private  String password; // 应用密码或普通密码
    /**
     * 修改图片标题，描述作者等信息全部为bili-fun.com
     *
     */
    private void modifyMedia(String path){

    }
    /**
     * 上传本地图片到 WordPress 媒体库
     *
     * @param imagePath 本地图片路径，例如 "C:/images/photo.jpg"
     * @return 上传后的媒体信息，包含 ID、URL 等
     */
    public Map<String, Object> uploadMedia(String imagePath) {
        logger.info("Uploading media: {}", imagePath);
        ObjectMapper objectMapper = new ObjectMapper();
        String url = wordpressUrl + "wp-json/wp/v2/media";

        // 读取本地图片文件（需使用合适的方法读取文件为字节数组）
        byte[] imageBytes;
        String fileName = imagePath.substring(imagePath.lastIndexOf('/') + 1);
        String mimeType = getMimeType(fileName);

        try {
            imageBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(imagePath));
        } catch (Exception e) {
            logger.error("Failed to read image file", e);
            return null;
        }

        // 设置 HTTP 头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // 设置认证头
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        // 构建请求实体
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(imageBytes, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
logger.info("response:{}",response);
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseData = objectMapper.readValue(response.getBody(), Map.class);
                logger.info("Media uploaded successfully: {}", responseData);
                return responseData;
            } else {
                logger.error("Failed to upload media. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return null;
            }
        } catch (RestClientException e) {
            logger.error("Error occurred while uploading media", e);
            return null;
        } catch (Exception e) {
            logger.error("Failed to parse media upload response", e);
            return null;
        }
    }

    /**
     * 发布新的文章到 WordPress
     *
     * @param title   文章标题
     * @param content 文章内容（可以包含图片 URL）
     * @param status  文章状态，如 "publish" 或 "draft"
     * @return 发布后的文章信息，包含 ID、链接等
     */
    public Map<String, Object> publishPost(String title, String content, String status) {
        ObjectMapper objectMapper = new ObjectMapper();
        String url = wordpressUrl + "wp-json/wp/v2/posts";

        // 构建请求体
        Map<String, Object> postData = new HashMap<>();
        postData.put("title", title);
        postData.put("content", content);
        postData.put("status", status); // "publish" 或 "draft"

        // 设置 HTTP 头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));

        // 设置认证头
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        // 构建请求实体
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(postData, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseData = objectMapper.readValue(response.getBody(), Map.class);
                logger.info("Post published successfully: {}", responseData);
                return responseData;
            } else {
                logger.error("Failed to publish post. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return null;
            }
        } catch (RestClientException e) {
            logger.error("Error occurred while publishing post", e);
            return null;
        } catch (Exception e) {
            logger.error("Failed to parse post publish response", e);
            return null;
        }
    }

    /**
     * 根据文件名获取 MIME 类型
     *
     * @param fileName 文件名
     * @return MIME 类型字符串
     */
    private String getMimeType(String fileName) {
        String mimeType = "application/octet-stream"; // 默认 MIME 类型

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            mimeType = "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            mimeType = "image/png";
        } else if (fileName.endsWith(".gif")) {
            mimeType = "image/gif";
        } else if (fileName.endsWith(".svg")) {
            mimeType = "image/svg+xml";
        }

        return mimeType;
    }
}
