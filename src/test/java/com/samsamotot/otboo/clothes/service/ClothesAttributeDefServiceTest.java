package com.samsamotot.otboo.clothes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeOption;
import com.samsamotot.otboo.clothes.mapper.ClothesAttributeDefMapper;
import com.samsamotot.otboo.clothes.repository.ClothesAttributeDefRepository;
import com.samsamotot.otboo.clothes.service.impl.ClothesAttributeDefServiceImpl;
import com.samsamotot.otboo.common.fixture.ClothesAttributeDefFixture;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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

}