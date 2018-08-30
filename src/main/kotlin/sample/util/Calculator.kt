package sample.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicReference

/**
 * 計算ユーティリティ。
 * <p>単純計算の簡易化を目的とした割り切った実装なのでスレッドセーフではありません。
 */
class Calculator {
    private val value = AtomicReference<BigDecimal>()
    /** 小数点以下桁数  */
    private var scale: Int = 0
    /** 端数定義。標準では切り捨て  */
    private var mode: RoundingMode = RoundingMode.DOWN
    /** 計算の都度端数処理をする時はtrue  */
    private var roundingAlways: Boolean = false

    /** 計算結果をint型で返します。 */
    fun intValue(): Int = decimal().toInt()

    /** 計算結果をlong型で返します。 */
    fun longValue(): Long = decimal().toLong()

    /** 計算結果をBigDecimal型で返します。 */
    fun decimal(): BigDecimal {
        val v = value.get()
        return if (v != null) v.setScale(scale, mode) else BigDecimal.ZERO
    }

    private constructor(v: Number) {
        try {
            this.value.set(BigDecimal(v.toString()))
        } catch (e: NumberFormatException) {
            this.value.set(BigDecimal.ZERO)
        }
    }

    private constructor(v: BigDecimal) {
        this.value.set(v)
    }

    /**
     * 計算前処理定義。
     * @param scale 小数点以下桁数　
     * @return 自身のインスタンス
     */
    fun scale(scale: Int): Calculator =
            scale(scale, RoundingMode.DOWN)

    /**
     * 計算前処理定義。
     * @param scale 小数点以下桁数
     * @param mode 端数定義
     */
    fun scale(scale: Int, mode: RoundingMode): Calculator {
        this.scale = scale
        this.mode = mode
        return this
    }

    /**
     * 計算前の端数処理定義をします。
     * @param roundingAlways 計算の都度端数処理をする時はtrue
     */
    fun roundingAlways(roundingAlways: Boolean): Calculator {
        this.roundingAlways = roundingAlways
        return this
    }

    /** 与えた計算値を自身が保持する値に加えます。  */
    fun add(v: Number): Calculator {
        try {
            add(BigDecimal(v.toString()))
        } catch (e: NumberFormatException) {
        }
        return this
    }

    /** 与えた計算値を自身が保持する値に加えます。 */
    fun add(v: BigDecimal): Calculator {
        value.set(rounding(value.get().add(v)))
        return this
    }

    private fun rounding(v: BigDecimal): BigDecimal =
            if (roundingAlways) v.setScale(scale, mode) else v

    /** 自身が保持する値へ与えた計算値を引きます。 */
    fun subtract(v: Number): Calculator {
        try {
            subtract(BigDecimal(v.toString()))
        } catch (e: NumberFormatException) {
        }
        return this
    }

    /** 自身が保持する値へ与えた計算値を引きます。  */
    fun subtract(v: BigDecimal): Calculator {
        value.set(rounding(value.get().subtract(v)))
        return this
    }

    /** 自身が保持する値へ与えた計算値を掛けます。 */
    fun multiply(v: Number): Calculator {
        try {
            multiply(BigDecimal(v.toString()))
        } catch (e: NumberFormatException) {
        }
        return this
    }

    /** 自身が保持する値へ与えた計算値を掛けます。 */
    fun multiply(v: BigDecimal): Calculator {
        value.set(rounding(value.get().multiply(v)))
        return this
    }

    /** 与えた計算値で自身が保持する値を割ります。 */
    fun divideBy(v: Number): Calculator {
        try {
            divideBy(BigDecimal(v.toString()))
        } catch (e: NumberFormatException) {
        }
        return this
    }

    /** 与えた計算値で自身が保持する値を割ります。 */
    fun divideBy(v: BigDecimal): Calculator {
        val ret = when {
            roundingAlways -> value.get().divide(v, scale, mode)
            else -> value.get().divide(v, defaultScale, mode)
        }
        value.set(ret)
        return this
    }

    companion object {
        /** scale未設定時の除算scale値  */
        const val defaultScale: Int = 18

        /** 開始値0で初期化されたCalculator  */
        fun init(): Calculator = Calculator(BigDecimal.ZERO)

        /**
         * @param v 初期値
         * @return 初期化されたCalculator
         */
        fun of(v: Number): Calculator = Calculator(v)

        /**
         * @param v 初期値
         * @return 初期化されたCalculator
         */
        fun of(v: BigDecimal): Calculator = Calculator(v)

    }

}