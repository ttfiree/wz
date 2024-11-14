package com.lyc.wangzhan.service.wordpress;

import java.util.Map;

public interface WordpressService {


    Map<String, Object> uploadMedia(String imagePath);


    Map<String, Object> publishPost(String title, String content, String status);
}
