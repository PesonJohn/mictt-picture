package com.mict.mictpicture.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.mict.mictpicture.config.CosClientConfig;
import com.mict.mictpicture.exception.BusinessException;
import com.mict.mictpicture.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * COS 通用使用
 */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 删除对象
     * @param key
     * @return
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(),key);
    }

    /**
     * 上传并解析图片（附带图片信息）
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        //对图片进行处理 (获取图片基本信息也视作一种图片的处理)
        PicOperations picOperations = new PicOperations();
        //1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        //图片处理规则列表
        List<PicOperations.Rule> rules = new ArrayList<>();
        //1.图片压缩（转成webp格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        rules.add(compressRule);
        //2.缩略图处理 对20kB以上的图片进行处理
        if (file.length() > 2 * 1024){
            PicOperations.Rule thumnailRule = new PicOperations.Rule();
            //拼接缩略图路径
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumnailRule.setFileId(thumbnailKey);
            //缩放规则 /thumbnail/<Width>x<Height>> 大于原图宽高不处理
            thumnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>",256,256));
            thumnailRule.setBucket(cosClientConfig.getBucket());
            rules.add(thumnailRule);
        }
        //构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 获取图片主色调
     * @param key 图片的唯一键
     * @return 图片的主色调信息
     */
    public String getImageAve(String key) {
        // 创建获取对象的请求
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        // 设置图片处理规则为获取主色调
        String rule = "imageAve";
        getObjectRequest.putCustomQueryParameter(rule, null);
        // 获取对象
        COSObject cosObject = cosClient.getObject(getObjectRequest);
        // 读取内容流并解析主色调信息
        try (COSObjectInputStream objectContent = cosObject.getObjectContent();
             ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            // 读取流内容
            byte[] buffer = new byte[1024];
            int length;
            while ((length = objectContent.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            // 将字节数组转换为字符串
            String aveColor = result.toString("UTF-8");
            return JSONUtil.parseObj(aveColor).getStr("RGB");
        } catch (IOException e) {
            log.error("获取图片主色调失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取图片主色调失败");
        }
    }

}
