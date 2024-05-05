typealias DummyFun<T> = () -> T

fun Logger(dummyFun: DummyFun<Unit>) =
    NamedLogger(classNameResolver(dummyFun))

private fun classNameResolver(dummyFun: DummyFun<Unit>) =
    dummyFun.javaClass.enclosingClass.name

private fun <T> funNameResolver(dummyFun: DummyFun<T>) =
    dummyFun.javaClass.enclosingMethod.name

class NamedLogger(
    private val name: String
) {
    fun log(block: () -> String) =
        println("[$name][${funNameResolver(block)}] ${block()}")

    fun logFunName(dummyFun: DummyFun<Unit>) =
        log { funNameResolver(dummyFun) }
}