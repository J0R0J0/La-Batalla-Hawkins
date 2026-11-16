package hawkins.zonas.upsidedown;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import hawkins.hilos.Demogorgon;
import hawkins.hilos.Nino;

/**
 * Clase base abstracta que representa una zona dentro del Upside Down.
 * 
 * Gestiona concurrentemente las colecciones de niños y demogorgons presentes,
 * proporcionando mecanismos de sincronización para el tránsito seguro
 * y garantizando la exclusión mutua durante la selección de ataques.
 */
public abstract class ZonaUpsideDown {

    /** Nombre identificativo de la zona. */
    protected String nombre;

    /** Lista de niños que se encuentran explorando la zona. */
    protected List<Nino> ninos;

    /** Lista de Demogorgons que patrullan actualmente la zona. */
    protected List<Demogorgon> demogorgons;

    /**
     * Cerrojo utilizado para garantizar el acceso seguro y concurrente a las listas
     * de entidades.
     */
    protected final ReentrantLock lock;

    /**
     * Construye una nueva zona del Upside Down e inicializa sus estructuras.
     * 
     * @param nombre Nombre descriptivo asignado a la zona.
     */
    public ZonaUpsideDown(String nombre) {
        this.nombre = nombre;
        this.ninos = new ArrayList<>();
        this.demogorgons = new ArrayList<>();
        this.lock = new ReentrantLock();
    }

    // Niños

    /**
     * Registra de forma concurrente la entrada de un niño a la zona.
     * 
     * @param nino Instancia del niño que ingresa.
     */
    public void entrarNino(Nino nino) {
        lock.lock();
        try {
            ninos.add(nino);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Registra de forma concurrente la salida de un niño de la zona.
     * 
     * @param nino Instancia del niño que abandona la zona.
     */
    public void salirNino(Nino nino) {
        lock.lock();
        try {
            ninos.remove(nino);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Obtiene una copia segura de la lista de niños actualmente en la zona.
     * 
     * @return Nueva lista conteniendo las referencias de los niños presentes.
     */
    public List<Nino> getNinos() {
        lock.lock();
        try {
            return new ArrayList<>(ninos); // Devolvemos copia para evitar problemas
        } finally {
            lock.unlock();
        }
    }

    /**
     * Consulta el número total de niños presentes en la zona.
     * 
     * @return Cantidad de niños alojados en la zona.
     */
    public int getNumNinos() {
        lock.lock();
        try {
            return ninos.size();
        } finally {
            lock.unlock();
        }
    }

    // Demogorgons

    /**
     * Registra de forma concurrente la entrada de un Demogorgon a la zona.
     * 
     * @param demogorgon Instancia del Demogorgon que ingresa.
     */
    public void entrarDemogorgon(Demogorgon demogorgon) {
        lock.lock();
        try {
            demogorgons.add(demogorgon);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Registra de forma concurrente la salida de un Demogorgon de la zona.
     * 
     * @param demogorgon Instancia del Demogorgon que abandona la zona.
     */
    public void salirDemogorgon(Demogorgon demogorgon) {
        lock.lock();
        try {
            demogorgons.remove(demogorgon);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Obtiene una copia segura de la lista de Demogorgons actualmente en la zona.
     * 
     * @return Nueva lista conteniendo las referencias de los Demogorgons presentes.
     */
    public List<Demogorgon> getDemogorgons() {
        lock.lock();
        try {
            return new ArrayList<>(demogorgons);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Consulta el número total de Demogorgons presentes en la zona.
     * 
     * @return Cantidad de Demogorgons alojados en la zona.
     */
    public int getNumDemogorgons() {
        lock.lock();
        try {
            return demogorgons.size();
        } finally {
            lock.unlock();
        }
    }

    // En ZonaUpsideDown.java
    // Método para seleccionar un niño aleatorioamente de una zona,
    // y reservarlo de forma que otro Demogorgon no pueda seleccionarlo al
    // mismo tiempo. Con esto solucionamos las condiciones de carrera y
    // generamos EXCLUSIÓN MUTUA
    /**
     * Selecciona un niño al azar asegurando la exclusión mutua para evitar
     * condiciones de carrera.
     * 
     * @return Instancia del niño seleccionado, o {@code null} si la zona está vacía
     *         de niños.
     */
    public synchronized Nino elegirYReservarNino() {
        if (ninos.isEmpty())
            return null;
        return ninos.get((int) (Math.random() * ninos.size()));
    }

    // Esto lo utilizamos para que el Demogorgon se "reserve" el niño que
    // va a atacar, evitando que otro Demogorgon lo seleccione al mismo
    // tiempo. Si el ataque falla, el niño se devuelve a la zona.
    /**
     * Extrae un niño temporalmente de la zona para procesar un ataque exclusivo.
     * Garantiza que el mismo niño no sea seleccionado por múltiples Demogorgons a
     * la vez.
     * 
     * @return Instancia del niño extraído para el ataque, o {@code null} si no hay
     *         niños.
     */
    public synchronized Nino tomarNinoParaAtaque() {
        if (ninos.isEmpty())
            return null;
        // Lo quitamos temporalmente de la zona
        return ninos.remove((int) (Math.random() * ninos.size()));
    }

    /**
     * Reincorpora a un niño en la zona una vez ha finalizado el ataque y este ha
     * sobrevivido.
     * 
     * @param nino Instancia del niño a devolver.
     */
    public synchronized void devolverNino(Nino nino) {
        ninos.add(nino);
    }

    // Getters generales

    /**
     * Obtiene el nombre asignado a la zona.
     * 
     * @return Cadena de texto con el nombre de la zona.
     */
    public String getNombre() {
        return nombre;
    }
}