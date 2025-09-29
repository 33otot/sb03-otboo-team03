package com.samsamotot.otboo.clothes.service.impl;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDto;
import com.samsamotot.otboo.clothes.dto.ClothesAttributeWithDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesSearchRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesUpdateRequest;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesAttribute;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeOption;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.exception.ClothesNotFoundException;
import com.samsamotot.otboo.clothes.exception.ClothesOwnerMismatchException;
import com.samsamotot.otboo.clothes.exception.definition.ClothesAttributeDefNotFoundException;
import com.samsamotot.otboo.clothes.mapper.ClothesMapper;
import com.samsamotot.otboo.clothes.repository.ClothesAttributeDefRepository;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.clothes.service.ClothesService;
import com.samsamotot.otboo.clothes.util.ClothesServiceHelper;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.storage.S3ImageStorage;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.exception.UserNotFoundException;
import com.samsamotot.otboo.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 의상 관련 비즈니스 로직을 담당하는 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesServiceImpl implements ClothesService {
    private static final String SERVICE_NAME = "[ClothesServiceImpl] ";

    private final ClothesRepository clothesRepository;
    private final ClothesAttributeDefRepository defRepository;
    private final UserRepository userRepository;
    private final S3ImageStorage s3ImageStorage;
    private final ClothesMapper clothesMapper;
    private final ClothesServiceHelper clothesServiceHelper;

    // 이미지 없는 생성
    @Override
    @Transactional
    public ClothesDto create(UUID ownerId, ClothesCreateRequest request) {
        log.info(SERVICE_NAME + "create - 이미지 없는 의상 생성 호출됨");

        List<ClothesAttributeWithDefDto> attributes = new ArrayList<>();

        // 유저 조회
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new UserNotFoundException());

        // Clothes 객체 생성
        Clothes clothes = Clothes.createClothes(
            request.name(),
            request.type(),
            owner
        );

        // Attributes 처리
        process(request.attributes(), clothes, attributes);

        Clothes saved = clothesRepository.save(clothes);

        log.info(SERVICE_NAME + "ClothesDto 생성중");
        // DTO 반환
        return new ClothesDto(
            saved.getId(),
            ownerId,
            request.name(),
            null,
            request.type(),
            attributes
        );
    }

    // 이미지 있는 생성
    @Override
    @Transactional
    public ClothesDto create(UUID ownerId, ClothesCreateRequest request, MultipartFile clothesImage) {
        log.info(SERVICE_NAME + "create - 이미지 있는 의상 생성 호출됨");

        List<ClothesAttributeWithDefDto> attributes = new ArrayList<>();

        // 유저 조회
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new UserNotFoundException());

        // Clothes 객체 생성
        Clothes clothes = Clothes.createClothes(
            request.name(),
            request.type(),
            owner
        );

        // Attributes 처리
        process(request.attributes(), clothes, attributes);

        // imageUrl 처리
        String imageUrl;
        log.debug(SERVICE_NAME + "S3에 의상 이미지 업로드 요청: {}", clothesImage.getOriginalFilename());
        imageUrl = s3ImageStorage.uploadImage(clothesImage, "clothes/");

        clothes.updateImageUrl(imageUrl);
        Clothes saved = clothesRepository.save(clothes);

        log.info(SERVICE_NAME + "ClothesDto 생성중");
        // DTO 반환
        return new ClothesDto(
            saved.getId(),
            ownerId,
            request.name(),
            imageUrl,
            request.type(),
            attributes
        );
    }

    // 이미지 없는 수정
    @Override
    @Transactional
    public ClothesDto update(UUID clothesId, UUID ownerId, ClothesUpdateRequest updateRequest) {
        log.info(SERVICE_NAME + "update - 이미지 없는 의상 수정 호출됨");

        String newName = updateRequest.name();
        ClothesType newType = updateRequest.type();

        // Clothes 조회
        Clothes clothes = clothesRepository.findById(clothesId)
            .orElseThrow(() -> new ClothesNotFoundException());

        // 해당 clothes 객체가 유저의 의상이 맞는지 더블체크
        if (!clothes.getOwner().getId().equals(ownerId)) {
            throw new ClothesOwnerMismatchException();
        }

        // 의상 이름 업데이트
        if (!newName.equals(clothes.getName())) {
            clothes.updateName(newName);
        }

        // 의상 타입 업데이트
        if (!newType.equals(clothes.getType())) {
            clothes.updateType(newType);
        }

        // 현재의 attributes 맵핑
        Map<UUID, ClothesAttribute> currentAttrs = currentAttrsMapping(clothes);

        // 속성 업데이트 - 기존에 있는 def면 옵션을 수정, 없으면 새로 추가 (이미 선택된 정의를 제거할 수는 없음)
        attributesUpdate(updateRequest.attributes(), currentAttrs, clothes);

        Clothes saved = clothesRepository.save(clothes);

        return clothesMapper.toClothesDto(saved);
    }

    // 이미지 있는 수정
    @Override
    @Transactional
    public ClothesDto update(UUID clothesId, UUID ownerId, ClothesUpdateRequest updateRequest, MultipartFile clothesImage) {
        log.info(SERVICE_NAME + "update - 이미지 있는 의상 수정 호출됨");

        String newName = updateRequest.name();
        ClothesType newType = updateRequest.type();

        // Clothes 조회
        Clothes clothes = clothesRepository.findById(clothesId)
            .orElseThrow(() -> new ClothesNotFoundException());

        // 해당 clothes 객체가 유저의 의상이 맞는지 더블체크
        if (!clothes.getOwner().getId().equals(ownerId)) {
            throw new ClothesOwnerMismatchException();
        }

        // 의상 이름 업데이트
        if (!newName.equals(clothes.getName())) {
            clothes.updateName(newName);
        }

        // 의상 타입 업데이트
        if (!newType.equals(clothes.getType())) {
            clothes.updateType(newType);
        }

        // 현재의 attributes 맵핑
        Map<UUID, ClothesAttribute> currentAttrs = currentAttrsMapping(clothes);

        // 속성 업데이트 - 기존에 있는 def면 옵션을 수정, 없으면 새로 추가 (이미 선택된 정의를 제거할 수는 없음)
        attributesUpdate(updateRequest.attributes(), currentAttrs, clothes);
        clothesRepository.save(clothes);

        // 기존 이미지 경로 보관
        String previousImageUrl = clothes.getImageUrl();

        // 요청 들어온 새 이미지 업로드
        log.debug(SERVICE_NAME + "S3에 의상 이미지 업로드 요청: {}", clothesImage.getOriginalFilename());
        String imageUrl = s3ImageStorage.uploadImage(clothesImage, "clothes/");

        // 엔티티 반영 및 저장
        clothes.updateImageUrl(imageUrl);
        Clothes saved = clothesRepository.save(clothes);

        // 기존 이미지 삭제 (실패해도 흐름 지속)
        if (previousImageUrl != null) {
            try {
                s3ImageStorage.deleteImage(previousImageUrl);
            } catch (Exception e) {
                log.warn(SERVICE_NAME + "이전 이미지 삭제 실패 - url: {}, err: {}", previousImageUrl, e.getMessage(), e);
            }
        }

        return clothesMapper.toClothesDto(saved);
    }

    // 삭제 기능
    @Override
    @Transactional
    public void delete(UUID ownerId, UUID clothesId) {
        log.info(SERVICE_NAME + "delete - 의상 삭제 메서드 호출됨");

        // Clothes 조회
        Clothes clothes = clothesRepository.findById(clothesId)
            .orElseThrow(() -> new ClothesNotFoundException());

        // 해당 clothes 객체가 유저의 의상이 맞는지 더블체크
        if (!clothes.getOwner().getId().equals(ownerId)) {
            throw new ClothesOwnerMismatchException();
        }

        // 기존 이미지 경로 보관
        String previousImageUrl = clothes.getImageUrl();

        log.info(SERVICE_NAME + "delete - 삭제될 의상 이름: {}", clothes.getName());

        // 의상 삭제
        clothesRepository.delete(clothes);

        // 의상 이미지 삭제
        if (previousImageUrl != null) {
            try {
                s3ImageStorage.deleteImage(previousImageUrl);
            } catch (Exception e) {
                log.warn(SERVICE_NAME + "의상 이미지 삭제 실패 - url: {}, err: {}", previousImageUrl, e.getMessage(), e);
            }
        }
    }

    // 의상 목록 조회
    @Override
    @Transactional(readOnly = true)
    public CursorResponse<ClothesDto> find(ClothesSearchRequest request) {
        log.info(SERVICE_NAME + "find - 의상 목록 조회 메서드 호출됨");

        // Pageable 생성
        log.info(SERVICE_NAME + "find - Pageable 생성 중");
        Pageable pageable = PageRequest.of(0, request.limit());

        // Slice 메서드 호출
        log.info(SERVICE_NAME + "find - slice 생성을 위해 clothesRepository.findClothesWithCursor 호출");
        Slice<Clothes> slice = clothesRepository.findClothesWithCursor(request, pageable);

        // Dto 변환
        List<ClothesDto> returnDto = slice.getContent().stream()
            .map(clothesMapper::toClothesDto)
            .toList();

        log.info(SERVICE_NAME + "find - 의상 목록 조회 완료");

        // totalElement 값
        long totalElement = clothesRepository.totalElementCount(request);

        // 다음 커서 생성
        String nextCursor = slice.hasNext() ? clothesServiceHelper.generateCursor(slice.getContent()) : null;

        // idAfter 생성
        UUID idAfter = slice.hasNext() ? slice.getContent().get(slice.getContent().size() - 1).getId() : null;

        log.info(SERVICE_NAME + "의상 목록 조회 커서 생성 완료");

        // 반환값 생성
        return new CursorResponse<>(
            returnDto,
            nextCursor,
            idAfter,
            slice.hasNext(),
            totalElement,
            "createdAt",
            SortDirection.DESCENDING
        );
    }

    // ===== 공통 로직 메서드 ===== //

    /**
     *
     * 1. 요청받은 dto에서 선택한 속성 값으로 def 객체를 조회하고 attribute 객체와 연관관계를 세팅함
     * 2. ClothesDto에 사용할 ClothesAttributeWithDefDto을 생성하고 List에 추가함
     */
    void process(List<ClothesAttributeDto> attributes, Clothes clothes, List<ClothesAttributeWithDefDto> clothesAttributeWithDefDtoList) {
        // Attributes 처리
        if (attributes != null) {
            for (ClothesAttributeDto dto : attributes) {
                log.debug(SERVICE_NAME + "Attribute 처리와 ClothesAttributeWithDefDto 생성을 위한 반복문");
                log.debug(SERVICE_NAME + "{} 에서 현재 위치: {}", attributes, dto);
                // def 객체 조회
                ClothesAttributeDef definition = defRepository.findById(dto.definitionId())
                    .orElseThrow(() -> new ClothesAttributeDefNotFoundException());

                attributeValid(definition, dto.value());

                ClothesAttribute attribute = ClothesAttribute.createClothesAttribute(definition, dto.value());

                // 연관관계 세팅
                clothes.addAttribute(attribute);

                // ClothesAttributeWithDefDto 생성
                ClothesAttributeWithDefDto withDefDto = new ClothesAttributeWithDefDto(
                    dto.definitionId(),
                    definition.getName(),
                    definition.getOptions().stream()
                        .map(ClothesAttributeOption::getValue)
                        .toList(),
                    dto.value()
                );
                clothesAttributeWithDefDtoList.add(withDefDto);
            }
        }
    }

    /**
     *
     * 들어온 옵션이 정의에 존재하는지 확인하는 메서드
     */
    void attributeValid(ClothesAttributeDef definition, String value) {
        boolean valid = definition.getOptions().stream()
            .map(ClothesAttributeOption::getValue)
            .anyMatch(v -> v.equals(value));
        if (!valid) {
            log.warn(SERVICE_NAME + "정의에 없는 속성 값: defId: {}, value: {}", definition.getId(), value);
            throw new IllegalArgumentException("정의된 옵션에 없는 속성 값입니다.");
        }
    }

    /**
     *  현재의 attributes 맵핑하는 메서드
     */
    Map<UUID, ClothesAttribute> currentAttrsMapping(Clothes clothes) {
        return clothes.getAttributes().stream()
            .collect(Collectors.toMap(
                clothesAttribute -> clothesAttribute.getDefinition().getId(),
                clothesAttribute -> clothesAttribute)
            );
    }

    /**
     * 속성 Patch 방식으로 업데이트
     */
    void attributesUpdate(List<ClothesAttributeDto> attributes, Map<UUID, ClothesAttribute> currentAttrs, Clothes clothes) {
        if (attributes != null) {
            log.info(SERVICE_NAME + "속성 업데이트 시작");
            for (ClothesAttributeDto dto : attributes) {
                // 이미 존재하는 속성 뽑기
                ClothesAttribute existing = currentAttrs.get(dto.definitionId());

                // 이미 존재하는 속성이면 -> 옵션 값 수정
                if (existing != null) {
                    log.info(SERVICE_NAME + "기존에 선택된 속성입니다. 옵션값을 수정합니다.");
                    if (!existing.getValue().equals(dto.value())) {
                        attributeValid(existing.getDefinition(), dto.value());

                        existing.updateValue(dto.value());
                    }
                }
                // 새로운 속성이면 -> 추가
                else {
                    log.info(SERVICE_NAME + "새로 추가되는 속성을 생성합니다.");
                    ClothesAttributeDef definition = defRepository.findById(dto.definitionId())
                        .orElseThrow(() -> new ClothesAttributeDefNotFoundException());

                    attributeValid(definition, dto.value());

                    ClothesAttribute newAttr = ClothesAttribute.createClothesAttribute(definition, dto.value());
                    clothes.addAttribute(newAttr);
                }
            }
        }
    }

}
