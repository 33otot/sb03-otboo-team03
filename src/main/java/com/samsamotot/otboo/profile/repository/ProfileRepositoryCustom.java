package com.samsamotot.otboo.profile.repository;

import com.samsamotot.otboo.profile.entity.Profile;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProfileRepositoryCustom {
    /**
     * 격자 ID(Grid ID)를 기반으로 해당 격자의 위치 정보에 소속된 모든 프로필을 조회합니다.
     *
     * @param gridId 격자 ID
     * @return 격자에 해당되는 모든 사용자 프로필 목록
     */
    List<Profile> findAllByLocationGridId(UUID gridId);

    /**
     * 여러 사용자 ID 목록에 해당하는 프로필 목록을 일괄 조회합니다.
     *
     * @param userIds 사용자 ID 목록
     * @return 일치하는 사용자 프로필 목록
     */
    List<Profile> findByUserIdIn(Collection<UUID> userIds);
}
