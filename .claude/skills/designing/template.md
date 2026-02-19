# CHG-XXX: <機能名> - 技術設計

要件: `docs/01_requirements/CHG-XXX_<機能名>.md`
作成日: YYYY-MM-DD

**SSOT（唯一の真実）**:
- API契約: このドキュメントの「API契約」セクション（OpenAPIなし）
- 状態遷移: `モジュール名/entity/XxxStatus` enum（Javaコードが正）
- DBスキーマ: `db/flyway/VX__xxx.sql`（Flywayが正）
- モジュール境界: `backend/AGENTS.md` + ArchUnit テスト

---

## 1. 設計方針

（Why この設計か。既存パターンとの整合性方針、採用/非採用の判断理由）

## 2. API契約

（エンドポイント、リクエスト/レスポンス構造、HTTPステータス、エラーコード）
（ここで固定したものは Codex が勝手に変えてはいけない契約）

## 3. モジュール・レイヤ構成

（影響するモジュール一覧、レイヤ依存方向）

```
module/
  domain/      - エンティティ・リポジトリIF
  application/ - UseCase・Port・Publisher
  adapter/     - Controller・Handler
```

## 4. 主要クラス/IFの責務

（登場するクラス・インターフェースの責務を一覧。契約として固定するシグネチャのみ最小限で示す）

| クラス/IF | 責務 | レイヤ |
|-----------|------|--------|
| XxxUseCase | ... | application |
| XxxHandler | ... | adapter |

## 5. トランザクション・非同期方針

（トランザクション境界の定義、非同期方式の選択理由、リトライ/冪等性方針）

## 6. 処理フロー

（登場コンポーネントと責務の流れを「箱と矢印」で示す。if/for・メソッド呼び順の逐次記述はしない）

```
Request → Controller → UseCase → [DB write + Outbox write] → Response
                                      ↓ (async polling, 5s)
                                OutboxProcessor → Handler
```

## 7. 影響範囲

（何を触るか。どう触るかは書かない）

| 区分 | 対象（クラス名）| 変更概要 |
|------|----------------|---------|
| 新規作成 | `module/ClassName` | ... |
| 既存変更 | `module/ClassName` | （概要のみ。手順は書かない） |
| 影響なし | ... | |

## 8. テスト観点

（テストすべき境界条件・異常系の列挙。実装手順は書かない）

- 正常系: ...
- 異常系: ...
- 境界値: ...
