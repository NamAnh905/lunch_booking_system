package vn.vnpost.lunchorder.core.modules.guestmeal.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.audit.AuditEvent;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.constant.PaginationConstants;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.guestmeal.entity.GuestMeal;
import vn.vnpost.lunchorder.core.modules.guestmeal.repository.GuestMealRepository;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.GuestMealService;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.dto.GuestMealCreateRequest;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.dto.GuestMealResponse;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.dto.GuestMealUpdateRequest;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.mapstruct.GuestMealMapper;
import vn.vnpost.lunchorder.core.modules.price.service.MealPricePolicy;
import vn.vnpost.lunchorder.core.policy.CutOffPolicy;
import vn.vnpost.lunchorder.core.policy.OrderableDates;
import vn.vnpost.lunchorder.system.modules.department.entity.Department;
import vn.vnpost.lunchorder.system.modules.user.entity.User;
import vn.vnpost.lunchorder.system.modules.user.service.UserLookupService;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GuestMealServiceImpl implements GuestMealService {

    private static final String AUDIT_TARGET = "GuestMeal";

    private final GuestMealRepository guestMealRepository;
    private final GuestMealMapper guestMealMapper;
    private final UserLookupService userLookupService;
    private final MealPricePolicy mealPricePolicy;
    private final CutOffPolicy cutOffPolicy;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public GuestMealResponse create(GuestMealCreateRequest request) {
        User requestedBy = userLookupService.getById(request.getRequestedByUserId());
        Department department = resolveDepartment(requestedBy, request.getDepartmentId());

        validateQuantities(request.getNormalQuantity(), request.getSpecialQuantity());
        validateMealDate(request.getMealDate());

        GuestMeal guestMeal = new GuestMeal();
        guestMeal.setDepartment(department);
        guestMeal.setRequestedBy(requestedBy);
        guestMeal.setMealDate(request.getMealDate());
        guestMeal.setNormalQuantity(request.getNormalQuantity());
        guestMeal.setSpecialQuantity(normalizeQuantity(request.getSpecialQuantity()));
        guestMeal.setNormalUnitPrice(mealPricePolicy.getNormalPrice());
        guestMeal.setSpecialUnitPrice(mealPricePolicy.getSpecialPrice());
        guestMeal.setNote(request.getNote());
        applyTotalAmount(guestMeal);

        guestMeal = guestMealRepository.save(guestMeal);
        GuestMealResponse created = guestMealMapper.toDto(guestMeal);

        eventPublisher.publishEvent(
                new AuditEvent("CREATE_GUEST_MEAL", AUDIT_TARGET, guestMeal.getId(), null, created));

        return created;
    }

    @Override
    @Transactional
    public GuestMealResponse update(Long id, GuestMealUpdateRequest request) {
        GuestMeal guestMeal = guestMealRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_MEAL_NOT_FOUND));

        assertNotLocked(guestMeal.getMealDate());

        GuestMealResponse before = guestMealMapper.toDto(guestMeal);

        User requestedBy = userLookupService.getById(request.getRequestedByUserId());
        Department department = resolveDepartment(requestedBy, request.getDepartmentId());

        validateQuantities(request.getNormalQuantity(), request.getSpecialQuantity());
        validateMealDate(request.getMealDate());

        guestMeal.setDepartment(department);
        guestMeal.setRequestedBy(requestedBy);
        guestMeal.setMealDate(request.getMealDate());
        guestMeal.setNormalQuantity(request.getNormalQuantity());
        guestMeal.setSpecialQuantity(normalizeQuantity(request.getSpecialQuantity()));
        guestMeal.setNote(request.getNote());
        applyTotalAmount(guestMeal);

        guestMeal = guestMealRepository.save(guestMeal);
        GuestMealResponse updated = guestMealMapper.toDto(guestMeal);

        eventPublisher.publishEvent(new AuditEvent("UPDATE_GUEST_MEAL", AUDIT_TARGET, id, before, updated));

        return updated;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        GuestMeal guestMeal = guestMealRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_MEAL_NOT_FOUND));

        assertNotLocked(guestMeal.getMealDate());

        GuestMealResponse before = guestMealMapper.toDto(guestMeal);
        guestMealRepository.delete(guestMeal);

        eventPublisher.publishEvent(new AuditEvent("DELETE_GUEST_MEAL", AUDIT_TARGET, id, before, null));
    }

    @Override
    public PageResponse<GuestMealResponse> findAll(int page, int size, LocalDate startDate, LocalDate endDate,
            Long departmentId, Long requestedByUserId) {
        Pageable pageable = PaginationConstants.toPageable(page, size);

        Page<GuestMeal> entityPage = guestMealRepository.findForAdmin(
                startDate, endDate, departmentId, requestedByUserId, pageable);

        return PageResponse.of(entityPage, guestMealMapper.toDtoList(entityPage.getContent()), page, size);
    }

    private Department resolveDepartment(User requestedBy, Long departmentId) {
        Department department = requestedBy.getDepartment();
        if (department == null || !department.getId().equals(departmentId)) {
            throw new AppException(ErrorCode.GUEST_MEAL_DEPARTMENT_MISMATCH);
        }
        return department;
    }

    private void validateQuantities(Integer normalQuantity, Integer specialQuantity) {
        if (normalizeQuantity(normalQuantity) + normalizeQuantity(specialQuantity) < 1) {
            throw new AppException(ErrorCode.GUEST_MEAL_QUANTITY_REQUIRED);
        }
    }

    private void validateMealDate(LocalDate mealDate) {
        if (mealDate.isBefore(cutOffPolicy.today())) {
            throw new AppException(ErrorCode.GUEST_MEAL_DATE_IN_PAST);
        }

        OrderableDates.snapshot(cutOffPolicy).assertOrderable(mealDate);
    }

    private void assertNotLocked(LocalDate mealDate) {
        if (cutOffPolicy.isCutOffReached(mealDate)) {
            throw new AppException(ErrorCode.GUEST_MEAL_LOCKED);
        }
    }

    private void applyTotalAmount(GuestMeal guestMeal) {
        BigDecimal normalAmount = guestMeal.getNormalUnitPrice()
                .multiply(BigDecimal.valueOf(normalizeQuantity(guestMeal.getNormalQuantity())));
        BigDecimal specialAmount = guestMeal.getSpecialUnitPrice()
                .multiply(BigDecimal.valueOf(normalizeQuantity(guestMeal.getSpecialQuantity())));
        guestMeal.setTotalAmount(normalAmount.add(specialAmount));
    }

    private int normalizeQuantity(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }
}
