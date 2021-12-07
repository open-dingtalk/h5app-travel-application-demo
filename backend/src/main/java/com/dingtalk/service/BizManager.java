package com.dingtalk.service;

import com.aliyun.dingboot.common.token.ITokenManager;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.*;
import com.dingtalk.api.response.*;
import com.dingtalk.config.AppConfig;
import com.dingtalk.constant.UrlConstant;
import com.dingtalk.model.FlowEntity;
import com.dingtalk.model.TripEntity;
import com.taobao.api.ApiException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * 主业务service，编写你的代码
 */
@Service
public class BizManager {
    @Autowired
    ITokenManager tokenManager;
    @Autowired
    private AppConfig appConfig;
    private HashMap<String, Object> hashMap = new HashMap<>();

    public String hello() {
        return "HelloWorld";
    }


    public String getProcessCode() throws ApiException {
        // 1. 获取access_token
        // String accessToken = AccessTokenUtil.getAccessToken();
        String accessToken = tokenManager.getAccessToken(appConfig.getAppKey(), appConfig.getAppSecret());
        //2获取
        DingTalkClient client = new DefaultDingTalkClient(UrlConstant.PROCESS_GET_NAME);
        OapiProcessGetByNameRequest oapiProcessGetByNameRequest = new OapiProcessGetByNameRequest();
        oapiProcessGetByNameRequest.setName("易快报差旅申请单");
        OapiProcessGetByNameResponse execute = client.execute(oapiProcessGetByNameRequest, accessToken);
        if (StringUtils.isNotEmpty(execute.getProcessCode())) {
            return execute.getProcessCode();
        } else {
            return createProcess();
        }
//        return createProcess();
    }

    /**
     * 创建模板
     *
     * @return
     */
    public String createProcess() throws ApiException {
        // 1. 获取access_token
        //String accessToken = AccessTokenUtil.getAccessToken();
        String accessToken = tokenManager.getAccessToken(appConfig.getAppKey(), appConfig.getAppSecret());
        // 2. 创建模板
        DingTalkClient client = new DefaultDingTalkClient(UrlConstant.PROCESS_SAVE);
        OapiProcessSaveRequest oapiProcessSaveRequest = new OapiProcessSaveRequest();
        OapiProcessSaveRequest.SaveProcessRequest saveProcessRequest = new OapiProcessSaveRequest.SaveProcessRequest();
        saveProcessRequest.setAgentid(appConfig.getAgentId());
        ArrayList<OapiProcessSaveRequest.FormComponentVo> formComponentVos = new ArrayList<>();
        saveProcessRequest.setName("易快报差旅申请单");
//        saveProcessRequest.setProcessCode("PROC-A6A89562-474A-46EE-BDC3-0413986DED0D");
        saveProcessRequest.setDescription("易快报差旅申请单");
        OapiProcessSaveRequest.FormComponentVo formComponentVo = new OapiProcessSaveRequest.FormComponentVo();
        formComponentVo.setComponentName("TextField");
        OapiProcessSaveRequest.FormComponentPropVo formComponentPropVo = new OapiProcessSaveRequest.FormComponentPropVo();
        formComponentPropVo.setId("TextField-submitId");
        formComponentPropVo.setLabel("提交人");
        formComponentPropVo.setRequired(true);
        formComponentVo.setProps(formComponentPropVo);
        formComponentVos.add(formComponentVo);
        formComponentVo = new OapiProcessSaveRequest.FormComponentVo();
        formComponentPropVo = new OapiProcessSaveRequest.FormComponentPropVo();
        formComponentVo.setComponentName("TextField");
        formComponentPropVo.setId("TextField-flowDesc");
        formComponentPropVo.setLabel("出差事由");
        formComponentPropVo.setRequired(true);
        formComponentVo.setProps(formComponentPropVo);
        formComponentVos.add(formComponentVo);

        saveProcessRequest.setFormComponentList(formComponentVos);
        saveProcessRequest.setFakeMode(true);
        oapiProcessSaveRequest.setSaveProcessRequest(saveProcessRequest);
        OapiProcessSaveResponse execute = client.execute(oapiProcessSaveRequest, accessToken);
        // 3. 返回模板id
        if (execute.getErrcode() != 0) {
            throw new ApiException("-1", execute.getErrmsg());
        }
        return execute.getResult().getProcessCode();
    }

