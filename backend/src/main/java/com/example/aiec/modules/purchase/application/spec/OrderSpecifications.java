package com.example.aiec.modules.purchase.application.spec;

import com.example.aiec.modules.purchase.application.port.AdminOrderSearchParams;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理向け注文検索 Specification
 */
public final class OrderSpecifications {
    private static final ZoneId BUSINESS_TIME_ZONE = ZoneId.of("Asia/Tokyo");
    private static final List<Order.OrderStatus> UNSHIPPED_STATUSES = List.of(
            Order.OrderStatus.PENDING,
            Order.OrderStatus.CONFIRMED,
            Order.OrderStatus.PREPARING_SHIPMENT
    );

    private OrderSpecifications() {
    }

    public static Specification<Order> byAdminSearchParams(AdminOrderSearchParams searchParams) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchParams.getOrderNumber() != null && !searchParams.getOrderNumber().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("orderNumber")),
                        "%" + searchParams.getOrderNumber().trim().toLowerCase() + "%"
                ));
            }

            if (searchParams.getCustomerEmail() != null && !searchParams.getCustomerEmail().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("user").get("email")),
                        "%" + searchParams.getCustomerEmail().trim().toLowerCase() + "%"
                ));
            }

            if (searchParams.getStatuses() != null && !searchParams.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(searchParams.getStatuses()));
            }

            if (searchParams.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        startOfDay(searchParams.getDateFrom())
                ));
            }

            if (searchParams.getDateTo() != null) {
                predicates.add(cb.lessThan(
                        root.get("createdAt"),
                        startOfDay(searchParams.getDateTo().plusDays(1))
                ));
            }

            if (searchParams.getTotalPriceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalPrice"), searchParams.getTotalPriceMin()));
            }

            if (searchParams.getTotalPriceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalPrice"), searchParams.getTotalPriceMax()));
            }

            if (Boolean.TRUE.equals(searchParams.getAllocationIncomplete())) {
                predicates.add(buildAllocationIncompletePredicate(root, query.subquery(Long.class), cb));
            }

            if (Boolean.TRUE.equals(searchParams.getUnshipped())) {
                predicates.add(root.get("status").in(UNSHIPPED_STATUSES));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate buildAllocationIncompletePredicate(
            Root<Order> root,
            Subquery<Long> subquery,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        Root<OrderItem> orderItemRoot = subquery.from(OrderItem.class);
        subquery.select(orderItemRoot.get("id"));
        subquery.where(
                cb.equal(orderItemRoot.get("order").get("id"), root.get("id")),
                cb.lessThan(orderItemRoot.get("committedQty"), orderItemRoot.get("quantity"))
        );
        return cb.exists(subquery);
    }

    private static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(BUSINESS_TIME_ZONE).toInstant();
    }
}
