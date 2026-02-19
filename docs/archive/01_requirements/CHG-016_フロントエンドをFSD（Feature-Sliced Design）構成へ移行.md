# CHG-015: フロントエンドをFSD（Feature-Sliced Design）構成へ移行

作成日: 2026-02-18
---
## 1. 背景
- フロントエンドの機能追加・改修が進むにつれて、画面・機能・ドメイン・共通基盤の責務が混在し、変更影響範囲が拡大しやすい。
- 「変更理由（ビジネス/UXの単位）」で分割し、依存方向を固定することで、スケールしやすい構造にしたい。

---
## 2. 要件
- フロントエンドのディレクトリ/モジュール構成をFSDのレイヤに沿って再編する。
  - app / pages / widgets / features / entities / shared のレイヤを採用する。
- 依存方向を「上位→下位」のみに統一し、逆方向参照を禁止する。
  - app → pages → widgets → features → entities → shared
- 機能（ユーザー行動）とドメイン（名詞）を分離する。
  - ユースケース（操作手順・ユーザー行動）は features
  - ドメイン実体（型・取得/変換・最小表示単位）は entities
- 画面の合成責務を pages / widgets に集約する。
- 共通基盤（UI部品・API基盤・型・utils・config）は shared に集約する。
- （参考）TanStack Query を利用する場合、GET系は entities、更新系は features に配置する方針とする。

---
## 3. 受け入れ条件
- src 配下が FSD の各レイヤ（app/pages/widgets/features/entities/shared）で構成されている。
- レイヤ依存ルール（上位→下位のみ）が定義され、違反が検出/防止できる状態になっている（例：ルール化・チェック導入など、方法は問わない）。
- 主要な画面が pages として整理され、画面内の大きなUIブロックは widgets へ分割されている。
- 代表的なユーザー行動（例：ログイン、カート追加、注文確定）が features として切り出されている。
- 代表的なドメイン（例：Product/Cart/Order）の型・API・最小UIが entities にまとまっている。
- 共通UI・共通API基盤・共通型/utility が shared に集約されている。

---
## 4. 仕様への反映
- フロントエンドの構成方針として FSD を採用する旨を、設計資料/リポジトリ規約に追記する。
- ディレクトリ構成・命名規約・責務分担（features/entities/shared の境界、pages/widgets の合成責務）を明文化する。
- レイヤ依存ルール（上位→下位のみ）を開発ルールとして明記する。

---
## 5. 関連資料
- ユーザー提示の「FSD（Feature-Sliced Design）簡易まとめ（React向け）」メモ
