package com.example.data

trait DragonTestData {

  val names = Seq(
    "Puff",
    "Fafnir",
    "Spot",
    "Tiamat",
    "Smaug",
    "Meatlug",
    "Barf & Belch",
    "Stormfly",
    "Hookfang",
    "Torch",
    "Jabberwock",
    "Kalessin",
    "Katla",
    "Falkor",
    "Errol",
    "Ninereeds"
  )

  val r = scala.util.Random

  val dragons: Seq[Dragon] = names.map(Dragon(None, _, r.nextInt(100)))

  val dragonsWithId: Seq[Dragon] = dragons.zipWithIndex map { case (d, i) => (d, Some(i + 1)) } map {
    case (d, i) => d.copy(id = i)
  }

}
