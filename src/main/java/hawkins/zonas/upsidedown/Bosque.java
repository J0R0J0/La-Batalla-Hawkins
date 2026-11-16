package hawkins.zonas.upsidedown;

/**
 * Representa el bosque del Upside Down.
 * 
 * Esta clase especializa a {@link ZonaUpsideDown} para identificar de forma
 * explícita la zona boscosa dentro de la simulación.
 */
public class Bosque extends ZonaUpsideDown {
    /**
     * Crea un bosque del Upside Down con el nombre indicado.
     * 
     * @param nombre Nombre de la zona
     */
    public Bosque(String nombre) {
        super(nombre);
    }
}
