package com.mict.mictpicture.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.mict.mictpicture.config.CosClientConfig;
import com.mict.mictpicture.exception.BusinessException;
import com.mict.mictpicture.exception.ErrorCode;
import com.mict.mictpicture.exception.ThrowUtils;
import com.mict.mictpicture.manager.CosManager;
import com.mict.mictpicture.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * 图片上传模板 （设计模式-模板方法）
 */
@Slf4j
public abstract class PictureUploadTemplate {
  
    @Resource
    private CosClientConfig cosClientConfig;
  
    @Resource  
    private CosManager cosManager;


    /**
     * 上传图片
     * @param inputSource 文件 (此处文件类型只有两种因此用Object，如果类型多可用泛型)
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource,String uploadPathPrefix){
        //1 校验图片
        validPicture(inputSource);
        //2 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        //自己拼接文件上传路径，不使用原始文件名称，增强安全性
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid,FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s",uploadPathPrefix,uploadFileName);
        //解析结果并返回
        File file = null;
        try {
            //3 创建临时文件,获取文件到服务器
            file = File.createTempFile(uploadPath,null);
            //处理文件来源
            processFile(inputSource,file);
            //4 上传到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //5 获取图片信息对象 封装返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //有的图片色调在imageInfo中会出现少位现象，再次调用api获取六位的图片主色调
            imageInfo.setAve(cosManager.getImageAve(uploadPath));
            //获取图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)){
                //获取压缩之后得到的文件信息
                CIObject compressedCiObject = objectList.get(0);
                //缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                if (objectList.size() > 1){
                    thumbnailCiObject = objectList.get(1);
                }
                return buildResult(originalFilename, compressedCiObject,thumbnailCiObject,imageInfo);
            }
            return buildResult(originalFilename, file, uploadPath, imageInfo);
        } catch (IOException e) {
            log.error("图片上传到对象存储失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        }finally {
            //6 删除临时文件
            deleteTempFile(file);
        }
        //临时文件清理
    }

    /**
     * 校验输入源（url或者本地文件）
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源生成本地临时文件
     * @param inputSource
     * @param file
     */
    protected abstract void processFile(Object inputSource,File file) throws IOException;

    /**
     * 封装返回结果（压缩后的）
     * @param originalFilename
     * @param compressedCiObject 压缩后的对象
     * @param thumbnailCiObject 缩略图对象
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressedCiObject, CIObject thumbnailCiObject,ImageInfo imageInfo) {
        //计算宽高
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        //计算宽高比
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight,2).doubleValue();
        //封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        //设置压缩后的原图地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        //设置缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        //返回可访问的地址
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     * @param originalFilename
     * @param file
     * @param uploadPath
     * @param imageInfo 对象存储返回的图片信息
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename,File file, String uploadPath, ImageInfo imageInfo) {
        //计算宽高
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        //计算宽高比
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight,2).doubleValue();
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        //返回可访问的地址
        return uploadPictureResult;
    }


    /**
     * 清理临时文件
     * @param file
     */
    public static void deleteTempFile(File file) {
        if (file != null){
            boolean deleteRes = file.delete();
            if (!deleteRes){
                log.error("file delete error,filepath is {}",file.getAbsoluteFile());
            }
        }
    }


}
