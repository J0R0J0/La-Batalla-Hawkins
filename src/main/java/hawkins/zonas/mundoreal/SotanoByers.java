package hawkins.zonas.mundoreal;

/**
 * Representa el sótano de la familia Byers en Hawkins.
 * 
 * Esta clase solo especializa a {@link ZonaHawkins} para identificar de forma
 * explícita esta ubicación dentro del mundo real.
 */
public class SotanoByers extends ZonaHawkins {
    /**
     * Crea un nuevo sótano de los Byers con el nombre indicado.
     * 
     * @param nombre Nombre de la zona
     */
    public SotanoByers(String nombre) {
        super(nombre);
    }

}
