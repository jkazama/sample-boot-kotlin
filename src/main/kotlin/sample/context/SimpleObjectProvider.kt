package sample.context

import org.springframework.beans.factory.ObjectProvider

/** 常に単一の値を返す ObjectProvider */
class SimpleObjectProvider<T>(val value: T? = null): ObjectProvider<T> {
    override fun getObject(): T = value!!
    override fun getObject(vararg args: Any?): T = value!!
    override fun getIfAvailable(): T? = value
    override fun getIfUnique(): T? = value
}