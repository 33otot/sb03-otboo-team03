package com.samsamotot.otboo.feed.repository;

import com.samsamotot.otboo.feed.entity.FeedClothes;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedClothesRepository extends JpaRepository<FeedClothes, UUID> {

}
