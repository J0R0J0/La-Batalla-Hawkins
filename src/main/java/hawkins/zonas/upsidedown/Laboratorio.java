package hawkins.zonas.upsidedown;

/**
 * Representa el laboratorio del Upside Down.
 * 
 * Se trata de una especialización de {@link ZonaUpsideDown} sin lógica extra,
 * usada para distinguir esta ubicación en la simulación.
 */
public class Laboratorio extends ZonaUpsideDown {
    /**
     * Crea un laboratorio del Upside Down con el nombre indicado.
     * 
     * @param nombre Nombre de la zona
     */
    public Laboratorio(String nombre) {
        super(nombre);
    }

}
