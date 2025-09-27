package com.samsamotot.otboo.feed.repository;

import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.weather.entity.Grid;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedRepository extends JpaRepository<Feed, UUID>, FeedRepositoryCustom {

    // 논리적으로 삭제되지 않는 피드를 id로 조회합니다.
    Optional<Feed> findByIdAndIsDeletedFalse(UUID id);

    // 특정 피드의 좋아요 수를 1 증가시킵니다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Feed f set f.likeCount = f.likeCount + 1 where f.id = :feedId")
    int incrementLikeCount(@Param("feedId") UUID feedId);

    // 특정 피드의 좋아요 수를 1 감소시킵니다. (0 이하로 내려가지 않음)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Feed f set f.likeCount = case when f.likeCount > 0 then f.likeCount - 1 else 0 end where f.id = :feedId")
    int decrementLikeCount(@Param("feedId") UUID feedId);

    // 특정 피드의 댓글 수를 1 증가시킵니다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Feed f set f.commentCount = f.commentCount + 1 where f.id = :feedId")
    int incrementCommentCount(@Param("feedId") UUID feedId);

    // 특정 피드의 댓글 수를 1 감소시킵니다. (0 이하로 내려가지 않음)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Feed f set f.commentCount = case when f.commentCount > 0 then f.commentCount - 1 else 0 end where f.id = :feedId")
    int decrementCommentCount(@Param("feedId") UUID feedId);
    
    // 특정 격자에서 피드가 참조하는 날씨 데이터 ID들을 조회합니다.
    @Query("select f.weather.id from Feed f where f.weather.grid = :grid and f.weather is not null")
    Set<UUID> findWeatherIdsByGrid(@Param("grid") Grid grid);
}
