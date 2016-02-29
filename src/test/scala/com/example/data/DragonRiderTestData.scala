package com.example.data


trait DragonRiderTestData {

  case class Dragon(id: Option[Int] = None, name: String, firepower: Int, riderId: Option[Int] = None)

  val riders = Seq(Rider(Some(1), "Hiccup", 100), Rider(Some(2), "Astrid", 80), Rider(Some(3), "Fishlegs", 20),
    Rider(Some(4), "Snotlout", 40), Rider(Some(5), "Tuffnut", 60))

  val dragons = Seq(Dragon(Some(1), "Puff", 10), Dragon(Some(2), "Fafnir", 20), Dragon(Some(3), "Spot", 25),
    Dragon(Some(4), "Tiamat", 30), Dragon(Some(5), "Smaug", 100), Dragon(Some(6), "Meatlug", 20, Some(3)),
    Dragon(Some(7), "Barf & Belch", 40, Some(4)), Dragon(Some(8), "Stormfly", 50, Some(2)), Dragon(Some(9), "Hookfang", 55, Some(5)),
    Dragon(Some(10), "Torch", 10), Dragon(Some(11), "Jabberwock", 0), Dragon(Some(12), "Kalessin", 70),
    Dragon(Some(13), "Toothless", 80, Some(1)))
//  "Katla", "Falkor", "Errol", "Ninereeds",

}
