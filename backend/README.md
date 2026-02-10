# Backend (Spring Boot)

## 技術スタック
- Java 21
- Spring Boot
- SQLite

## 開発環境セットアップ

### Dockerで起動
```bash
# プロジェクトルートで
docker-compose up backend
```

### ローカルで起動
```bash
cd backend
./mvnw spring-boot:run
```

## API エンドポイント
- ポート: 8080
- ベースURL: http://localhost:8080

詳細は `/docs/api-spec.md` を参照