    /**
     * 创建并获取实例id
     *
     * @param flowEntity
     * @return
     * @throws ApiException
     */
    public String createWorkRecord(FlowEntity flowEntity) throws ApiException {
        // 1. 获取access_token
//        String accessToken = AccessTokenUtil.getAccessToken();
        String accessToken = tokenManager.getAccessToken(appConfig.getAppKey(), appConfig.getAppSecret());
        OapiProcessWorkrecordCreateRequest oapiProcessWorkrecordCreateRequest = new OapiProcessWorkrecordCreateRequest();
        OapiProcessWorkrecordCreateRequest.SaveFakeProcessInstanceRequest saveFakeProcessInstanceRequest = new OapiProcessWorkrecordCreateRequest.SaveFakeProcessInstanceRequest();
        String processCode = getProcessCode();
        saveFakeProcessInstanceRequest.setProcessCode(processCode);
        saveFakeProcessInstanceRequest.setAgentid(appConfig.getAgentId());
        saveFakeProcessInstanceRequest.setOriginatorUserId(flowEntity.getUserId());
        OapiProcessWorkrecordCreateRequest.FormComponentValueVo formComponentValueVo = new OapiProcessWorkrecordCreateRequest.FormComponentValueVo();
        ArrayList<OapiProcessWorkrecordCreateRequest.FormComponentValueVo> formComponentValueVos = new ArrayList<>();
        formComponentValueVo.setName("提交人");
        formComponentValueVo.setValue(flowEntity.getUserName());
        formComponentValueVos.add(formComponentValueVo);

        formComponentValueVo = new OapiProcessWorkrecordCreateRequest.FormComponentValueVo();
        formComponentValueVo.setName("出差事由");
        formComponentValueVo.setValue(flowEntity.getFlowDesc());
        formComponentValueVos.add(formComponentValueVo);

       /* formComponentValueVo = new OapiProcessWorkrecordCreateRequest.FormComponentValueVo();
        formComponentValueVo.setName("申请金额");
        formComponentValueVo.setValue(flowEntity.getFlowAmount().setScale(2, BigDecimal.ROUND_UP).toString());
        formComponentValueVos.add(formComponentValueVo);*/

        saveFakeProcessInstanceRequest.setFormComponentValues(formComponentValueVos);
        saveFakeProcessInstanceRequest.setUrl(flowEntity.getUrl());
        saveFakeProcessInstanceRequest.setTitle(flowEntity.getUserName()+"的"+flowEntity.getFlowName());
        oapiProcessWorkrecordCreateRequest.setRequest(saveFakeProcessInstanceRequest);
        DingTalkClient client = new DefaultDingTalkClient(UrlConstant.PROCESS_WORK_RECORD_CREATE);
        OapiProcessWorkrecordCreateResponse execute = client.execute(oapiProcessWorkrecordCreateRequest, accessToken);
        if (execute.getErrcode() != 0) {
            throw new ApiException("-1", execute.getErrmsg());
        }
        return execute.getResult().getProcessInstanceId();
    }

    /**
     * 更新实例
     *
     * @param flowEntity
     * @return
     * @throws ApiException
     */
    public Long updateWorkRecord(FlowEntity flowEntity) throws Exception {
        // 1. 获取access_token
//        String accessToken = AccessTokenUtil.getAccessToken();
        String accessToken = tokenManager.getAccessToken(appConfig.getAppKey(), appConfig.getAppSecret());
        OapiProcessWorkrecordUpdateRequest oapiProcessWorkrecordUpdateRequest = new OapiProcessWorkrecordUpdateRequest();
        OapiProcessWorkrecordUpdateRequest.UpdateProcessInstanceRequest updateProcessInstanceRequest = new OapiProcessWorkrecordUpdateRequest.UpdateProcessInstanceRequest();
        updateProcessInstanceRequest.setAgentid(appConfig.getAgentId());
        updateProcessInstanceRequest.setProcessInstanceId(flowEntity.getWorkRecordId());
        updateProcessInstanceRequest.setStatus("COMPLETED");
        updateProcessInstanceRequest.setResult(flowEntity.getFlowStatus());
        oapiProcessWorkrecordUpdateRequest.setRequest(updateProcessInstanceRequest);
        DingTalkClient client = new DefaultDingTalkClient(UrlConstant.PROCESS_WORK_RECORD_UPDATE);
        OapiProcessWorkrecordUpdateResponse execute = client.execute(oapiProcessWorkrecordUpdateRequest, accessToken);
        if (execute.getErrcode() != 0) {
            throw new ApiException("-1", execute.getErrmsg());
        }
        if (flowEntity.getFlowStatus().equals("agree")) {
            FlowEntity flowEntityCache = (FlowEntity) hashMap.get(flowEntity.getUuid());
            approveFinish(flowEntityCache);
        }
        FlowEntity flowEntityCache = (FlowEntity) hashMap.get(flowEntity.getUuid());
        flowEntityCache.setFlowStatus(flowEntity.getFlowStatus());
        hashMap.put(flowEntityCache.getUuid(),flowEntityCache);
        return execute.getErrcode();
    }

