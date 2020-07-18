package gelato.riso.bossapi.support.exception;

import org.springframework.http.HttpStatus;

public abstract class BaseException extends Exception {
    private static final long serialVersionUID = -5528407500439117339L;

    public abstract HttpStatus getHttpStatus();
}
