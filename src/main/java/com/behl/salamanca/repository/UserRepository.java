package com.behl.salamanca.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.behl.salamanca.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailId(final String emailId);

    Optional<User> findByEmailId(final String emailId);

}
