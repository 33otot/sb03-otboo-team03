package com.samsamotot.otboo.directmessage.repository;

import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.repository
 * FileName     : DirectMessageRepository
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {
}
