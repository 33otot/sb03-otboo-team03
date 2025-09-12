package com.samsamotot.otboo.clothes.repository;

import com.samsamotot.otboo.clothes.entity.ClothesAttribute;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesAttributeRepository extends JpaRepository<ClothesAttribute, UUID> {

}
