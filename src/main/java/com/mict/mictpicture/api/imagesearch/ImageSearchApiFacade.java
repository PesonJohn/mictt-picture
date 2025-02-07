package com.mict.mictpicture.api.imagesearch;

import com.mict.mictpicture.api.imagesearch.model.ImageSearchResult;
import com.mict.mictpicture.api.imagesearch.sub.GetImageFirstUrlApi;
import com.mict.mictpicture.api.imagesearch.sub.GetImageListApi;
import com.mict.mictpicture.api.imagesearch.sub.GetImagePageUrlApi;

import java.util.List;

/**
 * 设计模式——门面模式：通过一个统一的接口来简化多个接口的调用
 * 以图搜图api整合到一个门面类中
 */
public class ImageSearchApiFacade {

    public static List<ImageSearchResult> searchImage(String imageUrl){
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }
}
