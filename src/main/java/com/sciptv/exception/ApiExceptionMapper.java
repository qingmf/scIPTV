package com.sciptv.exception;

import com.sciptv.model.response.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        int status = resolveStatus(exception);
        String message = resolveMessage(exception);

        if (status >= 500) {
            log.error("Unhandled exception, status={}, message={}", status, message, exception);
        } else if (!(exception instanceof com.sciptv.exception.ApiException)) {
            log.warn("Request failed, status={}, message={}", status, message, exception);
        }

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(ErrorResponse.builder()
                        .status(status)
                        .message(message)
                        .build())
                .build();
    }

    private int resolveStatus(Throwable exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.getStatusCode();
        }
        if (exception instanceof WebApplicationException webApplicationException) {
            return webApplicationException.getResponse().getStatus();
        }
        if (exception instanceof IllegalArgumentException) {
            return 400;
        }
        return 500;
    }

    private String resolveMessage(Throwable exception) {
        if (exception instanceof NotFoundException) {
            return Status.NOT_FOUND.getReasonPhrase();
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "服务器内部错误" : message;
    }
}
