package com.mict.mictpicture.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.mict.mictpicture.exception.BusinessException;
import com.mict.mictpicture.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取以图搜图页面地址
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl){
        //1. 准备请求参数
        Map<String,Object> formData = new HashMap<>();
        /*
            image: https://xxx/logo.png
            tn: pc
            from: pc
            image_source: PC_UPLOAD_URL
         */
        formData.put("image",imageUrl);
        formData.put("tn","pc");
        formData.put("from","pc");
        formData.put("image_source","PC_UPLOAD_URL");
        //获取当前时间戳
        long upTime = System.currentTimeMillis();
        //请求地址
        String url = "https://graph.baidu.com/upload?uptime="+upTime;
        try{
            //2. 发送请求
            HttpResponse httpResponse = HttpRequest.post(url)
                    .form(formData)
                    .timeout(5000)
                    .execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"接口调用失败");
            }
            //解析响应
            String body = httpResponse.body();
            Map<String,Object> res = JSONUtil.toBean(body, Map.class);
            //3. 处理响应结果
            if (res == null || !Integer.valueOf(0).equals(res.get("status"))){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) res.get("data");
            //对url解码
            String rawUrl = (String) data.get("url");
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            if (StrUtil.isBlank(searchResultUrl)){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"未返回有效结果地址");
            }
            return searchResultUrl;
        }catch (Exception e){
            log.error("调用百度以图搜图接口失败 ",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"搜索失败");
        }

    }

    public static void main(String[] args) {
        String imageUrl = "https://www.codefather.cn/logo.png";
        String searchResultUrl = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果url为：" + searchResultUrl);
    }
}
