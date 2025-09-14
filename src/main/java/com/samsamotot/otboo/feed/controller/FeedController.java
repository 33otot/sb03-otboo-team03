package com.samsamotot.otboo.feed.controller;

import com.samsamotot.otboo.feed.controller.api.FeedApi;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.service.FeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds")
public class FeedController implements FeedApi {

    private final FeedService feedService;

    @Override
    @PostMapping
    public ResponseEntity<FeedDto> createFeed(
        @Valid @RequestBody FeedCreateRequest feedCreateRequest
    ) {
        log.info("[FeedController] 피드 등록 요청: {}", feedCreateRequest);
        FeedDto feedDto = feedService.create(feedCreateRequest);
        log.info("[FeedController] 피드 등록 완료: {}", feedDto);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(feedDto);
    }
}
