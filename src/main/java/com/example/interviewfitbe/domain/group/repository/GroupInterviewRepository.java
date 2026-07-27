package com.example.interviewfitbe.domain.group.repository;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInterviewRepository extends JpaRepository<GroupInterviewEntity, Long> {
}