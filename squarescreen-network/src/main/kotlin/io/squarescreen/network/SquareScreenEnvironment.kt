package io.squarescreen.network

enum class SquareScreenEnvironment(internal val baseUrl: String) {
    LIVE("https://api.squarescreen.io/api/v1"),
    TEST("https://testapi.squarescreen.io/api/v1");

    companion object {
        // Change this line to switch environments before building.
        val current: SquareScreenEnvironment = TEST
    }
}
