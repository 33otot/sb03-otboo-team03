package com.samsamotot.otboo.notification.listener;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.follow.repository.FollowRepository;
import com.samsamotot.otboo.notification.dto.event.*;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.notification.service.NotificationService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PackageName  : com.samsamotot.otboo.notification.listener
 * FileName     : NotificationListener
 * Author       : dounguk
 * Date         : 2025. 10. 7.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final FollowRepository followRepository;

    private static final String ROLE_TITLE = "권한 변경";
    private static final String CLOTHES_ATTRIBUTE_TITLE = "의상 속성 추가";
    private static final String LIKE_TITLE = "새 좋아요";
    private static final String COMMENT_TITLE = "새 댓글";
    private static final String FEED_TITLE = "새 피드";
    private static final String FOLLOW_TITLE = "새 팔로워";
    private static final String DIRECT_MESSAGE_TITLE = "새 쪽지";

    private static final String ROLE_CONTENT = "변경된 권한을 확인하세요";
    private static final String CLOTHES_ATTRIBUTE_CONTENT = "의상 속성이 추가되었습니다.";

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoleChanged(RoleChangedEvent e) {
        notificationService.save(e.currentUserId(), ROLE_TITLE, ROLE_CONTENT, NotificationLevel.INFO);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClothesAttributeCreated(ClothesAttributeCreatedEvent e) {
        notificationService.saveBatchNotification(CLOTHES_ATTRIBUTE_TITLE, CLOTHES_ATTRIBUTE_CONTENT, NotificationLevel.INFO);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedLiked(FeedLikedEvent e) {
        User liker = userRepository.findById(e.likerId()).orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));
        Feed feed = feedRepository.findById(e.feedId()).orElseThrow(() -> new OtbooException(ErrorCode.FEED_NOT_FOUND));
        notificationService.save(feed.getAuthor().getId(), LIKE_TITLE, "[" + liker.getUsername() + "] 가 좋아요를 눌렀습니다", NotificationLevel.INFO);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent e) {
        User commenter = userRepository.findById(e.commenterId()).orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));
        Feed feed = feedRepository.findById(e.feedId()).orElseThrow(() -> new OtbooException(ErrorCode.FEED_NOT_FOUND));

        String commentPreview = shortenContent(e.content());
        notificationService.save(feed.getAuthor().getId(), COMMENT_TITLE, "작성자 [" + commenter.getUsername() + "], 메세지: [" + commentPreview + "]", NotificationLevel.INFO);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedCreated(FeedCreatedEvent e) {
        String authorName = e.author().getUsername();

        followRepository.findFollowerIdsByFolloweeId(e.author().getId()).stream()
            .forEach(followerId -> {
                notificationService.save(followerId, FEED_TITLE, "작성자 [" + authorName + "]", NotificationLevel.INFO);
            });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFollowCreated(FollowCreatedEvent e) {
        notificationService.save(e.followeeId(), FOLLOW_TITLE, "팔로워 [" + e.followerName() + "]", NotificationLevel.INFO);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDirectMessageCreated(DirectMessageReceivedEvent e) {
        User sender = userRepository.findById(e.senderId()).orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));

        String messagePreview = shortenContent(e.content());
        notificationService.save(e.receiverId(), DIRECT_MESSAGE_TITLE, "작성자: [" + sender.getUsername() + "], 메세지: [" + messagePreview + "]", NotificationLevel.INFO);
    }

    private String shortenContent(String content) {
        if (content.length() > 10) {
            return content.substring(0, 10) + "...";
        }
        return content;
    }
}
