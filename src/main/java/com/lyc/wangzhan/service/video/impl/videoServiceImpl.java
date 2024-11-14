package com.lyc.wangzhan.service.video.impl;

import com.lyc.wangzhan.dto.ClipInfo;
import com.lyc.wangzhan.dto.VideoInfo;
import com.lyc.wangzhan.enums.DownloadModeEnum;
import com.lyc.wangzhan.enums.VideoQualityEnum;
import com.lyc.wangzhan.exceptions.ApiLinkQueryParseError;
import com.lyc.wangzhan.exceptions.QualityTooLowException;
import com.lyc.wangzhan.utils.API;
import com.lyc.wangzhan.utils.HttpCookies;
import com.lyc.wangzhan.utils.HttpHeaders;
import com.lyc.wangzhan.utils.HttpRequestUtil;
import com.lyc.wangzhan.service.video.VideoService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class videoServiceImpl implements VideoService {

    //Logger
private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(videoServiceImpl.class);
    private final static Pattern pattern = Pattern
            .compile("www\\.bilibili\\.com/medialist/play/([0-9]+)\\?.*&business=space&");
    private final static Pattern patternParams = Pattern.compile("(tid|sort_field)=([^=&]+)");

    // 针对 https://space.bilibili.com/378034/video?tid=3&keyword=&order=stow
    // (keyword必须为空)
    private final static Pattern pattern2 = Pattern
            .compile("space\\.bilibili\\.com/([0-9]+)(/video|/search/video\\?|/? *$|/?\\?)");
    public final static Pattern patternKeyNotEmpty = Pattern.compile("keyword=[^=&]+");
    private final static Pattern patternParams2 = Pattern.compile("(tid|order)=([^=&]+)");

    private final static String[][] paramDicts = new String[][] { { "pubtime", "pubdate" }, { "play", "click" },
            { "fav", "stow" } };
    private String spaceID;
    private String listName;
    private int API_PMAX = 20;
    private HashMap<String, String> params;

    private VideoInfo pageQueryResult;
    @Autowired
    private HttpRequestUtil util;

    @Override
    public void initPageQueryParam() {
        pageQueryResult = new VideoInfo();
        pageQueryResult.setClips(new LinkedHashMap<>());
    }




    /**
     * 分页查询
     *
     * @param pageSize
     * @param page
     * @param obj
     * @return 以pageSize 进行分页查询，获取第page页的结果
     */
    @Override
    public VideoInfo result(int pageSize, int page, Object... obj) {
        initPageQueryParam();
        Logger.info("pageSize: %d, page: %d", pageSize, page);
        int videoFormat = (int) obj[0];
        boolean getVideoLink = (boolean) obj[1];
        String sortField = params.get("sort_field");
        try {
            // 先获取合集信息
            HashMap<String, String> headers = new HttpHeaders().getCommonHeaders("api.bilibili.com");
            HashMap<String, String> headersRefer = new HashMap<>(headers);
            headersRefer.put("Referer", "https://space.bilibili.com/");
            headersRefer.put("Origin", "https://space.bilibili.com/");
            if (pageQueryResult.getVideoName() == null) {
                String url = "https://api.bilibili.com/x/v1/medialist/info?type=1&tid=0&biz_id=" + spaceID;
                Logger.info(url);
                String json = util.getContent(url, headers);
                JSONObject jobj = new JSONObject(json).getJSONObject("data");
                listName = jobj.getString("title") + "的视频列表";
                pageQueryResult.setVideoId("space" + spaceID);
                //todo param
                pageQueryResult.setVideoName(listName );
                pageQueryResult.setBrief(jobj.getString("intro"));
                pageQueryResult.setAuthorId(spaceID);
                pageQueryResult.setAuthor(jobj.getJSONObject("upper").getString("name"));
            }
            // 获取oid(返回结果的第一个视频id)
            String sortFieldParam = paramDicts[0][1];
            for (int i = 0; i < paramDicts.length; i++) {
                if (paramDicts[i][0].equals(sortField)) {
                    sortFieldParam = paramDicts[i][1];
                    break;
                }
            }
            String firstOid = position2Oid((page - 1) * pageSize + 1, headersRefer, sortFieldParam);
            if(firstOid.equals("end"))
                return pageQueryResult;
            String lastOidPlus1 = position2Oid(page * pageSize + 1, headersRefer, sortFieldParam);

            // 根据oid查询分页的详细信息
            String urlFormat = "https://api.bilibili.com/x/v2/medialist/resource/list?type=1&oid=%s&otype=2&biz_id=%s&bvid=&with_current=%s&mobi_app=web&ps=%d&direction=false&sort_field=%d&tid=%s&desc=true";
            boolean withCurrent = true;
            int sortFieldIndex = 1;
            for (int i = 0; i < paramDicts.length; i++) {
                if (paramDicts[i][0].equals(sortField)) {
                    sortFieldIndex = i + 1;
                    break;
                }
            }
            boolean findLastOid = false;
            String currentOid = firstOid;
            int pageRemark = (page - 1) * pageSize;
            while(!findLastOid) {
                String url = String.format(urlFormat, currentOid, spaceID, withCurrent, API_PMAX, sortFieldIndex, params.get("tid"));
                Logger.info(url);
                withCurrent = false; // 接下来的查询不需要包括定位的 oid
                String json = util.getContent(url, headers);
                JSONObject jobj = new JSONObject(json);
                JSONArray arr = jobj.getJSONObject("data").getJSONArray("media_list");

                if (pageQueryResult.getVideoPreview() == null) {
                    pageQueryResult.setVideoPreview(arr.getJSONObject(0).getString("cover"));
                }

                LinkedHashMap<Long, ClipInfo> map = pageQueryResult.getClips();
                for (int i = 0; i < arr.length(); i++) {
                    pageRemark++;
                    JSONObject jAV = arr.getJSONObject(i);
                    String oid = jAV.optString("id");
                    if(oid.equals(lastOidPlus1)) {
                        findLastOid = true;
                        break;
                    }
                    currentOid = oid;
                    String avId = jAV.getString("bv_id");
                    String avTitle = jAV.getString("title");
                    String upName = jAV.getJSONObject("upper").getString("name");
                    String upId = jAV.getJSONObject("upper").optString("mid");
                    long cTime = jAV.optLong("pubtime") * 1000;
                    JSONArray jClips = jAV.optJSONArray("pages");
                    if (jClips == null) {
                        continue;
                    }
                    for (int pointer = 0; pointer < jClips.length(); pointer++) {
                        JSONObject jClip = jClips.getJSONObject(pointer);
                        ClipInfo clip = new ClipInfo();
                        clip.setAvId(avId);
                        clip.setcId(jClip.getLong("id"));
                        clip.setPage(jClip.getInt("page"));
                        clip.setRemark(pageRemark); //这个已经没法计算准确了
                        clip.setPicPreview(jAV.getString("cover"));
                        // >= V3.6, ClipInfo 增加可选ListXXX字段，将收藏夹信息移入其中
                        clip.setListName(listName.replaceAll("[/\\\\]", "_"));
                        clip.setListOwnerName(pageQueryResult.getAuthor().replaceAll("[/\\\\]", "_"));
                        clip.setUpName(upName);
                        clip.setUpId(upId);
                        clip.setAvTitle(avTitle);
                        clip.setTitle(jClip.getString("title"));
                        clip.setcTime(cTime);

                        LinkedHashMap<Integer, String> links = new LinkedHashMap<Integer, String>();
                        try {
                            for (VideoQualityEnum VQ : VideoQualityEnum.values()) {
                                if (getVideoLink) {
                                    String link = getVideoLink(avId, String.valueOf(clip.getcId()), VQ.getQn(),
                                            videoFormat);
                                    links.put(VQ.getQn(), link);
                                } else {
                                    links.put(VQ.getQn(), "");
                                }
                            }
                        } catch (Exception e) {
                        }
                        clip.setLinks(links);

                        map.put(clip.getcId(), clip);
                    }
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return pageQueryResult;
    }

    /**
     * 查询视频链接
     *
     * @external input HttpRequestUtil util
     * @external input downFormat
     * @external output linkQN 保存返回链接的清晰度
     * @param bvId 视频的bvId
     * @param cid  av下面可能不只有一个视频, avId + cid才能确定一个真正的视频
     * @param qn   112: hdflv2;80: flv; 64: flv720; 32: flv480; 16: flv360
     * @return
     */
    public String getVideoLink(String bvId, String cid, int qn, int downFormat) {
        if (bvId.startsWith("au") && (qn > 3 || qn < 0)) {
            qn = 3;
        }
        switch (qn) {
            case 800:
                return getVideoSubtitleLink(bvId, cid, qn);
            case 3:
            case 2:
            case 1:
            case 0:
                return getAudioLink(bvId, cid, qn);
            default:
                return getVideoLinkByFormat(bvId, cid, qn, downFormat);
        }
    }

    protected String getVideoSubtitleLink(String bvId, String cid, int qn) {
        String url = String.format("https://api.bilibili.com/x/player.so?id=cid:%s&bvid=%s", cid, bvId);
        Logger.info(url);
        HashMap<String, String> headers_json = new HttpHeaders().getBiliJsonAPIHeaders(bvId);
        String xml = util.getContent(url, headers_json, HttpCookies.globalCookiesWithFingerprint());
        Pattern p = Pattern.compile("<subtitle>(.*?)</subtitle>");
        Matcher matcher = p.matcher(xml);
        if (matcher.find()) {
            JSONArray subList = new JSONObject(matcher.group(1)).getJSONArray("subtitles");
            for (int i = 0; i < subList.length(); i++) {
                JSONObject sub = subList.getJSONObject(i);
                String subLang = sub.getString("lan");
                    return "https:" + sub.getString("subtitle_url");
            }

            return "https:" + subList.getJSONObject(0).getString("subtitle_url");
        }

        return null;
    }

    protected String getAudioLink(String auId, String _auId, int qn) {
        String auIdNum = auId.substring(2);
//		String url = String.format("https://www.bilibili.com/audio/music-service-c/web/url?sid=%s&privilege=2&quality=2", auIdNum);
        String url = String.format("https://www.bilibili.com/audio/music-service-c/url?songid=%s&privilege=2&quality=%d&mid=&platform=web", auIdNum, qn);
        Logger.info(url);
        HashMap<String, String> headers = new HttpHeaders().getCommonHeaders();
        String r = util.getContent(url, headers, HttpCookies.globalCookiesWithFingerprint());
        Logger.info(r);
        JSONObject data = new JSONObject(r).getJSONObject("data");
        int realQn = data.optInt("type");
        Logger.info("预期下载清晰度：%d, 实际清晰度：%d", qn, realQn);
        String link = data.getJSONArray("cdns").getString(0);
        Logger.info(link);
        return link;
    }

    String getVideoLinkByFormat(String bvId, String cid, int qn, int downloadFormat) {
        System.out.print("正在查询MP4/FLV链接...");
        HttpHeaders headers = new HttpHeaders();
        JSONObject jObj = null;
        // 根据downloadFormat确定fnval
        String fnval = "4048" ;
        // 先判断类型
        String url = "https://api.bilibili.com/x/web-interface/view/detail?aid=&jsonp=jsonp&callback=__jp0&bvid="
                + bvId;
        HashMap<String, String> header = headers.getBiliJsonAPIHeaders(bvId);
        String callBack = util.getContent(url, header);
        JSONObject infoObj = new JSONObject(callBack.substring(6, callBack.length() - 1)).getJSONObject("data")
                .getJSONObject("View");
        Long aid = infoObj.optLong("aid");

        if (infoObj.optString("redirect_url").isEmpty()) {
            String trylookTail = "";
            // 普通类型
            url = downloadFormat == 2 ?
                    // 下面这个API清晰度没法选择，编码方式没法选择，固定返回1080P? mp4
                    // https://api.bilibili.com/x/player/wbi/playurl?avid=857672756&bvid=BV1HV4y1p7ce&cid=1084157816&qn=80&fnver=0&fnval=4048&fourk=1
                    "https://api.bilibili.com/x/player/wbi/playurl?cid=%s&bvid=%s&qn=%d":
                    "https://api.bilibili.com/x/player/wbi/playurl?cid=%s&bvid=%s&qn=%d&type=&otype=json&fnver=0&fnval=%s&fourk=1";
            url += trylookTail;
            url = String.format(url, cid, bvId, qn, fnval);
            url = API.encWbi(url);
            Logger.info(url);
//			List cookie = downloadFormat == 2 ? null : HttpCookies.globalCookiesWithFingerprint();
            List<HttpCookie> cookie = HttpCookies.globalCookiesWithFingerprint();
            String json = util.getContent(url, headers.getBiliJsonAPIHeaders(bvId), cookie);
            jObj = new JSONObject(json).getJSONObject("data");
        } else {
            // 非普通类型
            url = "https://api.bilibili.com/pgc/player/web/playurl?fnver=0&fourk=1&otype=json&avid=%s&cid=%s&qn=%s&fnval=%s";
            url = String.format(url, aid, cid, qn, fnval);
            String json = util.getContent(url, headers.getBiliJsonAPIHeaders("av" + aid),
                    HttpCookies.globalCookiesWithFingerprint());
            Logger.info(url);
            Logger.info(json);
            jObj = new JSONObject(json).getJSONObject("result");
        }
        int linkQN = jObj.getInt("quality");
        if(qn != linkQN) { // 只有和预期不符才会去判断
            // 有时候，api返回的列表中含有比指定清晰度更高的内容
            JSONObject dash = jObj.optJSONObject("dash");
            if(dash != null) {
                JSONArray videos = dash.getJSONArray("video");
                int firstLinkQN = videos.getJSONObject(0).getInt("id");
                if(linkQN < firstLinkQN) {
                    linkQN = firstLinkQN > qn? qn: firstLinkQN;
                }
            }
        }
        String tips = String.format("%s:%s - 查询质量为: %d的链接, 得到质量为: %d的链接", bvId, cid, qn, linkQN);
        Logger.info(tips);
        if(linkQN < 64 && qn > linkQN ) {
            String notes = tips + "\n该视频的最高画质清晰度较低，请更换相匹配的优先清晰度之后再进行尝试。\n"
                    + "如果你认为此处应当继续下载，而不是报错，请在配置页搜索 qualityUnexpected 并进行配置\n";
            throw new QualityTooLowException(notes);
        }
        try {
            return parseType1(jObj, linkQN, headers.getBiliWwwM4sHeaders(bvId));
        } catch (Exception e) {
            // e.printStackTrace();
            Logger.info("切换解析方式");
            try {
                // 鉴于部分视频如 https://www.bilibili.com/video/av24145318 H5仍然是用的是Flash源,此处切为FLV
                return parseType2(jObj);
            }catch (Exception e1) {
                e.printStackTrace();
                e1.printStackTrace();
                throw new ApiLinkQueryParseError("查询下载链接时api解析失败", e);
            }
        }
    }

    protected String parseType1(JSONObject jObj, int linkQN, HashMap<String, String> headerForValidCheck) {
        JSONObject dash = jObj.getJSONObject("dash");
        StringBuilder link = new StringBuilder();
        // 获取视频链接
        if(0 == DownloadModeEnum.AudioOnly.getMode()) {
            link.append("#");
        }else {
            JSONArray videos = dash.getJSONArray("video");
            // 获取所有符合清晰度要求的视频
            List<JSONObject> qnVideos = new ArrayList<>(3);
            for (int i = 0; i < videos.length(); i++) {
                JSONObject video = videos.getJSONObject(i);
                if (video.getInt("id") == linkQN) {
                    qnVideos.add(video);
                }
            }
            // 如果没有找到对应的清晰度 eg. BV1K14y1g7iU 无cookie
            //（API 返回的链接里并没有对应实际清晰度的链接）
            // 那么只能随便加一个了
            if(qnVideos.size() == 0) {
                JSONObject v = videos.getJSONObject(0);
                System.out.println("API返回质量为:" + linkQN + "的链接, 实际上只有质量为:"  + "的链接");
                qnVideos.add(v);
            }
            // 根据清晰度选择合适的编码优先级
            //80:7, 12, 13| 64:7, 12, 13
            int[] videoCodecPriority = linkQN == 80? new int[]{7, 12, 13}: new int[]{7, 12, 13};
            // 根据需求选择编码合适的视频
            JSONObject video = findMediaByPriList(qnVideos, videoCodecPriority, 0);
            // 选择可以连通的链接
            String videoLink = getUrlOfMedia(video, false, headerForValidCheck);
            link.append(videoLink).append("#");
        }

        // 获取音频链接
            // 获取所有音频
            List<JSONObject> listAudios = new ArrayList<>(5);
            JSONArray audios = dash.optJSONArray("audio");// 普通
            if (audios != null) {
                for (int i = 0; i < audios.length(); i++) {
                    listAudios.add(audios.getJSONObject(i));
                }
            }
            JSONObject dolby = dash.optJSONObject("dolby");// 杜比
            if (dolby != null && linkQN == 126) {
                audios = dolby.optJSONArray("audio");
                if (audios != null) {
                    for (int i = 0; i < audios.length(); i++) {
                        listAudios.add(audios.getJSONObject(i));
                    }
                }
            }
            JSONObject flac = dash.optJSONObject("flac");// flac
            JSONObject flacAudio = flac == null? null: flac.optJSONObject("audio");// audio
            if (flacAudio != null) {
                listAudios.add(flacAudio);
            }
            if(listAudios.size() > 0) { // 存在没有音频的投稿
                JSONObject audio = findMediaByPriList(listAudios, new int[] {30280, 30232, 30216, -1}, 1);
                String audioLink = getUrlOfMedia(audio, false, headerForValidCheck);
                link.append(audioLink);
            }
//		Logger.println(link);
        return link.toString();
    }

    String getUrlOfMedia(JSONObject media, boolean checkValid, HashMap<String, String> headerForValidCheck) {
        String baseUrl = media.getString("base_url");
        if(!checkValid) {
            return baseUrl;
        }else {
            if (util.checkValid(baseUrl, headerForValidCheck, null)) {
                return baseUrl;
            } else {
                JSONArray backup_urls = media.getJSONArray("backup_url");
                for (int j = 0; j < backup_urls.length(); j++) {
                    String backup_url = backup_urls.getString(j);
                    if (util.checkValid(backup_url, headerForValidCheck, null)) {
                        return backup_url;
                    }
                }
                return null;
            }
        }
    }
    JSONObject findMediaByPriList(List<JSONObject> medias, int[] priorities, int mediaType) {
        for(int priority: priorities) {
            JSONObject media = findMediaByPri(medias, priority, mediaType);
            if(media != null)
                return media;
        }
        return medias.get(0);
    }
    JSONObject findMediaByPri(List<JSONObject> medias, int priority, int mediaType) {
        for(JSONObject media: medias) {
            if(-1 == priority || (mediaType == 0 &&media.getInt("codecid") == priority)
                    || (mediaType == 1 &&media.getInt("id") == priority))
                return media;
        }
        return null;
    }


    private String position2Oid(int pageNumber, HashMap<String, String> headers, String sortFieldParam) {
        if(pageNumber == 1)
            return "";
        // String urlFormat = "https://api.bilibili.com/x/space/arc/search?mid=%s&ps=%d&tid=%s&pn=%d&keyword=&order=%s&jsonp=jsonp";
        String urlFormat = "https://api.bilibili.com/x/space/wbi/arc/search?mid=%s&ps=%d&tid=%s&special_type=&pn=%d&keyword=&order=%s&platform=web"; // &web_location=1550101&order_avoided=true
        String url = String.format(urlFormat, spaceID, 1, params.get("tid"), pageNumber, sortFieldParam);
        url += API.genDmImgParams();
        url = API.encWbi(url);
        String json = util.getContent(url, headers, HttpCookies.globalCookiesWithFingerprint());
        Logger.info(url);
        Logger.info(json);
        JSONArray vlist = new JSONObject(json).getJSONObject("data").getJSONObject("list").getJSONArray("vlist");
        if(vlist.length() == 0) {
            Logger.info("position: %d, oid: search till end", pageNumber);
            return "end";
        } else {
            String oid = vlist.getJSONObject(0).optString("aid");
            Logger.info("position: %d, oid: %s", pageNumber, oid);
            return oid;
        }
    }

    protected String parseType2(JSONObject jObj) {
        JSONArray urlList = jObj.getJSONArray("durl");
        if (urlList.length() == 1) {
            return urlList.getJSONObject(0).getString("url");

        } else {
            StringBuilder link = new StringBuilder();
            for (int i = 0; i < urlList.length(); i++) {
                JSONObject obj = urlList.getJSONObject(i);
                link.append(obj.getInt("order"));
                link.append(obj.getString("url"));
                link.append("#");
            }
            System.out.println(link.substring(0, link.length() - 1));
            return link.substring(0, link.length() - 1);
        }
    }

}
