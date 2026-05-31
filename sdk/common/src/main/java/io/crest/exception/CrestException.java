package io.crest.exception;

import io.crest.result.ResultCode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class CrestException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 8170873998824378304L;
    private int code;

    private String msg;

    public CrestException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    private CrestException(String message) {
        this(ResultCode.SYSTEM_INNER_ERROR.code(), message);
    }

    private CrestException(Throwable t) {
        super(t);
        this.code = ResultCode.SYSTEM_INNER_ERROR.code();
        this.msg = t.getMessage();
    }

    public static void throwException(String message) {
        throw new CrestException(message);
    }

    public static void throwException(int code, String message) {
        throw new CrestException(code, message);
    }

    public static CrestException getException(String message) {
        throw new CrestException(message);
    }

    public static void throwException(Throwable t) {
        throw new CrestException(t);
    }
}
