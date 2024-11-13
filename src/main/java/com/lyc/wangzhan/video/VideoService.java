package com.lyc.wangzhan.video;

import com.lyc.wangzhan.dto.VideoInfo;

public interface VideoService {

    void initPageQueryParam();



    VideoInfo result(int pageSize, int page, Object... obj);

}
