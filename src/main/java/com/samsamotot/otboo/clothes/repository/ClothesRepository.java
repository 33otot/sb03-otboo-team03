package com.samsamotot.otboo.clothes.repository;

import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.repository.custom.ClothesRepositoryCustom;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesRepository extends JpaRepository<Clothes, UUID>, ClothesRepositoryCustom {

    List<Clothes> findAllByType(ClothesType type);

    List<Clothes> findAllByOwnerId(UUID ownerId);
}
