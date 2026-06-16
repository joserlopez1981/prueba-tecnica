package com.inditex.priceservice.infrastructure.adapter.persistence.mapper;

import com.inditex.priceservice.domain.model.Price;
import com.inditex.priceservice.infrastructure.adapter.persistence.entity.PriceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PricePersistenceMapper {

    Price toDomain(PriceEntity entity);
}
