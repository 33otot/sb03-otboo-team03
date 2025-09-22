package com.samsamotot.otboo.clothes.service;
import static org.assertj.core.api.Assertions.assertThat;

import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.common.fixture.ClothesAttributeDefFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.user.entity.User;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

/**
 * 의상 기능 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@Transactional
class ClothesServiceTest {

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private ClothesMapper clothesMapper;

    @InjectMocks
    private ClothesServiceImpl clothesService;

    User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = UserFixture.createUser();
    }

    @Nested
    @DisplayName("의상 등록 서비스 테스트")
    class ClothesCreateServiceTest {

        @Test
        void 의상_이미지와_의상_속성_없이_의상_등록이_가능해야_한다() {
            // given
            UUID ownerId = mockUser.getId();

            ClothesCreateRequest request = new ClothesCreateRequest(
                ownerId,
                "부들부들 바지",
                ClothesType.BOTTOM,
                Collections.emptyList()
            );

            // when
            ClothesDto result = clothesService.create(request, Optional.empty());

            // then
            assertThat(result.getName()).isEqualTo("부들부들 바지");
            assertThat(result.getAttributes()).isEmpty();
        }

        @Test
        void 의상_속성과_함께_등록이_가능해야_한다() {
            // given
            UUID ownerId = mockUser.getId();

            // 속성 정의와 옵션 미리 준비
            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef();

            ClothesCreateRequest request = new ClothesCreateRequest(
                ownerId,
                "부들부들 티셔츠",
                ClothesType.TOP,
                List.of(
                    new ClothesAttributeDto(defEntity.getId(), defEntity.getOptions().get(0).getValue())
                )
            );

            // when
            ClothesDto result = clothesService.create(request, Optional.empty());

            // then
            assertThat(result.getAttributes()).hasSize(1);
            assertThat(result.getAttributes().get(0).getValue()).isEqualTo("봄");
        }

        @Test
        void 의상_이미지와_함께_의상_등록이_가능해야_한다() {
            // given
            UUID ownerId = mockUser.getId();

            ClothesCreateRequest request = new ClothesCreateRequest(
                ownerId,
                "부들부들 셔츠",
                ClothesType.TOP,
                Collections.emptyList()
            );

            // 가짜 이미지 파일
            MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "dummy-image".getBytes()
            );

            // when
            ClothesDto result = clothesService.create(request, Optional.of(imageFile));

            // then
            assertThat(result.getName()).isEqualTo("부들부들 셔츠");
            assertThat(result.getType()).isEqualTo(ClothesType.TOP);
            assertThat(result.getImageUrl()).isNotBlank();  // S3 업로드 후 URL이 들어갔는지 확인
            assertThat(result.getAttributes()).isEmpty();
        }
    }

}