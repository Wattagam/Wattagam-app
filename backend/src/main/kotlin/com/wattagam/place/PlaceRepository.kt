package com.wattagam.place

import org.springframework.data.jpa.repository.JpaRepository

interface PlaceRepository : JpaRepository<Place, Long> {

    fun findAllByOrderByCourseOrderAsc(): List<Place>
}
