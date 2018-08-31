package sample.model.constraints;

/**
 * 正規表現定数インターフェース。
 * <p>Checker.matchと組み合わせて利用してください。
 */
public interface JavaRegex {
    /** Ascii */
    String rAscii = "^\\p{ASCII}*$";
    /** 文字 */
    String rWord = "^(?s).*$";
}
