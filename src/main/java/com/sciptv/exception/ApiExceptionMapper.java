package com.sciptv.exception;

import com.sciptv.model.response.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        int status = resolveStatus(exception);
        String message = resolveMessage(exception);

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
        if (exception instanceof IllegalArgumentException) {
            return 400;
        }
        return 500;
    }

    private String resolveMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "服务器内部错误" : message;
    }
}
