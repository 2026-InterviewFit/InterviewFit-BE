package com.example.interviewfitbe.domain.coverletter.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StarItem {
    private String situation;
    private String task;
    private String action;
    private String result;
}
