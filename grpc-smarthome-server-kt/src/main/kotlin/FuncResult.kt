sealed class FuncResult<T>(val data: T)

class Failure<T>(data: T) : FuncResult<T>(data)

class Success<T>(data: T) : FuncResult<T>(data)

infix fun <T> FuncResult<T>.orElse(block: T.() -> Unit) {
    if (this is Failure<T>) {
        data.block()
    }
}