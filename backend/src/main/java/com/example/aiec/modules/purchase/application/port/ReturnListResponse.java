package com.example.aiec.modules.purchase.application.port;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "返品一覧レスポンス")
public class ReturnListResponse {

    @Schema(description = "返品一覧")
    private List<ReturnShipmentDto> returns;

    @Schema(description = "ページング")
    private Pagination pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "ページング情報")
    public static class Pagination {
        @Schema(description = "ページ番号", example = "1")
        private int page;

        @Schema(description = "取得件数", example = "20")
        private int limit;

        @Schema(description = "総件数", example = "5")
        private long totalCount;
    }
}
