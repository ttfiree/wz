package com.lyc.wangzhan.constant;

import org.springframework.stereotype.Component;

@Component
public class CommonConstant {

    //1 已下载
    public static final Integer DOWNLOAD_STATUS_YES = 1;

    //2 已压缩
    public static final Integer DOWNLOAD_STATUS_COMPRESS = 2;

    //3已上传
    public static final Integer DOWNLOAD_STATUS_UPLOAD = 3;

    //4 下载错误

    public static final Integer DOWNLOAD_STATUS_ERROR = 4;

    //5 压缩错误

    public static final Integer DOWNLOAD_STATUS_COMPRESS_ERROR = 5;

    //6 上传错误

    public static final Integer DOWNLOAD_STATUS_UPLOAD_ERROR = 6;
}
