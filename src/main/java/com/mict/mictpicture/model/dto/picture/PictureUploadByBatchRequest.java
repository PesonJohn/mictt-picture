package com.mict.mictpicture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量导入请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {
  
    /**  
     * 搜索关键词
     */  
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix;
  
    private static final long serialVersionUID = 1L;  
}
