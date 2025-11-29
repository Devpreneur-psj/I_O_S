package com.soi.spirit.repository;

import com.soi.spirit.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByItemCode(String itemCode);
    List<Item> findByItemType(String itemType);
    List<Item> findByIsAvailableTrue();
}

