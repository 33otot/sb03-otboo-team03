package com.samsamotot.otboo.feed.service;


import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.samsamotot.otboo.common.fixture.FeedFixture;
import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.feed.document.FeedDocument;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.mapper.FeedMapper;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.feed.repository.FeedSearchRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedDataSync 서비스 단위 테스트")
class FeedDataSyncServiceTest {

    @Mock
    FeedRepository feedRepository;

    @Mock
    FeedSearchRepository feedSearchRepository;

    @Mock
    FeedMapper feedMapper;

    @InjectMocks
    FeedDataSyncService service;

    UUID ID1, ID2;
    Feed feed1, feed2;
    FeedDto dto1, dto2;

    @BeforeEach
    void setUp() {

        User user = UserFixture.createUser();
        Grid grid = GridFixture.createGrid();
        Weather weather = WeatherFixture.createWeather(grid);

        ID1 = UUID.randomUUID();
        ID2 = UUID.randomUUID();

        feed1 = FeedFixture.createFeed(user, weather);
        ReflectionTestUtils.setField(feed1, "id", ID1);
        feed2 = FeedFixture.createFeed(user, weather);
        ReflectionTestUtils.setField(feed2, "id", ID2);

        dto1 = FeedFixture.createFeedDto(feed1);
        dto2 = FeedFixture.createFeedDto(feed2);
    }

    @Nested
    @DisplayName("Elasticsearch 전체 동기화 테스트")
    class SyncAll {

        @Test
        void Feed_전체를_DTO로_변환해_ES에_저장한다() {

            // given
            given(feedRepository.findAll()).willReturn(List.of(feed1, feed2));
            given(feedMapper.toDto(feed1)).willReturn(dto1);
            given(feedMapper.toDto(feed2)).willReturn(dto2);

            // when
            service.syncAllFeedsToElasticsearch();

            // then
            ArgumentCaptor<List<FeedDocument>> captor = ArgumentCaptor.forClass(List.class);
            verify(feedSearchRepository).saveAll(captor.capture());

            List<FeedDocument> docs = captor.getValue();
            assertThat(docs).hasSize(2);
            FeedDocument d1 = docs.get(0);
            FeedDocument d2 = docs.get(1);

            assertThat(d1.id()).isEqualTo(ID1);
            assertThat(d2.id()).isEqualTo(ID2);
        }

        @Test
        void 동기화할_피드가_없으면_saveAll을_호출하지_않는다() {
            given(feedRepository.findAll()).willReturn(List.of());
            service.syncAllFeedsToElasticsearch();
            verify(feedSearchRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("Elasticsearch 단일 동기화 테스트")
    class SyncOne {

        @Test
        void 단일_DTO를_ES에_저장한다() {

            service.syncFeedToElasticsearch(dto1);

            ArgumentCaptor<FeedDocument> captor = ArgumentCaptor.forClass(FeedDocument.class);
            verify(feedSearchRepository).save(captor.capture());

            FeedDocument saved = captor.getValue();
            assertThat(saved.id()).isEqualTo(dto1.id());
            assertThat(saved.content()).isEqualTo(dto1.content());
            assertThat(saved.createdAt()).isEqualTo(dto1.createdAt());
            assertThat(saved.updatedAt()).isEqualTo(dto1.updatedAt());
            assertThat(saved.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Elasticsearch 하드 삭제 테스트")
    class HardDelete {

        @Test
        void ID로_ES에서_문서를_삭제한다() {
            service.deleteFeedFromElasticsearch(ID1);
            verify(feedSearchRepository).deleteById(ID1);
        }
    }

    @Nested
    @DisplayName("Elasticsearch 소프트 삭제 테스트")
    class SoftDelete {

        @Test
        void 문서가_있으면_isDeleted를_true로_저장한다() {
            FeedDocument existing = FeedDocument.builder()
                .id(ID1)
                .author(dto1.author())
                .weather(dto1.weather())
                .content(dto1.content())
                .createdAt(dto1.createdAt())
                .updatedAt(dto1.updatedAt())
                .ootds(dto1.ootds())
                .likeCount(dto1.likeCount())
                .commentCount(dto1.commentCount())
                .likedByMe(dto1.likedByMe())
                .isDeleted(false)
                .build();

            given(feedSearchRepository.findById(ID1)).willReturn(Optional.of(existing));

            service.softDeleteFeedFromElasticsearch(ID1);

            ArgumentCaptor<FeedDocument> captor = ArgumentCaptor.forClass(FeedDocument.class);
            verify(feedSearchRepository).save(captor.capture());

            FeedDocument updated = captor.getValue();
            assertThat(updated.id()).isEqualTo(existing.id());
            assertThat(updated.isDeleted()).isTrue();
            // 나머지 필드 보존 확인(대표 필드 몇 개만)
            assertThat(updated.content()).isEqualTo(existing.content());
            assertThat(updated.likeCount()).isEqualTo(existing.likeCount());
        }

        @Test
        void 문서가_없으면_아무_동작도_하지_않는다() {
            given(feedSearchRepository.findById(ID2)).willReturn(Optional.empty());
            service.softDeleteFeedFromElasticsearch(ID2);
            verify(feedSearchRepository, never()).save(any());
        }
    }
}