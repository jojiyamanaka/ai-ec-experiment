package com.example.aiec.modules.product.application.spec;

import com.example.aiec.modules.inventory.domain.entity.LocationStock;
import com.example.aiec.modules.inventory.domain.entity.SalesLimit;
import com.example.aiec.modules.inventory.domain.entity.StockReservation;
import com.example.aiec.modules.product.application.port.AdminProductSearchParams;
import com.example.aiec.modules.product.domain.entity.AllocationType;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理向け商品検索 Specification
 */
public final class ProductSpecifications {
    private static final int DEFAULT_LOCATION_ID = 1;

    private ProductSpecifications() {
    }

    public static Specification<Product> byAdminSearchParams(AdminProductSearchParams searchParams, Instant now) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchParams.getKeyword() != null && !searchParams.getKeyword().isBlank()) {
                String keyword = "%" + searchParams.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("productCode")), keyword)
                ));
            }

            if (searchParams.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), searchParams.getCategoryId()));
            }

            if (searchParams.getIsPublished() != null) {
                predicates.add(cb.equal(root.get("isPublished"), searchParams.getIsPublished()));
            }

            if (Boolean.TRUE.equals(searchParams.getInSalePeriod())) {
                predicates.add(cb.or(
                        cb.isNull(root.get("saleStartAt")),
                        cb.lessThanOrEqualTo(root.get("saleStartAt"), now)
                ));
                predicates.add(cb.or(
                        cb.isNull(root.get("saleEndAt")),
                        cb.greaterThanOrEqualTo(root.get("saleEndAt"), now)
                ));
            }

            if (searchParams.getAllocationType() != null) {
                predicates.add(cb.equal(root.get("allocationType"), searchParams.getAllocationType()));
            }

            Integer threshold = null;
            if (Boolean.TRUE.equals(searchParams.getZeroStockOnly())) {
                threshold = 0;
            } else if (searchParams.getStockThreshold() != null) {
                threshold = searchParams.getStockThreshold();
            }
            if (threshold != null) {
                Expression<Integer> effectiveStockExpression = effectiveStockExpression(root, query.subquery(Integer.class), cb, now);
                predicates.add(cb.lessThanOrEqualTo(effectiveStockExpression, threshold));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Expression<Integer> effectiveStockExpression(
            Root<Product> root,
            Subquery<Integer> subquery,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Instant now
    ) {
        Root<Product> productRoot = subquery.from(Product.class);
        subquery.select(cb.<Integer>selectCase()
                .when(cb.equal(productRoot.get("allocationType"), AllocationType.REAL),
                        realEffectiveStock(productRoot, subquery, cb, now))
                .otherwise(frameEffectiveStock(productRoot, subquery, cb, now)));
        subquery.where(cb.equal(productRoot.get("id"), root.get("id")));
        return cb.coalesce(subquery.getSelection(), 0);
    }

    private static Expression<Integer> realEffectiveStock(
            Root<Product> productRoot,
            Subquery<Integer> parentSubquery,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Instant now
    ) {
        Subquery<Integer> remainingSubquery = parentSubquery.subquery(Integer.class);
        Root<LocationStock> locationStockRoot = remainingSubquery.from(LocationStock.class);
        remainingSubquery.select(cb.diff(
                cb.coalesce(locationStockRoot.get("availableQty"), 0),
                cb.coalesce(locationStockRoot.get("committedQty"), 0)
        ));
        remainingSubquery.where(
                cb.equal(locationStockRoot.get("product").get("id"), productRoot.get("id")),
                cb.equal(locationStockRoot.get("locationId"), DEFAULT_LOCATION_ID)
        );

        Subquery<Integer> tentativeSubquery = parentSubquery.subquery(Integer.class);
        Root<StockReservation> tentativeRoot = tentativeSubquery.from(StockReservation.class);
        tentativeSubquery.select(cb.coalesce(cb.sum(tentativeRoot.get("quantity")), 0));
        tentativeSubquery.where(
                cb.equal(tentativeRoot.get("product").get("id"), productRoot.get("id")),
                cb.equal(tentativeRoot.get("type"), StockReservation.ReservationType.TENTATIVE),
                cb.greaterThan(tentativeRoot.get("expiresAt"), now)
        );

        Expression<Integer> raw = cb.diff(
                cb.coalesce(remainingSubquery.getSelection(), 0),
                cb.coalesce(tentativeSubquery.getSelection(), 0)
        );
        return cb.<Integer>selectCase()
                .when(cb.lessThan(raw, 0), cb.literal(0))
                .otherwise(raw);
    }

    private static Expression<Integer> frameEffectiveStock(
            Root<Product> productRoot,
            Subquery<Integer> parentSubquery,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Instant now
    ) {
        Subquery<Integer> frameLimitSubquery = parentSubquery.subquery(Integer.class);
        Root<SalesLimit> salesLimitRoot = frameLimitSubquery.from(SalesLimit.class);
        frameLimitSubquery.select(cb.coalesce(salesLimitRoot.get("frameLimitQty"), 0));
        frameLimitSubquery.where(cb.equal(salesLimitRoot.get("product").get("id"), productRoot.get("id")));

        Subquery<Integer> consumedSubquery = parentSubquery.subquery(Integer.class);
        Root<OrderItem> consumedRoot = consumedSubquery.from(OrderItem.class);
        consumedSubquery.select(cb.coalesce(cb.sum(consumedRoot.get("quantity")), 0));
        consumedSubquery.where(
                cb.equal(consumedRoot.get("product").get("id"), productRoot.get("id")),
                cb.equal(consumedRoot.get("product").get("allocationType"), AllocationType.FRAME),
                cb.notEqual(consumedRoot.get("order").get("status"), Order.OrderStatus.CANCELLED)
        );

        Subquery<Integer> tentativeSubquery = parentSubquery.subquery(Integer.class);
        Root<StockReservation> tentativeRoot = tentativeSubquery.from(StockReservation.class);
        tentativeSubquery.select(cb.coalesce(cb.sum(tentativeRoot.get("quantity")), 0));
        tentativeSubquery.where(
                cb.equal(tentativeRoot.get("product").get("id"), productRoot.get("id")),
                cb.equal(tentativeRoot.get("type"), StockReservation.ReservationType.TENTATIVE),
                cb.greaterThan(tentativeRoot.get("expiresAt"), now)
        );

        Expression<Integer> raw = cb.diff(
                cb.diff(
                        cb.coalesce(frameLimitSubquery.getSelection(), 0),
                        cb.coalesce(consumedSubquery.getSelection(), 0)
                ),
                cb.coalesce(tentativeSubquery.getSelection(), 0)
        );
        return cb.<Integer>selectCase()
                .when(cb.lessThan(raw, 0), cb.literal(0))
                .otherwise(raw);
    }
}
