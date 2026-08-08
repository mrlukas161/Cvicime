package sk.havrila.cvicime.deti

/**
 * Zvieratá pre detskú vrstvu.
 *
 * Pravidlá z rešerše o motivácii (Lepper 1973, Deci/Koestner/Ryan 1999):
 * - identita zvieraťa sa NIKDY neukazuje vopred (žiadna silueta cieľa pri pásiku),
 * - text pri odomknutí je OPISNÝ (pomenúva, čo dieťa robilo), nie "Výborne! Vyhral si!",
 * - odomknuté zviera sa nikdy neodoberá.
 */
data class Zviera(
    val kluc: String,
    val meno: String,
    val emoji: String,
    /** Opisná veta pri odomknutí - pomenúva zručnosť, nie výhru. */
    val veta: String,
)

object Zvierata {

    val vsetky: List<Zviera> = listOf(
        Zviera("medveď", "Medveď", "🐻", "Prešiel si izbu po štyroch ako medveď."),
        Zviera("krab", "Krab", "🦀", "Chodil si bokom s bruškom hore ako krab."),
        Zviera("had", "Had", "🐍", "Plazil si sa po zemi ako had."),
        Zviera("kačica", "Kačica", "🦆", "Kráčal si v drepe ako kačica."),
        Zviera("húsenica", "Húsenica", "🐛", "Naťahoval si sa a skrčoval ako húsenica."),
        Zviera("krokodíl", "Krokodíl", "🐊", "Plazil si sa nízko nad zemou ako krokodíl."),
        Zviera("tučniak", "Tučniak", "🐧", "Kolísal si sa na pätách ako tučniak."),
        Zviera("slon", "Slon", "🐘", "Dupal si ťažkými krokmi ako slon."),
        Zviera("pavúk", "Pavúk", "🕷️", "Chodil si po rukách a nohách ako pavúk."),
        Zviera("rak", "Rak", "🦞", "Cúval si dozadu ako rak."),
        Zviera("žirafa", "Žirafa", "🦒", "Chodil si vysoko na špičkách ako žirafa."),
        Zviera("žaba", "Žaba", "🐸", "Skákal si z drepu ako žaba."),
        Zviera("zajac", "Zajac", "🐰", "Poskakoval si rýchlo ako zajac."),
        Zviera("klokan", "Klokan", "🦘", "Skákal si znožmo dopredu ako klokan."),
        Zviera("kobylka", "Kobylka", "🦗", "Skákal si na jednej nohe ako kobylka."),
        Zviera("delfín", "Delfín", "🐬", "Vyskočil si vysoko ako delfín z vody."),
        Zviera("vrabec", "Vrabec", "🐦", "Preskakoval si čiaru ako vrabec."),
        Zviera("bocian", "Bocian", "🦩", "Stál si na jednej nohe ako bocian."),
        Zviera("plameniak", "Plameniak", "🦢", "Udržal si rovnováhu ako plameniak."),
        Zviera("korytnačka", "Korytnačka", "🐢", "Schoval si sa do klbka ako korytnačka."),
        Zviera("mačka", "Mačka", "🐱", "Vyhrbil si chrbát ako mačka."),
        Zviera("pes", "Pes", "🐶", "Držal si sa hlavou dole ako psík."),
        Zviera("motýľ", "Motýľ", "🦋", "Sedel si s krídlami z kolien ako motýľ."),
        Zviera("kobra", "Kobra", "🐍", "Zdvihol si hlavu vo vzpore ako kobra."),
        Zviera("opica", "Opica", "🐵", "Visel si na hrazde ako opica."),
        Zviera("lenochod", "Leňochod", "🦥", "Visel si s pokrčenými rukami ako leňochod."),
    )

    private val podlaKluca = vsetky.associateBy { it.kluc }

    fun podla(kluc: String): Zviera? = podlaKluca[kluc]
}
