package hawkins.hilos;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import hawkins.eventos.GestorEventos;
import hawkins.log.Log;
import hawkins.zonas.comunes.Portal;
import hawkins.zonas.mundoreal.ZonaHawkins;

/**
 * Hilo que representa a un niño de Hawkins dentro de la simulación.
 * 
 * El niño recorre zonas del mundo real, intenta cruzar al Upside Down,
 * explora su zona, puede ser atacado por un Demogorgon y, si sobrevive,
 * recolecta sangre de Vecna antes de regresar a Hawkins. Si es capturado,
 * espera en la Colmena hasta ser liberado por Eleven.
 */
public class Nino extends Thread {

    /** Identificador numérico del niño. */
    private int id;

    /** Nombre legible del niño. */
    private String nombre;

    /**
     * Zonas del mundo real por las que se desplaza el niño. (índice
     * 0=CallePrincipal, 1=SotanoByers, 2=RadioWSQK)
     */
    private ZonaHawkins[] zonasHawkins;

    /** Portal elegido para entrar al Upside Down. */
    private Portal portalSeleccionado;

    /** Portales disponibles para cruzar al Upside Down. */
    private Portal[] portales;

    /** Logger para registrar la actividad del niño. */
    private Log logger;

    /** Indica si el niño ha sido capturado. */
    private boolean capturado;

    /** Cantidad de sangre de Vecna recolectada por el niño. */
    private int sangreVecnaRecolectada;

    /** Indica si el niño está siendo atacado por un Demogorgon. */
    private boolean bajoAtaque;

    /** Lock para sincronizar el estado de ataque. */
    private ReentrantLock lockAtaque;

    /** Condición usada para esperar a que termine un ataque. */
    private Condition finAtaque;

    /** Gestor de eventos globales para consultar estados especiales. */
    private GestorEventos gestorEventos;

    /** Controlador para la pausa en las simulaciones. */
    private ControlSimulacion controlSimulacion;

    /**
     * Crea un niño con sus dependencias de mundo real, portal y eventos.
     * 
     * @param id                Identificador numérico del niño
     * @param zonasHawkins      Zonas del mundo real disponibles
     * @param portales          Portales disponibles para entrar al Upside Down
     * @param gestorEventos     Gestor de eventos globales
     * @param logger            Logger para registrar actividad
     * @param controlSimulacion Controlador para gestionar las pausas en la
     *                          simulación
     */
    public Nino(int id, ZonaHawkins[] zonasHawkins, Portal[] portales, GestorEventos gestorEventos,
            Log logger, ControlSimulacion controlSimulacion) {
        // Atributos de identificación (Id y Nombre)
        this.id = id;
        this.nombre = String.format("N%04d", this.id);

        // Referencia a zonas del mundo real
        this.zonasHawkins = zonasHawkins;

        // Referencia a los portales para entrar al Upside Down
        this.portalSeleccionado = null;
        this.portales = portales;

        // Logger para imprimir los eventos del niño
        this.logger = logger;

        // Estado del niño
        this.capturado = false;
        this.sangreVecnaRecolectada = 0;

        // Flag para verificar si el niño está siendo atacado por un Demogorgon
        this.bajoAtaque = false;

        // Lock y condición para controlar el estado de ataque
        this.lockAtaque = new ReentrantLock();
        this.finAtaque = lockAtaque.newCondition();

        // GestorEventos para el evento de Eleven
        this.gestorEventos = gestorEventos;

        // Controlador de pausa para las simulaciones
        this.controlSimulacion = controlSimulacion;
    }

