package com.socialchat.repository;

import com.socialchat.entity.FriendRequest;
import com.socialchat.entity.FriendRequest.FriendRequestStatus;
import com.socialchat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findBySenderAndReceiver(User sender, User receiver);

    @Query("SELECT fr FROM FriendRequest fr WHERE (fr.sender = :user OR fr.receiver = :user) AND fr.status = :status")
    List<FriendRequest> findByUserAndStatus(@Param("user") User user, @Param("status") FriendRequestStatus status);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.receiver = :user AND fr.status = 'PENDING'")
    List<FriendRequest> findPendingRequestsForUser(@Param("user") User user);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.sender = :user AND fr.status = 'PENDING'")
    List<FriendRequest> findSentPendingRequestsByUser(@Param("user") User user);

    @Query("SELECT fr FROM FriendRequest fr WHERE ((fr.sender = :user1 AND fr.receiver = :user2) OR (fr.sender = :user2 AND fr.receiver = :user1)) AND fr.status = 'ACCEPTED'")
    Optional<FriendRequest> findAcceptedFriendship(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT fr FROM FriendRequest fr WHERE (fr.sender = :user1 AND fr.receiver = :user2) OR (fr.sender = :user2 AND fr.receiver = :user1)")
    Optional<FriendRequest> findAnyRequestBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT fr FROM FriendRequest fr WHERE (fr.sender = :user OR fr.receiver = :user) AND fr.status = 'ACCEPTED'")
    List<FriendRequest> findAcceptedFriendships(@Param("user") User user);

    @Query("SELECT fr FROM FriendRequest fr WHERE (fr.sender = :user OR fr.receiver = :user) AND fr.status = 'ACCEPTED'")
    Page<FriendRequest> findAcceptedFriendshipsPaged(@Param("user") User user, Pageable pageable);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.receiver = :user AND fr.status = 'PENDING'")
    Page<FriendRequest> findPendingRequestsReceivedPaged(@Param("user") User user, Pageable pageable);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.sender = :user AND fr.status = 'PENDING'")
    Page<FriendRequest> findPendingRequestsSentPaged(@Param("user") User user, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(fr) > 0 THEN true ELSE false END FROM FriendRequest fr " +
           "WHERE ((fr.sender = :user1 AND fr.receiver = :user2) OR (fr.sender = :user2 AND fr.receiver = :user1)) " +
           "AND fr.status = 'BLOCKED'")
    boolean isBlocked(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.sender = :blocker AND fr.receiver = :blocked AND fr.status = 'BLOCKED'")
    Optional<FriendRequest> findBlockBetweenUsers(@Param("blocker") User blocker, @Param("blocked") User blocked);

    boolean existsBySenderAndReceiverAndStatus(User sender, User receiver, FriendRequestStatus status);
}
