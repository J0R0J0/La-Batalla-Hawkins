package hawkins.eventos;

import hawkins.hilos.Nino;
import hawkins.log.Log;
import hawkins.zonas.upsidedown.Colmena;
import hawkins.zonas.upsidedown.ZonaUpsideDown;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gestor central de eventos globales en el mundo paralelo de Hawkins.
 * 
 * Esta clase gestiona los eventos globales que afectan al Upside Down y sus
 * habitantes.
 * Los eventos incluyen apagones del laboratorio, tormentas, intervención de
 * Eleven y
 * activación de la red mental. Utiliza mecanismos de sincronización para
 * garantizar
 * la consistencia y seguridad en un entorno concurrente multihilo.
 * 
 * @author PECL-PA
 * @version 1.0
 */
public class GestorEventos {

    /**
     * Identificador del evento activo actualmente ({@code null} si no hay ninguno
     * en curso).
     */
    private TipoEvento eventoActivo;

    /**
     * Marca de tiempo (en milisegundos) del instante en que inició el evento
     * activo.
     */
    private long tiempoInicioEvento;

    /** Duración total programada del evento activo en milisegundos. */
    private long duracionEvento;

    /**
     * Bandera que indica si el evento "Apagón del Laboratorio" está actualmente
     * activo.
     */
    private boolean apagonActivo;

    /**
     * Bandera que indica si el evento "Tormenta del Upside Down" está actualmente
     * activo.
     */
    private boolean tormentaActiva;

    /**
     * Bandera que indica si el evento "Intervención de Eleven" está actualmente
     * activo.
     */
    private boolean elevenActiva;

    /** Bandera que indica si el evento "Red Mental" está actualmente activo. */
    private boolean redMentalActiva;

    /** Cantidad total de unidades de sangre de Vecna recolectada por los niños. */
    private int sangreRecolectada;

    /**
     * Array que contiene las referencias a las distintas zonas del Upside Down
     * donde ocurren los eventos.
     */
    private ZonaUpsideDown[] zonas;

    /**
     * Referencia directa a la Colmena, utilizada para liberar a los niños cautivos
     * durante la intervención de Eleven.
     */
    private Colmena colmena;

    /**
     * Logger del sistema para registrar el inicio y fin de los eventos globales.
     */
    private Log logger;

    /**
     * Lock principal para la sincronización del acceso concurrente a los estados y
     * contadores del gestor.
     */
    private ReentrantLock lock;

    /**
     * Condición utilizada para bloquear y notificar a los hilos que deben esperar a
     * que finalice un evento.
     */
    private Condition finEvento;

    /**
     * Crea una instancia de GestorEventos inicializando sus estados y dependencias.
     * 
     * @param zonas   Array con las zonas instanciadas del Upside Down.
     * @param colmena Referencia a la Colmena para interactuar con los niños
     *                capturados.
     * @param logger  Logger para registrar la actividad de los eventos.
     */
    public GestorEventos(ZonaUpsideDown[] zonas, Colmena colmena, Log logger) {
        // Inicializamos el estado del gestor sin eventos activos y con contadores a
        // cero
        this.eventoActivo = null;

        // Las marcas de tiempo y duración se establecerán al activar un evento
        this.apagonActivo = false;
        this.tormentaActiva = false;
        this.elevenActiva = false;
        this.redMentalActiva = false;

        // Guardamos las referencias a las zonas, la colmena y el logger para su uso en
        // los métodos de gestión de eventos
        this.zonas = zonas;
        this.colmena = colmena;
        this.logger = logger;

        // Inicializamos el contador de sangre recolectada
        this.sangreRecolectada = 0;

        // Creamos el lock y la condición para la sincronización de eventos
        this.lock = new ReentrantLock();
        this.finEvento = lock.newCondition();
    }

