package com.example.interviewfitbe.domain.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobRoleDto {
    private Long id;
    private String category;
    private String name;
}
