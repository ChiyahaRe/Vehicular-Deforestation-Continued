# Vehicular Deforestation Continued

*A Sable 2.0.5 compatibility fix for Tree Overrun Sublevels.*

An unofficial compatibility patch for **Sable Vehicular Deforestation**
(`tree_overrun_sublevels`) 1.21.1-0.6.0 by **Leonardoinc22**, updated to work with
**Sable 2.0.5**.

The original mod hard-locks the Minecraft server thread the moment a sub-level touches a
tree. This repository contains the fix and the reasoning behind it.

> **Status:** unofficial. The upstream distribution has been taken down; this was
> reconstructed by decompiling the last released jar. See *Provenance* below.

---

## 症状

Sable のサブレベル（機体）を木にぶつけた瞬間、**ゲーム内 tick が完全に停止する**。
クライアントの描画は続くため、画面は動いたまま世界だけが止まる。

## 原因

`Rapier3D.step()` の実行中に呼ばれる衝突コールバックの中から、**別のリジッドボディに対して
`Rapier3D.getLinearVelocity()` を呼び返していた**ことによる、ネイティブ層での再入デッドロック。

フリーズ中のスレッドダンプ（16 秒間・8 回サンプリングで完全に同一、CPU 時間の進みは 0.21 ms）:

```
Rapier3D.step                                     ← 物理ステップ実行中（シーンをロック）
  BlockSubLevelCollisionCallback.onCollision
    FragileBlockCallback.sable$onCollision
      TreeLogCollisionCallback.onHit
        TreeLogCollisionCallback.lookupImpactingSubLevel
          TreeOverrunHandler.findImpactingVehicle
            RigidBodyHandle.getLinearVelocity
              Rapier3D.getLinearVelocity          ← ここで永久ブロック
```

CPU をほとんど消費していないことから、無限ループでも重い計算でもなく、ロック待ちである
ことが確定する。

### なぜ Sable 更新で発生したか

元の MOD は、これら3つが成立する前提で書かれている。Sable 2.0.5 ではいずれも成立しない。

| 前提 | Sable 2.0.5 での実際 |
|---|---|
| `sable$onCollision` は引数3個 | **引数4個**（`BlockPos, BlockPos, Vector3d, double`）。旧シグネチャのオーバーライドは成立せず死にコード化する |
| `getCurrentlySteppingSystem()` はステップ外で `null` を返す | **`IllegalStateException` を投げる** |
| 衝突コールバックは物理ステップの外で呼ばれる | **`Rapier3D.step()` の内側**で呼ばれる |

## 修正内容

### 1. 衝突コールバックから物理シーンへの呼び出しを全廃

`Rapier3D.step()` 中は `getLinearVelocity` / `queryIntersecting` / `getPhysicsHandle` を
一切呼ばない。必要な情報はすべて Sable がコールバック引数で渡してくれる。

- **衝突速度** — `sable$onCollision` の第4引数をそのまま使う
- **衝突相手の特定** — 第2引数 `otherHitBlockPos` は相手サブレベルのプロット内ブロックなので、
  `Sable.HELPER.getContaining()` でブロードフェーズ検索なしに機体を正確に特定できる
- **質量** — `MassData.getMass()` はバイトコード上 `getfield mass:D` の単純なフィールド読み出しで、
  ネイティブ呼び出しを含まない。よってコールバック内で安全に評価できる

質量スケール判定（`requiredImpactSpeedForMass`）は**コールバック内で行う必要がある**。
遅延させると、閾値に満たない軽い機体が `passThrough` のまま幹をすり抜けてしまうため。

### 2. 速度の引き継ぎだけを物理ステップ後へ遅延

破片サブレベルへ引き継ぐ速度の取得（`getLinearVelocity`）のみ、サーバー tick 側の
`TreeAssemblyQueue.processPendingImpacts` で行う。

### 3. 例外を投げる API の排除

`getCurrentlySteppingSystem()` の呼び出しを全廃し、例外を投げない公開フィールド
`SubLevelPhysicsSystem.currentlySteppingSystem`（ステップ外では `null`）と
`IN_PHYSICS_STEP` に置き換えた。Sable は `postPhysicsTicks` でも衝突エフェクトを処理するため、
コールバック経路でもステップ中である保証がない。

### 4. 3引数オーバーライドの修正

