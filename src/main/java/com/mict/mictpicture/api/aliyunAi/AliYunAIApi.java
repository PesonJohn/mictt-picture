package com.mict.mictpicture.api.aliyunAi;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.mict.mictpicture.api.aliyunAi.model.CreateOutPaintingTaskRequest;
import com.mict.mictpicture.api.aliyunAi.model.CreateOutPaintingTaskResponse;
import com.mict.mictpicture.api.aliyunAi.model.GetOutPaintingTaskResponse;
import com.mict.mictpicture.exception.BusinessException;
import com.mict.mictpicture.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAIApi {

    //读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    //创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    //查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    //创建任务
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest){
        if (createOutPaintingTaskRequest == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"扩图参数为空");
        }
        //发送请求
        HttpRequest request = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                //开启异步处理
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        //处理响应
        try(HttpResponse httpResponse = request.execute()){
            if (!httpResponse.isOk()){
                log.error("请求异常：{}",httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"AI扩图失败");
            }
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            if (createOutPaintingTaskResponse.getCode() != null){
                String errorMessage = createOutPaintingTaskResponse.getMessage();
                log.error("请求异常：{}",errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"AI扩图失败 " + errorMessage);
            }
            return createOutPaintingTaskResponse;
        }
    }

    /**
     * 查询创建的任务结果
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId){
        if (taskId == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"任务id为空");
        }
        String url = String.format(GET_OUT_PAINTING_TASK_URL,taskId);
        //处理响应
        try(HttpResponse httpResponse = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey)
                .execute()){
            if (!httpResponse.isOk()){
                log.error("请求异常：{}",httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取任务结果失败");
            }

            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
