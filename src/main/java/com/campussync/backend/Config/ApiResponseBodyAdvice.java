package com.campussync.backend.config;

import com.campussync.backend.Dto.ApiResponse;
import com.campussync.backend.Controller.AuthController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ControllerAdvice(annotations = {RestController.class, Controller.class})
@RequiredArgsConstructor
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (AuthController.class.isAssignableFrom(returnType.getContainingClass())) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body == null) {
            return ApiResponse.success(null);
        }
        if (body instanceof ApiResponse<?> || body instanceof ProblemDetail || body instanceof Resource || body instanceof SseEmitter) {
            return body;
        }
        if (selectedContentType != null && (MediaType.APPLICATION_PDF.includes(selectedContentType)
                || MediaType.TEXT_EVENT_STREAM.includes(selectedContentType)
                || "text".equalsIgnoreCase(selectedContentType.getType()) && "csv".equalsIgnoreCase(selectedContentType.getSubtype()))) {
            return body;
        }
        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            try {
                return objectMapper.writeValueAsString(ApiResponse.success(body));
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Unable to serialize API response", ex);
            }
        }
        return ApiResponse.success(body);
    }
}
