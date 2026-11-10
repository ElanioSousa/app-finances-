package com.example.app_finances.domain

/** Como um movimento afeta o saldo de uma conta. */
enum class MovementKind(val sign: Int) {
    INCOME(+1),
    EXPENSE(-1),
    TRANSFER_OUT(-1),
    TRANSFER_IN(+1),
    INVOICE_PAYMENT(-1),
}

/**
 * Soma dos movimentos pagos de uma conta por tipo, como vem do banco
 * (`TransactionDao.observePaidMovements`, que define *quais* linhas contam;
 * [MovementKind.sign] define o *sentido*).
 */
data class AccountMovement(
    val accountId: Long,
    val kind: MovementKind,
    val totalCents: Long,
)
