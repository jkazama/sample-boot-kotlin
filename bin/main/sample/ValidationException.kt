package sample

/**
 * 審査例外を表現します。
 * <p>ValidationExceptionは入力例外や状態遷移例外等の復旧可能な審査例外です。
 * その性質上ログ等での出力はWARNレベル(ERRORでなく)で行われます。
 * <p>審査例外はグローバル/フィールドスコープで複数保有する事が可能です。複数件の例外を取り扱う際は
 * Warnsを利用して初期化してください。
 */
class ValidationException {
}