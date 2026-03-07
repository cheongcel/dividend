package com.eunji.dividend;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "goals")
@Data
public class GoalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long targetMonthly; // 만원 단위
}