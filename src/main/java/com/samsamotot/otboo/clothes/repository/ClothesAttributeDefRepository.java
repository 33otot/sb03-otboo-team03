package com.samsamotot.otboo.clothes.repository;

import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesAttributeDefRepository extends JpaRepository<ClothesAttributeDef, UUID> {

    boolean existsByName(String name);

    List<ClothesAttributeDef> findByNameContaining(String name, Sort sort);
}
