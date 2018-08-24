package sample.context.orm

import sample.context.Entity
import java.time.LocalDateTime

/**
 * OrmActiveRecordに登録/変更メタ概念を付与した基底クラス。
 * 本クラスを継承して作成されたEntityは永続化時に自動的なメタ情報更新が行われます。
 * @see OrmInterceptor
 */
abstract class OrmActiveMetaRecord<T: Entity>(
        open var createId: String? = null,
        open var createDate: LocalDateTime? = null,
        open var updateId: String? = null,
        open var updateDate: LocalDateTime? = null
) : OrmActiveRecord<T>()
