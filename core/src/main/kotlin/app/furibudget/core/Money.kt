package app.furibudget.core

/** Money is always whole minor units (centavos, cents). Never floating point. */
@JvmInline
value class Money(val minor: Long) {
    operator fun plus(o: Money) = Money(minor + o.minor)
    operator fun minus(o: Money) = Money(minor - o.minor)
    operator fun unaryMinus() = Money(-minor)
    operator fun compareTo(o: Money) = minor.compareTo(o.minor)

    companion object { val ZERO = Money(0) }
}
