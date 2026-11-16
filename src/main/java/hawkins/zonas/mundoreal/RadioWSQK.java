package hawkins.zonas.mundoreal;

/**
 * Representa la radio WSQK del mundo real en Hawkins.
 * 
 * Es una especialización concreta de {@link ZonaHawkins} sin lógica adicional,
 * utilizada para modelar la ubicación de la emisora dentro de la simulación.
 */
public class RadioWSQK extends ZonaHawkins {
    /**
     * Crea una nueva radio WSQK con el nombre indicado.
     * 
     * @param nombre Nombre de la zona
     */
    public RadioWSQK(String nombre) {
        super(nombre);
    }

}
