package com.samsamotot.otboo.profile.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.samsamotot.otboo.location.entity.QLocation;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.entity.QProfile;
import com.samsamotot.otboo.weather.entity.QGrid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QProfile qProfile = QProfile.profile;
    private final QLocation qLocation = QLocation.location;
    private final QGrid qGrid = QGrid.grid;

    /**
     * 격자 ID(Grid ID)를 기반으로 해당 격자의 위치 정보에 소속된 모든 프로필을 조회합니다.
     *
     * @param gridId 격자 ID
     * @return 격자에 해당되는 모든 사용자 프로필 목록
     */
    @Override
    public List<Profile> findAllByLocationGridId(UUID gridId) {
        return queryFactory
                .selectFrom(qProfile)
                .join(qProfile.location, qLocation)
                .join(qLocation.grid, qGrid)
                .where(qGrid.id.eq(gridId))
                .fetch();
    }

    /**
     * 여러 사용자 ID 목록에 해당하는 프로필 목록을 일괄 조회합니다.
     *
     * @param userIds 사용자 ID 목록
     * @return 일치하는 사용자 프로필 목록
     */
    @Override
    public List<Profile> findByUserIdIn(Collection<UUID> userIds) {
        return queryFactory
                .selectFrom(qProfile)
                .where(qProfile.user.id.in(userIds))
                .fetch();
    }
}
