package hawkins.hilos;

import hawkins.eventos.GestorEventos;
import hawkins.log.Log;
import hawkins.zonas.comunes.Portal;
import hawkins.zonas.mundoreal.ZonaHawkins;

/**
 * Hilo responsable de la creación y puesta en marcha de los hilos de tipo Niño
 * en la simulación.
 * * Se encarga de instanciar una secuencia determinada de niños, iniciarlos
 * como hilos
 * independientes y gestionar los intervalos de tiempo entre cada creación para
 * simular
 * una entrada progresiva en el sistema.
 */

public class GeneradorNinos extends Thread {

    /**
     * Zonas del mundo real (Hawkins) donde los niños pueden ser instanciados
     * inicialmente.
     */
    private ZonaHawkins[] zonasHawkins;

    /**
     * Conjunto de portales que permiten el tránsito entre Hawkins y el Upside Down.
     */
    private Portal[] portales;

    /**
     * Sistema de registro para monitorizar la actividad y eventos del generador.
     */
    private Log logger;

    /** Coordinador global para la activación y gestión de eventos especiales. */
    private GestorEventos gestorEventos;

    /**
     * Mecanismo de control para sincronizar el estado de pausa de la simulación.
     */
    private ControlSimulacion controlSimulacion;

    /**
     * Construye una instancia del generador con todas las dependencias necesarias
     * para los niños.
     *
     * @param zonasHawkins      Zonas disponibles en el entorno de Hawkins.
     * @param portales          Puntos de acceso entre dimensiones.
     * @param gestorEventos     Instancia del gestor de eventos globales.
     * @param logger            Instancia del logger para trazas de ejecución.
     * @param controlSimulacion Objeto de control para la gestión de pausas.
     */
    public GeneradorNinos(ZonaHawkins[] zonasHawkins, Portal[] portales, GestorEventos gestorEventos, Log logger,
            ControlSimulacion controlSimulacion) {
        this.zonasHawkins = zonasHawkins;
        this.portales = portales;
        this.logger = logger;
        this.gestorEventos = gestorEventos;
        this.controlSimulacion = controlSimulacion;
    }

    /**
     * Inicia el ciclo de vida del generador, creando un total de 1500 niños.
     * * En cada iteración, verifica el estado de pausa, instancia un nuevo
     * {@link Nino},
     * inicia su hilo y espera un tiempo aleatorio de entre 500ms y 2000ms antes de
     * continuar con la siguiente creación.
     */
    @Override
    public void run() {
        try {
            for (int i = 1; i < 1501; i++) {
                // Comprobamos si tiene que pausarse debido a la pausa de la simulación
                controlSimulacion.checkPausa();

                // Le pasamos los parametros necesarios al constructor del niño
                Nino nino = new Nino(i, zonasHawkins, portales, gestorEventos, logger, controlSimulacion);

                // Iniciamos el hilo del niño
                nino.start();

                // Esperamos un tiempo aleatorio entre 0.5 y 2 segundos antes de generar el
                // siguiente niño
                Thread.sleep((long) (Math.random() * 1500) + 500);
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}