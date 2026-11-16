package hawkins.zonas.upsidedown;

/**
 * Representa el centro comercial del Upside Down.
 * 
 * Esta clase solo especializa a {@link ZonaUpsideDown} para modelar esta
 * ubicación concreta dentro del mundo paralelo.
 */
public class CentroComercial extends ZonaUpsideDown {
    /**
     * Crea un centro comercial del Upside Down con el nombre indicado.
     * 
     * @param nombre Nombre de la zona
     */
    public CentroComercial(String nombre) {
        super(nombre);
    }
}
