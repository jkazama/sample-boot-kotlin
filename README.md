sample-boot-kotlin
---

### はじめに

[Spring Boot](http://projects.spring.io/spring-boot/) / [Spring Security](http://projects.spring.io/spring-security/) / [Kotlin](https://kotlinlang.org/) を元にした DDD サンプル実装です。  
ベーシックな基盤は Java 版である [sample-boot-hibernate](https://github.com/jkazama/sample-boot-hibernate) から流用しています。  
フレームワークではないので、 Spring Boot を利用するプロジェクトを立ち上げる際に元テンプレートとして利用して下さい。

考え方の骨子については以前発表した資料 ( [Spring Bootを用いたドメイン駆動設計](http://www.slideshare.net/jkazama/jsug-20141127) ) を参照してください。

UI 側の実装サンプルについては [sample-ui-vue](https://github.com/jkazama/sample-ui-vue) / [sample-ui-react](https://github.com/jkazama/sample-ui-react) を参照してください。

---

本サンプルは Spring Boot を用いたドメインモデリングの実装例としても利用できます。実際にそれなりの規模の案件で運用されていた実装アプローチなので、モデリングする際の参考事例としてみてもらえればと思います。  

*※ JavaDoc に記載をしていますが参考実装レベルです。製品水準のコードが含まれているわけではありません。*

#### レイヤリングの考え方

オーソドックスな三層モデルですが、横断的な解釈としてインフラ層を考えています。

| レイヤ          | 特徴                                                        |
| -------------- | ----------------------------------------------------------- |
| UI             | ユースケース処理を公開 ( 必要に応じてリモーティングや外部サイトを連携 ) |
| アプリケーション | ユースケース処理を集約 ( 外部リソースアクセスも含む )                 |
| ドメイン        | 純粋なドメイン処理 ( 外部リソースに依存しない )                      |
| インフラ        | DI コンテナや ORM 、各種ライブラリ、メッセージリソースの提供          |

UI 層の公開処理は通常 JSP や Thymeleaf を用いて行いますが、本サンプルでは異なる種類のクライアント利用を想定して RESTfulAPI での API 提供のみをおこないます。 ( 利用クライアントは別途用意する必要があります )

#### Spring Boot の利用方針

Spring Boot は様々な利用方法が可能ですが、本サンプルでは以下のポリシーで利用します。

- 設定ファイルは yml を用いる。 Bean 定義に xml 等の拡張ファイルは用いない。
- 登録するBeanは起動速度/保守性の両面からコンポーネントスキャンは限定し、他は定義クラスで補完
    - `ApplicationConfig` / `ApplicationDbConfig` / `ApplicationSecurityConfig`
    - `controller` / `usecase` 配下のみ自動スキャンを有効に
- 例外処理は終端 ( RestErrorAdvice / RestErrorCotroller ) で定義。 whitelabel 機能は無効化。
- JPA 実装として Hibernate に特化。
- Spring Security の認証方式はベーシック認証でなく、昔からよくある HttpSession で。
- 基礎的なユーティリティで Spring がサポートしていないのは簡易な実装を用意。

#### Kotlin コーディング方針

- GradleのKotlinプラグインを積極的に利用
    - `kotlin-spring` / `kotlin-jpa`
- DIコンテナへ配置するシングルトンな Bean は通常 class で定義し @Component を付与
    - `kotlin-spring` の恩恵で open が自動的に適用される
- 要求引数や戻り値、エンティティ等のデータ転送オブジェクト(DTO)は data class で定義
- DI はコンストラクタインジェクションを中心におこなう
- Bean Validation アノテーションがそのままだとうまく適用されないので @field を付与
    - 拡張Bean Validation アノテーションは作り方が良く分からなかったためjavaの方で作成
- Bean Validation を利用するDTOクラスのフィールドは全て null を許容
    - 評価する際に null 状態を一次的に受け入れる必要があるため
- Entityにおいては副作用ある処理を許容し、業務要求となるDTO引数などでは許容しない
    - Entityへ副作用を与える要求は全て業務メソッド経由に集約する


#### パッケージ構成

パッケージ/リソース構成については以下を参照してください。

```
main
  kotlin
    sample
      context                         … インフラ層
      controller                      … UI 層
      model                           … ドメイン層
      usecase                         … アプリケーション層
      util                            … 汎用ユーティリティ
      - Application.kt                … 実行可能な起動クラス
  resources
    - application.yml                 … 設定ファイル
    - ehcache.xml                     … Spring Cache 設定ファイル
    - logback-spring.xml              … ロギング設定ファイル
    - messages-validation.properties  … 例外メッセージリソース
    - messages.properties             … メッセージリソース
```

### サンプルユースケース

サンプルユースケースとしては以下のようなシンプルな流れを想定します。

- **口座残高 100 万円を持つ顧客**が出金依頼 ( 発生 T, 受渡 T + 3 ) をする。
- **システム**が営業日を進める。
- **システム**が出金依頼を確定する。(確定させるまでは依頼取消行為を許容)
- **システム**が受渡日を迎えた入出金キャッシュフローを口座残高へ反映する。

### 動作確認

本サンプルは [Gradle](https://gradle.org/) を利用しているので、 IDE やコンソールで手間なく動作確認を行うことができます。

*※ライブラリダウンロードなどが自動で行われるため、インターネット接続が可能な端末で実行してください。*

#### サーバ起動 （ IntelliJ IDEA ）

[IntelliJ IDEA](https://www.jetbrains.com/idea/)を利用して起動するには、次の手順で本サンプルをプロジェクト化してください。  

1. 「 File -> New -> Project From Existing Sources... 」 で本サンプルの build.gradle を選択して *OK* を押下

次の手順で本サンプルを実行してください。

1. *Application.kt* に対し 「 右クリック -> Run `sample.Application.kt...` 」
1. *Console* タブに 「 Started Application 」 という文字列が出力されればポート 8080 で起動が完了
1. ブラウザを立ち上げて 「 http://localhost:8080/management/health 」 で状態を確認

#### サーバ起動 （ コンソール ）

Windows / Mac のコンソールから実行するには Gradle のコンソールコマンドで行います。  

*※事前に JDK8 以上のインストールが必要です。*

1. ダウンロードした *sample-boot-kotlin* ディレクトリ直下へコンソールで移動
1. 「 gradlew bootRun 」 を実行
1. コンソールに 「 Started Application 」 という文字列が出力されればポート 8080 で起動が完了
1. ブラウザを立ち上げて 「 http://localhost:8080/management/health 」 で状態を確認

#### クライアント検証

Eclipse またはコンソールでサーバを立ち上げた後、 test パッケージ配下にある `SampleClient` の各検証メソッドをユニットテストで実行してください。

##### 顧客向けユースケース

| URL                              | 処理                 | 実行引数 |
| -------------------------------- | ------------------- | ------------- |
| `/api/asset/cio/withdraw`        | 振込出金依頼          | [`accountId`: sample, `currency`: JPY, `absAmount`: 依頼金額] |
| `/api/asset/cio/unprocessedOut/` | 振込出金依頼未処理検索 | -       |

*※振込出金依頼はPOST、それ以外はGET*

##### 社内向けユースケース

| URL                     | 処理             | 実行引数                                           |
| ----------------------- | --------------- | ------------------------------------------------- |
| `/api/admin/asset/cio/` | 振込入出金依頼検索 | [`updFromDay`: yyyy-MM-dd, `updToDay`: yyyy-MM-dd]|

*※GET*

##### バッチ向けユースケース

| URL                                     | 処理                                          | 実行引数 |
| --------------------------------------- | --------------------------------------------- | ------ |
| `/api/system/job/daily/processDay`      | 営業日を進める(単純日回しのみ)                    | -      |
| `/api/system/job/daily/closingCashOut`  | 当営業日の出金依頼を締める                        | -      |
| `/api/system/job/daily/realizeCashflow` | 入出金キャッシュフローを実現する(受渡日に残高へ反映) | -      |

*※POST*

### 配布用jarの作成

Spring Boot では Executable Jar ( ライブラリや静的リソースなども内包する jar ) を作成する事で単一の配布ファイルでアプリケーションを実行することができます。

1. コンソールから 「 gradlew build 」 を実行
1. `build/libs` 直下に jar が出力されるので Java8 以降の実行環境へ配布
1. 実行環境でコンソールから 「 java -jar xxx.jar 」 を実行して起動

> 実行引数に 「 --spring.profiles.active=[プロファイル名]」 を追加する事で application.yml の設定値をプロファイル単位に変更できます。

### 依存ライブラリ

> 実際の詳細な定義は `build.gradle` を参照してください

### 補足解説（インフラ層）

インフラ層の簡単な解説です。

※より詳細な概要についてはコードに記載されているコメントを参照してください

#### DB / トランザクション

`sample.context.orm` 直下。ドメイン実装をより Entity に寄せるための ORM サポート実装です。 Repository ( スキーマ単位で定義 ) にドメイン処理を記載しないアプローチのため、 Spring Boot が提供する JpaRepository は利用していません。  
トランザクション定義はトラブルの種となるのでアプリケーション層でのみ許し、なるべく狭く限定した形で付与しています。単純な readOnly な処理のみ `@Transactional([Bean名称])` を利用してメソッド単位の対応を取ります。

スキーマは標準のビジネスロジック用途 ( `DefaultRepository` ) とシステム用途 ( `SystemRepository` ) の2種類を想定しています。 Entity 実装ではスキーマに依存させず、引数に渡す側 ( 主にアプリケーション層 ) で判断させます。

> Spring Data JPA が提供する JpaRepository を利用することも可能です。 ( 標準で DefaultRepository が管理しているスキーマへ接続します )

#### 認証/認可

`sample.context.security` 直下。顧客 ( ROLE_USER ) / 社員 ( ROLE_ADMIN ) の 2 パターンを想定しています。それぞれのユーザ情報 ( UserDetails ) 提供手段は `sample.usecase.SecurityService` において定義しています。

認証 / 認可の機能を有効にするには `application.yml` の `extension.security.auth.enabled` に `true` を設定してください ( 標準ではテスト用途にfalse ) 。顧客 / 社員それぞれ同一 VM での相乗りは考えていません。社員専用モードで起動する時は起動時のプロファイル切り替え等で `extension.security.auth.admin` を `true` に設定してください。

#### 利用者監査

`sample.context.audit` 直下。 「いつ」 「誰が」 「何をしたか」 の情報を顧客 /システムそれぞれの視点で取得します。アプリケーション層での利用を想定しています。ログインした `Actor` の種別 ( User / System ) によって書き出し先と情報を切り替えています。運用時に行動証跡を取る際に利用可能です。

#### 例外

汎用概念としてフィールド単位にスタックした例外を持つ `ValidationException` を提供します。  
例外は末端の UI 層でまとめて処理します。具体的にはアプリケーション層、ドメイン層では用途別の実行時例外をそのまま上位に投げるだけとし、例外捕捉は `sample.controller.RestErrorAdvise` において AOP を用いた暗黙的差し込みを行っています。

#### 日付/日時

`sample.context.Timestamper` を経由して Java8 で追加された `time` ライブラリを利用します。休日等を考慮した営業日算出はドメイン概念が含まれるので `sample.model.BusinessDayHandler` で別途定義しています。

#### キャッシング

`AccountService` 等で Spring が提供する @Cacheable を利用しています。 UI 層かアプリケーション層のどちらかに統一した方が良いですが、本サンプルではアプリケーション層だけ付与しています。 JPA のキャッシュ機構は Entity 内で必要となるケース以外では、利用しないことを推奨します。

#### テスト

パターンとしては通常の JUnitテストと、 Hibernate だけに閉じたEntityテスト（基底クラスは `EntityTestSupport` ）の合計 2 パターンで考えます。  

> Springのコンテナを利用したテストはコストが高くなりがちなので、ここではおこないません。

### License

本サンプルのライセンスはコード含めて全て *MIT License* です。  
Spring Boot を用いたプロジェクト立ち上げ時のベース実装サンプルとして気軽にご利用ください。
