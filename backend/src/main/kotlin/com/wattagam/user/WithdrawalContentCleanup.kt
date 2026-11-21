package com.wattagam.user

fun interface WithdrawalContentCleanup {

    /**
     * 사용자 행을 잠근 호출 트랜잭션에서 방명록과 OCI 이미지를 정리한다.
     * 모든 정리가 끝나면 true, 재시도가 필요한 실패는 false를 반환한다.
     * 이미 삭제된 이미지는 정리된 것으로 처리한다.
     */
    fun deleteForUser(userId: Long): Boolean
}
