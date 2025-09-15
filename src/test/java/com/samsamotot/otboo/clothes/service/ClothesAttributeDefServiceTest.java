package com.samsamotot.otboo.clothes.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.repository.ClothesAttributeDefRepository;
import com.samsamotot.otboo.clothes.repository.ClothesAttributeOptionRepository;
import com.samsamotot.otboo.clothes.service.impl.ClothesAttributeDefServiceImpl;
import java.util.List;
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
    private ClothesAttributeOptionRepository optionRepository;

    @InjectMocks
    private ClothesAttributeDefServiceImpl clothesAttributeDefService;

    @Test
    void 의상_속성_정의를_등록하면_ClothesAttributeDefDto를_반환한다() {
        // given
        String name = "계절";
        List<String> options = List.of("봄", "여름", "가을", "겨울");

        ClothesAttributeDefCreateRequest request =
            new ClothesAttributeDefCreateRequest(name, options);

        // when
        ClothesAttributeDefDto result = clothesAttributeDefService.create(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("계절");
        assertThat(result.selectableValues()).hasSize(4);
    }

}