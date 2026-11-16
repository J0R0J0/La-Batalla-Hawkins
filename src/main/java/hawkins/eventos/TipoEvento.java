package hawkins.eventos;

/**
 * Tipos de eventos globales que pueden activarse en la simulación.
 * Clase especial en la cual definimos los tipos de eventos especiales
 * que ocurren durante la ejecución. Este tipo de clases define nu conjutno fijo
 * y predeterminado de constantes.
 * Esto nos sirve para no tenener varias variable sueltas por distintas clases,
 * sino que centralizamos la definición de los eventos en un solo lugar, lo
 * que facilita su mantenimiento y uso.
 */
public enum TipoEvento {
    /** Apagón que bloquea el acceso al laboratorio. */
    APAGON_LABORATORIO,

    /** Tormenta que afecta al Upside Down. */
    TORMENTA_UPSIDE_DOWN,

    /** Intervención de Eleven para liberar niños de la Colmena. */
    INTERVENCION_ELEVEN,

    /** Activación de la Red Mental para localizar la zona con más niños. */
    RED_MENTAL
}