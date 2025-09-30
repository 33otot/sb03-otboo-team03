package com.samsamotot.otboo.clothes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.samsamotot.otboo.clothes.mapper.ClothesMapper;
import com.samsamotot.otboo.clothes.repository.ClothesAttributeDefRepository;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.clothes.service.impl.ClothesServiceImpl;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.fixture.ClothesAttributeDefFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.storage.S3ImageStorage;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
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
    private UserRepository userRepository;

    @Mock
    private ClothesAttributeDefRepository defRepository;

    @Mock
    private S3ImageStorage s3ImageStorage;

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
                UUID.randomUUID(),
                "부들부들 바지",
                ClothesType.BOTTOM,
                Collections.emptyList()
            );

            Clothes savedClothes = Clothes.createClothes(request.name(), request.type(), mockUser);

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(mockUser));
            when(clothesRepository.save(any(Clothes.class))).thenReturn(savedClothes);

            // when
            ClothesDto result = clothesService.create(ownerId, request);

            // then
            assertThat(result.name()).isEqualTo("부들부들 바지");
            assertThat(result.attributes()).isEmpty();
        }

        @Test
        void 의상_속성과_함께_등록이_가능해야_한다() {
            // given
            UUID ownerId = mockUser.getId();

            // 속성 정의와 옵션 미리 준비
            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef();
            ClothesAttributeDto attrDto = new ClothesAttributeDto(defEntity.getId(), "봄");
            ClothesCreateRequest request = new ClothesCreateRequest(
                UUID.randomUUID(),
                "부들부들 티셔츠",
                ClothesType.TOP,
                List.of(attrDto)
            );

            Clothes savedClothes = Clothes.createClothes(request.name(), request.type(), mockUser);

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(mockUser));
            when(defRepository.findById(defEntity.getId())).thenReturn(Optional.of(defEntity));
            when(clothesRepository.save(any(Clothes.class))).thenReturn(savedClothes);

            // when
            ClothesDto result = clothesService.create(ownerId, request);

            // then
            assertThat(result.attributes()).hasSize(1);
            assertThat(result.name()).isEqualTo("부들부들 티셔츠");
            assertThat(result.attributes().get(0).definitionName()).isEqualTo("계절");
            assertThat(result.attributes().get(0).value()).isEqualTo("봄");
        }

        @Test
        void 의상_이미지와_함께_의상_등록이_가능해야_한다() {
            // given
            UUID ownerId = mockUser.getId();

            ClothesCreateRequest request = new ClothesCreateRequest(
                UUID.randomUUID(),
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
            String mockImageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/clothes/test.jpg";

            Clothes savedClothes = Clothes.createClothes(request.name(), request.type(), mockUser);

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(mockUser));
            when(clothesRepository.save(any(Clothes.class))).thenReturn(savedClothes);
            when(s3ImageStorage.uploadImage(imageFile, "clothes/")).thenReturn(mockImageUrl);

            // when
            ClothesDto result = clothesService.create(ownerId, request, imageFile);

            // then
            assertThat(result.name()).isEqualTo("부들부들 셔츠");
            assertThat(result.type()).isEqualTo(ClothesType.TOP);
            assertThat(result.imageUrl()).isNotBlank();  // S3 업로드 후 URL이 들어갔는지 확인
            assertThat(result.attributes()).isEmpty();
        }

        @Test
        void 의상_이미지와_속성으로_의상_등록이_가능해야_한다() {
            // given
            UUID ownerId = mockUser.getId();

            // 속성 정의와 옵션 미리 준비
            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef();
            ClothesAttributeDto attrDto = new ClothesAttributeDto(defEntity.getId(), "봄");

            ClothesCreateRequest request = new ClothesCreateRequest(
                UUID.randomUUID(),
                "부들부들 티셔츠",
                ClothesType.TOP,
                List.of(attrDto)
            );

            // 가짜 이미지 파일
            MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "dummy-image".getBytes()
            );
            String mockImageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/clothes/test.jpg";

            Clothes savedClothes = Clothes.createClothes(request.name(), request.type(), mockUser);

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(mockUser));
            when(defRepository.findById(defEntity.getId())).thenReturn(Optional.of(defEntity));
            when(clothesRepository.save(any(Clothes.class))).thenReturn(savedClothes);

            when(s3ImageStorage.uploadImage(imageFile, "clothes/")).thenReturn(mockImageUrl);

            // when
            ClothesDto result = clothesService.create(ownerId, request, imageFile);

            // then
            assertThat(result.name()).isEqualTo("부들부들 티셔츠");
            assertThat(result.type()).isEqualTo(ClothesType.TOP);
            assertThat(result.imageUrl()).isNotBlank();  // S3 업로드 후 URL이 들어갔는지 확인
            assertThat(result.attributes().get(0).definitionName()).isEqualTo("계절");
            assertThat(result.attributes().get(0).value()).isEqualTo("봄");
        }
    }

    @Nested
    @DisplayName("의상 수정 서비스 테스트")
    class ClothesUpdateServiceTest {

        @BeforeEach
        void setUpMapper() {
            // 수정 테스트에서만 mapper 스텁 필요
            lenient().when(clothesMapper.toClothesDto(any(Clothes.class)))
                .thenAnswer(invocation -> {
                    Clothes c = invocation.getArgument(0);
                    return new ClothesDto(
                        c.getId(),
                        c.getOwner().getId(),
                        c.getName(),
                        c.getImageUrl(),
                        c.getType(),
                        c.getAttributes().stream()
                            .map(attr -> new ClothesAttributeWithDefDto(
                                attr.getDefinition().getId(),
                                attr.getDefinition().getName(),
                                attr.getDefinition().getOptions().stream()
                                    .map(ClothesAttributeOption::getValue)
                                    .toList(),
                                attr.getValue()
                            ))
                            .toList()
                    );
                });
        }

        @Test
        void 의상_이름과_타입을_수정할_수_있어야_한다() {
            // given
            UUID ownerId = UUID.randomUUID();
            ReflectionTestUtils.setField(mockUser, "id", ownerId);

            Clothes clothes = Clothes.createClothes("옛날 셔츠", ClothesType.BOTTOM, mockUser);

            ClothesUpdateRequest updateRequest = new ClothesUpdateRequest(
                "신상 셔츠",
                ClothesType.TOP,
                Collections.emptyList()
            );

            when(clothesRepository.findById(clothes.getId())).thenReturn(Optional.of(clothes));
            when(clothesRepository.save(any(Clothes.class))).thenReturn(clothes);

            // when
            ClothesDto result = clothesService.update(clothes.getId(), ownerId, updateRequest);

            // then
            assertThat(result.name()).isEqualTo("신상 셔츠");
            assertThat(result.type()).isEqualTo(ClothesType.TOP);
        }

        @Test
        void 기존_속성의_값을_수정할_수_있어야_한다() {
            // given
            UUID ownerId = UUID.randomUUID();
            ReflectionTestUtils.setField(mockUser, "id", ownerId);

            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef();
            Clothes clothes = Clothes.createClothes("계절 셔츠", ClothesType.TOP, mockUser);

            // 기존 속성 "봄" 추가
            ClothesAttribute existingAttr = ClothesAttribute.createClothesAttribute(defEntity, "봄");
            clothes.addAttribute(existingAttr);

            ClothesUpdateRequest updateRequest = new ClothesUpdateRequest(
                "계절 셔츠",
                ClothesType.TOP,
                List.of(new ClothesAttributeDto(defEntity.getId(), "여름")) // 여름으로 옵션 값 변경
            );

            when(clothesRepository.findById(clothes.getId())).thenReturn(Optional.of(clothes));
            when(clothesRepository.save(any(Clothes.class))).thenReturn(clothes);

            // when
            ClothesDto result = clothesService.update(clothes.getId(), ownerId, updateRequest);

            // then
            assertThat(result.attributes()).hasSize(1);
            assertThat(result.attributes().get(0).value()).isEqualTo("여름");
        }

        @Test
        void 새로운_속성을_추가할_수_있어야_한다() {
            // given
            UUID ownerId = UUID.randomUUID();
            ReflectionTestUtils.setField(mockUser, "id", ownerId);

            Clothes clothes = Clothes.createClothes("속성 셔츠", ClothesType.TOP, mockUser);

            ClothesAttributeDef newDef = ClothesAttributeDefFixture.createClothesAttributeDef("사이즈", List.of("FREE", "L", "XL"));

            ClothesUpdateRequest updateRequest = new ClothesUpdateRequest(
                "속성 셔츠",
                ClothesType.TOP,
                List.of(new ClothesAttributeDto(newDef.getId(), "FREE"))
            );

            when(clothesRepository.findById(clothes.getId())).thenReturn(Optional.of(clothes));
            when(clothesRepository.save(any(Clothes.class))).thenReturn(clothes);
            when(defRepository.findById(newDef.getId())).thenReturn(Optional.of(newDef));

            // when
            ClothesDto result = clothesService.update(clothes.getId(), ownerId, updateRequest);

            // then
            assertThat(result.attributes()).hasSize(1);
            assertThat(result.attributes().get(0).definitionName()).isEqualTo("사이즈");
            assertThat(result.attributes().get(0).value()).isEqualTo("FREE");
        }

        @Test
        void 이미지와_속성을_함께_수정할_수_있어야_한다() {
            // given
            UUID ownerId = UUID.randomUUID();
            ReflectionTestUtils.setField(mockUser, "id", ownerId);

            Clothes clothes = Clothes.createClothes("이미지 셔츠", ClothesType.TOP, mockUser);

            ClothesAttributeDef defEntity = ClothesAttributeDefFixture.createClothesAttributeDef("색상", List.of("빨강","검정"));
            ClothesUpdateRequest updateRequest = new ClothesUpdateRequest(
                "이미지 셔츠",
                ClothesType.TOP,
                List.of(new ClothesAttributeDto(defEntity.getId(), "빨강"))
            );

            // 가짜 이미지 파일
            MockMultipartFile newImage = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "dummy-image".getBytes()
            );
            String mockImageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/clothes/test.jpg";

            when(clothesRepository.findById(clothes.getId())).thenReturn(Optional.of(clothes));
            when(clothesRepository.save(any(Clothes.class))).thenReturn(clothes);
            when(defRepository.findById(defEntity.getId())).thenReturn(Optional.of(defEntity));
            when(s3ImageStorage.uploadImage(newImage, "clothes/")).thenReturn(mockImageUrl);

            // when
            ClothesDto result = clothesService.update(clothes.getId(), ownerId, updateRequest, newImage);

            // then
            assertThat(result.imageUrl()).isEqualTo(mockImageUrl);
            assertThat(result.attributes()).hasSize(1);
            assertThat(result.attributes().get(0).definitionName()).isEqualTo("색상");
            assertThat(result.attributes().get(0).value()).isEqualTo("빨강");
        }

        @Test
        void 의상_객체의_등록자와_인증객체의_유저가_다르다면_예외가_발생한다() {
            // given
            UUID ownerId = UUID.randomUUID();
            ReflectionTestUtils.setField(mockUser, "id", ownerId);

            UUID userId = UUID.randomUUID();
            User mockUser2 = UserFixture.createUser();
            ReflectionTestUtils.setField(mockUser2, "id", userId);

            Clothes clothes = Clothes.createClothes("속성 셔츠", ClothesType.TOP, mockUser2);

            ClothesAttributeDef newDef = ClothesAttributeDefFixture.createClothesAttributeDef("사이즈", List.of("FREE", "L", "XL"));

            ClothesUpdateRequest updateRequest = new ClothesUpdateRequest(
                "속성 셔츠",
                ClothesType.TOP,
                List.of(new ClothesAttributeDto(newDef.getId(), "FREE"))
            );

            when(clothesRepository.findById(clothes.getId())).thenReturn(Optional.of(clothes));

            // when & then
            assertThatThrownBy(() -> clothesService.update(clothes.getId(), ownerId, updateRequest))
                .isInstanceOf(ClothesOwnerMismatchException.class)
                .hasMessageContaining("해당 의상의 소유자가 아닙니다.");

        }

    }

    @Nested
    @DisplayName("의상 삭제 서비스 테스트")
    class ClothesDeleteServiceTest {

        @Test
        void 의상을_삭제하면_DB와_S3에서_삭제된다() {
            // given
            UUID ownerId = UUID.randomUUID();
            ReflectionTestUtils.setField(mockUser, "id", ownerId);

            Clothes clothes = Clothes.createClothes("청자켓", ClothesType.OUTER, mockUser);
            String imageUrl = "https://s3.test-bucket/clothes/test.png";
            clothes.updateImageUrl(imageUrl);

            when(clothesRepository.findById(clothes.getId())).thenReturn(Optional.of(clothes));

            // when
            clothesService.delete(ownerId, clothes.getId());

            // then
            verify(clothesRepository).delete(clothes);
            verify(s3ImageStorage).deleteImage(imageUrl);

        }

        @Test
        void 이미지가_없는_의상은_S3삭제가_호출되지_않는다() {
            // given
            UUID ownerId = UUID.randomUUID();
            ReflectionTestUtils.setField(mockUser, "id", ownerId);

            Clothes clothes = Clothes.createClothes("바지", ClothesType.BOTTOM, mockUser);

            when(clothesRepository.findById(clothes.getId()))
                .thenReturn(Optional.of(clothes));

            // when
            clothesService.delete(ownerId, clothes.getId());

            // then
            verify(clothesRepository).findById(clothes.getId());
            verify(clothesRepository).delete(clothes);
            verify(s3ImageStorage, never()).deleteImage(any());
        }

        @Test
        void 의상이_존재하지_않는다면_예외가_발생한다() {
            // given
            UUID notExistId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();

            when(clothesRepository.findById(notExistId))
                .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clothesService.delete(ownerId, notExistId))
                .isInstanceOf(ClothesNotFoundException.class);

            verify(clothesRepository).findById(notExistId);
            verify(clothesRepository, never()).delete(any());
            verify(s3ImageStorage, never()).deleteImage(any());
        }

        @Test
        void 의상_객체의_등록자와_인증객체의_유저가_다르다면_예외가_발생한다() {
            // given
            UUID ownerId = UUID.randomUUID();
            ReflectionTestUtils.setField(mockUser, "id", ownerId);

            UUID userId = UUID.randomUUID();
            User mockUser2 = UserFixture.createUser();
            ReflectionTestUtils.setField(mockUser2, "id", userId);

            Clothes clothes = Clothes.createClothes("속성 셔츠", ClothesType.TOP, mockUser2);

            when(clothesRepository.findById(clothes.getId())).thenReturn(Optional.of(clothes));

            // when & then
            assertThatThrownBy(() -> clothesService.delete(ownerId, clothes.getId()))
                .isInstanceOf(ClothesOwnerMismatchException.class)
                .hasMessageContaining("해당 의상의 소유자가 아닙니다.");
        }
    }

    @Nested
    @DisplayName("의상 목록 조회 서비스 테스트")
    class ClothesReadServiceTest {

        @Test
        void 조건_없이_조회하면_전체_의상_목록이_조회되어야_한다() {
            // given
            UUID ownerId = UUID.randomUUID();
            ReflectionTestUtils.setField(mockUser, "id", ownerId);

            ClothesSearchRequest request = ClothesSearchRequest.builder()
                .ownerId(UUID.randomUUID())
                .limit(10)
                .build();

            Clothes expectedClothes1 = mock(Clothes.class);
            Clothes expectedClothes2 = mock(Clothes.class);
            List<Clothes> expectedList = List.of(expectedClothes1, expectedClothes2);

            Pageable pageable = PageRequest.of(0, 10);
            Slice<Clothes> mockSlice = new SliceImpl<>(expectedList, pageable, false);

            when(clothesRepository.findClothesWithCursor(ownerId, request, pageable)).thenReturn(mockSlice);
            when(clothesRepository.totalElementCount(ownerId, request)).thenReturn(2L);

            ClothesDto clothesDto1 = mock(ClothesDto.class);
            ClothesDto clothesDto2 = mock(ClothesDto.class);

            when(clothesMapper.toClothesDto(expectedClothes1)).thenReturn(clothesDto1);
            when(clothesMapper.toClothesDto(expectedClothes2)).thenReturn(clothesDto2);

            // when
            CursorResponse<ClothesDto> result = clothesService.find(ownerId, request);

            // then
            assertAll(
                () -> assertEquals(2, result.data().size()),
                () -> assertNull(result.nextCursor()),
                () -> assertNull(result.nextIdAfter()),
                () -> assertFalse(result.hasNext())
            );

            verify(clothesRepository).findClothesWithCursor(ownerId, request, pageable);
            verify(clothesRepository).totalElementCount(ownerId, request);
            verify(clothesMapper).toClothesDto(expectedClothes1);
            verify(clothesMapper).toClothesDto(expectedClothes2);
        }
    }
}