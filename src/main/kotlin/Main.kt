import kotlin.math.min

fun main() {
    val game = Game.PartialInfo(2, 0, true, Card.Known.TEN, Card.Known.TEN)
    val searchResults = MonteCarloTreeSearch.search(game, playouts=1_000_000)
    for(i in 0..<min(5, searchResults.size)) {
        println("${(searchResults[i].second * 100).toInt()}%: ${searchResults[i].first}")
    }
}