package vn.vnpost.lunchorder.system.modules.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.vnpost.lunchorder.system.modules.user.entity.User;

public interface UserLookupService {

    User getById(Long id);

    Page<User> findAll(Pageable pageable);
}
