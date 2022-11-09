package hr.dtakac.prognoza.entities

import hr.dtakac.prognoza.entities.forecast.units.Angle
import hr.dtakac.prognoza.entities.forecast.units.AngleUnit
import hr.dtakac.prognoza.entities.forecast.units.CompassDirection
import org.testng.annotations.Test
import kotlin.math.PI
import kotlin.test.assertEquals

class AngleTest {
    private val tolerance = 0.0001

    @Test
    fun `when angles are on compass rose, directions are correct`() {
        val degreesToExpectedDirections = mapOf(
            0.0 to CompassDirection.N,
            30.0 to CompassDirection.N,

            45.0 to CompassDirection.NE,
            75.0 to CompassDirection.NE,

            90.0 to CompassDirection.E,
            120.0 to CompassDirection.E,

            135.0 to CompassDirection.SE,
            165.0 to CompassDirection.SE,

            180.0 to CompassDirection.S,
            210.0 to CompassDirection.S,

            225.0 to CompassDirection.SW,
            255.0 to CompassDirection.SW,

            270.0 to CompassDirection.W,
            300.0 to CompassDirection.W,

            315.0 to CompassDirection.NW,
            345.0 to CompassDirection.NW
        )
        degreesToExpectedDirections.keys
            .map { it to Angle(it, AngleUnit.DEGREE).compassDirection }
            .forEach { (degrees, actualDirection) ->
                assertEquals(
                    expected = degreesToExpectedDirections[degrees]!!,
                    actual = actualDirection
                )
            }
    }

    @Test
    fun `when angle is -90 deg, direction is west`() = assertEquals(
        expected = CompassDirection.W,
        actual = Angle(-90.0, AngleUnit.DEGREE).compassDirection
    )

    @Test
    fun `when angle is -450 deg, direction is west`() = assertEquals(
        expected = CompassDirection.W,
        actual = Angle(-450.0, AngleUnit.DEGREE).compassDirection
    )

    @Test
    fun `when angle is 450 deg, direction is east`() = assertEquals(
        expected = CompassDirection.E,
        actual = Angle(450.0, AngleUnit.DEGREE).compassDirection
    )

    @Test
    fun `when angle is 180 deg, radians are pi`() = assertEquals(
        expected = PI,
        actual = Angle(180.0, AngleUnit.DEGREE).radian,
        absoluteTolerance = tolerance
    )

    @Test
    fun `when angle is pi rad, degrees are 180`() = assertEquals(
        expected = 180.0,
        actual = Angle(PI, AngleUnit.RADIAN).degree,
        absoluteTolerance = tolerance
    )
}