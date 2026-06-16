package com.inditex.priceservice.infrastructure.entrypoint.rest.mapper;

import com.inditex.priceservice.domain.model.Price;
import com.inditex.priceservice.infrastructure.entrypoint.rest.dto.PriceResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PriceRestMapper {

    PriceResponse toResponse(Price price);
}
