# Backend 規約

## モジュラーモノリス構造

パッケージ: `com.example.aiec.modules.{module}.{layer}`

モジュール一覧: `product` / `inventory` / `purchase`（cart+order）/ `customer` / `backoffice` / `shared`

各モジュールのレイヤー:
```
domain/      → エンティティ・値オブジェクト・ドメインサービス
application/ → UseCase インターフェース（port）・UseCase 実装（パッケージプライベート）
adapter/     → controller・repository・外部サービス実装
```

**モジュール間依存ルール**（ArchUnit で検証）:
- 他モジュールの `domain.*` 直接参照禁止 → `application.port.*` 経由のみ
- UseCase 実装クラスはパッケージプライベート（`public class` 不可）
- クロスモジュール JPA 関連禁止 → 参照は ID のみ

## Lombok

全クラスで使用。以下が頻出:
- エンティティ・DTO: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- サービス: `@RequiredArgsConstructor`
- 例外の追加フィールド: `@Getter`

## 例外処理

カスタム例外をスローすると `GlobalExceptionHandler` が HTTP レスポンスに変換する:

| 例外クラス | HTTPステータス | 用途 |
|-----------|---------------|------|
| `BusinessException` | 400 | ビジネスルール違反 |
| `ResourceNotFoundException` | 404 | リソース不在 |
| `ConflictException` | 409 | 競合 |
| `InsufficientStockException` (extends `ConflictException`) | 409 | 在庫不足（details付き） |

サブクラス例外を追加する場合は `GlobalExceptionHandler` に専用ハンドラーを追加し、親クラスのハンドラーより上に配置する。

## APIレスポンス形式

全エンドポイントが `ApiResponse<T>` を返す:

```java
ApiResponse.success(data)                          // 成功
ApiResponse.error(code, message)                   // エラー
ApiResponse.errorWithDetails(code, message, details) // 詳細付きエラー
```

## DTO変換

エンティティ → DTO は `fromEntity()` 静的メソッドで行う。
