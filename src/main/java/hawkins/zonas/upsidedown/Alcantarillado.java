package hawkins.zonas.upsidedown;

/**
 * Representa las alcantarillas del Upside Down.
 * 
 * Es una especialización concreta de {@link ZonaUpsideDown} sin comportamiento
 * adicional, utilizada para modelar este entorno dentro de la simulación.
 */
public class Alcantarillado extends ZonaUpsideDown {
    /**
     * Crea unas alcantarillas del Upside Down con el nombre indicado.
     * 
     * @param nombre Nombre de la zona
     */
    public Alcantarillado(String nombre) {
        super(nombre);
    }
}
