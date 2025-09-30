package com.samsamotot.otboo.clothes.service.impl;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefUpdateRequest;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeOption;
import com.samsamotot.otboo.clothes.exception.definition.ClothesAttributeDefAlreadyExistException;
import com.samsamotot.otboo.clothes.exception.definition.ClothesAttributeDefNotFoundException;
import com.samsamotot.otboo.clothes.mapper.ClothesAttributeDefMapper;
import com.samsamotot.otboo.clothes.repository.ClothesAttributeDefRepository;
import com.samsamotot.otboo.clothes.service.ClothesAttributeDefService;
import com.samsamotot.otboo.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 의상 속성 정의의 비즈니스 로직을 담당하는 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAttributeDefServiceImpl implements ClothesAttributeDefService {

    private final ClothesAttributeDefRepository defRepository;
    private final ClothesAttributeDefMapper defMapper;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request) {

        // 이미 존재하는 정의인지 확인
        if (defRepository.existsByName(request.name())) {
            throw new ClothesAttributeDefAlreadyExistException(ErrorCode.CLOTHES_ATTRIBUTE_DEF_ALREADY_EXISTS);
        }

        // 엔티티 생성
        ClothesAttributeDef def = ClothesAttributeDef.createClothesAttributeDef(request.name());
        log.debug("[ClothesAttributeDefServiceImpl] create - 생성된 엔티티: {}", def);

        // 옵션이랑 연관관계
        request.selectableValues().forEach(value -> {
            ClothesAttributeOption option = ClothesAttributeOption.createClothesAttributeOption(def, value);
            def.addOption(option);
        });

        ClothesAttributeDef saved = defRepository.save(def);

        return defMapper.toDto(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public ClothesAttributeDefDto update(UUID defId, ClothesAttributeDefUpdateRequest request) {
        String newName = request.name();
        List<String> newOptions = request.selectableValues();

        ClothesAttributeDef def = defRepository.findById(defId)
            .orElseThrow(() -> new ClothesAttributeDefNotFoundException(ErrorCode.CLOTHES_ATTRIBUTE_DEF_NOT_FOUND));

        // 이름 업데이트
        if (newName != null && !newName.isBlank()) {
            if (!newName.equals(def.getName())) {
                def.updateName(newName);
            }
            else{
                log.info("[ClothesAttributeDefServiceImpl] update - 기존의 정의 이름과 동일합니다. 이름: {}", newName);
            }
        }
        // 옵션 업데이트
        if (newOptions != null) {
            Set<String> currentValues = def.getOptions().stream()
                .map(ClothesAttributeOption::getValue)
                .collect(Collectors.toSet());

            Set<String> newValueSet = new HashSet<>(newOptions);

            // 수정될 옵션값 요청에 없는 기존 옵션을 제거함
            List<ClothesAttributeOption> toRemove = new ArrayList<>();

            for (ClothesAttributeOption option : def.getOptions()) {
                if (!newValueSet.contains(option.getValue())) {
                    option.updateDefinition(null); // 옵션의 연관관계 끊기
                    toRemove.add(option);
                }
            }
            def.getOptions().removeAll(toRemove);

            // 기존 옵션에 없는 새로운 값이 수정될 옵션값에 있다면 추가함
            for (String value : newValueSet) {
                if (!currentValues.contains(value)) {
                    ClothesAttributeOption option = ClothesAttributeOption.createClothesAttributeOption(def, value);
                    def.addOption(option);
                }
            }
        }
        ClothesAttributeDefDto result = defMapper.toDto(def);
        log.debug("[ClothesAttributeDefServiceImpl] update - 수정된 dto: {}", result);
        return result;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public void delete(UUID defId) {
        ClothesAttributeDef def = defRepository.findById(defId)
                .orElseThrow(() -> new ClothesAttributeDefNotFoundException(ErrorCode.CLOTHES_ATTRIBUTE_DEF_NOT_FOUND));

        log.info("[ClothesAttributeDefServiceImpl] delete - 삭제될 속성 정의 이름: {}", def.getName());

        defRepository.delete(def);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ClothesAttributeDefDto> findAll(String sortBy, String sortDirection,
        String keywordLike) {

        log.info("[ClothesAttributeDefServiceImpl] findAll - 호출됨");
        // Sort 객체
        Direction direction = sortDirection.equalsIgnoreCase("ASCENDING") ? Direction.ASC : Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        List<ClothesAttributeDef> sorted;

        // 검색어 조건이 없는 경우
        if (keywordLike == null || keywordLike.isBlank()) {
            sorted = defRepository.findAll(sort);
        }
        else {
            sorted = defRepository.findByNameContaining(keywordLike, sort);
        }

        log.info("[ClothesAttributeDefServiceImpl] findAll - 반환된 리스트 크기: {}", sorted.size());

        return sorted.stream()
            .map(defMapper::toDto)
            .toList();
    }
}
