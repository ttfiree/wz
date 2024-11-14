package com.lyc.wangzhan.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.List;

public interface VideoController {

    @RequestMapping(value = "/download.do",method = RequestMethod.POST)
    public void downloadImg(@RequestBody List<String> url) throws IOException;

    /**
     * 压缩文件
     */
    @RequestMapping(value = "/compress.do",method = RequestMethod.POST)
    public void compressTo7z(@RequestBody String title);

    /**
     * 查詢視頻列表
     *
     */
    @RequestMapping(value = "/list.do",method = RequestMethod.POST)
    public void list(@RequestBody String spaceId) throws IOException;

    @RequestMapping(value = "/test.do",method = RequestMethod.POST)
    public void test(@RequestBody String spaceId) throws IOException;


    /**
     * 处理文件
     */
    @RequestMapping(value = "/deal.do",method = RequestMethod.POST)
    public void deal(@RequestBody String title) throws IOException;

}