    /**
     * Incrementa de forma segura la cantidad de sangre recolectada en el gestor.
     * 
     * @param cantidad Número de unidades de sangre a sumar al total.
     */
    public void depositarSangre(int cantidad) {
        lock.lock();
        try {
            sangreRecolectada += cantidad;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Consulta de forma segura la cantidad total de sangre recolectada hasta el
     * momento.
     * 
     * @return Total de unidades de sangre acumuladas.
     */
    public int getSangreRecolectada() {
        lock.lock();
        try {
            return sangreRecolectada;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inicia un evento global específico en el sistema.
     * Este método es invocado por el {@link GeneradorEventos} y configura las
     * variables
     * de estado y temporización según el tipo de evento solicitado.
     * 
     * @param tipo       Tipo de evento que se va a activar.
     * @param duracionMs Duración programada para el evento en milisegundos.
     */
    public void activarEvento(TipoEvento tipo, long duracionMs) {
        lock.lock();
        try {
            this.eventoActivo = tipo;
            this.tiempoInicioEvento = System.currentTimeMillis();
            this.duracionEvento = duracionMs;

            switch (tipo) {
                case APAGON_LABORATORIO:
                    apagonActivo = true;
                    logger.escribir("EVENTO GLOBAL: Apagón del Laboratorio iniciado");
                    break;
                case TORMENTA_UPSIDE_DOWN:
                    tormentaActiva = true;
                    logger.escribir("EVENTO GLOBAL: Tormenta del Upside Down iniciada");
                    break;
                case INTERVENCION_ELEVEN:
                    elevenActiva = true;
                    logger.escribir("EVENTO GLOBAL: Intervención de Eleven iniciada");
                    // Eleven libera niños según la sangre recolectada
                    ejecutarIntervencionEleven();
                    break;
                case RED_MENTAL:
                    redMentalActiva = true;
                    logger.escribir("EVENTO GLOBAL: Red Mental iniciada");
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Finaliza y desactiva el evento global que se encuentre actualmente en
     * progreso.
     * Restablece todas las banderas de estado a falso y despierta a todos los hilos
     * que hayan quedado bloqueados esperando el fin de dicho evento.
     */
    public void desactivarEvento() {
        lock.lock();
        try {
            if (eventoActivo != null) {
                logger.escribir("EVENTO GLOBAL: " + eventoActivo + " finalizado");
            }
            eventoActivo = null;
            apagonActivo = false;
            tormentaActiva = false;
            elevenActiva = false;
            redMentalActiva = false;
            finEvento.signalAll(); // despertar hilos bloqueados por el evento
        } finally {
            lock.unlock();
        }
    }

    /**
     * Lógica interna que ejecuta la intervención de Eleven, consumiendo la sangre
     * acumulada para ordenar a la Colmena la liberación de niños capturados.
     */
    private void ejecutarIntervencionEleven() {
        int sangreTotal = getSangreRecolectada();

        // Guardamos la lista de los que REALMENTE han sido liberados
        List<Nino> liberados = colmena.liberarNinos(sangreTotal);
        
        // Consumimos 1 unidad de sangre por cada niño salvado
        // (Como este método se llama desde activarEvento() que ya tiene el lock.lock() puesto, es totalmente seguro modificar la variable aquí)
        this.sangreRecolectada -= liberados.size(); 
        
        logger.escribir("EVENTO GLOBAL: Eleven ha consumido " + liberados.size() + " unidades de sangre. Quedan: " + this.sangreRecolectada);
    }

    /**
     * Detiene la ejecución del hilo que lo invoca si la Intervención de Eleven está
     * activa.
     * Utilizado para paralizar a las entidades (como los Demogorgons) mientras dure
     * el evento.
     * 
     * @throws InterruptedException Si el hilo es interrumpido durante el estado de
     *                              espera.
     */
    public void esperarSiParalizado() throws InterruptedException {
        lock.lock();
        try {
            while (elevenActiva) {
                finEvento.await();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inspecciona todas las zonas del Upside Down y determina cuál contiene
     * la mayor concentración de niños en el momento de la consulta.
     * Utilizado habitualmente por la Red Mental para dirigir a los Demogorgons.
     * 
     * @return La instancia de {@link ZonaUpsideDown} con mayor cantidad de niños.
     */
    public ZonaUpsideDown getZonaConMasNinos() {
        lock.lock();
        try {
            ZonaUpsideDown zonaMasNinos = zonas[0];
            for (ZonaUpsideDown zona : zonas) {
                if (zona.getNumNinos() > zonaMasNinos.getNumNinos()) {
                    zonaMasNinos = zona;
                }
            }
            return zonaMasNinos;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Calcula los segundos que faltan para que el evento actual llegue a su fin.
     * 
     * @return Tiempo restante en segundos, o {@code 0} si no hay ningún evento
     *         activo.
     */
    public long getTiempoRestante() {
        lock.lock();
        try {
            if (eventoActivo == null)
                return 0;
            long transcurrido = System.currentTimeMillis() - tiempoInicioEvento;
            long restante = duracionEvento - transcurrido;
            return restante > 0 ? restante / 1000 : 0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Consulta si el evento "Apagón del Laboratorio" está en curso.
     * 
     * @return {@code true} si el apagón está activo, {@code false} en caso
     *         contrario.
     */
    public boolean isApagonActivo() {
        lock.lock();
        try {
            return apagonActivo;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Consulta si el evento "Tormenta del Upside Down" está en curso.
     * 
     * @return {@code true} si la tormenta está activa, {@code false} en caso
     *         contrario.
     */
    public boolean isTormentaActiva() {
        lock.lock();
        try {
            return tormentaActiva;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Consulta si el evento "Intervención de Eleven" está en curso.
     * 
     * @return {@code true} si Eleven está interviniendo, {@code false} en caso
     *         contrario.
     */
    public boolean isElevenActiva() {
        lock.lock();
        try {
            return elevenActiva;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Consulta si el evento "Red Mental" está en curso.
     * 
     * @return {@code true} si la Red Mental está operando, {@code false} en caso
     *         contrario.
     */
    public boolean isRedMentalActiva() {
        lock.lock();
        try {
            return redMentalActiva;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Devuelve el identificador enumerado del evento que se está ejecutando.
     * 
     * @return Objeto {@link TipoEvento} que representa el evento actual, o
     *         {@code null} si no hay ninguno.
     */
    public TipoEvento getEventoActivo() {
        lock.lock();
        try {
            return eventoActivo;
        } finally {
            lock.unlock();
        }
    }
}