package dev.dexsr.klio.base.storage

// TODO: Bits
// TODO: we should probably put overflow flag, but currently we have no use case
// TODO: tests
sealed class ByteUnit(
    private val amount: Long,
    private val byteFactor: Long,
) {

    init {
        check(byteFactor >= B) {
            "byteFactor cannot be less than 1"
        }
    }

    val abbreviation: String
        get() = when(this) {
            is Bytes -> "B"
            is KiloBytes -> "KB"
            is MegaBytes -> "MB"
            is GigaBytes -> "GB"
            is TeraBytes -> "TB"
        }

    fun bytes(): Bytes = this as? Bytes ?: Bytes(byFactor(B))

    fun kiloBytes(): KiloBytes = this as? KiloBytes ?: KiloBytes(byFactor(KB))

    fun megaBytes(): MegaBytes = this as? MegaBytes ?: MegaBytes(byFactor(MB))

    fun gigaBytes(): GigaBytes = this as? GigaBytes ?: GigaBytes(byFactor(GB))

    fun teraBytes(): TeraBytes = this as? TeraBytes ?: TeraBytes(byFactor(TB))

    private fun byFactor(factor: Long): Long {
       check(factor != 0L)

        return if (byteFactor > factor)
            // higher factor means this is higher unit, possible Long overflow
            checkedUnderFactorOverflow(amount, byteFactor / factor)
        else
            // lower factor means this is lower unit, a division towards 0
            amount / (factor / byteFactor)
    }

    private fun checkedUnderFactorOverflow(
        amount: Long,
        byteFactor: Long,
    ): Long {
        if (amount == 0L) return 0L

        if (byteFactor > Long.MAX_VALUE / amount) {
            return Long.MAX_VALUE
        }

        if (byteFactor < Long.MIN_VALUE / amount) {
            return Long.MIN_VALUE
        }

        return amount * byteFactor
    }

    private fun checkedIntOverflow(): Int = when {
        amount >= Int.MAX_VALUE -> Int.MAX_VALUE
        amount <= Int.MIN_VALUE -> Int.MIN_VALUE
        else -> amount.toInt()
    }

    fun toLong(): Long = amount

    fun toInt(): Int = checkedIntOverflow()

    // make extension function for each use-case instead of making it public
    internal fun toIntUnsafe(): Int = amount.toInt()

    fun willIntOverflow(): Boolean = amount > Int.MAX_VALUE || amount < Int.MIN_VALUE

    operator fun compareTo(other: ByteUnit): Int {
        return when {
            this.byteFactor > other.byteFactor -> {
                val amountC = checkedUnderFactorOverflow(amount, byteFactor / other.byteFactor)
                    .compareTo(other.amount)
                return amountC
            }
            this.byteFactor < other.byteFactor -> {
                val amountC = amount
                    .compareTo(other.checkedUnderFactorOverflow(other.amount, other.byteFactor / byteFactor))
                return amountC
            }
            else -> {
                val amountC = amount
                    .compareTo(other.amount)
                return amountC
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ByteUnit) return false

        return other.amount == this.amount &&
                other.byteFactor == this.byteFactor
    }

    override fun hashCode(): Int {
        var result = 0
        result += amount.hashCode()
        result *= 31 ; result += byteFactor.hashCode()
        return result
    }

    override fun toString(): String {
        return "ByteUnit: $amount $abbreviation"
    }

    class Bytes(amount: Long) : ByteUnit(amount, B)

    class KiloBytes(amount: Long) : ByteUnit(amount, KB)

    class MegaBytes(amount: Long) : ByteUnit(amount, MB)

    class GigaBytes(amount: Long) : ByteUnit(amount, GB)

    class TeraBytes(amount: Long) : ByteUnit(amount, TB)

    // ..

    companion object {
        const val B = 1L
        const val KB = 1000L
        const val MB = 1000_000L
        const val GB = 1000_000_000L
        const val TB = 1000_000_000_000L

        // ..

        fun fromBits(amount: Long) = Bytes(amount / 8)
    }
}

fun Int.kbUnit() = kiloByteUnit()
fun Int.mbUnit() = megaByteUnit()
fun Int.gbUnit() = gigaByteUnit()
fun Int.tbUnit() = teraByteUnit()

fun Long.kbUnit() = kiloByteUnit()
fun Long.mbUnit() = megaByteUnit()
fun Long.gbUnit() = gigaByteUnit()
fun Long.tbUnit() = teraByteUnit()

fun Int.bitByteUnit() = toLong().bitByteUnit()
fun Int.byteUnit() = toLong().byteUnit()
fun Int.kiloByteUnit() = toLong().kiloByteUnit()
fun Int.megaByteUnit() = toLong().megaByteUnit()
fun Int.gigaByteUnit() = toLong().gigaByteUnit()
fun Int.teraByteUnit() = toLong().teraByteUnit()

fun Int.kiloByteInBytes(): Long = kiloByteUnit().byteToLong()

fun Long.bitByteUnit() = ByteUnit.fromBits(this)
fun Long.byteUnit() = ByteUnit.Bytes(this)
fun Long.kiloByteUnit() = ByteUnit.KiloBytes(this)
fun Long.megaByteUnit() = ByteUnit.MegaBytes(this)
fun Long.gigaByteUnit() = ByteUnit.GigaBytes(this)
fun Long.teraByteUnit() = ByteUnit.TeraBytes(this)

fun ByteUnit.byteToLong() = bytes().toLong()

inline fun ByteUnit.checkNoIntOverflow(
    lazyMessage: () -> Any
): ByteUnit {
    check(!willIntOverflow(), lazyMessage)
    return this
}

inline fun ByteUnit.checkNoIntOverflow(): ByteUnit {
    checkNoIntOverflow {
        "check failed: ${toString()} will Int Overflow"
    }
    return this
}

fun ByteUnit.toIntCheckNoOverflow(): Int {
    return checkNoIntOverflow().toIntUnsafe()
}
