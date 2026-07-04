package com.chwihap.server.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public class BaseEntity extends BaseTimeEntity {

    @Column(nullable = false)
    private boolean isDeleted = false;
}