    public void approveFinishAli(FlowEntity flowEntity) throws Exception {
        String accessToken = tokenManager.getAccessToken(appConfig.getAppKey(), appConfig.getAppSecret());
        OapiAlitripBtripApprovalNewRequest request = new OapiAlitripBtripApprovalNewRequest();
        OapiAlitripBtripApprovalNewRequest.OpenApiNewApplyRq rq = new OapiAlitripBtripApprovalNewRequest.OpenApiNewApplyRq();
        rq.setThirdpartApplyId(UUID.randomUUID().toString());
        rq.setTripTitle(flowEntity.getUserName()+"的"+flowEntity.getFlowName());
        ArrayList<OapiAlitripBtripApprovalNewRequest.OpenItineraryInfo> itineraryList = new ArrayList<>();
        for (TripEntity tripEntity : flowEntity.getTripList()) {
            OapiAlitripBtripApprovalNewRequest.OpenItineraryInfo openItineraryInfo = new OapiAlitripBtripApprovalNewRequest.OpenItineraryInfo();
            openItineraryInfo.setTripWay(1L);
            openItineraryInfo.setItineraryId(UUID.randomUUID().toString());
            if(tripEntity.getTripType().equals("飞机")){
                openItineraryInfo.setTrafficType(0L);
            }else {
                openItineraryInfo.setTrafficType(1L);
            }
            itineraryList.add(openItineraryInfo);
            openItineraryInfo.setDepCity(tripEntity.getTripStartCity());
            openItineraryInfo.setArrCity(tripEntity.getTripEndCity());

        }
        rq.setItineraryList(itineraryList);
        request.setRq(rq);
        DefaultDingTalkClient client = new DefaultDingTalkClient(UrlConstant.ATTENDANCE_APPROVE_FINISH);
        OapiAttendanceApproveFinishResponse execute = new OapiAttendanceApproveFinishResponse();
       /* try {
            execute = client.execute(request, accessToken);
        } catch (ApiException e) {
            e.printStackTrace();
        }*/
    }

