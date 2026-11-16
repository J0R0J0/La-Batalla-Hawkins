package hawkins.zonas.comunes;

import hawkins.eventos.GestorEventos;
import hawkins.hilos.Nino;
import hawkins.log.Log;
import hawkins.zonas.upsidedown.ZonaUpsideDown;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Representa el portal de conexión entre Hawkins y una zona del Upside Down.
 * 
 * La clase coordina el paso de niños en ambos sentidos, respetando reglas de
 * prioridad, formación de grupos para la ida y bloqueo temporal por apagón.
 * La sincronización interna garantiza que solo un niño cruce a la vez y que
 * los grupos se formen de manera ordenada.
 */
public class Portal {

    /** Nombre identificativo del portal. */
    private String nombre;

    /**
     * Número de niños necesarios para formar un grupo de ida hacia el Upside Down.
     */
    private int tamanoGrupo;

    /** Zona específica del Upside Down a la que conecta este portal. */
    private ZonaUpsideDown zona;

    /**
     * Lista de niños que están esperando para formar un grupo en el viaje de ida.
     */
    private List<Nino> esperandoGrupo;

    /**
     * Lista de niños que están esperando para realizar el viaje de regreso a
     * Hawkins.
     */
    private List<Nino> esperandoRegreso;

    /**
     * Referencia al niño que se encuentra cruzando físicamente el portal en este
     * instante.
     */
    private Nino ninoCruzando;

    /** Bandera que indica si hay un niño cruzando el portal en este momento. */
    private boolean cruzando;

    /**
     * Bandera que indica si el portal se encuentra bloqueado debido a un evento de
     * apagón.
     */
    private boolean apagado;

    /**
     * Bandera que indica si un grupo ya consolidado de ida está en proceso de
     * cruzar.
     */
    private boolean grupoCruzando;

    /**
     * Contador del número de niños pertenecientes al grupo de ida actual que ya han
     * cruzado.
     */
    private int ninosCruzados;

    /**
     * Logger utilizado para registrar los eventos y la actividad relacionada con
     * este portal.
     */
    private Log logger;

    /**
     * Lock principal para coordinar y sincronizar el acceso concurrente de los
     * hilos al portal.
     */
    private ReentrantLock lock;

    /**
     * Condición utilizada para hacer esperar a los hilos hasta que se complete un
     * grupo de ida.
     */
    private Condition condicionGrupo;

    /**
     * Condición utilizada para hacer esperar el turno individual de cruce hacia el
     * Upside Down.
     */
    private Condition condicionCruzar;

    /**
     * Condición utilizada para hacer esperar el turno individual de cruce de
     * regreso a Hawkins.
     */
    private Condition condicionRegreso;

    /**
     * Crea un portal con capacidad de grupo y zona de destino definidos.
     * 
     * @param nombre        Nombre del portal
     * @param tamanoGrupo   Número de niños necesarios para cruzar en grupo
     * @param zona          Zona del Upside Down conectada al portal
     * @param gestorEventos Gestor de eventos globales
     * @param logger        Logger para registrar la actividad del portal
     */
    public Portal(String nombre, int tamanoGrupo, ZonaUpsideDown zona, Log logger) {
        // Nombre del portal, tamño y zona a la que conecta
        this.nombre = nombre;
        this.tamanoGrupo = tamanoGrupo;
        this.zona = zona;
        this.logger = logger;

        // Inicialización de estructuras de control
        // Lista de niños esperando para formar grupo
        this.esperandoGrupo = new ArrayList<>();
        this.esperandoRegreso = new ArrayList<>();
        this.ninoCruzando = null;

        // Estado del portal
        this.cruzando = false;
        this.apagado = false;
        this.grupoCruzando = false;
        this.ninosCruzados = 0;

        // Lock y condiciones
        this.lock = new ReentrantLock();
        this.condicionGrupo = lock.newCondition();
        this.condicionCruzar = lock.newCondition();
        this.condicionRegreso = lock.newCondition();
    }

