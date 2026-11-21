package com.wattagam.app.data.location

import org.junit.Assert.assertEquals
import org.junit.Test

class VisitRuleTest {
    private val placeLat = 37.5050
    private val placeLng = 126.9570
    private val radius = 30.0

    // 위도 1도 ≈ 111 km → 0.0001도 ≈ 11 m
    @Test fun withinRadiusWithGoodAccuracyIsAllowed() {
        val fix = LocationFix.Found(placeLat + 0.0001, placeLng, accuracyMeters = 10f)
        assertEquals(VisitEligibility.Allowed, VisitRule.evaluate(fix, placeLat, placeLng, radius))
    }

    @Test fun beyondRadiusIsOutside() {
        val fix = LocationFix.Found(placeLat + 0.0005, placeLng, accuracyMeters = 10f) // 약 55 m
        assertEquals(VisitEligibility.Outside, VisitRule.evaluate(fix, placeLat, placeLng, radius))
    }

    @Test fun radiusIsPerPlace() {
        val fix = LocationFix.Found(placeLat + 0.0005, placeLng, accuracyMeters = 10f) // 약 55 m
        assertEquals(VisitEligibility.Allowed, VisitRule.evaluate(fix, placeLat, placeLng, radiusMeters = 75.0)) // 310관 반경
        assertEquals(VisitEligibility.Outside, VisitRule.evaluate(fix, placeLat, placeLng, radiusMeters = 40.0)) // 도서관 반경
    }

    @Test fun inaccurateFixIsNotTreatedAsVisit() {
        val fix = LocationFix.Found(placeLat, placeLng, accuracyMeters = 120f)
        assertEquals(VisitEligibility.Unavailable, VisitRule.evaluate(fix, placeLat, placeLng, radius))
    }

    @Test fun missingPermissionAndUnavailableArePassedThrough() {
        assertEquals(VisitEligibility.PermissionMissing, VisitRule.evaluate(LocationFix.PermissionMissing, placeLat, placeLng, radius))
        assertEquals(VisitEligibility.Unavailable, VisitRule.evaluate(LocationFix.Unavailable, placeLat, placeLng, radius))
    }

    @Test fun distanceIsRoughlyCorrect() {
        val d = VisitRule.distanceMeters(placeLat, placeLng, placeLat + 0.001, placeLng)
        assertEquals(111.2, d, 1.0)
    }
}
