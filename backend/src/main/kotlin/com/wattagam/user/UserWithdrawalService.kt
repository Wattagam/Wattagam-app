package com.wattagam.user

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import org.springframework.beans.factory.ObjectProvider
import org.springframework.dao.TransientDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class UserWithdrawalService(
    private val userRepository: UserRepository,
    private val contentCleanup: ObjectProvider<WithdrawalContentCleanup>,
    transactionManager: PlatformTransactionManager,
) {
    private val transaction = TransactionTemplate(transactionManager)

    fun withdraw(userId: Long) {
        val cleanup = contentCleanup.getIfAvailable() ?: throw ApiException(ErrorCode.WITHDRAWAL_UNAVAILABLE)
        try {
            transaction.executeWithoutResult {
                val user = userRepository.findByIdForUpdate(userId) ?: throw ApiException(ErrorCode.UNAUTHORIZED)
                if (!cleanup.deleteForUser(userId)) {
                    throw ApiException(ErrorCode.WITHDRAWAL_UNAVAILABLE)
                }
                userRepository.delete(user)
            }
        } catch (_: TransientDataAccessException) {
            throw ApiException(ErrorCode.WITHDRAWAL_UNAVAILABLE)
        }
    }
}
