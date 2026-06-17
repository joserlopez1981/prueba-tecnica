package com.inditex.priceservice.infrastructure.adapter.persistence.mapper;

import com.inditex.priceservice.domain.model.Price;
import com.inditex.priceservice.infrastructure.adapter.persistence.entity.PriceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PricePersistenceMapper {

    @Mapping(source = "price", target = "amount")
    Price toDomain(PriceEntity entity);
}
