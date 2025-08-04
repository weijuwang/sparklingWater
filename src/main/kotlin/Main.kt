fun main() {
    val s = Game.PartialInfo(3, 1, true, Card.Known.EIGHT, Card.Known.JACK)
    val t = Game.Determinized(s)
    println(t)
}