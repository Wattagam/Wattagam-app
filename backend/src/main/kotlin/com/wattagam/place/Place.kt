package com.wattagam.place

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal

@Entity
@Table(
    name = "places",
    uniqueConstraints = [UniqueConstraint(name = "uk_places_course_order", columnNames = ["course_order"])],
)
class Place(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "course_order", nullable = false)
    val courseOrder: Int,
    @Column(nullable = false, length = 50)
    val name: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,
    @Column(nullable = false, length = 100)
    val highlight: String,
    @Column(nullable = false, length = 255)
    val directions: String,
    @Column(name = "visit_point_name", nullable = false, length = 100)
    val visitPointName: String,
    @Column(nullable = false, precision = 10, scale = 7)
    val latitude: BigDecimal,
    @Column(nullable = false, precision = 10, scale = 7)
    val longitude: BigDecimal,
    @Column(name = "visit_radius_m", nullable = false)
    val visitRadiusMeters: Int,
)
