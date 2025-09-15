package com.samsamotot.otboo.follow.repository;

import com.samsamotot.otboo.common.config.QuerydslConfig;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PackageName  : com.samsamotot.otboo.follow.repository
 * FileName     : FollowRepositoryImplTest
 * Author       : dounguk
 * Date         : 2025. 9. 15.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({FollowRepositoryImpl.class, QuerydslConfig.class})
@DisplayName("Follow Repository QueryDSL 슬라이스 테스트")
class FollowRepositoryImplTest {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;


    @Test
    void 팔로우_목록을_DESC로_정렬해_응답한다() throws Exception {
        // given

        // when

        // then

    }

    @Test
    void limit_만큼의_응답을_반환합니다() throws Exception {
        // given

        // when

        // then

    }

    @Test
    void 팔로우_생성_기준으로_정렬한다() throws Exception {
        // given

        // when

        // then

    }


    @Test
    void 같은_커서값을_가진_객체가_여러개일_경우_보조커서_기준으로_조회한다() throws Exception {
        // given

        // when

        // then

    }

    @Test
    void nameLike_조건이_있으면_일치하는_값을_반환한다() throws Exception {
        // given

        // when

        // then

    }

    @Test
    void 조건이_없으면_모든_팔로잉_유저를_반환한다() throws Exception {
        // given

        // when

        // then

    }



}