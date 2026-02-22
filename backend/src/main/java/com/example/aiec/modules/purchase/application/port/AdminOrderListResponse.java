package com.example.aiec.modules.purchase.application.port;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理向け注文一覧レスポンス
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderListResponse {
    private List<OrderDto> orders;
    private Pagination pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private Integer page;
        private Integer pageSize;
        private Long totalCount;
    }
}
