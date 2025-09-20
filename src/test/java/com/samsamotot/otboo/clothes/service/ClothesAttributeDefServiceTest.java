package com.samsamotot.otboo.clothes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefUpdateRequest;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeOption;
import com.samsamotot.otboo.clothes.mapper.ClothesAttributeDefMapper;
import com.samsamotot.otboo.clothes.repository.ClothesAttributeDefRepository;
import com.samsamotot.otboo.clothes.service.impl.ClothesAttributeDefServiceImpl;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.clothes.definition.ClothesAttributeDefAlreadyExistException;
import com.samsamotot.otboo.common.exception.clothes.definition.ClothesAttributeDefNotFoundException;
import com.samsamotot.otboo.common.fixture.ClothesAttributeDefFixture;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@Transactional
class ClothesAttributeDefServiceTest {

    @Mock
    private ClothesAttributeDefRepository defRepository;

    @Mock
    private ClothesAttributeDefMapper defMapper;

    @InjectMocks
    private ClothesAttributeDefServiceImpl clothesAttributeDefService;

    @Nested
    @DisplayName("의상 속성 정의 등록 서비스 테스트")
    class ClothesAttributeDefCreateServiceTest {

        @Test
        void 의상_속성_정의를_등록하면_ClothesAttributeDefDto를_반환한다() {
            // given
            String name = "계절";
            List<String> options = List.of("봄", "여름", "가을", "겨울");

            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef();

            ClothesAttributeDefCreateRequest request =
                new ClothesAttributeDefCreateRequest(name, options);

            when(defRepository.existsByName(request.name())).thenReturn(false);

            when(defRepository.save(any(ClothesAttributeDef.class)))
                .thenAnswer(invocation -> defEntity);

            when(defMapper.toDto(any(ClothesAttributeDef.class)))
                .thenAnswer(invocation -> {
                    return new ClothesAttributeDefDto(
                        UUID.randomUUID(),
                        defEntity.getName(),
                        defEntity.getOptions().stream().map(ClothesAttributeOption::getValue).toList(),
                        Instant.now()
                    );
                });

            // when
            ClothesAttributeDefDto result = clothesAttributeDefService.create(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("계절");
            assertThat(result.selectableValues()).hasSize(4);
        }

        @Test
        void 이미_존재하는_의상_속성_정의의_경우_예외가_발생한다() {
            // given
            String name = "계절";
            List<String> options = List.of("봄", "여름", "가을", "겨울");

            ClothesAttributeDefCreateRequest request =
                new ClothesAttributeDefCreateRequest(name, options);

            when(defRepository.existsByName(request.name())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> clothesAttributeDefService.create(request))
                .isInstanceOf(ClothesAttributeDefAlreadyExistException.class)
                .hasMessage(ErrorCode.CLOTHES_ATTRIBUTE_DEF_ALREADY_EXISTS.getMessage());

            verify(defRepository, times(1)).existsByName(name);
            verify(defRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("의상 속성 정의 수정 서비스 테스트")
    class ClothesAttributeDefUpdateServiceTest {
        @Test
        void 의상_속성_정의를_수정하면_dto를_반환한다() {
            // given
            // 수정할 정보
            String newName = "계절";
            List<String> newOptions = List.of("봄", "여름", "겨울");

            // 수정될 객체
            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef();

            ClothesAttributeDefUpdateRequest request =
                new ClothesAttributeDefUpdateRequest(newName, newOptions);

            when(defRepository.findById(defEntity.getId())).thenReturn(Optional.of(defEntity));

            // 변경 후 dto
            ClothesAttributeDefDto expectedDto = new ClothesAttributeDefDto(
                defEntity.getId(),
                newName,
                newOptions,
                defEntity.getCreatedAt()
            );
            when(defMapper.toDto(any(ClothesAttributeDef.class))).thenReturn(expectedDto);

            // when
            ClothesAttributeDefDto result = clothesAttributeDefService.update(defEntity.getId(), request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(newName);
            assertThat(result.selectableValues()).containsExactlyInAnyOrderElementsOf(newOptions);

            verify(defRepository, times(1)).findById(defEntity.getId());
            verify(defMapper, times(1)).toDto(defEntity);
        }

        @Test
        void id로_조회되는_ClothesAttributeDef가_없다면_예외가_발생해야_한다() {
            UUID testId = UUID.randomUUID();

            String newName = "계절";
            List<String> newOptions = List.of("봄", "여름", "겨울");

            ClothesAttributeDefUpdateRequest request =
                new ClothesAttributeDefUpdateRequest(newName, newOptions);

            // Optional 반환 시
            when(defRepository.findById(testId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clothesAttributeDefService.update(testId, request))
                .isInstanceOf(ClothesAttributeDefNotFoundException.class)
                .hasMessageContaining(ErrorCode.CLOTHES_ATTRIBUTE_DEF_NOT_FOUND.getMessage());

            verify(defRepository, times(1)).findById(testId);
            verifyNoInteractions(defMapper);
        }

        @Test
        void 입력된_이름이_새로운_값이면_업데이트된다(){
            // given
            // 수정할 정보
            String newName = "계절 구분";
            List<String> options  = List.of("봄", "여름", "가을", "겨울");

            // 수정될 객체
            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef();

            ClothesAttributeDefUpdateRequest request =
                new ClothesAttributeDefUpdateRequest(newName, options);

            when(defRepository.findById(defEntity.getId())).thenReturn(Optional.of(defEntity));

            // 변경 후 dto
            ClothesAttributeDefDto expectedDto = new ClothesAttributeDefDto(
                defEntity.getId(),
                newName,
                options,
                defEntity.getCreatedAt()
            );
            when(defMapper.toDto(any(ClothesAttributeDef.class))).thenReturn(expectedDto);

            // when
            ClothesAttributeDefDto result = clothesAttributeDefService.update(defEntity.getId(), request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(newName);

            verify(defRepository, times(1)).findById(defEntity.getId());
            verify(defMapper, times(1)).toDto(defEntity);
        }

        @Test
        void 이름이_null_이거나_blank라면_변경되지_않는다() {
            // given
            // 수정할 정보
            String newName = "";
            List<String> options  = List.of("봄", "여름", "가을", "겨울");

            // 수정될 객체
            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef();

            ClothesAttributeDefUpdateRequest request =
                new ClothesAttributeDefUpdateRequest(newName, options);

            when(defRepository.findById(defEntity.getId())).thenReturn(Optional.of(defEntity));

            // 변경 후 dto
            ClothesAttributeDefDto expectedDto = new ClothesAttributeDefDto(
                defEntity.getId(),
                defEntity.getName(),
                options,
                defEntity.getCreatedAt()
            );
            when(defMapper.toDto(any(ClothesAttributeDef.class))).thenReturn(expectedDto);

            // when
            ClothesAttributeDefDto result = clothesAttributeDefService.update(defEntity.getId(), request);

            // then
            assertThat(result.name()).isEqualTo(defEntity.getName()); // 이름이 그대로인지?
        }

        @Test
        void 옵션이_null이면_업데이트되지_않는다() {
            // given
            // 수정할 정보
            String newName = "계절 속성";

            // 수정될 객체
            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef();
            List<String> defOptions = defEntity.getOptions().stream()
                .map(ClothesAttributeOption::getValue)
                .toList();

            ClothesAttributeDefUpdateRequest request =
                new ClothesAttributeDefUpdateRequest(newName, null);

            when(defRepository.findById(defEntity.getId())).thenReturn(Optional.of(defEntity));

            // 변경 후 dto
            ClothesAttributeDefDto expectedDto = new ClothesAttributeDefDto(
                defEntity.getId(),
                newName,
                defOptions,
                defEntity.getCreatedAt()
            );
            when(defMapper.toDto(any(ClothesAttributeDef.class))).thenReturn(expectedDto);

            // when
            ClothesAttributeDefDto result = clothesAttributeDefService.update(defEntity.getId(), request);

            // then
            assertThat(result.selectableValues()).containsExactlyInAnyOrderElementsOf(defOptions);
        }
    }

    @Nested
    @DisplayName("의상 속성 정의 삭제 서비스 테스트")
    class ClothesAttributeDefDeleteServiceTest {
        @Test
        void 관리자는_의상_속성_정의를_삭제할_수_있다() {
            // given
            UUID defId = UUID.randomUUID();

            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef();
            when(defRepository.findById(defId)).thenReturn(Optional.of(defEntity));

            // when
            clothesAttributeDefService.delete(defId);

            // then
            verify(defRepository).delete(defEntity);
        }

        @Test
        void 해당하는_의상_속성_정의가_없다면_예외를_발생시킨다() {
            // given
            UUID defId = UUID.randomUUID();

            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef();
            when(defRepository.findById(defId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clothesAttributeDefService.delete(defId))
                .isInstanceOf(ClothesAttributeDefNotFoundException.class)
                .hasMessageContaining(ErrorCode.CLOTHES_ATTRIBUTE_DEF_NOT_FOUND.getMessage());

            verify(defRepository, times(1)).findById(defId);
        }
    }

    @Nested
    @DisplayName("의상 속성 정의 조회 서비스 테스트")
    class ClothesAttributeDefFindServiceTest {
        @Test
        void 정렬_조건에_맞는_결과를_반환한다() {
            // given
            String sortBy = "createdAt";
            String sortDirection = "ASCENDING";

            ClothesAttributeDef def1 = ClothesAttributeDef.createClothesAttributeDef("상의", List.of("반팔", "긴팔"));
            ReflectionTestUtils.setField(def1, "createdAt", Instant.parse("2023-01-01T00:00:00Z"));

            ClothesAttributeDef def2 = ClothesAttributeDef.createClothesAttributeDef("하의", List.of("반바지", "긴바지"));
            ReflectionTestUtils.setField(def2, "createdAt", Instant.parse("2023-02-03T00:01:00Z"));

            ClothesAttributeDef def3 = ClothesAttributeDef.createClothesAttributeDef("모자", List.of("캡모자", "베레모", "중절모"));
            ReflectionTestUtils.setField(def3, "createdAt", Instant.parse("2023-02-03T00:05:01Z"));

            List<ClothesAttributeDef> mockResult = List.of(def1, def2, def3);

            // Repository가 정렬된 결과를 반환한다고 가정
            when(defRepository.findAll(any(Sort.class))).thenReturn(mockResult);

            // Mapper가 mock이므로 stub 해줘야 함
            when(defMapper.toDto(any(ClothesAttributeDef.class)))
                .thenAnswer(invocation -> {
                    ClothesAttributeDef entity = invocation.getArgument(0);
                    return ClothesAttributeDefDto.builder()
                        .id(entity.getId())
                        .name(entity.getName())
                        .selectableValues(entity.getOptions().stream().map(ClothesAttributeOption::getValue).toList())
                        .createdAt(entity.getCreatedAt())
                        .build();
                });

            // when
            List<ClothesAttributeDefDto> results =
                clothesAttributeDefService.findAll(sortBy, sortDirection, null);

            // isSorted() 검증
            assertThat(results)
                .extracting(ClothesAttributeDefDto::createdAt)
                .isSorted();
        }
    }
}