package com.samsamotot.otboo.common.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PackageName  : com.samsamotot.otboo.common.config
 * FileName     : QuerydslConfig
 * Author       : dounguk
 * Date         : 2025. 9. 15.
 */
@Configuration
public class QuerydslConfig {
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}