    /**
     * Hace que un niño intente cruzar el portal hacia el Upside Down.
     * 
     * El niño debe esperar a que se forme un grupo completo del tamaño requerido.
     * Además, los niños que regresan a Hawkins tienen prioridad sobre los que
     * quieren entrar.
     * 
     * @param nino Niño que intenta cruzar
     * @throws InterruptedException Si el hilo es interrumpido mientras espera
     */
    public void cruzarHaciaUpsideDown(Nino nino) throws InterruptedException {
        lock.lock();
        try {

            // 1. Si hay apagón, esperar ANTES de unirse al grupo
            while (apagado || grupoCruzando || esperandoGrupo.size() >= tamanoGrupo) {
                condicionGrupo.await();
            }

            // Unirse al grupo de espera
            esperandoGrupo.add(nino);
            logger.escribir("El niño " + nino.getNombre() + " se une al grupo del portal " +
                    nombre + " (" + esperandoGrupo.size() + "/" + tamanoGrupo + " niños)");

            // 4. Esperar hasta que el grupo esté completo
            while (!grupoCruzando && (esperandoGrupo.size() < tamanoGrupo || apagado)) {
                if (apagado) {
                    esperandoGrupo.remove(nino);

                    // Si el apagón se activa mientras espera, el niño se retira del grupo y vuelve
                    // a esperar
                    while (apagado || grupoCruzando || esperandoGrupo.size() >= tamanoGrupo) {
                        condicionGrupo.await();
                    }

                    // El niño se vuelve a unir al grupo después del apagón
                    esperandoGrupo.add(nino);
                    logger.escribir("El niño " + nino.getNombre() + " re-entra al grupo del portal " +
                            nombre + " (" + esperandoGrupo.size() + "/" + tamanoGrupo + " niños)");
                } else {
                    condicionGrupo.await();
                }
            }

            // Grupo completo, marcar que está cruzando y resetear contador
            if (!grupoCruzando) {
                grupoCruzando = true;
                ninosCruzados = 0;
            }

            // Notificar al resto del grupo
            condicionGrupo.signalAll();

            // Esperar turno para cruzar uno a uno
            // Miramos si la LISTA de regreso tiene a alguien (prioridad)
            while (cruzando || !esperandoRegreso.isEmpty() || apagado) {
                condicionCruzar.await();
            }

            // Le toca cruzar
            cruzando = true;
            ninoCruzando = nino;

            // El niño que cruza se quita de la lista de espera del grupo
            esperandoGrupo.remove(nino);

        } finally {
            lock.unlock();
        }

        // Cruzar el portal (1 segundo fuera del lock)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        lock.lock();
        try {
            cruzando = false;
            ninoCruzando = null;
            ninosCruzados++;

            // Si todos del grupo han cruzado, liberar el portal
            if (ninosCruzados >= tamanoGrupo) {
                grupoCruzando = false;
                ninosCruzados = 0;
                condicionGrupo.signalAll();
            }

            // Miramos la lista para dar paso
            if (!esperandoRegreso.isEmpty()) {
                condicionRegreso.signal();
            } else {
                condicionCruzar.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Hace que un niño regrese desde el Upside Down hacia Hawkins.
     * 
     * Este cruce tiene prioridad frente a los niños que intentan entrar al
     * Upside Down.
     * 
     * @param nino Niño que regresa a Hawkins
     * @throws InterruptedException Si el hilo es interrumpido mientras espera
     */
    public void cruzarHaciaHawkins(Nino nino) throws InterruptedException {
        lock.lock();
        try {
            // Metemos al niño en la lista (para que se vea en la caja derecha
            // de la Interfaz)
            esperandoRegreso.add(nino);

            // Esperar turno para cruzar
            while (cruzando || apagado) {
                condicionRegreso.await();
            }

            // Le toca cruzar
            cruzando = true;
            ninoCruzando = nino;
            esperandoRegreso.remove(nino);

        } finally {
            lock.unlock();
        }

        // Cruzar el portal (1 segundo fuera del lock)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        lock.lock();
        try {
            cruzando = false;
            ninoCruzando = null;

            // Prioridad: primero los que regresan, luego los de ida
            if (!esperandoRegreso.isEmpty()) {
                condicionRegreso.signal();
            } else {
                condicionCruzar.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Activa el apagón del portal.
     * 
     * Mientras el apagón está activo, ningún niño puede cruzar.
     */
    public void activarApagon() {
        lock.lock();
        try {
            apagado = true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Desactiva el apagón y despierta a los niños que estaban esperando.
     */
    public void desactivarApagon() {
        lock.lock();
        try {
            apagado = false;
            condicionGrupo.signalAll(); // desbloquea a los que esperan grupo
            condicionCruzar.signalAll(); // desbloquea a los que esperan cruzar

            condicionRegreso.signalAll(); // Desbloquea a los que esperan volver
        } finally {
            lock.unlock();
        }
    }

    /**
     * Obtiene el nombre del portal.
     * 
     * @return Nombre del portal
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Obtiene el tamaño del grupo necesario para cruzar hacia el Upside Down.
     * 
     * @return Tamaño del grupo
     */
    public int getTamanoGrupo() {
        return tamanoGrupo;
    }

    /**
     * Obtiene la zona del Upside Down conectada al portal.
     * 
     * @return Zona conectada al portal
     */
    public ZonaUpsideDown getZona() {
        return zona;
    }

    /**
     * Obtiene la cantidad de niños que están esperando para formar grupo.
     * 
     * @return Número de niños esperando en el grupo de ida
     */
    public int getNumEsperandoGrupo() {
        lock.lock();
        try {
            return esperandoGrupo.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Devuelve una copia de la lista de niños que están esperando para regresar a
     * Hawkins.
     * 
     * @return Lista con los niños en espera de retorno.
     */
    public List<Nino> getEsperandoRegreso() {
        lock.lock();
        try {
            return new ArrayList<>(esperandoRegreso);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Obtiene el nombre del niño que se encuentra cruzando actualmente el portal.
     * 
     * @return Nombre del niño cruzando, o una cadena vacía si no hay nadie.
     */
    public String getNombreNinoCruzando() {
        lock.lock();
        try {
            return (ninoCruzando != null) ? ninoCruzando.getNombre() : "";
        } finally {
            lock.unlock();
        }
    }

    /**
     * Devuelve una copia de la lista de niños esperando para formar grupo.
     * Usado por la interfaz gráfica para mostrar sus identificadores.
     *
     * @return Lista con los niños actualmente en espera
     */
    public List<Nino> getEsperandoGrupo() {
        lock.lock();
        try {
            return new ArrayList<>(esperandoGrupo);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Indica si el portal se encuentra bloqueado por apagón.
     * 
     * @return true si el portal está apagado, false en caso contrario
     */
    public boolean isApagado() {
        lock.lock();
        try {
            return apagado;
        } finally {
            lock.unlock();
        }
    }
}