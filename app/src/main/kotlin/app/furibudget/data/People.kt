package app.furibudget.data

import java.time.LocalDate

/** Where one loan stands: what was lent or borrowed, what has come back, and what is still open. */
data class LoanStatus(val loan: LoanEntity, val repaidMinor: Long) {
    val outstandingMinor: Long get() = (loan.principalMinor - repaidMinor).coerceAtLeast(0)
    val settled: Boolean get() = outstandingMinor == 0L
    val fraction: Float get() = if (loan.principalMinor <= 0) 1f else (repaidMinor.toFloat() / loan.principalMinor).coerceIn(0f, 1f)
    fun overdue(today: LocalDate) = !settled && loan.dueEpochDay?.let { it < today.toEpochDay() } == true
}

/** Money between you and one person. Positive [netMinor] means they owe you. */
data class PersonBalance(val person: PersonEntity, val loans: List<LoanStatus>) {
    val owedToYouMinor: Long get() = loans.filter { it.loan.direction == LoanDirection.LENT }.sumOf { it.outstandingMinor }
    val youOweMinor: Long get() = loans.filter { it.loan.direction == LoanDirection.OWED }.sumOf { it.outstandingMinor }
    val netMinor: Long get() = owedToYouMinor - youOweMinor
}

object People {
    /** Repayments are the REPAYMENT transactions pointing at a loan; deleted ones are already filtered out of [txs]. */
    fun balances(people: List<PersonEntity>, loans: List<LoanEntity>, txs: List<TransactionEntity>): List<PersonBalance> {
        val repaid = txs.filter { it.role == TxRole.REPAYMENT && it.loanId != null && it.deletedAt == null }
            .groupBy({ it.loanId!! }, { it.amountMinor }).mapValues { it.value.sum() }
        val byPerson = loans.groupBy { it.personId }
        return people.map { p -> PersonBalance(p, byPerson[p.id].orEmpty().map { LoanStatus(it, repaid[it.id] ?: 0L) }) }
    }

    /** Totals across everyone: (owed to you, you owe). */
    fun totals(b: List<PersonBalance>): Pair<Long, Long> = b.sumOf { it.owedToYouMinor } to b.sumOf { it.youOweMinor }

    /** A repayment can never be larger than what is left on the loan. */
    fun cap(requestedMinor: Long, status: LoanStatus) = requestedMinor.coerceIn(0, status.outstandingMinor)
}
