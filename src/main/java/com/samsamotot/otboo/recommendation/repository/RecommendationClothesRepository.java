package com.samsamotot.otboo.recommendation.repository;

import com.samsamotot.otboo.recommendation.entity.RecommendationClothes;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationClothesRepository extends JpaRepository<RecommendationClothes, UUID> {

}
