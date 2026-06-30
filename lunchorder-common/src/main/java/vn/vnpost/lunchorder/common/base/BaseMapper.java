package vn.vnpost.lunchorder.common.base;

import java.util.List;

public interface BaseMapper<REQ, RES, E> {
    E toEntity(REQ request);

    RES toDto(E entity);

    List<E> toEntityList(List<REQ> dtoList);

    List<RES> toDtoList(List<E> entityList);
}
