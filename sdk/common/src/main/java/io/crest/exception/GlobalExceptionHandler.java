package io.crest.exception;


import io.crest.i18n.Translator;
import io.crest.result.ResultCode;
import io.crest.result.ResultMessage;
import io.crest.utils.LogUtil;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 统一处理控制器层抛出的业务异常和系统异常。
 */
@RestControllerAdvice
@SuppressWarnings("deprecation")
public class GlobalExceptionHandler {


    /**
     * 处理参数校验失败异常并返回本地化提示。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultMessage MethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        ObjectError objectError = e.getBindingResult().getAllErrors().get(0);
        String msg = objectError.getDefaultMessage();
        msg = Translator.get(msg);
        LogUtil.info(msg);
        return new ResultMessage(ResultCode.PARAM_IS_INVALID.code(), msg);
    }

    /**
     * 处理业务异常并保留业务错误码。
     */
    @ExceptionHandler(CrestException.class)
    public ResultMessage crestExceptionHandler(CrestException e) {
        LogUtil.info(e.getMessage());
        return new ResultMessage(e.getCode(), e.getMessage());
    }

    /**
     * 处理客户端主动断开连接的异常。
     */
    @ExceptionHandler(ClientAbortException.class)
    public void clientAbortExceptionHandler(ClientAbortException e) {
        LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
    }

    /**
     * 处理异步请求通道不可用的异常。
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void asyncRequestNotUsableExceptionHandler(AsyncRequestNotUsableException e) {
        LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
    }

    /**
     * 处理静态资源或接口资源不存在的异常。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResultMessage noResourceFoundExceptionHandler(NoResourceFoundException e) {
        String message = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
        LogUtil.debug(message);
        return new ResultMessage(ResultCode.RESOURCE_NOT_FOUND.code(), message);
    }

    /**
     * 处理请求方法不被当前接口支持的异常。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResultMessage methodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException e) {
        LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
        return new ResultMessage(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持");
    }

    /**
     * 处理上传文件超过限制的异常。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ResultMessage maxUploadSizeExceededExceptionHandler(MaxUploadSizeExceededException e) {
        LogUtil.info(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
        return new ResultMessage(ResultCode.PARAM_IS_INVALID.code(), "文件大小超过上传限制，请压缩文件或拆分后重新上传");
    }

    /**
     * 处理空指针异常中可识别的未登录场景。
     */
    @ExceptionHandler(NullPointerException.class)
    public ResultMessage noUserExceptionHandler(Exception e) {
        String message = e.getMessage();
        LogUtil.error("NullPointerException: " + message, e);
        if (Strings.CS.contains(message, "Cannot invoke \"io.crest.auth.bo.TokenUserBO.getUserId()\" because \"user\" is null")) {
            return new ResultMessage(ResultCode.USER_NOT_LOGGED_IN.code(), ResultCode.USER_NOT_LOGGED_IN.message());
        }
        // 不泄露内部错误信息
        return new ResultMessage(ResultCode.PARAM_IS_BLANK.code(), "参数错误");
    }

    /**
     * 处理未被更具体处理器捕获的系统异常。
     */
    @ExceptionHandler(Exception.class)
    public ResultMessage exceptionHandler(Exception e) {
        // 记录完整错误日志
        LogUtil.error("系统内部错误: " + e.getMessage(), e);
        // 返回通用错误消息，不泄露内部信息
        return new ResultMessage(ResultCode.SYSTEM_INNER_ERROR.code(), "系统内部错误，请联系管理员");
    }

}
