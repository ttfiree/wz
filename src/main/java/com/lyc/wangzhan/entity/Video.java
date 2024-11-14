package com.lyc.wangzhan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/*
create table video
(
    id          integer not null,
    avid        text,
    name        text,
    bid         text,
    up          text,
    url         text,
    createTime  timestamp,
    process     integer,
    is_del      integer,
    video_path  text,
    pic_path    text,
    packet_path text,
    pass        text,
    tag         text,
    category    text
);
 */
@Data
@TableName("video")
public class Video {

    private Integer id;
    private String avid;
    private String name;

    private String bid;
    private String up;
    private String url;
    private Date createTime;
    private Integer process;
    private Integer is_del;
    private String video_path;
    private String pic_path;

    private String packet_path;
    private String pass;
    private String tag;
    private String category;
}
