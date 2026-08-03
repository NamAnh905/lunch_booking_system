package vn.vnpost.lunchorder.system.modules.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.vnpost.lunchorder.system.modules.user.entity.User;

public interface UserLookupService {

    User getById(Long id);

    /**
     * Loads the user with a pessimistic write lock so concurrent transactions acting
     * on behalf of the same user are serialized. Must be called inside a transaction.
     */
    User getByIdForUpdate(Long id);

    Page<User> findAll(Pageable pageable);
}
