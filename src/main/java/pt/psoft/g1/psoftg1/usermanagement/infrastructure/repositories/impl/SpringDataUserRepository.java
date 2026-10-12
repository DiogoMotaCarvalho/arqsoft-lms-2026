/*
 * Copyright (c) 2022-2024 the original author or authors.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package pt.psoft.g1.psoftg1.usermanagement.infrastructure.repositories.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

/** Based on https://github.com/Yoh0xFF/java-spring-security-example. */
@Repository
@CacheConfig(cacheNames = "users")
public interface SpringDataUserRepository
    extends UserRepository, UserRepoCustom, CrudRepository<User, Long> {

  @Override
  @CacheEvict(allEntries = true)
  <S extends User> List<S> saveAll(Iterable<S> entities);

  @Override
  @Caching(
      evict = {
        @CacheEvict(key = "#p0.id", condition = "#p0.id != null"),
        @CacheEvict(key = "#p0.username", condition = "#p0.username != null")
      })
  <S extends User> S save(S entity);

  /** findById searches a specific user and returns an optional. */
  @Override
  @Cacheable
  Optional<User> findById(Long objectId);

  /**
   * getById explicitly loads a user or throws an exception if the user does not exist or the
   * account is not enabled.
   *
   * @param id the identifier of the user
   * @return the user with the given identifier
   */
  @Cacheable
  default User getById(final Long id) {
    final Optional<User> maybeUser = findById(id);
    // throws 404 Not Found if the user does not exist or is not enabled
    return maybeUser
        .filter(User::isEnabled)
        .orElseThrow(() -> new NotFoundException(User.class, id));
  }

  @Cacheable
  Optional<User> findByUsername(String username);

  @Cacheable
  List<User> findByNameName(String name);
}
