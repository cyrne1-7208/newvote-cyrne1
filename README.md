# NewVotePlugin

投票の進行、投票状況の可視化、結果表示を行う Minecraft サーバープラグインです。

`/vs` で投票を開始し、`/v` で投票、`/vget` で投票先を開示できます。Tab リスト上に投票状態を表示できる点が特徴です。

## 特徴

- **シンプルな投票フロー** - `/vs` で開始、`/v <対象名>` で投票、`/vs` で締切
- **Tab リスト可視化** - 未投票/投票済みや投票先を Tab リストで確認
- **投票先固定モード** - `config.yml` の `List` に候補を設定可能
- **やなー演出モード** - 条件に応じて専用演出を表示 (`/yvote`)
- **Tab 補完対応** - `/v` 入力時に候補を補完

## 必要なもの

- Java 8 以上
- Paper / Spigot サーバー
- Maven 3.8 以上 (ビルド時)

## インストール

```powershell
git clone https://github.com/cyrne1-7208/NewVote-cyrne1.git
cd NewVote-cyrne1
mvn -DskipTests package
```

生成された `target/NewVote-8.77778.jar` をサーバーの `plugins` フォルダに配置してください。

## 使い方

```text
1. /vs で投票を開始
2. 各プレイヤーが /v <投票先名> で投票
3. /vs で投票締切と結果表示
4. /vget で投票者ごとの投票先を Tab リストに開示
5. 必要に応じて /vs で Tab 表示をクリア
```

## コマンド一覧

| コマンド | 権限 | 説明 |
|----------|------|------|
| `/v <投票先>` | `newvote.v` | 投票する |
| `/vs` | `newvote.vs` (OP) | 投票開始 / 締切 / Tab クリア |
| `/vget` | `newvote.vs` (OP) | 投票先を開示 |
| `/yvote` | `newvote.v` | `Yanaaaaa` 用の特殊モード切替 |

## 設定 (`config.yml`)

| キー | デフォルト | 説明 |
|------|-----------|------|
| `List` | 空 | 投票候補の固定リスト。空の場合はオンラインプレイヤーを使用 |

例:

```yaml
List:
	- "Alice"
	- "Bob"
	- "Charlie"
```

## 権限

| 権限ノード | デフォルト |
|------------|-----------|
| `newvote.v` | true |
| `newvote.vs` | op |

## 謝辞

- やにゃー版のフォーク元: [TeamKun/NewVote-YanaaaaaEdition](https://github.com/TeamKun/NewVote-YanaaaaaEdition)
- 本プロジェクトは上記フォーク元をベースに作成しています。

- AI である [GPT-5.3-Codex](https://openai.com/codex)（OpenAI）の支援を受けて開発されました。

## ライセンス

MIT - [LICENSE](LICENSE) を参照してください。