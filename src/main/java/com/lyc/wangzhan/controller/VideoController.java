package com.lyc.wangzhan.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;

public interface VideoController {

    @RequestMapping(value = "/download.do",method = RequestMethod.POST)
    public void downloadImg(@RequestBody String url) throws IOException;

    /**
     * 压缩文件
     */
    @RequestMapping(value = "/compress.do",method = RequestMethod.POST)
    public void compressTo7z(@RequestBody String title);
}