    public void approveFinish(FlowEntity flowEntity) throws Exception {
        String accessToken = tokenManager.getAccessToken(appConfig.getAppKey(), appConfig.getAppSecret());
        OapiAttendanceApproveFinishRequest request = new OapiAttendanceApproveFinishRequest();
        request.setUserid(flowEntity.getUserId());
        request.setBizType(2L);
        request.setFromTime(flowEntity.getFlowStartTime());
        request.setToTime(flowEntity.getFlowEndTime());
        request.setDurationUnit("day");
        request.setCalculateModel(1L);
        request.setTagName("出差");
        request.setSubType(flowEntity.getFlowName());
        request.setApproveId(UUID.randomUUID().toString());
        request.setJumpUrl(flowEntity.getUrl());
        DefaultDingTalkClient client = new DefaultDingTalkClient(UrlConstant.ATTENDANCE_APPROVE_FINISH);
        OapiAttendanceApproveFinishResponse execute = new OapiAttendanceApproveFinishResponse();
        try {
            execute = client.execute(request, accessToken);
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建待办
     *
     * @param flowEntity userName,userId，
     * @return
     * @throws ApiException
     */
    public String createWorkRecordTask(FlowEntity flowEntity) throws ApiException {
        // 1. 获取access_token
//        String accessToken = AccessTokenUtil.getAccessToken();
        String accessToken = tokenManager.getAccessToken(appConfig.getAppKey(), appConfig.getAppSecret());
        //2创建实例
        String workRecord = createWorkRecord(flowEntity);
        //3创建待办任务
        OapiProcessWorkrecordTaskCreateRequest request = new OapiProcessWorkrecordTaskCreateRequest();
        OapiProcessWorkrecordTaskCreateRequest.SaveTaskRequest saveTaskRequest = new OapiProcessWorkrecordTaskCreateRequest.SaveTaskRequest();
        ArrayList<OapiProcessWorkrecordTaskCreateRequest.TaskTopVo> taskTopVos = new ArrayList<>();
        OapiProcessWorkrecordTaskCreateRequest.TaskTopVo taskTopVo = new OapiProcessWorkrecordTaskCreateRequest.TaskTopVo();
        taskTopVo.setUserid(flowEntity.getUserId());
        taskTopVo.setUrl(flowEntity.getUrl());
        taskTopVos.add(taskTopVo);
        saveTaskRequest.setTasks(taskTopVos);
        saveTaskRequest.setProcessInstanceId(workRecord);
        request.setRequest(saveTaskRequest);
        DingTalkClient client = new DefaultDingTalkClient(UrlConstant.PROCESS_WORK_RECORD_TASK_CREATE);
        OapiProcessWorkrecordTaskCreateResponse execute = client.execute(request, accessToken);
        if (execute.getErrcode() != 0) {
            throw new ApiException("-1", execute.getErrmsg());
        }
        flowEntity.setWorkRecordId(workRecord);
        hashMap.put(flowEntity.getUuid(), flowEntity);
        return workRecord;
    }

    /**
     * 先不用
     *
     * @param flowEntity
     * @throws ApiException
     */
    public void UpdateWorkRecordTask(FlowEntity flowEntity) throws ApiException {
        // 1. 获取access_token
//        String accessToken = AccessTokenUtil.getAccessToken();
        String accessToken = tokenManager.getAccessToken(appConfig.getAppKey(), appConfig.getAppSecret());
        OapiProcessWorkrecordUpdateRequest oapiProcessWorkrecordUpdateRequest = new OapiProcessWorkrecordUpdateRequest();
        OapiProcessWorkrecordUpdateRequest.UpdateProcessInstanceRequest updateProcessInstanceRequest = new OapiProcessWorkrecordUpdateRequest.UpdateProcessInstanceRequest();
        updateProcessInstanceRequest.setAgentid(appConfig.getAgentId());
        updateProcessInstanceRequest.setProcessInstanceId(flowEntity.getWorkRecordId());
        updateProcessInstanceRequest.setStatus("COMPLETED");
        updateProcessInstanceRequest.setResult(flowEntity.getFlowStatus());
        oapiProcessWorkrecordUpdateRequest.setRequest(updateProcessInstanceRequest);
        DingTalkClient client = new DefaultDingTalkClient(UrlConstant.PROCESS_WORK_RECORD_UPDATE);
        OapiProcessWorkrecordUpdateResponse execute = client.execute(oapiProcessWorkrecordUpdateRequest, accessToken);
    }

    /**
     * 获取待办列表
     *
     * @param flowEntity
     * @throws ApiException
     */
    public OapiProcessWorkrecordTaskQueryResponse getTasks(FlowEntity flowEntity) throws ApiException {
        // 1. 获取access_token
//        String accessToken = AccessTokenUtil.getAccessToken();
        String accessToken = tokenManager.getAccessToken(appConfig.getAppKey(), appConfig.getAppSecret());
        OapiProcessWorkrecordTaskQueryRequest oapiProcessWorkrecordTaskQueryRequest = new OapiProcessWorkrecordTaskQueryRequest();
        oapiProcessWorkrecordTaskQueryRequest.setUserid(flowEntity.getUserId());
        oapiProcessWorkrecordTaskQueryRequest.setOffset(0L);
        oapiProcessWorkrecordTaskQueryRequest.setCount(50L);
        oapiProcessWorkrecordTaskQueryRequest.setStatus(flowEntity.getTaskStatus());
        DingTalkClient client = new DefaultDingTalkClient(UrlConstant.PROCESS_WORK_RECORD_TASK_QUERY);
        OapiProcessWorkrecordTaskQueryResponse execute = client.execute(oapiProcessWorkrecordTaskQueryRequest, accessToken);
        return execute;
    }

    /**
     * 获取根部门用户集合
     */
    public List<OapiUserListsimpleResponse.ListUserSimpleResponse> getUserlist() throws ApiException {
        String accessToken = tokenManager.getAccessToken(appConfig.getAppKey(), appConfig.getAppSecret());
        OapiUserListsimpleRequest oapiUserListsimpleRequest = new OapiUserListsimpleRequest();
        oapiUserListsimpleRequest.setDeptId(1L);
        oapiUserListsimpleRequest.setCursor(0L);
        oapiUserListsimpleRequest.setSize(100L);
        oapiUserListsimpleRequest.setOrderField("entry_asc");
        DingTalkClient client = new DefaultDingTalkClient(UrlConstant.USER_LIST_SIMPLE);
        OapiUserListsimpleResponse execute = client.execute(oapiUserListsimpleRequest, accessToken);
        if (execute.getErrcode() != 0) {
            throw new ApiException(execute.getErrmsg());
        }
        List<OapiUserListsimpleResponse.ListUserSimpleResponse> list = execute.getResult().getList();
        return list;
    }

    public Object selectUuid(String uuid) {
        return hashMap.get(uuid);
    }

}
