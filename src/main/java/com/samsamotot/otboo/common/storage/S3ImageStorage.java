package com.samsamotot.otboo.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3 버킷에서 이미지를 관리하기 위한 컴포넌트
 */
@Slf4j
@ConditionalOnProperty(name = "otboo.storage.type", havingValue = "s3")
@Component
public class S3ImageStorage {

    public String uploadImage(MultipartFile file, String folderPath) {
        // 최소 구현
        return null;
    }
}