    /*
     * Ciclo de Vida de los Niños:
     * 1. Nada más nacer los niños accederán al SOTANO DE LOS BYERS, donde
     * se preparán para salir a explorar el Upside Down. Permanecerán en el
     * Sotano durante 1 y 2 s.
     * 2. Tras salir del SOTANO, seleccionarán aleatoriamente un Portal
     * para salir al Upside Down. Formarán grupos de 2, 3 y 4 personas
     * dependiendo del portal seleccionado:
     * 2.1. Si el portal seleccionado es el BOSQUE, se agruparán 2 niños.
     * 2.2. Si el portal seleccionado es el LABORATORIO, se agruparán 3 niños.
     * 2.3. Si el portal seleccionado es el CENTRO COMERCIAL, se agruparán 4 niños.
     * 2.4. Si el portal seleccionado es el ALCANTARILLADO, se agruparán 2 niños.
     * 3. Una vez se han formado los grupos, se cruzará el PORTAl de uno en uno
     * (1 segundo por niño).
     * 3.1. Si llega un niño cuando el grupo ya está formado, se tendrá que
     * esperar a formar otro grupo.
     * 4. En el Upside Down, los niños estarán un tiempo aleatorio entre
     * 3 y 5 segundos, y recolectarán 1 unidad de sangre de Vecna.
     * 4.1. Cuando un niño recoge la sangre y no ha sido atacado, entonces
     * vuelve al portal sin esperar a sus compañeros.
     * 4.2. Si un niño está siendo atacado, se intentará resistir durante todo
     * el ataque (la duración la marca el Demogorgon), por lo que el niño no podrá
     * reanudar su marcha hasta que acabe el ataque del Demogorgon.
     * Las probabilidades de resistir el ataque son de 2/3.
     * Si la resistencia falla, el niño es CAPTURADO por el Demogorgon y se traslada
     * a la COLMENA.
     * 4.3. Si el niño resiste el ataque seguirá con su camino hacia el portal.
     * 5. El Demogorgon que captura al niño aumentará su contador de capturas, el
     * mismo
     * demogorgon se puede quedar en la misma zona atacando a otros niños o
     * desplazarse a otra zona.
     * 6. Una vez en Hawkins, los niños depositarán la sangra automáticamente y
     * acudirán a la RADIO DE HAWKINS y permanecerán un tiempo aleatorio entre
     * 2 y 4 segundos. Los niños que vuelven tiene prioridad frente a los que están
     * formando un grupo para entrar en el Upside Down.
     * 7. Tras esto, van a la CALLE PRINCIPAL y se dan un paseo que dura un tiempo
     * aleatorio entre 3 y 5 segundos.
     * 8. Tras esto vuelven a repetir su ciclo de vida
     */

