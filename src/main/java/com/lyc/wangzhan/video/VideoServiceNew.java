package com.lyc.wangzhan.video;

import java.io.IOException;

public interface VideoServiceNew {

    String createConnectionToJson(Integer avid);

    void downloadMovie(String avid, String cid,String title,String bv );

    void downloadImage(String imageUrl, String destinationPath) throws IOException;
    String createConnection(String url);
}
