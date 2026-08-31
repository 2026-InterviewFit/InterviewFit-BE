package com.example.interviewfitbe.domain.job.repository;

import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRoleRepository extends JpaRepository<JobRoleEntity, Long> {
    Optional<JobRoleEntity> findByName(String name);
    List<JobRoleEntity> findAllByNameNot(String jobName);
}
