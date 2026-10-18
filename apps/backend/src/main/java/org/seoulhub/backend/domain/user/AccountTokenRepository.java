package org.seoulhub.backend.domain.user;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
public interface AccountTokenRepository extends JpaRepository<AccountToken,String> {
    @Query("select t.userId from AccountToken t where t.tokenHash=:hash and t.purpose=:purpose")
    Optional<Long> findOwner(@Param("hash") String hash, @Param("purpose") AccountToken.Purpose purpose);
    @Modifying
    @Query("delete from AccountToken t where t.tokenHash=:hash and t.userId=:userId and t.purpose=:purpose and t.expiresAt>:now")
    int consume(@Param("hash") String hash, @Param("userId") Long userId, @Param("purpose") AccountToken.Purpose purpose, @Param("now") Instant now);
    @Modifying @Query("delete from AccountToken t where t.userId=:id and t.purpose=:purpose")
    int removePurpose(@Param("id") Long id, @Param("purpose") AccountToken.Purpose purpose);
    @Modifying @Query("delete from AccountToken t where t.userId=:id")
    int removeAllForUser(@Param("id") Long id);
    @Modifying @Query("delete from AccountToken t where t.expiresAt<:now")
    int removeExpired(@Param("now") Instant now);
}
