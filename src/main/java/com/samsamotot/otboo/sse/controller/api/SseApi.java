package com.samsamotot.otboo.sse.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * PackageName  : com.samsamotot.otboo.sse.controller.api
 * FileName     : SseApi
 * Author       : dounguk
 * Date         : 2025. 9. 29.
 */


@Tag(name = "sse-controller")
@RequestMapping("/api")
public interface SseApi {

    @Operation(
        parameters = {
            @Parameter(
                name = "Authorization",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "OK",
                content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)
            )
        }
    )
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(HttpServletRequest request) throws Exception;
}