    @Override
    public void run() {
        boolean activo = true;
        while (activo) {
            try {
                // Verificamos si la simulación se ha pausado al inicio de cada ciclo
                // para que el niño se quede esperando en el punto de control correspondiente
                controlSimulacion.checkPausa();

                // 1. Sótano Byers (1-2s)
                zonasHawkins[1].entrarNino(this);
                logger.escribir("El niño " + nombre + " entra al SÓTANO BYERS");
                Thread.sleep(1000 + (int) (Math.random() * 1000));

                // Verificamos si la simulación se ha pausado antes de intentar salir del Sótano
                // de
                // los Byers, para que el niño se quede esperando en el punto de control
                // correspondiente
                controlSimulacion.checkPausa();

                // Salimos del Sótano de los Byers
                zonasHawkins[1].salirNino(this);

                // Verificamos si la simulación se ha pausado antes de intentar seleccionar un
                // portal
                // para entrar al Upside Down, para que el niño se quede esperando en el punto
                // de
                // control correspondiente
                controlSimulacion.checkPausa();

                // 2. Seleccionar portal aleatorio
                portalSeleccionado = portales[(int) (Math.random() * portales.length)];
                logger.escribir("El niño " + nombre + " selecciona el portal " +
                        portalSeleccionado.getNombre());

                // Verificamos si la simulación se ha pausado antes de intentar cruzar al Upside
                // Down,
                // para que el niño se quede esperando en el punto de control correspondiente
                controlSimulacion.checkPausa();

                // 3. Cruzar portal hacia el Upside Down (espera grupo + cruza 1 a 1)
                portalSeleccionado.cruzarHaciaUpsideDown(this);
                logger.escribir("El niño " + nombre + " cruza hacia " +
                        portalSeleccionado.getNombre());

                // Verificamos si la simulación se ha pausado antes de intentar entrar al Upside
                // Down,
                // para que el niño se quede esperando en el punto de control correspondiente
                controlSimulacion.checkPausa();

                // 4. En el Upside Down (3-5s)
                portalSeleccionado.getZona().entrarNino(this);
                logger.escribir("El niño " + nombre + " explora " +
                        portalSeleccionado.getZona().getNombre());

                // Verificamos si la simulación se ha pausado antes de intentar entrar al Upside
                // Down,
                // para que el niño se quede esperando en el punto de control correspondiente
                controlSimulacion.checkPausa();

                if (gestorEventos.isTormentaActiva()) {
                    Thread.sleep(6000 + (int) (Math.random() * 4000));
                } else {
                    Thread.sleep(3000 + (int) (Math.random() * 2000));
                }

                // Verificamos si la simulación se ha pausado antes de intentar recoger sangre o
                // ser atacado por un Demogorgon,
                // para que el niño se quede esperando en el punto de control correspondiente
                controlSimulacion.checkPausa();

                // 4.2. Esperar si está siendo atacado
                esperarSiAtacado();

                if (!capturado) {
                    // 4.1. No fue capturado: recoger sangre y volver
                    sangreVecnaRecolectada++;

                    // Verificamos si la simulación se ha pausado antes de intentar salir del Upside
                    // Down,
                    // para que el niño se quede esperando en el punto de control correspondiente
                    controlSimulacion.checkPausa();

                    // Salir del Upside Down por el mismo portal por el que entró
                    portalSeleccionado.getZona().salirNino(this);
                    logger.escribir("El niño " + nombre + " recoge sangre y vuelve al portal");

                    // Verificamos si la simulación se ha pausado antes de intentar cruzar de vuelta
                    // a Hawkins,
                    // para que el niño se quede esperando en el punto de control correspondiente
                    controlSimulacion.checkPausa();

                    // 5. Cruzar portal de vuelta a Hawkins (con prioridad)
                    portalSeleccionado.cruzarHaciaHawkins(this);
                    logger.escribir("El niño " + nombre + " regresa a Hawkins");

                    // 6. Depositar sangre automáticamente
                    gestorEventos.depositarSangre(sangreVecnaRecolectada);
                    logger.escribir("El niño " + nombre + " deposita " +
                            sangreVecnaRecolectada + " unidades de sangre");
                    sangreVecnaRecolectada = 0;

                    // Verificamos si la simulación se ha pausado antes de intentar ir a la radio,
                    // para que
                    // el niño se quede esperando en el punto de control correspondiente
                    controlSimulacion.checkPausa();

                    // Ir a Radio WSQK (2-4s)
                    zonasHawkins[2].entrarNino(this);
                    logger.escribir("El niño " + nombre + " descansa en RADIO WSQK");
                    Thread.sleep(2000 + (int) (Math.random() * 2000));
                    controlSimulacion.checkPausa();
                    zonasHawkins[2].salirNino(this);

                    // Verificamos si la simulación se ha pausado antes de intentar ir a la calle,
                    // para que
                    // el niño se quede esperando en el punto de control correspondiente
                    controlSimulacion.checkPausa();

                    // 7. Calle Principal (3-5s)
                    zonasHawkins[0].entrarNino(this);
                    logger.escribir("El niño " + nombre + " deambula por CALLE PRINCIPAL");
                    Thread.sleep(3000 + (int) (Math.random() * 2000));
                    controlSimulacion.checkPausa();
                    zonasHawkins[0].salirNino(this);

                } else {
                    // 4.2. Fue capturado: esperar en la Colmena hasta ser liberado por Eleven
                    logger.escribir("El niño " + nombre + " está atrapado en la COLMENA");
                    lockAtaque.lock();
                    try {
                        while (capturado) {
                            finAtaque.await();
                        }

                        logger.escribir("El niño " + nombre + " ha sido liberado de la COLMENA");
                    } finally {
                        lockAtaque.unlock();
                    }

                    // Verificamos si la simulación se ha pausado antes de intentar salir de la
                    // Colmena,
                    // para que el niño se quede esperando en el punto de control correspondiente
                    controlSimulacion.checkPausa();

                    // Eleven le libera: vuelve a Calle Principal
                    zonasHawkins[0].entrarNino(this);
                    logger.escribir("El niño " + nombre + " fue liberado por ELEVEN");
                    Thread.sleep(3000 + (int) (Math.random() * 2000));
                    controlSimulacion.checkPausa();
                    zonasHawkins[0].salirNino(this);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                activo = false;
            }
        }
    }

    /**
     * Procesa un ataque de Demogorgon sobre el niño.
     * 
     * El niño permanece bajo ataque durante la duración indicada y luego se
     * determina aleatoriamente si es capturado o resiste.
     * 
     * @param duracion Duración del ataque en milisegundos
     * @return true si el niño es capturado, false si resiste el ataque
     */
    public boolean serAtacado(int duracion) {
        // Activamos el flag de bajo ataque para que el niño no pueda continuar su
        // camino
        lockAtaque.lock();
        try {
            bajoAtaque = true;
        } finally {
            lockAtaque.unlock();
        }

        // El demogorgon ataca al niño durante la duración especificada
        try {
            Thread.sleep(duracion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Probabilidad de captura: 1/3, probabilidad de resistir: 2/3
        boolean capturado = Math.random() < 0.33;

        // Desactivamos el flag de bajo ataque para que el niño pueda continuar su
        // camino
        // Desactivar flag y notificar al hilo del Niño
        lockAtaque.lock();
        try {
            // El niño ya no está bajo ataque, puede continuar su camino o ser capturado
            bajoAtaque = false;
            this.capturado = capturado;
            // Si el niño ha sido capturado, el niño no podrá continuar su camino,
            // por lo que no se notifica al hilo del niño para que reanude su marcha
            if (capturado) {
                logger.escribir("El niño " + nombre + " ha sido capturado");
            } else {
                logger.escribir("El niño " + nombre + " resistió el ataque");
                finAtaque.signal();
            }
        } finally {
            lockAtaque.unlock();
        }

        return capturado;
    }

    /**
     * Bloquea el hilo actual mientras el niño esté siendo atacado.
     * 
     * @throws InterruptedException Si el hilo es interrumpido mientras espera
     */
    public void esperarSiAtacado() throws InterruptedException {
        lockAtaque.lock();
        try {
            while (bajoAtaque) {
                finAtaque.await();
            }
        } finally {
            lockAtaque.unlock();
        }
    }

    /**
     * Obtiene el nombre del niño.
     * 
     * @return Nombre del niño
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Indica si el niño ha sido capturado.
     * 
     * @return true si está capturado, false en caso contrario
     */
    public boolean isCapturado() {
        return capturado;
    }

    /**
     * Marca si el niño está capturado o no.
     * 
     * @param capturado Nuevo estado de captura
     */
    public void setCapturado(boolean capturado) {
        this.capturado = capturado;
    }

    /**
     * Obtiene la cantidad de sangre de Vecna recolectada.
     * 
     * @return Unidades de sangre recolectadas
     */
    public int getSangreVecnaRecolectada() {
        return sangreVecnaRecolectada;
    }

    /**
     * Establece la cantidad de sangre de Vecna recolectada.
     * 
     * @param sangreVecnaRecolectada Nueva cantidad de sangre recolectada
     */
    public void setSangreVecnaRecolectada(int sangreVecnaRecolectada) {
        this.sangreVecnaRecolectada = sangreVecnaRecolectada;
    }

    /**
     * Método llamado cuando Eleven interviene para liberar al niño de la Colmena.
     * Cambia su estado y despierta al hilo para que regrese a Hawkins.
     */
    public void serLiberado() {
        lockAtaque.lock();
        try {
            this.capturado = false;
            finAtaque.signal(); 
        } finally {
            lockAtaque.unlock();
        }
    }
}