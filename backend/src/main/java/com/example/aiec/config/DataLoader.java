package com.example.aiec.config;

import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 初期データローダー
 * アプリケーション起動時に初期商品データを投入
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        // 既にデータが存在する場合はスキップ
        if (productRepository.count() > 0) {
            log.info("初期データは既に存在します。スキップします。");
            return;
        }

        log.info("初期商品データを投入します...");

        List<Product> products = Arrays.asList(
                new Product(null, "ワイヤレスイヤホン", 8980,
                        "https://placehold.co/600x800/E7E5E4/71717A?text=Product+1",
                        "高音質で長時間バッテリー対応のワイヤレスイヤホン", 12, true),

                new Product(null, "スマートウォッチ", 24800,
                        "https://placehold.co/600x800/E7E5E4/71717A?text=Product+2",
                        "健康管理とフィットネストラッキングに最適なスマートウォッチ", 8, true),

                new Product(null, "ノートパソコン", 89800,
                        "https://placehold.co/600x800/E7E5E4/71717A?text=Product+3",
                        "高性能プロセッサ搭載の薄型軽量ノートパソコン", 5, true),

                new Product(null, "ワイヤレスマウス", 3980,
                        "https://placehold.co/600x800/E7E5E4/71717A?text=Product+4",
                        "精密トラッキングと長時間バッテリーを備えたワイヤレスマウス", 15, true),

                new Product(null, "USB-C ハブ", 5480,
                        "https://placehold.co/600x800/E7E5E4/71717A?text=Product+5",
                        "複数ポート搭載のコンパクトUSB-Cハブ", 20, true),

                new Product(null, "ゲーミングキーボード", 12800,
                        "https://placehold.co/600x800/E7E5E4/71717A?text=Product+6",
                        "メカニカルスイッチとRGBバックライト搭載のゲーミングキーボード", 3, true),

                new Product(null, "Webカメラ", 6980,
                        "https://placehold.co/600x800/E7E5E4/71717A?text=Product+7",
                        "フルHD対応でオートフォーカス機能付きWebカメラ", 10, true),

                new Product(null, "外付けSSD", 15800,
                        "https://placehold.co/600x800/E7E5E4/71717A?text=Product+8",
                        "高速データ転送可能な1TB外付けSSD", 0, true)
        );

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            product.setProductCode(String.format("P%06d", i + 1));
            product.setCategoryId(1L);
        }

        productRepository.saveAll(products);
        log.info("{}件の商品データを投入しました。", products.size());
    }

}
