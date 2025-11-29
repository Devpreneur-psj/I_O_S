package com.soi.spirit.repository;

import com.soi.spirit.entity.SpiritType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpiritTypeRepository extends JpaRepository<SpiritType, Long> {
    Optional<SpiritType> findByTypeCode(String typeCode);
    List<SpiritType> findByIsRare(Boolean isRare);
    List<SpiritType> findByIsRareTrue();
    List<SpiritType> findByUnlockLevelLessThanEqual(Integer unlockLevel);
}

