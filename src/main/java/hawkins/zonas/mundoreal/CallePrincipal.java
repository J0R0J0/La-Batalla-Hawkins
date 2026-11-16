package hawkins.zonas.mundoreal;

/**
 * Representa la calle principal del mundo real en Hawkins.
 * 
 * Esta clase es una especialización concreta de {@link ZonaHawkins} sin
 * comportamiento adicional, utilizada para identificar la zona principal
 * del mundo real dentro de la simulación.
 */
public class CallePrincipal extends ZonaHawkins {
    /**
     * Crea una nueva calle principal con el nombre indicado.
     * 
     * @param nombre Nombre de la zona
     */
    public CallePrincipal(String nombre) {
        super(nombre);
    }
}
