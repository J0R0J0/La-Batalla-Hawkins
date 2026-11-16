package hawkins.zonas.upsidedown;

import hawkins.eventos.GestorEventos;
import hawkins.hilos.ControlSimulacion;
import hawkins.hilos.Demogorgon;
import hawkins.hilos.Nino;
import hawkins.log.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Representa la Colmena del Upside Down, donde se retiene a los niños capturados.
 * 
 * Esta clase centraliza el almacenamiento de niños capturados, la liberación
 * provocada por la intervención de Eleven y la generación de nuevos Demogorgons
 * cuando se alcanza un umbral de capturas. Toda la gestión se realiza con
 * sincronización para preservar la consistencia en un entorno multihilo.
 */
public class Colmena {

    /** Lista de niños actualmente capturados. */
    private List<Nino> ninosCapturados;

    /** Lista de Demogorgons activos en la simulación. */
    private List<Demogorgon> demogorgonsActivos;

    /** Zonas del Upside Down asociadas a la Colmena. */
    private ZonaUpsideDown[] zonas;

    /** Gestor de eventos globales utilizado para coordinar acciones especiales. */
    private GestorEventos gestorEventos;

    /** Logger para registrar capturas, liberaciones y generación de Demogorgons. */
    private Log logger;

    /** Número total de capturas realizadas. */
    private int totalCapturas;

    /** Contador de capturas necesarias para generar un nuevo Demogorgon. */
    private int contadorParaNuevoDemogorgon;

    /** Identificador incremental para los nuevos Demogorgons. */
    private int siguienteIdDemogorgon;

    /** Lock para proteger el acceso concurrente al estado de la Colmena. */
    private ReentrantLock lock;

    private ControlSimulacion controlSimulacion;

    /**
     * Crea una Colmena con las zonas y los Demogorgons activos indicados.
     * 
     * @param zonas Zonas del Upside Down asociadas a la Colmena
     * @param demogorgonsActivos Lista compartida de Demogorgons activos
     * @param logger Logger para registrar la actividad de la Colmena
     * @param controlSimulacion Controlador de la simulación
     */
    public Colmena(ZonaUpsideDown[] zonas, List<Demogorgon> demogorgonsActivos, Log logger, ControlSimulacion controlSimulacion) {
        this.ninosCapturados = new ArrayList<>();
        this.demogorgonsActivos = demogorgonsActivos;
        this.zonas = zonas;
        this.gestorEventos = null;
        this.logger = logger;
        this.totalCapturas = 0;
        this.contadorParaNuevoDemogorgon = 0;
        this.siguienteIdDemogorgon = 1; // D0000 ya existe, empezamos en D0001
        this.lock = new ReentrantLock();
        this.controlSimulacion = controlSimulacion;
    }

    /**
     * El demogorgon deposita al niño capturado en la Colmena.
     * Tarda entre 0.5 y 1 segundo en depositarlo.
     * Por cada 8 capturas se genera un nuevo Demogorgon.
     * 
     * @param nino Niño capturado que será depositado en la Colmena
     */
    public void depositarNino(Nino nino) {
        lock.lock();
        try {
            ninosCapturados.add(nino);
            totalCapturas++;
            contadorParaNuevoDemogorgon++;

            logger.escribir("El niño " + nino.getNombre() + " ha sido depositado en la COLMENA" +
                    " (total capturas: " + totalCapturas + ")");

            // Por cada 8 capturas, Vecna genera un nuevo Demogorgon
            if (contadorParaNuevoDemogorgon >= 8) {
                contadorParaNuevoDemogorgon = 0;
                generarNuevoDemogorgon();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Genera un nuevo Demogorgon y lo arranca como hilo.
     */
    private void generarNuevoDemogorgon() {
        if (gestorEventos == null) {
            logger.escribir("No se puede generar un Demogorgon: GestorEventos no inicializado en Colmena");
            return;
        }

        int id = siguienteIdDemogorgon++;
        Demogorgon nuevoDemogorgon = new Demogorgon(id, zonas, this, gestorEventos, logger, controlSimulacion);
        demogorgonsActivos.add(nuevoDemogorgon);
        nuevoDemogorgon.start();
        logger.escribir("VECNA genera un nuevo Demogorgon: " + nuevoDemogorgon.getNombre());
    }

    /**
     * Eleven libera niños de la Colmena (evento global).
     * Se liberan tantos niños como unidades de sangre recolectadas.
     * 
     * @param cantidad Número máximo de niños a liberar
     * @return Lista de niños liberados
     */
    public List<Nino> liberarNinos(int cantidad) {
        lock.lock();
        try {
            List<Nino> liberados = new ArrayList<>();
            int aLiberar = Math.min(cantidad, ninosCapturados.size());
            for (int i = 0; i < aLiberar; i++) {
                Nino nino = ninosCapturados.remove(0);
                nino.serLiberado(); 
                liberados.add(nino);
                logger.escribir("ELEVEN libera al niño " + liberados.get(liberados.size() - 1).getNombre() + " de la COLMENA");
            }
            logger.escribir("ELEVEN ha liberado " + liberados.size() + " niños de la COLMENA");
            return liberados;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Obtiene el número total de capturas realizadas.
     * 
     * @return Total de capturas registradas
     */
    public int getTotalCapturas() {
        lock.lock();
        try {
            return totalCapturas;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Obtiene el número de niños actualmente capturados.
     * 
     * @return Cantidad de niños capturados
     */
    public int getNumNinosCapturados() {
        lock.lock();
        try {
            return ninosCapturados.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Obtiene una copia de la lista de niños capturados.
     * 
     * @return Lista nueva con los niños capturados
     */
    public List<Nino> getNinosCapturados() {
        lock.lock();
        try {
            return new ArrayList<>(ninosCapturados);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Obtiene una copia de la lista de Demogorgons activos.
     * 
     * @return Lista nueva con los Demogorgons activos
     */
    public List<Demogorgon> getDemogorgonsActivos() {
        lock.lock();
        try {
            return new ArrayList<>(demogorgonsActivos);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Asigna el GestorEventos utilizado por la Colmena.
     * 
     * @param gestorEventos Gestor de eventos globales
     */
    public void setGestorEventos(GestorEventos gestorEventos) {
        lock.lock();
        try {
            this.gestorEventos = gestorEventos;
        } finally {
            lock.unlock();
        }
    }
}
