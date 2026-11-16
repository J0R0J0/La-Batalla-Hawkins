package hawkins.zonas.mundoreal;

import java.util.ArrayList;
import java.util.List;

import hawkins.hilos.Nino;

/**
 * Clase base abstracta para las zonas del mundo real en Hawkins.
 * 
 * Gestiona una colección de niños presentes en la zona y proporciona
 * operaciones seguras para añadirlos, eliminarlos y consultar su estado
 * mediante métodos sincronizados (palabra clave {@code synchronized}).
 */
public abstract class ZonaHawkins {

    /** Nombre identificativo de la zona. */
    protected String nombre;

    /** Lista de niños (hilos) actualmente presentes y explorando la zona. */
    protected List<Nino> ninos;

    /**
     * Crea una zona del mundo real con el nombre indicado e inicializa la colección
     * de niños.
     * 
     * @param nombre Nombre descriptivo de la zona.
     */
    public ZonaHawkins(String nombre) {
        this.nombre = nombre;
        this.ninos = new ArrayList<>();
    }

    /**
     * Registra la entrada de un niño en la zona de forma sincronizada.
     * 
     * @param nino Instancia del niño que entra en la zona.
     */
    public synchronized void entrarNino(Nino nino) {
        ninos.add(nino);
    }

    /**
     * Registra la salida de un niño de la zona de forma sincronizada.
     * 
     * @param nino Instancia del niño que sale de la zona.
     */
    public synchronized void salirNino(Nino nino) {
        ninos.remove(nino);
    }

    /**
     * Obtiene una copia segura de la lista de niños presentes en la zona.
     * La sincronización evita problemas de concurrencia al leer o iterar la lista.
     * 
     * @return Lista nueva que contiene las referencias a los niños actuales.
     */
    public synchronized List<Nino> getNinos() {
        return new ArrayList<>(ninos);
    }

    /**
     * Obtiene el número total de niños presentes en la zona de forma concurrente y
     * segura.
     * 
     * @return Cantidad de niños actualmente en la zona.
     */
    public synchronized int getNumNinos() {
        return ninos.size();
    }

    /**
     * Obtiene el nombre identificativo de la zona.
     * 
     * @return Nombre de la zona.
     */
    public String getNombre() {
        return nombre;
    }
}