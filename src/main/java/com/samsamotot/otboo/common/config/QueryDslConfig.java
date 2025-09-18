package com.samsamotot.otboo.common.config;

/**
 * PackageName  : com.samsamotot.otboo.common.config
 * FileName     : QuerydslConfig
 * Author       : dounguk
 * Date         : 2025. 9. 18.
 */

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class QueryDslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}

