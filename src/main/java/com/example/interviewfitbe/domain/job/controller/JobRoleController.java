package com.example.interviewfitbe.domain.job.controller;

import com.example.interviewfitbe.domain.job.dto.JobRoleDto;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/job-role")
@RequiredArgsConstructor
@RestController
public class JobRoleController {
    private final JobRoleService jobRoleService;

    @GetMapping("/all")
    public ResponseEntity<List<JobRoleDto>> getAllJobRoles() {
        return ResponseEntity.ok(jobRoleService.getAllJobRoles());
    }
}