`TreeLeafFragileCallback` と `ChainedFragileBlockCallback` の `sable$onCollision` を
4引数に修正。これらは Sable 2.0.5 に対してオーバーライドが成立しておらず、伐採確保済みの葉を
通過させる保護が丸ごと効いていなかった。

### 変更したクラス

| クラス | 変更 |
|---|---|
| `physics/TreeLogCollisionCallback` | 全面書き換え。物理呼び出しの排除、衝突相手の特定、質量判定 |
| `physics/TreeOverrunHandler` | `enqueueTreeImpact` に車両 UUID を追加、ステップ中ガード |
| `physics/TreeAssemblyQueue` | `resolveImpactingVehicle` を新設し、速度引き継ぎを遅延 |
| `physics/TreeLeafFragileCallback` | シグネチャ修正 |
| `compat/ChainedFragileBlockCallback` | シグネチャ修正、衝突速度の受け渡し |
| `compat/TreePhysicsCompat` | `subLevelSpeed` にステップ中ガード |

## 設定

必要衝突速度は質量でスケールする。

```
必要速度 = max(0.2, minImpactSpeed × √(impactMassReference ÷ 質量))
```

`config/tree_overrun_sublevels-server.toml` の `minImpactSpeed` と `impactMassReference` は
積でしか効かないため、「基準質量 X のとき必要速度 Y」と読める組み合わせにしておくと扱いやすい。

```toml
[impact]
	minImpactSpeed = 5.66        # 質量 8 のとき 5.66 が必要
	impactMassReference = 8.0
```

下限 `0.2` は `TreeOverrunSettings.MIN_EFFECTIVE_IMPACT_SPEED` としてコードに埋め込まれている。

## ビルド

Gradle 構成は付属しない。upstream のビルドスクリプトが失われているため、
ローカルの依存 jar に対して直接コンパイルする `build.sh` を用意した。

```bash
./build.sh
```

必要な jar（パスはスクリプト冒頭で指定する）:

- `sable-neoforge-1.21.1-2.0.5.jar` と、その `META-INF/jarjar/` 内の
  `sable_rapier` / `sable-companion-common`
- Minecraft 1.21.1 クライアント jar（Mojang 公式マッピング）
- NeoForge 21.1.x
- `joml`, `fastutil`, `slf4j-api`, `datafixerupper`

`neoforge/` と `mixin/` パッケージは変更していないため、`build.sh` は差分クラスのみを
コンパイルし、元の jar に差し替えて出力する。これにより元 jar とのエントリ構成の一致を保てる。

## 既知の未修正問題

フリーズとは無関係のため、この修正には含めていない。

- `TreeLeafHelper.isOwnedByLogCluster` が `O(|logs| × |foreignLogs|)` で、近傍候補ごとに
  呼ばれる。却下された候補が `visited` に入らないため再評価も繰り返す。密林でのラグ要因
- `TreeOverrunSettings.maxActiveDebrisSubLevels` のフィールド初期値が `0`（＝上限なし）。
  config の既定値 `200` と食い違っており、config 適用前は上限が効かない
- `TreeAssemblyQueue.processPendingImpacts` が、他ディメンションの保留インパクトを削除する
- `PhysicsDeferredSync.flushBreaks` で、コンテナから引けなかった ID がマップに残り続ける
- `COOLDOWNS` / `UPROOT_CLAIMS` / `PENDING_*` がワールド切り替えで初期化されない

## Provenance

元 MOD の配布元は削除済み。本リポジトリのソースは、最後に配布された
`tree-overrun-sublevels-1.21.1-0.6.0.jar` を CFR 0.152 で逆コンパイルしたものに、
上記の修正を加えたもの。逆コンパイラのバナーは出自を示すため各ファイル先頭に残してある。

元 MOD の `META-INF/neoforge.mods.toml` に記載されたライセンスは **Unlicense**
（パブリックドメイン相当）であり、再配布および派生物の公開が認められている。

- Original mod: **Sable Vehicular Deforestation** by **Leonardoinc22**
- Original license: **Unlicense**
- Requires: [Sable](https://modrinth.com/mod/sable) 2.0.5, Minecraft 1.21.1, NeoForge 21.1.x

このリポジトリは作者とは無関係の非公式パッチであり、作者による承認を受けたものではない。
