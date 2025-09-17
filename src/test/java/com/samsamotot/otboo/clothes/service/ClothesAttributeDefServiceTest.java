package com.samsamotot.otboo.clothes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeOption;
import com.samsamotot.otboo.clothes.mapper.ClothesAttributeDefMapper;
import com.samsamotot.otboo.clothes.repository.ClothesAttributeDefRepository;
import com.samsamotot.otboo.clothes.service.impl.ClothesAttributeDefServiceImpl;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.clothes.definition.ClothesAttributeDefAlreadyExist;
import com.samsamotot.otboo.common.fixture.ClothesAttributeDefFixture;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
                .isInstanceOf(ClothesAttributeDefAlreadyExist.class)
                .hasMessage(ErrorCode.CLOTHES_ATTRIBUTE_DEF_ALREADY_EXISTS.getMessage());

            verify(defRepository, times(1)).existsByName(name);
            verify(defRepository, never()).save(any());
        }
    }
}