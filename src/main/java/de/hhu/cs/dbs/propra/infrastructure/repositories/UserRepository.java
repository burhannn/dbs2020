package de.hhu.cs.dbs.propra.infrastructure.repositories;

import de.hhu.cs.dbs.propra.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByName(String name);

    long countByNameAndPassword(String name, String password);
}
