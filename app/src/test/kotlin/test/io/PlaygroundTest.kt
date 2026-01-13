package test.io

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

class PlaygroundTest {

    @ParameterizedTest(name = "{index} ==> the sum of {0} and {1} is {2}")
    @MethodSource("argumentsFactory")
    fun testAddition(fst: Int, snd: Int, expected: Int) {
        assert(fst + snd == expected)
    }

    companion object {
        @JvmStatic
        fun argumentsFactory(): List<Arguments> = listOf(
            arguments(1, 1, 2),
            arguments(2, 2, 4),
            arguments(3, 3, 6),
        )
    }
}

